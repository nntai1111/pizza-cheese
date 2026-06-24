package pizza_cheese.todo.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Immutable registry of named SQL statements loaded from a single resource file.
 */
public final class SqlQueries {

    private final String source;
    private final Map<String, String> queries;

    SqlQueries(String source, Map<String, String> queries) {
        this.source = source;
        this.queries = Collections.unmodifiableMap(queries);
    }

    /**
     * Returns the SQL for {@code name}, or throws if the query is missing or blank.
     */
    public String require(String name) {
        String sql = queries.get(name);
        if (sql == null) {
            throw new IllegalArgumentException(
                    "SQL query '" + name + "' not found in " + source + ". Available: " + queries.keySet());
        }
        if (sql.isBlank()) {
            throw new IllegalStateException("SQL query '" + name + "' in " + source + " is empty");
        }
        return sql;
    }

    public boolean contains(String name) {
        return queries.containsKey(name);
    }

    public Set<String> names() {
        return queries.keySet();
    }

    public String source() {
        return source;
    }

    public int size() {
        return queries.size();
    }
}
