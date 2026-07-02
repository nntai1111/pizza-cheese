package pizza_cheese.todo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import pizza_cheese.todo.util.CodedEnums;

public enum PaymentStatus implements CodedEnum {

    PENDING(1, "Chờ thanh toán"),
    PAID(2, "Đã thanh toán"),
    FAILED(3, "Thất bại"),
    REFUNDED(4, "Đã hoàn tiền");

    private final int code;
    private final String label;

    PaymentStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static PaymentStatus fromJson(Object value) {
        return CodedEnums.parse(PaymentStatus.class, value);
    }

    public static PaymentStatus fromCode(int code) {
        return CodedEnums.fromCode(PaymentStatus.class, code);
    }
}
