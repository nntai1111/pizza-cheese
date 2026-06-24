package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.RowMappers;
import pizza_cheese.todo.domain.Category;
import pizza_cheese.todo.util.JdbcTimeUtil;

@Repository
public class CategoryDao extends SqlDaoSupport {

    public CategoryDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        super(jdbc, resourceLoader, "classpath:sql/category.sql");
    }

    public List<Category> findAll(boolean activeOnly) {
        return jdbc.query(queries.require("findAll"), Map.of("activeOnly", activeOnly), RowMappers.forEntity(Category.class));
    }

    public Optional<Category> findById(UUID id) {
        List<Category> categories = jdbc.query(queries.require("findById"), Map.of("id", id), RowMappers.forEntity(Category.class));
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    public boolean existsBySlug(String slug) {
        Boolean exists = jdbc.queryForObject(queries.require("existsBySlug"), Map.of("slug", slug), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public boolean existsBySlugExcludingId(String slug, UUID id) {
        Boolean exists = jdbc.queryForObject(
                queries.require("existsBySlugExcludingId"),
                Map.of("slug", slug, "id", id),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public Category save(Category category) {
        LocalDateTime now = LocalDateTime.now();

        if (category.getId() == null) {
            category.setId(UUID.randomUUID());
            category.setCreatedAt(now);
            category.setUpdatedAt(now);
            jdbc.update(queries.require("insert"), toParams(category));
        } else {
            category.setUpdatedAt(now);
            jdbc.update(queries.require("update"), toParams(category));
        }

        return category;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.require("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
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
