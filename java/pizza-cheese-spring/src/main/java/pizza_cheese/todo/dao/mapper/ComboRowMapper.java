package pizza_cheese.todo.dao.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.Combo;

public class ComboRowMapper implements RowMapper<Combo> {

    @Override
    public Combo mapRow(ResultSet rs, int rowNum) throws SQLException {
        Combo combo = new Combo();
        combo.setId(rs.getObject("id", UUID.class));
        combo.setName(rs.getString("name"));
        combo.setSlug(rs.getString("slug"));
        combo.setDescription(rs.getString("description"));
        combo.setPrice(rs.getObject("price", BigDecimal.class));
        combo.setDiscountPercent(rs.getObject("discount_percent", BigDecimal.class));
        combo.setImageUrl(rs.getString("image_url"));
        combo.setActive(rs.getBoolean("is_active"));
        combo.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        combo.setUpdatedAt(toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
        return combo;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
