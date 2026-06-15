package pizza_cheese.todo.util;

import java.sql.Timestamp;
import java.time.Instant;

public final class JdbcTimeUtil {

    private JdbcTimeUtil() {
    }

    public static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
