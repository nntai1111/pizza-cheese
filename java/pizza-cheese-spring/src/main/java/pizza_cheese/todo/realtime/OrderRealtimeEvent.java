package pizza_cheese.todo.realtime;

import java.util.UUID;

import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderStatus;

public record OrderRealtimeEvent(
        String type,
        UUID orderId,
        String orderCode,
        String status,
        Integer statusCode,
        UUID kitchenStaffId,
        UUID deliveryStaffId) {

    public static final String TYPE_ORDER_UPDATED = "ORDER_UPDATED";

    public static OrderRealtimeEvent updated(Order order) {
        OrderStatus status = order.getStatus();
        return new OrderRealtimeEvent(
                TYPE_ORDER_UPDATED,
                order.getId(),
                order.getOrderCode(),
                status != null ? status.name() : null,
                status != null ? status.getCode() : null,
                order.getKitchenStaffId(),
                order.getDeliveryStaffId());
    }
}
