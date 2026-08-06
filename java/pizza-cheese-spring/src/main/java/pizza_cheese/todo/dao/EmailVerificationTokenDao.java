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
import pizza_cheese.todo.domain.EmailVerificationToken;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class EmailVerificationTokenDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final UserDao userDao;
    private final Map<String, String> queries;

    public EmailVerificationTokenDao(
            NamedParameterJdbcTemplate jdbc,
            UserDao userDao,
            ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.userDao = userDao;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/email_verification_token.sql"));
    }

    public Optional<EmailVerificationToken> findByToken(String token) {
        List<EmailVerificationToken> tokens = jdbc.query(
                queries.get("findByToken"),
                Map.of("token", token),
                RowMappers.forEntity(EmailVerificationToken.class));
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        EmailVerificationToken verificationToken = tokens.get(0);
        userDao.findById(verificationToken.getUserId()).ifPresent(verificationToken::setUser);
        return Optional.of(verificationToken);
    }

    public Optional<EmailVerificationToken> findLatestByUserId(UUID userId) {
        List<EmailVerificationToken> tokens = jdbc.query(
                queries.get("findLatestByUserId"),
                Map.of("userId", userId),
                RowMappers.forEntity(EmailVerificationToken.class));
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tokens.get(0));
    }

    public EmailVerificationToken save(EmailVerificationToken verificationToken) {
        if (verificationToken.getCreatedAt() == null) {
            verificationToken.setCreatedAt(LocalDateTime.now());
        }
        if (verificationToken.getId() == null) {
            verificationToken.setId(UUID.randomUUID());
        }

        UUID userId = verificationToken.getUser() != null
                ? verificationToken.getUser().getId()
                : verificationToken.getUserId();

        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", verificationToken.getId())
                .addValue("token", verificationToken.getToken())
                .addValue("userId", userId)
                .addValue("expiresAt", JdbcTimeUtil.toTimestamp(verificationToken.getExpiresAt()))
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(verificationToken.getCreatedAt())));
        return verificationToken;
    }

    public void delete(EmailVerificationToken verificationToken) {
        jdbc.update(queries.get("deleteById"), Map.of("id", verificationToken.getId()));
    }

    public void deleteByUser(User user) {
        jdbc.update(queries.get("deleteByUserId"), Map.of("userId", user.getId()));
    }
}
