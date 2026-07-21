package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.dto.CodedEnumValue;

@Getter
@Setter
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private String orderCode;
    private CodedEnumValue paymentMethod;
    private BigDecimal amount;
    private String transactionId;
    private CodedEnumValue status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponse of(
            UUID id,
            UUID orderId,
            String orderCode,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String transactionId,
            PaymentStatus status,
            LocalDateTime paidAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        PaymentResponse response = new PaymentResponse();
        response.setId(id);
        response.setOrderId(orderId);
        response.setOrderCode(orderCode);
        response.setPaymentMethod(CodedEnumValue.from(paymentMethod));
        response.setAmount(amount);
        response.setTransactionId(transactionId);
        response.setStatus(CodedEnumValue.from(status));
        response.setPaidAt(paidAt);
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(updatedAt);
        return response;
    }
}
