package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.RowMappers;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class UserDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;

    public UserDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/user.sql"));
    }

    public Optional<User> findByEmail(String email) {
        return findOne(queries.get("findByEmail"), Map.of("email", email));
    }

    public Optional<User> findByUsername(String username) {
        return findOne(queries.get("findByUsername"), Map.of("username", username));
    }

    public Optional<User> findByEmailOrUsername(String login) {
        return findOne(queries.get("findByEmailOrUsername"), Map.of("login", login));
    }

    public Optional<User> findById(UUID id) {
        return findOne(queries.get("findById"), Map.of("id", id));
    }

    public Map<UUID, UserDisplayInfo> findDisplayInfoByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<UserDisplayInfo> users = jdbc.query(
                queries.get("findDisplayInfoByIds"),
                Map.of("ids", ids),
                (rs, rowNum) -> new UserDisplayInfo(
                        rs.getObject("id", UUID.class),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone")));
        return users.stream()
                .collect(Collectors.toMap(UserDisplayInfo::id, Function.identity()));
    }

    public boolean existsByEmail(String email) {
        Boolean exists = jdbc.queryForObject(
                queries.get("existsByEmail"),
                Map.of("email", email),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public boolean existsByUsername(String username) {
        Boolean exists = jdbc.queryForObject(
                queries.get("existsByUsername"),
                Map.of("username", username),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public long count() {
        Long count = jdbc.queryForObject(queries.get("count"), Map.of(), Long.class);
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
            jdbc.update(queries.get("update"), new MapSqlParameterSource()
                    .addValue("id", user.getId())
                    .addValue("email", user.getEmail())
                    .addValue("passwordHash", user.getPasswordHash())
                    .addValue("fullName", user.getFullName())
                    .addValue("phone", user.getPhone())
                    .addValue("avatarUrl", user.getAvatarUrl())
                    .addValue("active", user.isActive())
                    .addValue("updatedAt", JdbcTimeUtil.toTimestamp(user.getUpdatedAt())));
            jdbc.update(queries.get("deleteRolesByUserId"), Map.of("userId", user.getId()));
        }

        saveRoles(user.getId(), user.getRoles());
        return user;
    }

    public long countStaff(Role role) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = queries.get("countStaffBase") + buildStaffRoleFilter(role, params);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    public List<User> findStaffPage(Role role, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = queries.get("findStaffPageBase")
                + buildStaffRoleFilter(role, params)
                + " ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset";
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);
        List<User> users = jdbc.query(sql, params, RowMappers.forEntity(User.class));
        for (User user : users) {
            user.setRoles(findRolesByUserId(user.getId()));
        }
        return users;
    }

    private String buildStaffRoleFilter(Role role, MapSqlParameterSource params) {
        if (role == null) {
            return "";
        }
        params.addValue("role", role.name());
        return """
                 AND EXISTS (
                    SELECT 1
                    FROM user_roles ur2
                    JOIN roles r2 ON r2.id = ur2.role_id
                    WHERE ur2.user_id = u.id
                      AND r2.name = :role
                )
                """;
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
                queries.get("findRolesByUserId"),
                Map.of("userId", userId),
                (rs, rowNum) -> Role.fromJson(rs.getString("role")));
        return new HashSet<>(roles);
    }

    private void insert(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("username", user.getUsername())
                .addValue("email", user.getEmail())
                .addValue("passwordHash", user.getPasswordHash())
                .addValue("fullName", user.getFullName())
                .addValue("phone", user.getPhone())
                .addValue("avatarUrl", user.getAvatarUrl())
                .addValue("active", user.isActive())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(user.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(user.getUpdatedAt())));
    }

    private void saveRoles(UUID userId, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        for (Role role : roles) {
            jdbc.update(queries.get("insertRole"), Map.of("userId", userId, "role", role.name()));
        }
    }

    public long countCustomers() {
        Long count = jdbc.queryForObject(queries.get("countCustomersBase"), Map.of(), Long.class);
        return count != null ? count : 0L;
    }

    public List<User> findCustomersPage(int page, int size) {
        List<User> users = jdbc.query(
                queries.get("findCustomersPageBase") + " ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset",
                new MapSqlParameterSource()
                        .addValue("limit", size)
                        .addValue("offset", (long) page * size),
                RowMappers.forEntity(User.class));
        for (User user : users) {
            user.setRoles(findRolesByUserId(user.getId()));
        }
        return users;
    }

    public record UserDisplayInfo(UUID id, String fullName, String email, String phone) {
    }
}
