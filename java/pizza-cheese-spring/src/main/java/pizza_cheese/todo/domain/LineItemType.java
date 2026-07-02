package pizza_cheese.todo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import pizza_cheese.todo.util.CodedEnums;

public enum LineItemType implements CodedEnum {

    PIZZA(1, "Pizza"),
    COMBO(2, "Combo");

    private final int code;
    private final String label;

    LineItemType(int code, String label) {
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
    public static LineItemType fromJson(Object value) {
        return CodedEnums.parse(LineItemType.class, value);
    }

    public static LineItemType fromCode(int code) {
        return CodedEnums.fromCode(LineItemType.class, code);
    }
}
