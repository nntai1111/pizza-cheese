package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.RowMappers;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.util.JdbcTimeUtil;

@Repository
public class UserDao extends SqlDaoSupport {

    public UserDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        super(jdbc, resourceLoader, "classpath:sql/user.sql");
    }

    public Optional<User> findByEmail(String email) {
        return findOne(queries.require("findByEmail"), Map.of("email", email));
    }

    public Optional<User> findByUsername(String username) {
        return findOne(queries.require("findByUsername"), Map.of("username", username));
    }

    public Optional<User> findByEmailOrUsername(String login) {
        return findOne(queries.require("findByEmailOrUsername"), Map.of("login", login));
    }

    public Optional<User> findById(UUID id) {
        return findOne(queries.require("findById"), Map.of("id", id));
    }

    public boolean existsByEmail(String email) {
        Boolean exists = jdbc.queryForObject(
                queries.require("existsByEmail"),
                Map.of("email", email),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public boolean existsByUsername(String username) {
        Boolean exists = jdbc.queryForObject(
                queries.require("existsByUsername"),
                Map.of("username", username),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public long count() {
        Long count = jdbc.queryForObject(queries.require("count"), Map.of(), Long.class);
        return count != null ? count : 0L;
    }

    public User save(User user) {
        LocalDateTime now = LocalDateTime.now();

        if (user.getId() == null) {
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            insert(user);
        } else {
            user.setUpdatedAt(now);
            jdbc.update(queries.require("update"), new MapSqlParameterSource()
                    .addValue("id", user.getId())
                    .addValue("email", user.getEmail())
                    .addValue("passwordHash", user.getPasswordHash())
                    .addValue("fullName", user.getFullName())
                    .addValue("phone", user.getPhone())
                    .addValue("avatarUrl", user.getAvatarUrl())
                    .addValue("updatedAt", JdbcTimeUtil.toTimestamp(user.getUpdatedAt())));
            jdbc.update(queries.require("deleteRolesByUserId"), Map.of("userId", user.getId()));
        }

        saveRoles(user.getId(), user.getRoles());
        return user;
    }

    private Optional<User> findOne(String sql, Map<String, ?> params) {
        List<User> users = jdbc.query(sql, params, RowMappers.forEntity(User.class));
        if (users.isEmpty()) {
            return Optional.empty();
        }
        User user = users.get(0);
        user.setRoles(findRolesByUserId(user.getId()));
        return Optional.of(user);
    }

    private Set<Role> findRolesByUserId(UUID userId) {
        List<Role> roles = jdbc.query(
                queries.require("findRolesByUserId"),
                Map.of("userId", userId),
                (rs, rowNum) -> Role.valueOf(rs.getString("role")));
        return new HashSet<>(roles);
    }

    private void insert(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        jdbc.update(queries.require("insert"), new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("username", user.getUsername())
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("fullName", user.getFullName())
                .addValue("phone", user.getPhone())
                .addValue("avatarUrl", user.getAvatarUrl())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(user.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(user.getUpdatedAt())));
    }

    private void saveRoles(UUID userId, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        for (Role role : roles) {
            jdbc.update(queries.require("insertRole"), Map.of("userId", userId, "role", role.name()));
        }
    }
}
