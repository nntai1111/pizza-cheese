package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.Topping;

// RowMapper = đọc từng dòng kết quả SQL.
// ToppingRowMapper = biến từng dòng đó thành object Topping.
public class ToppingRowMapper implements RowMapper<Topping> {

    @Override
    public Topping mapRow(ResultSet rs, int rowNum) throws SQLException {
        Topping topping = new Topping();
        topping.setId(rs.getObject("id", UUID.class));
        topping.setName(rs.getString("name"));
        topping.setPrice(rs.getBigDecimal("price"));
        topping.setActive(rs.getBoolean("is_active"));
        topping.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        topping.setUpdatedAt(toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
        return topping;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
