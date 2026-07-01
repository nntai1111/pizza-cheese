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
import pizza_cheese.todo.domain.Cart;
import pizza_cheese.todo.domain.CartItem;
import pizza_cheese.todo.domain.CartItemComboLine;
import pizza_cheese.todo.domain.CartItemTopping;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class CartDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;

    public CartDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/cart.sql"));
    }

    public Optional<Cart> findByUserId(UUID userId) {
        List<Cart> carts = jdbc.query(queries.get("findByUserId"), Map.of("userId", userId), RowMappers.forEntity(Cart.class));
        if (carts.isEmpty()) {
            return Optional.empty();
        }
        Cart cart = carts.get(0);
        loadItems(cart);
        return Optional.of(cart);
    }

    public Cart createForUser(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setCreatedAt(now);
        cart.setUpdatedAt(now);

        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", cart.getId())
                .addValue("userId", cart.getUserId())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(cart.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(cart.getUpdatedAt())));

        return cart;
    }

    public Optional<CartItem> findItemById(UUID itemId) {
        List<CartItem> items = jdbc.query(queries.get("findItemById"), Map.of("id", itemId), RowMappers.forEntity(CartItem.class));
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    public CartItem insertItem(CartItem item) {
        LocalDateTime now = LocalDateTime.now();
        if (item.getId() == null) {
            item.setId(UUID.randomUUID());
        }
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        jdbc.update(queries.get("insertItem"), new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("cartId", item.getCartId())
                .addValue("itemType", item.getItemType().getCode())
                .addValue("pizzaId", item.getPizzaId())
                .addValue("pizzaVariantId", item.getPizzaVariantId())
                .addValue("comboId", item.getComboId())
                .addValue("quantity", item.getQuantity())
                .addValue("unitPrice", item.getUnitPrice())
                .addValue("lineTotal", item.getLineTotal())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(item.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(item.getUpdatedAt())));

        return item;
    }

    public void insertItemTopping(UUID cartItemId, UUID toppingId, java.math.BigDecimal price) {
        jdbc.update(queries.get("insertItemTopping"), Map.of(
                "cartItemId", cartItemId,
                "toppingId", toppingId,
                "price", price));
    }

    public void insertComboLine(CartItemComboLine line) {
        if (line.getId() == null) {
            line.setId(UUID.randomUUID());
        }
        jdbc.update(queries.get("insertComboLine"), new MapSqlParameterSource()
                .addValue("id", line.getId())
                .addValue("cartItemId", line.getCartItemId())
                .addValue("pizzaId", line.getPizzaId())
                .addValue("pizzaVariantId", line.getPizzaVariantId())
                .addValue("quantity", line.getQuantity())
                .addValue("pizzaName", line.getPizzaName())
                .addValue("pizzaSize", line.getPizzaSize().getCode()));
    }

    public void updateItemQuantity(CartItem item) {
        item.setUpdatedAt(LocalDateTime.now());
        jdbc.update(queries.get("updateItemQuantity"), new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("quantity", item.getQuantity())
                .addValue("lineTotal", item.getLineTotal())
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(item.getUpdatedAt())));
    }

    public void deleteItemById(UUID itemId) {
        jdbc.update(queries.get("deleteItemById"), Map.of("id", itemId));
    }

    public void deleteItemsByCartId(UUID cartId) {
        jdbc.update(queries.get("deleteItemsByCartId"), Map.of("cartId", cartId));
    }

    public void touchUpdatedAt(UUID cartId) {
        jdbc.update(queries.get("touchUpdatedAt"), new MapSqlParameterSource()
                .addValue("id", cartId)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    private void loadItems(Cart cart) {
        List<CartItem> items = jdbc.query(
                queries.get("findItemsByCartId"),
                Map.of("cartId", cart.getId()),
                RowMappers.forEntity(CartItem.class));
        items.forEach(this::loadRelations);
        cart.setItems(items);
    }

    private void loadRelations(CartItem item) {
        item.setToppings(jdbc.query(
                queries.get("findToppingsByCartItemId"),
                Map.of("cartItemId", item.getId()),
                RowMappers.forEntity(CartItemTopping.class)));
        item.setComboLines(jdbc.query(
                queries.get("findComboLinesByCartItemId"),
                Map.of("cartItemId", item.getId()),
                RowMappers.forEntity(CartItemComboLine.class)));
    }
}
