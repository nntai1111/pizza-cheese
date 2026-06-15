package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.CategoryRowMapper;
import pizza_cheese.todo.domain.Category;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class CategoryDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;
    private final CategoryRowMapper rowMapper = new CategoryRowMapper();

    public CategoryDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/category.sql"));
    }

    public List<Category> findAll(boolean activeOnly) {
        return jdbc.query(queries.get("findAll"), Map.of("activeOnly", activeOnly), rowMapper);
    }

    public Optional<Category> findById(UUID id) {
        List<Category> categories = jdbc.query(queries.get("findById"), Map.of("id", id), rowMapper);
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    public boolean existsBySlug(String slug) {
        Boolean exists = jdbc.queryForObject(queries.get("existsBySlug"), Map.of("slug", slug), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public boolean existsBySlugExcludingId(String slug, UUID id) {
        Boolean exists = jdbc.queryForObject(
                queries.get("existsBySlugExcludingId"),
                Map.of("slug", slug, "id", id),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public Category save(Category category) {
        Instant now = Instant.now();

        if (category.getId() == null) {
            category.setId(UUID.randomUUID());
            category.setCreatedAt(now);
            category.setUpdatedAt(now);
            jdbc.update(queries.get("insert"), toParams(category));
        } else {
            category.setUpdatedAt(now);
            jdbc.update(queries.get("update"), toParams(category));
        }

        return category;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.get("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(Instant.now())));
    }

    private MapSqlParameterSource toParams(Category category) {
        return new MapSqlParameterSource()
                .addValue("id", category.getId())
                .addValue("name", category.getName())
                .addValue("slug", category.getSlug())
                .addValue("description", category.getDescription())
                .addValue("imageUrl", category.getImageUrl())
                .addValue("sortOrder", category.getSortOrder())
                .addValue("isActive", category.isActive())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(category.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(category.getUpdatedAt()));
    }
}
