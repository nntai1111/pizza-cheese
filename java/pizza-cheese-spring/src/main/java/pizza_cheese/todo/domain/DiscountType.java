package pizza_cheese.todo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import pizza_cheese.todo.util.CodedEnums;

public enum DiscountType implements CodedEnum {

    PERCENT(1, "Giảm theo phần trăm"),
    FIXED(2, "Giảm cố định");

    private final int code;
    private final String label;

    DiscountType(int code, String label) {
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
    public static DiscountType fromJson(Object value) {
        return CodedEnums.parse(DiscountType.class, value);
    }

    public static DiscountType fromCode(int code) {
        return CodedEnums.fromCode(DiscountType.class, code);
    }
}
