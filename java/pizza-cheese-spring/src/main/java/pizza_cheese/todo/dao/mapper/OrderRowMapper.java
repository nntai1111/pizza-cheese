package pizza_cheese.todo.dao.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.domain.PaymentMethod;

public class OrderRowMapper implements RowMapper<Order> {

    @Override
    public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
        Order order = new Order();
        order.setId(rs.getObject("id", UUID.class));
        order.setOrderCode(rs.getString("order_code"));
        order.setUserId(rs.getObject("user_id", UUID.class));
        order.setAddressId(rs.getObject("address_id", UUID.class));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setTotalAmount(rs.getObject("total_amount", BigDecimal.class));
        order.setDiscountAmount(rs.getObject("discount_amount", BigDecimal.class));
        order.setFinalAmount(rs.getObject("final_amount", BigDecimal.class));
        order.setCouponId(rs.getObject("coupon_id", UUID.class));
        String paymentMethod = rs.getString("payment_method_selected");
        order.setPaymentMethodSelected(paymentMethod != null ? PaymentMethod.valueOf(paymentMethod) : null);
        order.setNote(rs.getString("note"));
        order.setEstimatedDeliveryTime(toInstant(rs.getObject("estimated_delivery_time", OffsetDateTime.class)));
        order.setKitchenStaffId(rs.getObject("kitchen_staff_id", UUID.class));
        order.setDeliveryStaffId(rs.getObject("delivery_staff_id", UUID.class));
        order.setDeliveryAddressSnapshot(rs.getString("delivery_address_snapshot"));
        order.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        order.setUpdatedAt(toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
        return order;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
