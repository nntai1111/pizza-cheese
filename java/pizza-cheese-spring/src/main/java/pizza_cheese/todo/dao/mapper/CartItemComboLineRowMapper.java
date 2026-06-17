package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.CartItemComboLine;
import pizza_cheese.todo.domain.PizzaSize;

public class CartItemComboLineRowMapper implements RowMapper<CartItemComboLine> {

    @Override
    public CartItemComboLine mapRow(ResultSet rs, int rowNum) throws SQLException {
        CartItemComboLine line = new CartItemComboLine();
        line.setId(rs.getObject("id", UUID.class));
        line.setCartItemId(rs.getObject("cart_item_id", UUID.class));
        line.setPizzaId(rs.getObject("pizza_id", UUID.class));
        line.setPizzaVariantId(rs.getObject("pizza_variant_id", UUID.class));
        line.setQuantity(rs.getInt("quantity"));
        line.setPizzaName(rs.getString("pizza_name"));
        line.setPizzaSize(PizzaSize.valueOf(rs.getString("pizza_size")));
        return line;
    }
}
