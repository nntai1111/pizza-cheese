package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.PizzaSize;
import pizza_cheese.todo.domain.PizzaVariant;

public class PizzaVariantRowMapper implements RowMapper<PizzaVariant> {

    @Override
    public PizzaVariant mapRow(ResultSet rs, int rowNum) throws SQLException {
        PizzaVariant variant = new PizzaVariant();
        variant.setId(rs.getObject("id", UUID.class));
        variant.setPizzaId(rs.getObject("pizza_id", UUID.class));
        variant.setSize(PizzaSize.valueOf(rs.getString("size")));
        variant.setPrice(rs.getBigDecimal("price"));
        return variant;
    }
}
