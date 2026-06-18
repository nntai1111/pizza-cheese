package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order {

    private UUID id;
    private String orderCode;
    private UUID userId;
    private UUID addressId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private UUID couponId;
    private PaymentMethod paymentMethodSelected;
    private String note;
    private Instant estimatedDeliveryTime;
    private UUID kitchenStaffId;
    private UUID deliveryStaffId;
    private String deliveryAddressSnapshot;
    private Instant createdAt;
    private Instant updatedAt;

    private List<OrderItem> items = new ArrayList<>();
    private Payment latestPayment;
}
