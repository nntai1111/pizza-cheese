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
import pizza_cheese.todo.domain.Coupon;
import pizza_cheese.todo.domain.DiscountType;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class CouponDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;

    public CouponDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/coupon.sql"));
    }

    public List<Coupon> findAll(boolean activeOnly) {
        return jdbc.query(queries.get("findAll"), Map.of("activeOnly", activeOnly), RowMappers.forEntity(Coupon.class));
    }

    public Optional<Coupon> findById(UUID id) {
        List<Coupon> coupons = jdbc.query(queries.get("findById"), Map.of("id", id), RowMappers.forEntity(Coupon.class));
        return coupons.isEmpty() ? Optional.empty() : Optional.of(coupons.get(0));
    }

    public Optional<Coupon> findByCode(String code) {
        List<Coupon> coupons = jdbc.query(
                queries.get("findByCode"),
                Map.of("code", code),
                RowMappers.forEntity(Coupon.class));
        return coupons.isEmpty() ? Optional.empty() : Optional.of(coupons.get(0));
    }

    public long count() {
        Long count = jdbc.queryForObject(queries.get("count"), Map.of(), Long.class);
        return count != null ? count : 0L;
    }

    public Coupon save(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();

        if (coupon.getId() == null) {
            coupon.setId(UUID.randomUUID());
            coupon.setCreatedAt(now);
            coupon.setUpdatedAt(now);
            jdbc.update(queries.get("insert"), toParams(coupon));
        } else {
            coupon.setUpdatedAt(now);
            jdbc.update(queries.get("update"), toParams(coupon));
        }

        return coupon;
    }

    public void deactivate(UUID id) {
        jdbc.update(queries.get("deactivate"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    public boolean incrementUsedCount(UUID id) {
        int updated = jdbc.update(queries.get("incrementUsedCount"), new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
        return updated > 0;
    }

    public void insertUsage(UUID couponId, UUID userId, UUID orderId) {
        jdbc.update(queries.get("insertUsage"), new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("couponId", couponId)
                .addValue("userId", userId)
                .addValue("orderId", orderId)
                .addValue("usedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    public int countUsagesByUser(UUID couponId, UUID userId) {
        Integer count = jdbc.queryForObject(
                queries.get("countUsagesByUser"),
                Map.of("couponId", couponId, "userId", userId),
                Integer.class);
        return count != null ? count : 0;
    }

    private MapSqlParameterSource toParams(Coupon coupon) {
        return new MapSqlParameterSource()
                .addValue("id", coupon.getId())
                .addValue("code", coupon.getCode())
                .addValue("description", coupon.getDescription())
                .addValue("discountType", coupon.getDiscountType().getCode())
                .addValue("discountValue", coupon.getDiscountValue())
                .addValue("minOrderValue", coupon.getMinOrderValue())
                .addValue("maxDiscount", coupon.getMaxDiscount())
                .addValue("startDate", JdbcTimeUtil.toTimestamp(coupon.getStartDate()))
                .addValue("endDate", JdbcTimeUtil.toTimestamp(coupon.getEndDate()))
                .addValue("usageLimit", coupon.getUsageLimit())
                .addValue("usedCount", coupon.getUsedCount())
                .addValue("perUserLimit", coupon.getPerUserLimit())
                .addValue("active", coupon.isActive())
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(coupon.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(coupon.getUpdatedAt()));
    }
}
