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
import pizza_cheese.todo.domain.Combo;
import pizza_cheese.todo.domain.ComboItem;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class ComboDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;

    public ComboDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/combo.sql"));
    }

    public List<Combo> findAll(boolean activeOnly) {
        List<Combo> combos = jdbc.query(
                queries.get("findAll"),
                Map.of("activeOnly", activeOnly),
                RowMappers.forEntity(Combo.class));
        combos.forEach(this::loadItems);
        return combos;
    }

    public long countAll(boolean activeOnly) {
        Long count = jdbc.queryForObject(
                queries.get("countAll"),
                Map.of("activeOnly", activeOnly),
                Long.class);
        return count != null ? count : 0L;
    }

    public List<Combo> findPage(boolean activeOnly, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("activeOnly", activeOnly)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        List<Combo> combos = jdbc.query(queries.get("findPage"), params, RowMappers.forEntity(Combo.class));
        combos.forEach(this::loadItems);
        return combos;
    }

    public Optional<Combo> findById(UUID id) {
        List<Combo> combos = jdbc.query(queries.get("findById"), Map.of("id", id), RowMappers.forEntity(Combo.class));
        if (combos.isEmpty()) {
            return Optional.empty();
        }
        Combo combo = combos.get(0);
        loadItems(combo);
        return Optional.of(combo);
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

    public Combo save(Combo combo) {
        LocalDateTime now = LocalDateTime.now();

        if (combo.getId() == null) {
            combo.setId(UUID.randomUUID());
            combo.setCreatedAt(now);
            combo.setUpdatedAt(now);
            insert(combo);
        } else {
            combo.setUpdatedAt(now);
            update(combo);
            jdbc.update(queries.get("deleteItemsByComboId"), Map.of("comboId", combo.getId()));
        }

        saveItems(combo);
        return combo;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.get("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    private void loadItems(Combo combo) {
        combo.setItems(jdbc.query(
                queries.get("findItemsByComboId"),
                Map.of("comboId", combo.getId()),
                RowMappers.forEntity(ComboItem.class)));
    }

    private void insert(Combo combo) {
        jdbc.update(queries.get("insert"), toParams(combo));
    }

    private void update(Combo combo) {
        jdbc.update(queries.get("update"), toParams(combo));
    }

    private MapSqlParameterSource toParams(Combo combo) {
        return new MapSqlParameterSource()
                .addValue("id", combo.getId())
                .addValue("name", combo.getName())
                .addValue("slug", combo.getSlug())
                .addValue("description", combo.getDescription())
                .addValue("price", combo.getPrice())
                .addValue("discountPercent", combo.getDiscountPercent())
                .addValue("imageUrl", combo.getImageUrl())
                .addValue("isActive", combo.isActive())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(combo.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(combo.getUpdatedAt()));
    }

    private void saveItems(Combo combo) {
        if (combo.getItems() == null || combo.getItems().isEmpty()) {
            return;
        }
        for (ComboItem item : combo.getItems()) {
            jdbc.update(queries.get("insertItem"), new MapSqlParameterSource()
                    .addValue("comboId", combo.getId())
                    .addValue("pizzaId", item.getPizzaId())
                    .addValue("pizzaVariantId", item.getPizzaVariantId())
                    .addValue("quantity", item.getQuantity()));
        }
    }
}
