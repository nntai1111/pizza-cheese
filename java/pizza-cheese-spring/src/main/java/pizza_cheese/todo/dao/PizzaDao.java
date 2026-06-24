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
import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaImage;
import pizza_cheese.todo.domain.PizzaVariant;
import pizza_cheese.todo.domain.Topping;
import pizza_cheese.todo.util.JdbcTimeUtil;

@Repository
public class PizzaDao extends SqlDaoSupport {

    public PizzaDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        super(jdbc, resourceLoader, "classpath:sql/pizza.sql");
    }

    public List<Pizza> findAll(boolean activeOnly, UUID categoryId) {
        List<Pizza> pizzas = jdbc.query(
                queries.require("findAll"),
                pizzaQueryParams(activeOnly, categoryId),
                RowMappers.forEntity(Pizza.class));
        pizzas.forEach(this::loadRelations);
        return pizzas;
    }

    public long countAll(boolean activeOnly, UUID categoryId) {
        Long count = jdbc.queryForObject(
                queries.require("countAll"),
                pizzaQueryParams(activeOnly, categoryId),
                Long.class);
        return count != null ? count : 0L;
    }

    public List<Pizza> findPage(boolean activeOnly, UUID categoryId, int page, int size) {
        MapSqlParameterSource params = pizzaQueryParams(activeOnly, categoryId)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        List<Pizza> pizzas = jdbc.query(
                queries.require("findPage"),
                params,
                RowMappers.forEntity(Pizza.class));
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
        return findOne(queries.require("findById"), Map.of("id", id));
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

    public Pizza save(Pizza pizza) {
        LocalDateTime now = LocalDateTime.now();

        if (pizza.getId() == null) {
            pizza.setId(UUID.randomUUID());
            pizza.setCreatedAt(now);
            pizza.setUpdatedAt(now);
            insert(pizza);
        } else {
            pizza.setUpdatedAt(now);
            update(pizza);
            jdbc.update(queries.require("deleteVariantsByPizzaId"), Map.of("pizzaId", pizza.getId()));
            jdbc.update(queries.require("deleteToppingsByPizzaId"), Map.of("pizzaId", pizza.getId()));
            jdbc.update(queries.require("deleteImagesByPizzaId"), Map.of("pizzaId", pizza.getId()));
        }

        saveVariants(pizza);
        saveToppings(pizza);
        saveImages(pizza);
        return pizza;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.require("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    private Optional<Pizza> findOne(String sql, Map<String, ?> params) {
        List<Pizza> pizzas = jdbc.query(sql, params, RowMappers.forEntity(Pizza.class));
        if (pizzas.isEmpty()) {
            return Optional.empty();
        }
        Pizza pizza = pizzas.get(0);
        loadRelations(pizza);
        return Optional.of(pizza);
    }

    private void loadRelations(Pizza pizza) {
        pizza.setVariants(jdbc.query(
                queries.require("findVariantsByPizzaId"),
                Map.of("pizzaId", pizza.getId()),
                RowMappers.forEntity(PizzaVariant.class)));
        var toppings = jdbc.query(
                queries.require("findToppingsByPizzaId"),
                Map.of("pizzaId", pizza.getId()),
                RowMappers.forEntity(Topping.class));
        pizza.setToppings(toppings);
        pizza.setToppingIds(toppings.stream().map(t -> t.getId()).toList());
        pizza.setImages(jdbc.query(
                queries.require("findImagesByPizzaId"),
                Map.of("pizzaId", pizza.getId()),
                RowMappers.forEntity(PizzaImage.class)));
    }

    private void insert(Pizza pizza) {
        jdbc.update(queries.require("insert"), new MapSqlParameterSource()
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
        jdbc.update(queries.require("update"), new MapSqlParameterSource()
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
            jdbc.update(queries.require("insertVariant"), new MapSqlParameterSource()
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
            jdbc.update(queries.require("insertPizzaTopping"), Map.of("pizzaId", pizza.getId(), "toppingId", toppingId));
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
            jdbc.update(queries.require("insertImage"), new MapSqlParameterSource()
                    .addValue("id", image.getId())
                    .addValue("pizzaId", image.getPizzaId())
                    .addValue("imageUrl", image.getImageUrl())
                    .addValue("isMain", image.isMain())
                    .addValue("sortOrder", image.getSortOrder()));
        }
    }
}
