package pizza_cheese.todo.dao.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.CartItem;
import pizza_cheese.todo.domain.LineItemType;
import pizza_cheese.todo.domain.PizzaSize;

public class CartItemRowMapper implements RowMapper<CartItem> {

    @Override
    public CartItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        CartItem item = new CartItem();
        item.setId(rs.getObject("id", UUID.class));
        item.setCartId(rs.getObject("cart_id", UUID.class));
        item.setItemType(LineItemType.valueOf(rs.getString("item_type")));
        item.setPizzaId(rs.getObject("pizza_id", UUID.class));
        item.setPizzaVariantId(rs.getObject("pizza_variant_id", UUID.class));
        item.setComboId(rs.getObject("combo_id", UUID.class));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getObject("unit_price", BigDecimal.class));
        item.setLineTotal(rs.getObject("line_total", BigDecimal.class));
        item.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        item.setUpdatedAt(toInstant(rs.getObject("updated_at", OffsetDateTime.class)));

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

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
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
