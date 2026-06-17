package pizza_cheese.todo.dao.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.CartItemTopping;

public class CartItemToppingRowMapper implements RowMapper<CartItemTopping> {

    @Override
    public CartItemTopping mapRow(ResultSet rs, int rowNum) throws SQLException {
        CartItemTopping topping = new CartItemTopping();
        topping.setCartItemId(rs.getObject("cart_item_id", UUID.class));
        topping.setToppingId(rs.getObject("topping_id", UUID.class));
        topping.setToppingName(rs.getString("topping_name"));
        topping.setPrice(rs.getObject("price", BigDecimal.class));
        return topping;
    }
}
