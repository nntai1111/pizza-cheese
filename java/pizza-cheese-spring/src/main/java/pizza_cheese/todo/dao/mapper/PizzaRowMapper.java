package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.Pizza;

public class PizzaRowMapper implements RowMapper<Pizza> {

    @Override
    public Pizza mapRow(ResultSet rs, int rowNum) throws SQLException {
        Pizza pizza = new Pizza();
        pizza.setId(rs.getObject("id", UUID.class));
        pizza.setCategoryId(rs.getObject("category_id", UUID.class));
        pizza.setName(rs.getString("name"));
        pizza.setSlug(rs.getString("slug"));
        pizza.setDescription(rs.getString("description"));
        pizza.setBasePrice(rs.getBigDecimal("base_price"));
        pizza.setActive(rs.getBoolean("is_active"));
        pizza.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        pizza.setUpdatedAt(toInstant(rs.getObject("updated_at", OffsetDateTime.class)));

        try {
            pizza.setCategoryName(rs.getString("category_name"));
            pizza.setCategorySlug(rs.getString("category_slug"));
        } catch (SQLException ignored) {
            // queries without category join
        }

        return pizza;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
