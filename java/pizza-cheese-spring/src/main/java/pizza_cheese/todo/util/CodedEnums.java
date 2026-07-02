package pizza_cheese.todo.util;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import pizza_cheese.todo.domain.CodedEnum;

public final class CodedEnums {

    private static final Map<Class<? extends CodedEnum>, Map<Integer, ? extends CodedEnum>> BY_CODE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends CodedEnum>, Map<String, ? extends CodedEnum>> BY_LABEL = new ConcurrentHashMap<>();
    private static final Map<Class<? extends CodedEnum>, Map<String, ? extends CodedEnum>> BY_NAME = new ConcurrentHashMap<>();

    private CodedEnums() {
    }

    // Role.fromCode(5) → Trả về Role.ADMIN
    // CodedEnums.fromCode(class, code);
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E> & CodedEnum> E fromCode(Class<? extends CodedEnum> type, int code) {
        // Class<E> enumType = (Class<E>) type;
        Map<Integer, E> lookup = (Map<Integer, E>) BY_CODE.computeIfAbsent(type, CodedEnums::buildCodeLookup);
        E value = lookup.get(code);
        if (value == null) {
            throw new IllegalArgumentException("Unknown code " + code + " for enum " + type.getSimpleName());
        }
        return value;
    }

    // Nhận biết bạn truyền vào là gì (số, tên, hay label)-> chuyển thành Enum.
    // CodedEnums.parse(class, value);
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E> & CodedEnum> E parse(Class<E> type, Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Map<?, ?> map) {
            Object name = map.get("name");
            if (name != null) {
                return parseByText(type, name.toString());
            }
            Object code = map.get("code");
            if (code instanceof Number number) {
                return fromCode(type, number.intValue());
            }
            Object label = map.get("label");
            if (label != null) {
                return parseByText(type, label.toString());
            }
            return null;
        }
        if (raw instanceof Number number) {
            return fromCode(type, number.intValue());
        }
        return parseByText(type, raw.toString());
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E> & CodedEnum> E parseByText(Class<E> type, String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return fromCode(type, Integer.parseInt(trimmed));
        }
        Map<String, E> byName = (Map<String, E>) BY_NAME.computeIfAbsent(type, CodedEnums::buildNameLookup);
        E byConstant = byName.get(trimmed.toUpperCase());
        if (byConstant != null) {
            return byConstant;
        }
        Map<String, E> byLabel = (Map<String, E>) BY_LABEL.computeIfAbsent(type, CodedEnums::buildLabelLookup);
        E byLabelValue = byLabel.get(trimmed);
        if (byLabelValue != null) {
            return byLabelValue;
        }
        throw new IllegalArgumentException("Unknown value '" + trimmed + "' for enum " + type.getSimpleName());
    }

    private static <E extends Enum<E> & CodedEnum> Map<Integer, E> buildCodeLookup(Class<? extends CodedEnum> type) {
        @SuppressWarnings("unchecked")
        Class<E> enumType = (Class<E>) type;
        return Arrays.stream(enumType.getEnumConstants())
                .collect(Collectors.toMap(CodedEnum::getCode, Function.identity()));
    }

    private static <E extends Enum<E> & CodedEnum> Map<String, E> buildNameLookup(Class<? extends CodedEnum> type) {
        @SuppressWarnings("unchecked")
        Class<E> enumType = (Class<E>) type;
        return Arrays.stream(enumType.getEnumConstants())
                .collect(Collectors.toMap(value -> value.name(), Function.identity()));
    }

    private static <E extends Enum<E> & CodedEnum> Map<String, E> buildLabelLookup(Class<? extends CodedEnum> type) {
        @SuppressWarnings("unchecked")
        Class<E> enumType = (Class<E>) type;
        return Arrays.stream(enumType.getEnumConstants())
                .collect(Collectors.toMap(CodedEnum::getLabel, Function.identity()));
    }
}
