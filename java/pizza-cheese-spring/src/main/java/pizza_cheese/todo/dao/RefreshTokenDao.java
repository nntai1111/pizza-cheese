package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.RefreshTokenRowMapper;
import pizza_cheese.todo.domain.RefreshToken;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class RefreshTokenDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final UserDao userDao;
    private final Map<String, String> queries;
    private final RefreshTokenRowMapper refreshTokenRowMapper = new RefreshTokenRowMapper();

    public RefreshTokenDao(
            NamedParameterJdbcTemplate jdbc,
            UserDao userDao,
            ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.userDao = userDao;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/refresh_token.sql"));
    }

    public Optional<RefreshToken> findByToken(String token) {
        List<RefreshToken> tokens = jdbc.query(
                queries.get("findByToken"),
                Map.of("token", token),
                refreshTokenRowMapper);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken refreshToken = tokens.get(0);
        userDao.findById(refreshToken.getUserId()).ifPresent(refreshToken::setUser);
        return Optional.of(refreshToken);
    }

    public RefreshToken save(RefreshToken refreshToken) {
        if (refreshToken.getCreatedAt() == null) {
            refreshToken.setCreatedAt(Instant.now());
        }

        if (refreshToken.getId() == null) {
            refreshToken.setId(UUID.randomUUID());
        }

        UUID userId = refreshToken.getUser() != null
                ? refreshToken.getUser().getId()
                : refreshToken.getUserId();

        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", refreshToken.getId())
                .addValue("token", refreshToken.getToken())
                .addValue("userId", userId)
                .addValue("expiresAt", JdbcTimeUtil.toTimestamp(refreshToken.getExpiresAt()))
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(refreshToken.getCreatedAt())));
        return refreshToken;
    }

    public void delete(RefreshToken refreshToken) {
        jdbc.update(queries.get("deleteById"), Map.of("id", refreshToken.getId()));
    }

    public void deleteByUser(User user) {
        jdbc.update(queries.get("deleteByUserId"), Map.of("userId", user.getId()));
    }
}
