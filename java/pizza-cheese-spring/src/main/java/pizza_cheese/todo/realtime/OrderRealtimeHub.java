package pizza_cheese.todo.realtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * In-memory fan-out of order SSE events to connected kitchen/delivery clients.
 * Single-instance only — multi-instance would need Redis pub/sub.
 */
@Component
public class OrderRealtimeHub {

    private static final Logger log = LoggerFactory.getLogger(OrderRealtimeHub.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final ObjectMapper objectMapper;
    private final Map<OrderRealtimeBoard, CopyOnWriteArrayList<SseEmitter>> emittersByBoard =
            new ConcurrentHashMap<>();

    public OrderRealtimeHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (OrderRealtimeBoard board : OrderRealtimeBoard.values()) {
            emittersByBoard.put(board, new CopyOnWriteArrayList<>());
        }
    }

    public SseEmitter subscribe(OrderRealtimeBoard board) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByBoard.get(board);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"board\":\"" + board.name() + "\"}", MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            emitters.remove(emitter);
            emitter.completeWithError(ex);
        }

        log.debug("SSE subscribe {} ({} clients)", board, emitters.size());
        return emitter;
    }

    public void broadcast(OrderRealtimeBoard board, OrderRealtimeEvent event) {
        List<SseEmitter> emitters = emittersByBoard.get(board);
        if (emitters.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (IOException ex) {
            log.warn("Failed to serialize SSE event", ex);
            return;
        }

        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("order-updated")
                        .data(json, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                drop(board, emitter);
            }
        }
    }

    /** Keep proxies/load balancers from closing idle SSE connections. */
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        for (OrderRealtimeBoard board : OrderRealtimeBoard.values()) {
            CopyOnWriteArrayList<SseEmitter> emitters = emittersByBoard.get(board);
            if (emitters.isEmpty()) {
                continue;
            }
            for (SseEmitter emitter : List.copyOf(emitters)) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception ex) {
                    drop(board, emitter);
                }
            }
        }
    }

    private void drop(OrderRealtimeBoard board, SseEmitter emitter) {
        emittersByBoard.get(board).remove(emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // already closed
        }
    }
}
