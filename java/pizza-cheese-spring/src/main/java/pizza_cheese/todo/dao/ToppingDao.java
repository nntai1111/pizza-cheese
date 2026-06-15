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

import pizza_cheese.todo.dao.mapper.ToppingRowMapper;
import pizza_cheese.todo.domain.Topping;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class ToppingDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;
    private final ToppingRowMapper rowMapper = new ToppingRowMapper();

    public ToppingDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/topping.sql"));
    }

    public List<Topping> findAll(boolean activeOnly) {
        return jdbc.query(queries.get("findAll"), Map.of("activeOnly", activeOnly), rowMapper);
    }

    public Optional<Topping> findById(UUID id) {
        List<Topping> toppings = jdbc.query(queries.get("findById"), Map.of("id", id), rowMapper);
        return toppings.isEmpty() ? Optional.empty() : Optional.of(toppings.get(0));
    }

    public List<Topping> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbc.query(queries.get("findByIds"), Map.of("ids", ids), rowMapper);
    }

    public Topping save(Topping topping) {
        Instant now = Instant.now();

        if (topping.getId() == null) {
            topping.setId(UUID.randomUUID());
            topping.setCreatedAt(now);
            topping.setUpdatedAt(now);
            jdbc.update(queries.get("insert"), toParams(topping));
        } else {
            topping.setUpdatedAt(now);
            jdbc.update(queries.get("update"), toParams(topping));
        }

        return topping;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.get("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(Instant.now())));
    }

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
