package pizza_cheese.todo.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
