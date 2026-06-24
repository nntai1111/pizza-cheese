package pizza_cheese.todo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Loads named SQL statements from classpath resources.
 * <p>
 * File format: each query starts with a marker line {@code -- name: queryName},
 * followed by the SQL body until the next marker or EOF.
 *
 * @see SqlQueries
 */
public final class SqlLoader {

    private static final String NAME_PREFIX = "-- name:";

    private static final ConcurrentHashMap<String, SqlQueries> CACHE = new ConcurrentHashMap<>();

    private SqlLoader() {
    }

    public static SqlQueries load(Resource resource) throws IOException {
        String cacheKey = resource.getDescription();
        SqlQueries cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        SqlQueries parsed = parse(resource);
        SqlQueries existing = CACHE.putIfAbsent(cacheKey, parsed);
        return existing != null ? existing : parsed;
    }

    public static SqlQueries load(ResourceLoader resourceLoader, String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalArgumentException("SQL resource not found: " + location);
        }
        return load(resource);
    }

    private static SqlQueries parse(Resource resource) throws IOException {
        String source = resource.getDescription();
        Map<String, String> queries = new LinkedHashMap<>();
        String currentName = null;
        StringBuilder currentSql = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.startsWith(NAME_PREFIX)) {
                    finishQuery(source, queries, currentName, currentSql);
                    currentName = extractName(source, trimmed);
                    currentSql = new StringBuilder();
                } else {
                    currentSql.append(line).append('\n');
                }
            }
        }

        finishQuery(source, queries, currentName, currentSql);

        if (queries.isEmpty()) {
            throw new IllegalStateException("No SQL queries found in " + source
                    + ". Expected lines like: " + NAME_PREFIX + " findById");
        }

        return new SqlQueries(source, queries);
    }

    private static void finishQuery(
            String source,
            Map<String, String> queries,
            String currentName,
            StringBuilder currentSql) {
        if (currentName == null) {
            return;
        }
        if (queries.containsKey(currentName)) {
            throw new IllegalStateException(
                    "Duplicate SQL query name '" + currentName + "' in " + source);
        }
        String sql = currentSql.toString().strip();
        if (sql.isBlank()) {
            throw new IllegalStateException(
                    "SQL query '" + currentName + "' in " + source + " has no body");
        }
        queries.put(currentName, sql);
    }

    private static String extractName(String source, String markerLine) {
        String name = markerLine.substring(NAME_PREFIX.length()).strip();
        if (name.isBlank()) {
            throw new IllegalStateException("Blank query name in " + source + ": " + markerLine);
        }
        return name;
    }
}
