package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.RowMappers;
import pizza_cheese.todo.domain.PasswordResetToken;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class PasswordResetTokenDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final UserDao userDao;
    private final Map<String, String> queries;

    public PasswordResetTokenDao(
            NamedParameterJdbcTemplate jdbc,
            UserDao userDao,
            ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.userDao = userDao;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/password_reset_token.sql"));
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        List<PasswordResetToken> tokens = jdbc.query(
                queries.get("findByToken"),
                Map.of("token", token),
                RowMappers.forEntity(PasswordResetToken.class));
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        PasswordResetToken resetToken = tokens.get(0);
        userDao.findById(resetToken.getUserId()).ifPresent(resetToken::setUser);
        return Optional.of(resetToken);
    }

    public Optional<PasswordResetToken> findLatestByUserId(UUID userId) {
        List<PasswordResetToken> tokens = jdbc.query(
                queries.get("findLatestByUserId"),
                Map.of("userId", userId),
                RowMappers.forEntity(PasswordResetToken.class));
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        PasswordResetToken resetToken = tokens.get(0);
        userDao.findById(resetToken.getUserId()).ifPresent(resetToken::setUser);
        return Optional.of(resetToken);
    }

    public PasswordResetToken save(PasswordResetToken resetToken) {
        if (resetToken.getCreatedAt() == null) {
            resetToken.setCreatedAt(LocalDateTime.now());
        }
        if (resetToken.getId() == null) {
            resetToken.setId(UUID.randomUUID());
        }

        UUID userId = resetToken.getUser() != null
                ? resetToken.getUser().getId()
                : resetToken.getUserId();

        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", resetToken.getId())
                .addValue("userId", userId)
                .addValue("token", resetToken.getToken())
                .addValue("otpHash", resetToken.getOtpHash())
                .addValue("expiresAt", JdbcTimeUtil.toTimestamp(resetToken.getExpiresAt()))
                .addValue("used", resetToken.isUsed())
                .addValue("attempts", resetToken.getAttempts())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(resetToken.getCreatedAt())));
        return resetToken;
    }

    public void updateAfterOtpVerified(PasswordResetToken resetToken) {
        jdbc.update(queries.get("updateAfterOtpVerified"), new MapSqlParameterSource()
                .addValue("id", resetToken.getId())
                .addValue("token", resetToken.getToken())
                .addValue("expiresAt", JdbcTimeUtil.toTimestamp(resetToken.getExpiresAt()))
                .addValue("attempts", resetToken.getAttempts()));
    }

    public void incrementAttempts(PasswordResetToken resetToken) {
        jdbc.update(queries.get("incrementAttempts"), Map.of("id", resetToken.getId()));
        resetToken.setAttempts(resetToken.getAttempts() + 1);
    }

    public void markUsed(PasswordResetToken resetToken) {
        jdbc.update(queries.get("markUsed"), Map.of("id", resetToken.getId()));
        resetToken.setUsed(true);
    }

    public void deleteByUser(User user) {
        jdbc.update(queries.get("deleteByUserId"), Map.of("userId", user.getId()));
    }
}
