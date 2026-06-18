package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.OrderItemComboLine;
import pizza_cheese.todo.domain.PizzaSize;

public class OrderItemComboLineRowMapper implements RowMapper<OrderItemComboLine> {

    @Override
    public OrderItemComboLine mapRow(ResultSet rs, int rowNum) throws SQLException {
        OrderItemComboLine line = new OrderItemComboLine();
        line.setId(rs.getObject("id", UUID.class));
        line.setOrderItemId(rs.getObject("order_item_id", UUID.class));
        line.setPizzaId(rs.getObject("pizza_id", UUID.class));
        line.setPizzaVariantId(rs.getObject("pizza_variant_id", UUID.class));
        line.setQuantity(rs.getInt("quantity"));
        line.setPizzaName(rs.getString("pizza_name"));
        String size = rs.getString("pizza_size");
        line.setPizzaSize(size != null ? PizzaSize.valueOf(size) : null);
        return line;
    }
}
