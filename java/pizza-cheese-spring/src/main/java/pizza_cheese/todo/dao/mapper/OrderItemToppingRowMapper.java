package pizza_cheese.todo.dao.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.OrderItemTopping;

public class OrderItemToppingRowMapper implements RowMapper<OrderItemTopping> {

    @Override
    public OrderItemTopping mapRow(ResultSet rs, int rowNum) throws SQLException {
        OrderItemTopping topping = new OrderItemTopping();
        topping.setOrderItemId(rs.getObject("order_item_id", UUID.class));
        topping.setToppingId(rs.getObject("topping_id", UUID.class));
        topping.setPrice(rs.getObject("price", BigDecimal.class));
        topping.setToppingName(rs.getString("topping_name"));
        return topping;
    }
}
