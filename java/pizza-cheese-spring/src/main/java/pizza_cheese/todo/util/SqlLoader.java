package pizza_cheese.todo.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.Resource;

public final class SqlLoader {

    private static final String NAME_PREFIX = "-- name:";

    private SqlLoader() {
    }

    public static Map<String, String> load(Resource resource) throws IOException {
        String content = resource.getContentAsString(StandardCharsets.UTF_8);
        Map<String, String> queries = new LinkedHashMap<>();
        String currentName = null;
        StringBuilder currentSql = new StringBuilder();

        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(NAME_PREFIX)) {
                if (currentName != null) {
                    queries.put(currentName, currentSql.toString().strip());
                }
                currentName = trimmed.substring(NAME_PREFIX.length()).strip();
                currentSql = new StringBuilder();
            } else {
                currentSql.append(line).append('\n');
            }
        }

        if (currentName != null) {
            queries.put(currentName, currentSql.toString().strip());
        }

        return queries;
    }
}
