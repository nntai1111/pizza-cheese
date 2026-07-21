package pizza_cheese.todo.dao;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pizza_cheese.todo.dao.mapper.RowMappers;
import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.dto.response.PaymentResponse;
import pizza_cheese.todo.util.JdbcTimeUtil;
import pizza_cheese.todo.util.SqlLoader;

@Repository
public class PaymentDao {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, String> queries;

    public PaymentDao(NamedParameterJdbcTemplate jdbc, ResourceLoader resourceLoader) throws IOException {
        this.jdbc = jdbc;
        this.queries = SqlLoader.load(resourceLoader.getResource("classpath:sql/payment.sql"));
    }

    public void insert(Payment payment) {
        jdbc.update(queries.get("insert"), new MapSqlParameterSource()
                .addValue("id", payment.getId())
                .addValue("orderId", payment.getOrderId())
                .addValue("paymentMethod", payment.getPaymentMethod().getCode())
                .addValue("amount", payment.getAmount())
                .addValue("transactionId", payment.getTransactionId())
                .addValue("status", payment.getStatus().getCode())
                .addValue("paymentUrl", payment.getPaymentUrl())
                .addValue("callbackData", payment.getCallbackData() != null ? payment.getCallbackData() : "{}")
                .addValue("paidAt", JdbcTimeUtil.toTimestamp(payment.getPaidAt()))
                .addValue("createdAt", JdbcTimeUtil.toTimestamp(payment.getCreatedAt()))
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(payment.getUpdatedAt())));
    }

    public Optional<Payment> findById(UUID id) {
        List<Payment> payments = jdbc.query(queries.get("findById"), Map.of("id", id), RowMappers.forEntity(Payment.class));
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    public Optional<Payment> findLatestByOrderId(UUID orderId) {
        List<Payment> payments = jdbc.query(
                queries.get("findLatestByOrderId"),
                Map.of("orderId", orderId),
                RowMappers.forEntity(Payment.class));
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    public Map<UUID, Payment> findLatestByOrderIds(Collection<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<Payment> payments = jdbc.query(
                queries.get("findLatestByOrderIds"),
                Map.of("orderIds", orderIds),
                RowMappers.forEntity(Payment.class));
        return payments.stream()
                .collect(Collectors.toMap(Payment::getOrderId, Function.identity(), (left, right) -> left));
    }

    public Optional<Payment> findByTransactionId(String transactionId) {
        List<Payment> payments = jdbc.query(
                queries.get("findByTransactionId"),
                Map.of("transactionId", transactionId),
                RowMappers.forEntity(Payment.class));
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    public void updateStatus(Payment payment) {
        payment.setUpdatedAt(LocalDateTime.now());
        jdbc.update(queries.get("updateStatus"), new MapSqlParameterSource()
                .addValue("id", payment.getId())
                .addValue("status", payment.getStatus().getCode())
                .addValue("callbackData", payment.getCallbackData())
                .addValue("paidAt", JdbcTimeUtil.toTimestamp(payment.getPaidAt()))
                .addValue("transactionId", payment.getTransactionId())
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(payment.getUpdatedAt())));
    }

    public void updatePaymentUrl(UUID paymentId, String paymentUrl) {
        jdbc.update(queries.get("updatePaymentUrl"), new MapSqlParameterSource()
                .addValue("id", paymentId)
                .addValue("paymentUrl", paymentUrl)
                .addValue("updatedAt", JdbcTimeUtil.toTimestamp(LocalDateTime.now())));
    }

    public long countFiltered(
            PaymentStatus status,
            PaymentMethod method,
            LocalDateTime from,
            LocalDateTime to) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = queries.get("countFilteredBase") + buildPaymentFilterSql(status, method, from, to, params);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    public List<PaymentResponse> findPageFiltered(
            PaymentStatus status,
            PaymentMethod method,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = queries.get("findPageFilteredBase")
                + buildPaymentFilterSql(status, method, from, to, params)
                + " ORDER BY p.created_at DESC LIMIT :limit OFFSET :offset";
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);
        return jdbc.query(sql, params, (rs, rowNum) -> mapPaymentResponse(rs));
    }

    public Optional<PaymentResponse> findAdminById(UUID id) {
        List<PaymentResponse> payments = jdbc.query(
                queries.get("findAdminById"),
                Map.of("id", id),
                (rs, rowNum) -> mapPaymentResponse(rs));
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    private String buildPaymentFilterSql(
            PaymentStatus status,
            PaymentMethod method,
            LocalDateTime from,
            LocalDateTime to,
            MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null) {
            where.append(" AND p.status = :status");
            params.addValue("status", status.getCode());
        }
        if (method != null) {
            where.append(" AND p.payment_method = :method");
            params.addValue("method", method.getCode());
        }
        if (from != null) {
            where.append(" AND p.created_at >= :from");
            params.addValue("from", JdbcTimeUtil.toTimestamp(from));
        }
        if (to != null) {
            where.append(" AND p.created_at < :to");
            params.addValue("to", JdbcTimeUtil.toTimestamp(to));
        }
        return where.toString();
    }

    private PaymentResponse mapPaymentResponse(java.sql.ResultSet rs) throws java.sql.SQLException {
        return PaymentResponse.of(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getString("order_code"),
                PaymentMethod.fromCode(rs.getInt("payment_method")),
                rs.getBigDecimal("amount"),
                rs.getString("transaction_id"),
                PaymentStatus.fromCode(rs.getInt("status")),
                toLocalDateTime(rs.getTimestamp("paid_at")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
