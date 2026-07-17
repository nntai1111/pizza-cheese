package pizza_cheese.todo.realtime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import pizza_cheese.todo.domain.Order;

@Component
public class OrderRealtimePublisher {

    private final OrderRealtimeHub hub;

    public OrderRealtimePublisher(OrderRealtimeHub hub) {
        this.hub = hub;
    }

    public void publishKitchen(Order order) {
        publishAfterCommit(OrderRealtimeBoard.KITCHEN, OrderRealtimeEvent.updated(order));
    }

    public void publishDelivery(Order order) {
        publishAfterCommit(OrderRealtimeBoard.DELIVERY, OrderRealtimeEvent.updated(order));
    }

    /** Kitchen finished → delivery pool should refresh. */
    public void publishKitchenAndDelivery(Order order) {
        publishKitchen(order);
        publishDelivery(order);
    }

    private void publishAfterCommit(OrderRealtimeBoard board, OrderRealtimeEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    hub.broadcast(board, event);
                }
            });
            return;
        }
        hub.broadcast(board, event);
    }
}
