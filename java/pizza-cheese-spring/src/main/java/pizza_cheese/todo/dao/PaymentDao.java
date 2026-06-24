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
import pizza_cheese.todo.domain.Payment;
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
                .addValue("paymentMethod", payment.getPaymentMethod().name())
                .addValue("amount", payment.getAmount())
                .addValue("transactionId", payment.getTransactionId())
                .addValue("status", payment.getStatus().name())
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
                .addValue("status", payment.getStatus().name())
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
}
