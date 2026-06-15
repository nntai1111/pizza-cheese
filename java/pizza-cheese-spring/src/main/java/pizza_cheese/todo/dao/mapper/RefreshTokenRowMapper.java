package pizza_cheese.todo.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.RefreshToken;

public class RefreshTokenRowMapper implements RowMapper<RefreshToken> {

    @Override
    public RefreshToken mapRow(ResultSet rs, int rowNum) throws SQLException {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(rs.getLong("id"));
        refreshToken.setToken(rs.getString("token"));
        refreshToken.setUserId(rs.getLong("user_id"));
        refreshToken.setExpiresAt(toInstant(rs.getObject("expires_at", OffsetDateTime.class)));
        refreshToken.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        return refreshToken;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
