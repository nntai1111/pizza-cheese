package pizza_cheese.todo.dao.mapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

public final class RowMappers {

    private static final Map<Class<?>, RowMapper<?>> CACHE = new ConcurrentHashMap<>();

    private RowMappers() {
    }

    @SuppressWarnings("unchecked")
    public static <T> RowMapper<T> forEntity(Class<T> type) {
        return (RowMapper<T>) CACHE.computeIfAbsent(type, BeanPropertyRowMapper::newInstance);
    }
}
