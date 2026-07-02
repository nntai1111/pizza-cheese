package pizza_cheese.todo.dto;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import pizza_cheese.todo.domain.CodedEnum;

public record CodedEnumValue(int code, String name, String label) {

    public static CodedEnumValue from(CodedEnum value) {
        if (value == null) {
            return null;
        }
        Enum<?> enumConstant = (Enum<?>) value;
        return new CodedEnumValue(value.getCode(), enumConstant.name(), value.getLabel());
    }

    public static Set<CodedEnumValue> fromSet(Collection<? extends CodedEnum> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().map(CodedEnumValue::from).collect(Collectors.toSet());
    }
}
