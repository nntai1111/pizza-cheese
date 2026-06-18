package pizza_cheese.todo.dao.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.LineItemType;
import pizza_cheese.todo.domain.OrderItem;
import pizza_cheese.todo.domain.PizzaSize;

public class OrderItemRowMapper implements RowMapper<OrderItem> {

    @Override
    public OrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        OrderItem item = new OrderItem();
        item.setId(rs.getObject("id", UUID.class));
        item.setOrderId(rs.getObject("order_id", UUID.class));
        item.setItemType(LineItemType.valueOf(rs.getString("item_type")));
        item.setPizzaId(rs.getObject("pizza_id", UUID.class));
        item.setPizzaVariantId(rs.getObject("pizza_variant_id", UUID.class));
        item.setComboId(rs.getObject("combo_id", UUID.class));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getObject("unit_price", BigDecimal.class));
        item.setLineTotal(rs.getObject("line_total", BigDecimal.class));

        if (hasColumn(rs, "pizza_name")) {
            item.setPizzaName(rs.getString("pizza_name"));
            item.setPizzaSlug(rs.getString("pizza_slug"));
            String size = rs.getString("pizza_size");
            item.setPizzaSize(size != null ? PizzaSize.valueOf(size) : null);
            item.setPizzaImageUrl(rs.getString("pizza_image_url"));
            item.setComboName(rs.getString("combo_name"));
            item.setComboSlug(rs.getString("combo_slug"));
            item.setComboImageUrl(rs.getString("combo_image_url"));
        }

        return item;
    }

    private static boolean hasColumn(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }
}
