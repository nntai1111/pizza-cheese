package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderItem;
import pizza_cheese.todo.domain.OrderItemComboLine;
import pizza_cheese.todo.domain.OrderItemTopping;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.domain.LineItemType;
import pizza_cheese.todo.domain.PizzaSize;

@Getter
@Setter
public class OrderResponse {

    private UUID id;
    private String orderCode;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String note;
    private String deliveryAddressSnapshot;
    private String paymentUrl;
    private String paymentTxnRef;
    private Instant createdAt;
    private Instant paidAt;
    private List<OrderItemResponse> items;
    private String customerName;
    private String customerEmail;

    public static OrderResponse from(Order order, Payment payment) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderCode(order.getOrderCode());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethodSelected());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setFinalAmount(order.getFinalAmount());
        response.setNote(order.getNote());
        response.setDeliveryAddressSnapshot(order.getDeliveryAddressSnapshot());
        response.setCreatedAt(order.getCreatedAt());
        response.setItems(order.getItems().stream().map(OrderItemResponse::from).toList());

        if (payment != null) {
            response.setPaymentStatus(payment.getStatus());
            response.setPaymentUrl(payment.getPaymentUrl());
            response.setPaymentTxnRef(payment.getTransactionId());
            response.setPaidAt(payment.getPaidAt());
        }

        return response;
    }

    public static OrderResponse summary(Order order, Payment payment) {
        OrderResponse response = from(order, payment);
        response.setItems(null);
        return response;
    }

    @Getter
    @Setter
    public static class OrderItemResponse {

        private UUID id;
        private LineItemType itemType;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String pizzaName;
        private PizzaSize pizzaSize;
        private String pizzaImageUrl;
        private String comboName;
        private String comboImageUrl;
        private List<OrderItemToppingResponse> toppings;
        private List<OrderItemComboLineResponse> comboLines;

        public static OrderItemResponse from(OrderItem item) {
            OrderItemResponse response = new OrderItemResponse();
            response.setId(item.getId());
            response.setItemType(item.getItemType());
            response.setQuantity(item.getQuantity());
            response.setUnitPrice(item.getUnitPrice());
            response.setLineTotal(item.getLineTotal());
            response.setPizzaName(item.getPizzaName());
            response.setPizzaSize(item.getPizzaSize());
            response.setPizzaImageUrl(item.getPizzaImageUrl());
            response.setComboName(item.getComboName());
            response.setComboImageUrl(item.getComboImageUrl());
            response.setToppings(item.getToppings().stream().map(OrderItemToppingResponse::from).toList());
            response.setComboLines(item.getComboLines().stream().map(OrderItemComboLineResponse::from).toList());
            return response;
        }
    }

    @Getter
    @Setter
    public static class OrderItemToppingResponse {

        private UUID toppingId;
        private String toppingName;
        private BigDecimal price;

        public static OrderItemToppingResponse from(OrderItemTopping topping) {
            OrderItemToppingResponse response = new OrderItemToppingResponse();
            response.setToppingId(topping.getToppingId());
            response.setToppingName(topping.getToppingName());
            response.setPrice(topping.getPrice());
            return response;
        }
    }

    @Getter
    @Setter
    public static class OrderItemComboLineResponse {

        private int quantity;
        private String pizzaName;
        private PizzaSize pizzaSize;

        public static OrderItemComboLineResponse from(OrderItemComboLine line) {
            OrderItemComboLineResponse response = new OrderItemComboLineResponse();
            response.setQuantity(line.getQuantity());
            response.setPizzaName(line.getPizzaName());
            response.setPizzaSize(line.getPizzaSize());
            return response;
        }
    }
}
