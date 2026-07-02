package pizza_cheese.todo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import pizza_cheese.todo.util.CodedEnums;

public enum PaymentMethod implements CodedEnum {

    COD(1, "Thanh toán khi nhận hàng"),
    VNPAY(2, "VNPay"),
    MOMO(3, "MoMo"),
    STRIPE(4, "Stripe");

    private final int code;
    private final String label;

    PaymentMethod(int code, String label) {
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
    public static PaymentMethod fromJson(Object value) {
        return CodedEnums.parse(PaymentMethod.class, value);
    }

    public static PaymentMethod fromCode(int code) {
        return CodedEnums.fromCode(PaymentMethod.class, code);
    }
}
