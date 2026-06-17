package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.ComboItem;
import pizza_cheese.todo.domain.PizzaSize;

public class ComboItemRowMapper implements RowMapper<ComboItem> {

    @Override
    public ComboItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        ComboItem item = new ComboItem();
        item.setComboId(rs.getObject("combo_id", UUID.class));
        item.setPizzaId(rs.getObject("pizza_id", UUID.class));
        item.setPizzaVariantId(rs.getObject("pizza_variant_id", UUID.class));
        item.setPizzaName(rs.getString("pizza_name"));
        item.setPizzaSlug(rs.getString("pizza_slug"));
        String size = rs.getString("pizza_size");
        if (size != null) {
            item.setPizzaSize(PizzaSize.valueOf(size));
        }
        item.setQuantity(rs.getInt("quantity"));
        return item;
    }
}
