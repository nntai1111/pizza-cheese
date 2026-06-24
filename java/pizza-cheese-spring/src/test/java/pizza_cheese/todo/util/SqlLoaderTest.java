package pizza_cheese.todo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class SqlLoaderTest {

    @Test
    void loadsNamedQueries() throws IOException {
        String sql = """
                -- name: findById
                SELECT id FROM users
                WHERE id = :id

                -- name: insert
                INSERT INTO users (id) VALUES (:id)
                """;

        SqlQueries queries = SqlLoader.load(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getDescription() {
                return "test.sql";
            }
        });

        assertEquals(2, queries.size());
        assertTrue(queries.contains("findById"));
        assertTrue(queries.contains("insert"));
        assertEquals("SELECT id FROM users\nWHERE id = :id", queries.require("findById"));
        assertEquals("INSERT INTO users (id) VALUES (:id)", queries.require("insert"));
    }

    @Test
    void handlesWindowsLineEndings() throws IOException {
        String sql = "-- name: findAll\r\nSELECT 1\r\n";

        SqlQueries queries = SqlLoader.load(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getDescription() {
                return "crlf.sql";
            }
        });

        assertEquals("SELECT 1", queries.require("findAll"));
    }

    @Test
    void cachesByResourceDescription() throws IOException {
        ByteArrayResource resource = new ByteArrayResource(""" 
                -- name: one
                SELECT 1
                """.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getDescription() {
                return "cache.sql";
            }
        };

        SqlQueries first = SqlLoader.load(resource);
        SqlQueries second = SqlLoader.load(resource);

        assertSame(first, second);
    }

    @Test
    void requireThrowsWhenQueryMissing() throws IOException {
        SqlQueries queries = load("-- name: findById\nSELECT 1");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> queries.require("missing"));

        assertTrue(error.getMessage().contains("missing"));
        assertTrue(error.getMessage().contains("findById"));
    }

    @Test
    void rejectsDuplicateQueryNames() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> load("""
                -- name: findById
                SELECT 1

                -- name: findById
                SELECT 2
                """));

        assertTrue(error.getMessage().contains("Duplicate"));
    }

    @Test
    void rejectsEmptyQueryBody() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> load("""
                -- name: findById

                -- name: insert
                INSERT INTO users (id) VALUES (:id)
                """));

        assertTrue(error.getMessage().contains("findById"));
        assertTrue(error.getMessage().contains("no body"));
    }

    @Test
    void rejectsBlankQueryName() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> load("""
                -- name:
                SELECT 1
                """));

        assertTrue(error.getMessage().contains("Blank query name"));
    }

    @Test
    void rejectsFileWithoutQueries() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> load("""
                -- just a comment
                SELECT 1
                """));

        assertTrue(error.getMessage().contains("No SQL queries found"));
    }

    @Test
    void namesReturnsLoadedKeysInOrder() throws IOException {
        SqlQueries queries = load("""
                -- name: b
                SELECT 2

                -- name: a
                SELECT 1
                """);

        assertEquals(2, queries.names().size());
        assertFalse(queries.contains("missing"));
    }

    private static SqlQueries load(String content) throws IOException {
        String description = "inline-" + System.identityHashCode(content) + ".sql";
        return SqlLoader.load(new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getDescription() {
                return description;
            }
        });
    }
}
