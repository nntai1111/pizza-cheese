package pizza_cheese.todo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import pizza_cheese.todo.util.CodedEnums;

public enum Role implements CodedEnum {

    CUSTOMER(1, "Khách hàng"),
    CASHIER(2, "Thu ngân"),
    KITCHEN(3, "Bếp"),
    DELIVERY(4, "Giao hàng"),
    ADMIN(5, "Quản trị");

    private final int code;
    private final String label;

    Role(int code, String label) {
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
    public static Role fromJson(Object value) {
        return CodedEnums.parse(Role.class, value);
    }

    public static Role fromCode(int code) {
        return CodedEnums.fromCode(Role.class, code);
    }
}
