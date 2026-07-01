package pizza_cheese.todo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import pizza_cheese.todo.util.CodedEnums;

public enum OrderStatus implements CodedEnum {

    PENDING_PAYMENT(1, "Chờ thanh toán"),
    PAID(2, "Đã thanh toán"),
    CONFIRMED(3, "Đã xác nhận"),
    PREPARING(4, "Đang chế biến"),
    READY(5, "Sẵn sàng"),
    OUT_FOR_DELIVERY(6, "Đang giao hàng"),
    DELIVERED(7, "Đã giao"),
    CANCELLED(8, "Đã hủy"),
    REFUNDED(9, "Đã hoàn tiền");

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static OrderStatus fromJson(Object value) {
        return CodedEnums.parse(OrderStatus.class, value);
    }

    public static OrderStatus fromCode(int code) {
        return CodedEnums.fromCode(OrderStatus.class, code);
    }
}
