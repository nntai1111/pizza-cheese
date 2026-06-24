package pizza_cheese.todo.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class JdbcTimeUtil {

    private JdbcTimeUtil() {
    }

    public static Timestamp toTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
    }
}
