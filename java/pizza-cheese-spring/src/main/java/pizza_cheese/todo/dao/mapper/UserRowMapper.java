package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.User;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getObject("id", UUID.class));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        user.setUpdatedAt(toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
        return user;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
