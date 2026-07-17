package pizza_cheese.todo.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import pizza_cheese.todo.realtime.OrderRealtimeBoard;
import pizza_cheese.todo.realtime.OrderRealtimeHub;

@Tag(name = "Realtime", description = "SSE cập nhật đơn realtime")
@RestController
@RequestMapping("/api/v1")
public class OrderRealtimeController {

    private final OrderRealtimeHub hub;

    public OrderRealtimeController(OrderRealtimeHub hub) {
        this.hub = hub;
    }

    @Operation(summary = "SSE stream cho board bếp")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = "/kitchen/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN')")
    public SseEmitter kitchenEvents() {
        return hub.subscribe(OrderRealtimeBoard.KITCHEN);
    }

    @Operation(summary = "SSE stream cho board giao hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(path = "/delivery/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('DELIVERY', 'ADMIN')")
    public SseEmitter deliveryEvents() {
        return hub.subscribe(OrderRealtimeBoard.DELIVERY);
    }
}
