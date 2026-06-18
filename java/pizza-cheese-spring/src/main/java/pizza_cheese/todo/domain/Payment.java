package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Payment {

    private UUID id;
    private UUID orderId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String transactionId;
    private PaymentStatus status;
    private String paymentUrl;
    private String callbackData;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;
}
