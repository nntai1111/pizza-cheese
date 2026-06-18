package pizza_cheese.todo.dao.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;

public class PaymentRowMapper implements RowMapper<Payment> {

    @Override
    public Payment mapRow(ResultSet rs, int rowNum) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getObject("id", UUID.class));
        payment.setOrderId(rs.getObject("order_id", UUID.class));
        payment.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        payment.setAmount(rs.getObject("amount", BigDecimal.class));
        payment.setTransactionId(rs.getString("transaction_id"));
        payment.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        payment.setPaymentUrl(rs.getString("payment_url"));
        payment.setCallbackData(rs.getString("callback_data"));
        payment.setPaidAt(toInstant(rs.getObject("paid_at", OffsetDateTime.class)));
        payment.setCreatedAt(toInstant(rs.getObject("created_at", OffsetDateTime.class)));
        payment.setUpdatedAt(toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
        return payment;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
