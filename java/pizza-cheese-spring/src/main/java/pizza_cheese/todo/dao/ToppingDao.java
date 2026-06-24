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
import pizza_cheese.todo.domain.Topping;
import pizza_cheese.todo.util.JdbcTimeUtil;

@Repository
public class ToppingDao extends SqlDaoSupport {

    public ToppingDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        super(jdbc, resourceLoader, "classpath:sql/topping.sql");
    }

    public List<Topping> findAll(boolean activeOnly) {
        return jdbc.query(queries.require("findAll"), Map.of("activeOnly", activeOnly), RowMappers.forEntity(Topping.class));
    }

    public Optional<Topping> findById(UUID id) {
        List<Topping> toppings = jdbc.query(queries.require("findById"), Map.of("id", id), RowMappers.forEntity(Topping.class));
        return toppings.isEmpty() ? Optional.empty() : Optional.of(toppings.get(0));
    }

    public List<Topping> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query(queries.require("findByIds"), Map.of("ids", ids), RowMappers.forEntity(Topping.class));
    }

    // Chưa có id → tạo mới (INSERT)
    // Có id → cập nhật (UPDATE)
    public Topping save(Topping topping) {
        LocalDateTime now = LocalDateTime.now();

        if (topping.getId() == null) {
            topping.setId(UUID.randomUUID());
            topping.setCreatedAt(now);
            topping.setUpdatedAt(now);
            jdbc.update(queries.require("insert"), toParams(topping));
        } else {
            topping.setUpdatedAt(now);
            jdbc.update(queries.require("update"), toParams(topping));
        }

        return topping;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.require("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    // 👉 Chuyển object thành tham số SQL
    private MapSqlParameterSource toParams(Topping topping) {
        return new MapSqlParameterSource()
                .addValue("id", topping.getId())
                .addValue("name", topping.getName())
                .addValue("price", topping.getPrice())
                .addValue("isActive", topping.isActive())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(topping.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(topping.getUpdatedAt()));
    }
}
