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

import pizza_cheese.todo.dao.mapper.PizzaImageRowMapper;
import pizza_cheese.todo.dao.mapper.PizzaRowMapper;
import pizza_cheese.todo.dao.mapper.PizzaVariantRowMapper;
import pizza_cheese.todo.dao.mapper.ToppingRowMapper;
import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaImage;
import pizza_cheese.todo.domain.PizzaVariant;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class PizzaDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;
    private final PizzaRowMapper pizzaRowMapper = new PizzaRowMapper();
    private final PizzaVariantRowMapper variantRowMapper = new PizzaVariantRowMapper();
    private final ToppingRowMapper toppingRowMapper = new ToppingRowMapper();
    private final PizzaImageRowMapper imageRowMapper = new PizzaImageRowMapper();

    public PizzaDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/pizza.sql"));
    }

    public List<Pizza> findAll(boolean activeOnly, UUID categoryId) {
        List<Pizza> pizzas = jdbc.query(
                queries.get("findAll"),
                pizzaQueryParams(activeOnly, categoryId),
                pizzaRowMapper);
        pizzas.forEach(this::loadRelations);
        return pizzas;
    }

    public long countAll(boolean activeOnly, UUID categoryId) {
        Long count = jdbc.queryForObject(
                queries.get("countAll"),
                pizzaQueryParams(activeOnly, categoryId),
                Long.class);
        return count != null ? count : 0L;
    }

    public List<Pizza> findPage(boolean activeOnly, UUID categoryId, int page, int size) {
        MapSqlParameterSource params = pizzaQueryParams(activeOnly, categoryId)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        List<Pizza> pizzas = jdbc.query(
                queries.get("findPage"),
                params,
                pizzaRowMapper);
        pizzas.forEach(this::loadRelations);
        return pizzas;
    }

    private MapSqlParameterSource pizzaQueryParams(boolean activeOnly, UUID categoryId) {
        return new MapSqlParameterSource()
                .addValue("activeOnly", activeOnly)
                .addValue("filterByCategory", categoryId != null)
                .addValue("categoryId", categoryId);
    }

    public Optional<Pizza> findById(UUID id) {
        return findOne(queries.get("findById"), Map.of("id", id));
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

    public Pizza save(Pizza pizza) {
        Instant now = Instant.now();

        if (pizza.getId() == null) {
            pizza.setId(UUID.randomUUID());
            pizza.setCreatedAt(now);
            pizza.setUpdatedAt(now);
            insert(pizza);
        } else {
            pizza.setUpdatedAt(now);
            update(pizza);
            jdbc.update(queries.get("deleteVariantsByPizzaId"), Map.of("pizzaId", pizza.getId()));
            jdbc.update(queries.get("deleteToppingsByPizzaId"), Map.of("pizzaId", pizza.getId()));
            jdbc.update(queries.get("deleteImagesByPizzaId"), Map.of("pizzaId", pizza.getId()));
        }

        saveVariants(pizza);
        saveToppings(pizza);
        saveImages(pizza);
        return pizza;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.get("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(Instant.now())));
    }

    private Optional<Pizza> findOne(String sql, Map<String, ?> params) {
        List<Pizza> pizzas = jdbc.query(sql, params, pizzaRowMapper);
        if (pizzas.isEmpty()) {
            return Optional.empty();
        }
        Pizza pizza = pizzas.get(0);
        loadRelations(pizza);
        return Optional.of(pizza);
    }

    private void loadRelations(Pizza pizza) {
        pizza.setVariants(jdbc.query(
                queries.get("findVariantsByPizzaId"),
                Map.of("pizzaId", pizza.getId()),
                variantRowMapper));
        var toppings = jdbc.query(
                queries.get("findToppingsByPizzaId"),
                Map.of("pizzaId", pizza.getId()),
                toppingRowMapper);
        pizza.setToppings(toppings);
        pizza.setToppingIds(toppings.stream().map(t -> t.getId()).toList());
        pizza.setImages(jdbc.query(
                queries.get("findImagesByPizzaId"),
                Map.of("pizzaId", pizza.getId()),
                imageRowMapper));
    }

    private void insert(Pizza pizza) {
        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", pizza.getId())
                .addValue("categoryId", pizza.getCategoryId())
                .addValue("name", pizza.getName())
                .addValue("slug", pizza.getSlug())
                .addValue("description", pizza.getDescription())
                .addValue("basePrice", pizza.getBasePrice())
                .addValue("isActive", pizza.isActive())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(pizza.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(pizza.getUpdatedAt())));
    }

    private void update(Pizza pizza) {
        jdbc.update(queries.get("update"), new MapSqlParameterSource()
                .addValue("id", pizza.getId())
                .addValue("categoryId", pizza.getCategoryId())
                .addValue("name", pizza.getName())
                .addValue("slug", pizza.getSlug())
                .addValue("description", pizza.getDescription())
                .addValue("basePrice", pizza.getBasePrice())
                .addValue("isActive", pizza.isActive())
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(pizza.getUpdatedAt())));
    }

    private void saveVariants(Pizza pizza) {
        if (pizza.getVariants() == null || pizza.getVariants().isEmpty()) {
            return;
        }
        for (PizzaVariant variant : pizza.getVariants()) {
            if (variant.getId() == null) {
                variant.setId(UUID.randomUUID());
            }
            variant.setPizzaId(pizza.getId());
            jdbc.update(queries.get("insertVariant"), new MapSqlParameterSource()
                    .addValue("id", variant.getId())
                    .addValue("pizzaId", variant.getPizzaId())
                    .addValue("size", variant.getSize().name())
                    .addValue("price", variant.getPrice()));
        }
    }

    private void saveToppings(Pizza pizza) {
        if (pizza.getToppingIds() == null || pizza.getToppingIds().isEmpty()) {
            return;
        }
        for (UUID toppingId : pizza.getToppingIds()) {
            jdbc.update(queries.get("insertPizzaTopping"), Map.of("pizzaId", pizza.getId(), "toppingId", toppingId));
        }
    }

    private void saveImages(Pizza pizza) {
        if (pizza.getImages() == null || pizza.getImages().isEmpty()) {
            return;
        }
        for (PizzaImage image : pizza.getImages()) {
            if (image.getId() == null) {
                image.setId(UUID.randomUUID());
            }
            image.setPizzaId(pizza.getId());
            jdbc.update(queries.get("insertImage"), new MapSqlParameterSource()
                    .addValue("id", image.getId())
                    .addValue("pizzaId", image.getPizzaId())
                    .addValue("imageUrl", image.getImageUrl())
                    .addValue("isMain", image.isMain())
                    .addValue("sortOrder", image.getSortOrder()));
        }
    }
}
