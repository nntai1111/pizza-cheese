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

@Repository
public class CartDao extends SqlDaoSupport {

    public CartDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        super(jdbc, resourceLoader, "classpath:sql/cart.sql");
    }

    public Optional<Cart> findByUserId(UUID userId) {
        List<Cart> carts = jdbc.query(queries.require("findByUserId"), Map.of("userId", userId), RowMappers.forEntity(Cart.class));
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

        jdbc.update(queries.require("insert"), new MapSqlParameterSource()
                .addValue("id", cart.getId())
                .addValue("userId", cart.getUserId())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(cart.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(cart.getUpdatedAt())));

        return cart;
    }

    public Optional<CartItem> findItemById(UUID itemId) {
        List<CartItem> items = jdbc.query(queries.require("findItemById"), Map.of("id", itemId), RowMappers.forEntity(CartItem.class));
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    public CartItem insertItem(CartItem item) {
        LocalDateTime now = LocalDateTime.now();
        if (item.getId() == null) {
            item.setId(UUID.randomUUID());
        }
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        jdbc.update(queries.require("insertItem"), new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("cartId", item.getCartId())
                .addValue("itemType", item.getItemType().name())
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
        jdbc.update(queries.require("insertItemTopping"), Map.of(
                "cartItemId", cartItemId,
                "toppingId", toppingId,
                "price", price));
    }

    public void insertComboLine(CartItemComboLine line) {
        if (line.getId() == null) {
            line.setId(UUID.randomUUID());
        }
        jdbc.update(queries.require("insertComboLine"), new MapSqlParameterSource()
                .addValue("id", line.getId())
                .addValue("cartItemId", line.getCartItemId())
                .addValue("pizzaId", line.getPizzaId())
                .addValue("pizzaVariantId", line.getPizzaVariantId())
                .addValue("quantity", line.getQuantity())
                .addValue("pizzaName", line.getPizzaName())
                .addValue("pizzaSize", line.getPizzaSize().name()));
    }

    public void updateItemQuantity(CartItem item) {
        item.setUpdatedAt(LocalDateTime.now());
        jdbc.update(queries.require("updateItemQuantity"), new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("quantity", item.getQuantity())
                .addValue("lineTotal", item.getLineTotal())
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(item.getUpdatedAt())));
    }

    public void deleteItemById(UUID itemId) {
        jdbc.update(queries.require("deleteItemById"), Map.of("id", itemId));
    }

    public void deleteItemsByCartId(UUID cartId) {
        jdbc.update(queries.require("deleteItemsByCartId"), Map.of("cartId", cartId));
    }

    public void touchUpdatedAt(UUID cartId) {
        jdbc.update(queries.require("touchUpdatedAt"), new MapSqlParameterSource()
                .addValue("id", cartId)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    private void loadItems(Cart cart) {
        List<CartItem> items = jdbc.query(
                queries.require("findItemsByCartId"),
                Map.of("cartId", cart.getId()),
                RowMappers.forEntity(CartItem.class));
        items.forEach(this::loadRelations);
        cart.setItems(items);
    }

    private void loadRelations(CartItem item) {
        item.setToppings(jdbc.query(
                queries.require("findToppingsByCartItemId"),
                Map.of("cartItemId", item.getId()),
                RowMappers.forEntity(CartItemTopping.class)));
        item.setComboLines(jdbc.query(
                queries.require("findComboLinesByCartItemId"),
                Map.of("cartItemId", item.getId()),
                RowMappers.forEntity(CartItemComboLine.class)));
    }
}
