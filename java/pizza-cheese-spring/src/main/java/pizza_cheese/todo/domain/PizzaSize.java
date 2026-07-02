package pizza_cheese.todo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import pizza_cheese.todo.util.CodedEnums;

public enum PizzaSize implements CodedEnum {

    SMALL(1, "Nhỏ"),
    MEDIUM(2, "Vừa"),
    LARGE(3, "Lớn");

    private final int code;
    private final String label;

    PizzaSize(int code, String label) {
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
    public static PizzaSize fromJson(Object value) {
        return CodedEnums.parse(PizzaSize.class, value);
    }

    public static PizzaSize fromCode(int code) {
        return CodedEnums.fromCode(PizzaSize.class, code);
    }
}
