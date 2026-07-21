package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.RowMappers;
import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaImage;
import pizza_cheese.todo.domain.PizzaSize;
import pizza_cheese.todo.domain.PizzaVariant;
import pizza_cheese.todo.domain.Topping;
import pizza_cheese.todo.exception.ApiException;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class PizzaDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;

    public PizzaDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/pizza.sql"));
    }

    public List<Pizza> findAll(boolean activeOnly, UUID categoryId) {
        List<Pizza> pizzas = jdbc.query(
                queries.get("findAll"),
                pizzaQueryParams(activeOnly, categoryId),
                RowMappers.forEntity(Pizza.class));
        loadRelationsBatch(pizzas);
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
                RowMappers.forEntity(Pizza.class));
        loadRelationsBatch(pizzas);
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
        LocalDateTime now = LocalDateTime.now();

        if (pizza.getId() == null) {
            pizza.setId(UUID.randomUUID());
            pizza.setCreatedAt(now);
            pizza.setUpdatedAt(now);
            insert(pizza);
            saveVariants(pizza);
        } else {
            pizza.setUpdatedAt(now);
            update(pizza);
            syncVariants(pizza);
            jdbc.update(queries.get("deleteToppingsByPizzaId"), Map.of("pizzaId", pizza.getId()));
            jdbc.update(queries.get("deleteImagesByPizzaId"), Map.of("pizzaId", pizza.getId()));
        }

        saveToppings(pizza);
        saveImages(pizza);
        return pizza;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.get("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    private Optional<Pizza> findOne(String sql, Map<String, ?> params) {
        List<Pizza> pizzas = jdbc.query(sql, params, RowMappers.forEntity(Pizza.class));
        if (pizzas.isEmpty()) {
            return Optional.empty();
        }
        Pizza pizza = pizzas.get(0);
        loadRelationsBatch(List.of(pizza));
        return Optional.of(pizza);
    }

    private void loadRelationsBatch(Collection<Pizza> pizzas) {
        if (pizzas == null || pizzas.isEmpty()) {
            return;
        }

        List<UUID> pizzaIds = pizzas.stream().map(Pizza::getId).distinct().toList();
        if (pizzaIds.isEmpty()) {
            return;
        }

        Map<UUID, List<PizzaVariant>> variantsByPizza = loadVariantsByPizzaIds(pizzaIds);
        Map<UUID, List<Topping>> toppingsByPizza = loadToppingsByPizzaIds(pizzaIds);
        Map<UUID, List<PizzaImage>> imagesByPizza = loadImagesByPizzaIds(pizzaIds);

        for (Pizza pizza : pizzas) {
            UUID pizzaId = pizza.getId();
            List<Topping> toppings = toppingsByPizza.getOrDefault(pizzaId, List.of());
            pizza.setVariants(variantsByPizza.getOrDefault(pizzaId, List.of()));
            pizza.setToppings(toppings);
            pizza.setToppingIds(toppings.stream().map(Topping::getId).toList());
            pizza.setImages(imagesByPizza.getOrDefault(pizzaId, List.of()));
        }
    }

    private Map<UUID, List<PizzaVariant>> loadVariantsByPizzaIds(List<UUID> pizzaIds) {
        if (pizzaIds.isEmpty()) {
            return Map.of();
        }
        List<PizzaVariant> variants = jdbc.query(
                queries.get("findVariantsByPizzaIds"),
                Map.of("pizzaIds", pizzaIds),
                RowMappers.forEntity(PizzaVariant.class));
        return variants.stream().collect(Collectors.groupingBy(PizzaVariant::getPizzaId));
    }

    private Map<UUID, List<Topping>> loadToppingsByPizzaIds(List<UUID> pizzaIds) {
        if (pizzaIds.isEmpty()) {
            return Map.of();
        }
        RowMapper<Topping> toppingMapper = RowMappers.forEntity(Topping.class);
        return jdbc.query(queries.get("findToppingsByPizzaIds"), Map.of("pizzaIds", pizzaIds), rs -> {
            Map<UUID, List<Topping>> result = new HashMap<>();
            int rowNum = 0;
            while (rs.next()) {
                UUID pizzaId = rs.getObject("pizza_id", UUID.class);
                Topping topping = toppingMapper.mapRow(rs, rowNum++);
                result.computeIfAbsent(pizzaId, ignored -> new ArrayList<>()).add(topping);
            }
            return result;
        });
    }

    private Map<UUID, List<PizzaImage>> loadImagesByPizzaIds(List<UUID> pizzaIds) {
        if (pizzaIds.isEmpty()) {
            return Map.of();
        }
        List<PizzaImage> images = jdbc.query(
                queries.get("findImagesByPizzaIds"),
                Map.of("pizzaIds", pizzaIds),
                RowMappers.forEntity(PizzaImage.class));
        return images.stream().collect(Collectors.groupingBy(PizzaImage::getPizzaId));
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
            insertVariant(pizza.getId(), variant);
        }
    }

    /**
     * Upsert variants by size so existing IDs stay stable (combo/cart/order FKs).
     * Sizes removed from the request are deleted only when not referenced.
     */
    private void syncVariants(Pizza pizza) {
        List<PizzaVariant> existing = jdbc.query(
                queries.get("findVariantsByPizzaId"),
                Map.of("pizzaId", pizza.getId()),
                RowMappers.forEntity(PizzaVariant.class));

        Map<PizzaSize, PizzaVariant> existingBySize = new EnumMap<>(PizzaSize.class);
        for (PizzaVariant variant : existing) {
            existingBySize.put(variant.getSize(), variant);
        }

        List<PizzaVariant> incoming = pizza.getVariants() != null ? pizza.getVariants() : List.of();
        Set<PizzaSize> keptSizes = new HashSet<>();

        for (PizzaVariant variant : incoming) {
            keptSizes.add(variant.getSize());
            PizzaVariant current = existingBySize.get(variant.getSize());
            if (current != null) {
                variant.setId(current.getId());
                variant.setPizzaId(pizza.getId());
                jdbc.update(queries.get("updateVariant"), new MapSqlParameterSource()
                        .addValue("id", current.getId())
                        .addValue("price", variant.getPrice()));
            } else {
                insertVariant(pizza.getId(), variant);
            }
        }

        for (PizzaVariant current : existing) {
            if (keptSizes.contains(current.getSize())) {
                continue;
            }
            if (isVariantReferenced(current.getId())) {
                throw ApiException.badRequest(
                        "Không thể xóa size " + current.getSize().getLabel()
                                + " vì đang được dùng trong combo, giỏ hàng hoặc đơn hàng");
            }
            jdbc.update(queries.get("deleteVariantById"), Map.of("id", current.getId()));
        }
    }

    private void insertVariant(UUID pizzaId, PizzaVariant variant) {
        if (variant.getId() == null) {
            variant.setId(UUID.randomUUID());
        }
        variant.setPizzaId(pizzaId);
        jdbc.update(queries.get("insertVariant"), new MapSqlParameterSource()
                .addValue("id", variant.getId())
                .addValue("pizzaId", variant.getPizzaId())
                .addValue("size", variant.getSize().getCode())
                .addValue("price", variant.getPrice()));
    }

    private boolean isVariantReferenced(UUID variantId) {
        Boolean exists = jdbc.queryForObject(
                queries.get("isVariantReferenced"),
                Map.of("variantId", variantId),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
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
