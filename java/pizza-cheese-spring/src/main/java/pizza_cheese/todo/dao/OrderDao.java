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
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderItem;
import pizza_cheese.todo.domain.OrderItemComboLine;
import pizza_cheese.todo.domain.OrderItemTopping;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class OrderDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;

    public OrderDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/order.sql"));
    }

    public void insert(Order order) {
        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", order.getId())
                .addValue("orderCode", order.getOrderCode())
                .addValue("userId", order.getUserId())
                .addValue("addressId", order.getAddressId())
                .addValue("status", order.getStatus().getCode())
                .addValue("totalAmount", order.getTotalAmount())
                .addValue("discountAmount", order.getDiscountAmount())
                .addValue("finalAmount", order.getFinalAmount())
                .addValue("couponId", order.getCouponId())
                .addValue("paymentMethodSelected", order.getPaymentMethodSelected().getCode())
                .addValue("note", order.getNote())
                .addValue("estimatedDeliveryTime", JdbcTimeUtil.toTimestamp(order.getEstimatedDeliveryTime()))
                .addValue("kitchenStaffId", order.getKitchenStaffId())
                .addValue("deliveryStaffId", order.getDeliveryStaffId())
                .addValue("deliveryAddressSnapshot", order.getDeliveryAddressSnapshot())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(order.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(order.getUpdatedAt())));
    }

    public Optional<Order> findById(UUID id) {
        List<Order> orders = jdbc.query(queries.get("findById"), Map.of("id", id), RowMappers.forEntity(Order.class));
        return orders.isEmpty() ? Optional.empty() : Optional.of(loadDetails(orders.get(0)));
    }

    public Optional<Order> findByIdAndUserId(UUID id, UUID userId) {
        List<Order> orders = jdbc.query(
                queries.get("findByIdAndUserId"),
                Map.of("id", id, "userId", userId),
                RowMappers.forEntity(Order.class));
        return orders.isEmpty() ? Optional.empty() : Optional.of(loadDetails(orders.get(0)));
    }

    public List<Order> findByUserId(UUID userId) {
        List<Order> orders = jdbc.query(queries.get("findByUserId"), Map.of("userId", userId), RowMappers.forEntity(Order.class));
        orders.forEach(this::loadItems);
        return orders;
    }

    public List<Order> findAll() {
        return jdbc.query(queries.get("findAll"), Map.of(), RowMappers.forEntity(Order.class));
    }

    public long countAll() {
        Long count = jdbc.queryForObject(queries.get("countAll"), Map.of(), Long.class);
        return count != null ? count : 0L;
    }

    public List<Order> findPage(int page, int size) {
        return jdbc.query(
                queries.get("findPage"),
                new MapSqlParameterSource()
                        .addValue("limit", size)
                        .addValue("offset", (long) page * size),
                RowMappers.forEntity(Order.class));
    }

    public List<Order> findByStatus(OrderStatus status) {
        return jdbc.query(queries.get("findByStatus"), Map.of("status", status.getCode()), RowMappers.forEntity(Order.class));
    }

    public long countByStatus(OrderStatus status) {
        Long count = jdbc.queryForObject(
                queries.get("countByStatus"),
                Map.of("status", status.getCode()),
                Long.class);
        return count != null ? count : 0L;
    }

    public List<Order> findPageByStatus(OrderStatus status, int page, int size) {
        return jdbc.query(
                queries.get("findPageByStatus"),
                new MapSqlParameterSource()
                        .addValue("status", status.getCode())
                        .addValue("limit", size)
                        .addValue("offset", (long) page * size),
                RowMappers.forEntity(Order.class));
    }

    public boolean existsByOrderCode(String orderCode) {
        Integer count = jdbc.queryForObject(
                queries.get("existsByOrderCode"),
                Map.of("orderCode", orderCode),
                Integer.class);
        return count != null && count > 0;
    }

    public void updateStatus(UUID orderId, OrderStatus status) {
        jdbc.update(queries.get("updateStatus"), new MapSqlParameterSource()
                .addValue("id", orderId)
                .addValue("status", status.getCode())
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    public void insertStatusHistory(UUID orderId, OrderStatus status, UUID changedBy, String note) {
        jdbc.update(queries.get("insertStatusHistory"), new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("orderId", orderId)
                .addValue("status", status.getCode())
                .addValue("changedBy", changedBy)
                .addValue("note", note)
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    public OrderItem insertItem(OrderItem item) {
        if (item.getId() == null) {
            item.setId(UUID.randomUUID());
        }
        jdbc.update(queries.get("insertItem"), new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("orderId", item.getOrderId())
                .addValue("itemType", item.getItemType().getCode())
                .addValue("pizzaId", item.getPizzaId())
                .addValue("pizzaVariantId", item.getPizzaVariantId())
                .addValue("comboId", item.getComboId())
                .addValue("quantity", item.getQuantity())
                .addValue("unitPrice", item.getUnitPrice())
                .addValue("lineTotal", item.getLineTotal()));
        return item;
    }

    public void insertItemTopping(UUID orderItemId, UUID toppingId, java.math.BigDecimal price) {
        jdbc.update(queries.get("insertItemTopping"), Map.of(
                "orderItemId", orderItemId,
                "toppingId", toppingId,
                "price", price));
    }

    public void insertComboLine(OrderItemComboLine line) {
        if (line.getId() == null) {
            line.setId(UUID.randomUUID());
        }
        jdbc.update(queries.get("insertComboLine"), new MapSqlParameterSource()
                .addValue("id", line.getId())
                .addValue("orderItemId", line.getOrderItemId())
                .addValue("pizzaId", line.getPizzaId())
                .addValue("pizzaVariantId", line.getPizzaVariantId())
                .addValue("quantity", line.getQuantity())
                .addValue("pizzaName", line.getPizzaName())
                .addValue("pizzaSize", line.getPizzaSize() != null ? line.getPizzaSize().getCode() : null));
    }

    private Order loadDetails(Order order) {
        loadItems(order);
        return order;
    }

    private void loadItems(Order order) {
        List<OrderItem> items = jdbc.query(
                queries.get("findItemsByOrderId"),
                Map.of("orderId", order.getId()),
                RowMappers.forEntity(OrderItem.class));
        items.forEach(this::loadRelations);
        order.setItems(items);
    }

    private void loadRelations(OrderItem item) {
        item.setToppings(jdbc.query(
                queries.get("findToppingsByOrderItemId"),
                Map.of("orderItemId", item.getId()),
                RowMappers.forEntity(OrderItemTopping.class)));
        item.setComboLines(jdbc.query(
                queries.get("findComboLinesByOrderItemId"),
                Map.of("orderItemId", item.getId()),
                RowMappers.forEntity(OrderItemComboLine.class)));
    }
}
