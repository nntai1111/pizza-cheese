package pizza_cheese.todo.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import pizza_cheese.todo.dao.CartDao;
import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.PaymentDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Cart;
import pizza_cheese.todo.domain.CartItem;
import pizza_cheese.todo.domain.CartItemComboLine;
import pizza_cheese.todo.domain.CartItemTopping;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderItem;
import pizza_cheese.todo.domain.OrderItemComboLine;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.dto.request.CreateOrderRequest;
import pizza_cheese.todo.dto.request.DeliveryAddressRequest;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final CartDao cartDao;
    private final UserDao userDao;
    private final VnPayService vnPayService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public OrderService(
            OrderDao orderDao,
            PaymentDao paymentDao,
            CartDao cartDao,
            UserDao userDao,
            VnPayService vnPayService,
            ObjectMapper objectMapper) {
        this.orderDao = orderDao;
        this.paymentDao = paymentDao;
        this.cartDao = cartDao;
        this.userDao = userDao;
        this.vnPayService = vnPayService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request, HttpServletRequest httpRequest) {
        UUID userId = resolveUserId(userEmail);
        Cart cart = cartDao.findByUserId(userId)
                .filter(existing -> existing.getItems() != null && !existing.getItems().isEmpty())
                .orElseThrow(() -> ApiException.badRequest("Giỏ hàng trống, không thể đặt hàng"));

        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (paymentMethod != PaymentMethod.VNPAY && paymentMethod != PaymentMethod.COD) {
            throw new IllegalArgumentException("Phương thức thanh toán chưa được hỗ trợ");
        }

        List<CartItem> selectedItems = resolveSelectedCartItems(cart, request.getCartItemIds());

        BigDecimal totalAmount = selectedItems.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        Instant now = Instant.now();
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderCode(generateOrderCode());
        order.setUserId(userId);
        order.setStatus(paymentMethod == PaymentMethod.VNPAY ? OrderStatus.PENDING_PAYMENT : OrderStatus.CONFIRMED);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setPaymentMethodSelected(paymentMethod);
        order.setNote(request.getNote());
        order.setDeliveryAddressSnapshot(toAddressSnapshot(request.getDeliveryAddress()));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        orderDao.insert(order);
        orderDao.insertStatusHistory(
                order.getId(),
                order.getStatus(),
                userId,
                paymentMethod == PaymentMethod.VNPAY ? "Tao don cho thanh toan VNPay" : "Tao don COD");

        snapshotCartItems(selectedItems, order.getId());

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(order.getId());
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(finalAmount);
        payment.setTransactionId(order.getOrderCode());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentDao.insert(payment);

        if (paymentMethod == PaymentMethod.VNPAY) {
            String clientIp = VnPayService.extractClientIp(httpRequest);
            vnPayService.createPaymentUrl(order, payment, clientIp);
        }

        for (CartItem selectedItem : selectedItems) {
            cartDao.deleteItemById(selectedItem.getId());
        }
        cartDao.touchUpdatedAt(cart.getId());

        Order savedOrder = orderDao.findById(order.getId()).orElse(order);
        Payment latestPayment = paymentDao.findLatestByOrderId(order.getId()).orElse(payment);
        return OrderResponse.from(savedOrder, latestPayment);
    }

    public OrderResponse getOrder(String userEmail, UUID orderId) {
        UUID userId = resolveUserId(userEmail);
        Order order = orderDao.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        Payment payment = paymentDao.findLatestByOrderId(orderId).orElse(null);
        return OrderResponse.from(order, payment);
    }

    public OrderResponse getOrderByPaymentTxnRef(String userEmail, String txnRef) {
        Payment payment = paymentDao.findByTransactionId(txnRef)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));
        return getOrder(userEmail, payment.getOrderId());
    }

    public List<OrderResponse> getMyOrders(String userEmail) {
        UUID userId = resolveUserId(userEmail);
        return orderDao.findByUserId(userId).stream()
                .map(order -> OrderResponse.summary(order, paymentDao.findLatestByOrderId(order.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(String userEmail, UUID orderId) {
        UUID userId = resolveUserId(userEmail);
        Order order = orderDao.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        Payment payment = paymentDao.findLatestByOrderId(orderId).orElse(null);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderResponse.from(order, payment);
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            failPendingPayment(payment);
            orderDao.updateStatus(orderId, OrderStatus.CANCELLED);
            orderDao.insertStatusHistory(orderId, OrderStatus.CANCELLED, userId, "Khach huy don truoc thanh toan");
            order.setStatus(OrderStatus.CANCELLED);
            return OrderResponse.from(order, payment);
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.badRequest("Không thể hủy đơn ở trạng thái hiện tại");
        }

        if (payment != null && payment.getStatus() == PaymentStatus.PAID
                && order.getPaymentMethodSelected() == PaymentMethod.VNPAY) {
            throw ApiException.badRequest("Đơn đã thanh toán online, không thể hủy");
        }

        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            failPendingPayment(payment);
            orderDao.updateStatus(orderId, OrderStatus.CANCELLED);
            orderDao.insertStatusHistory(orderId, OrderStatus.CANCELLED, userId, "Khach huy don");
            order.setStatus(OrderStatus.CANCELLED);
            return OrderResponse.from(order, payment);
        }

        throw ApiException.badRequest("Không thể hủy đơn ở trạng thái hiện tại");
    }

    private List<CartItem> resolveSelectedCartItems(Cart cart, List<UUID> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw ApiException.badRequest("Chưa chọn món để đặt hàng");
        }

        Set<UUID> cartItemIdSet = cart.getItems().stream()
                .map(CartItem::getId)
                .collect(Collectors.toCollection(HashSet::new));

        Set<UUID> requestedIds = new HashSet<>(cartItemIds);
        if (requestedIds.size() != cartItemIds.size()) {
            throw new IllegalArgumentException("Món trong giỏ hàng bị trùng lặp");
        }

        for (UUID cartItemId : requestedIds) {
            if (!cartItemIdSet.contains(cartItemId)) {
                throw ApiException.notFound("Không tìm thấy món trong giỏ hàng");
            }
        }

        return cart.getItems().stream()
                .filter(item -> requestedIds.contains(item.getId()))
                .toList();
    }

    private void snapshotCartItems(List<CartItem> cartItems, UUID orderId) {
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderId);
            orderItem.setItemType(cartItem.getItemType());
            orderItem.setPizzaId(cartItem.getPizzaId());
            orderItem.setPizzaVariantId(cartItem.getPizzaVariantId());
            orderItem.setComboId(cartItem.getComboId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setLineTotal(cartItem.getLineTotal());

            OrderItem savedItem = orderDao.insertItem(orderItem);

            for (CartItemTopping topping : cartItem.getToppings()) {
                orderDao.insertItemTopping(savedItem.getId(), topping.getToppingId(), topping.getPrice());
            }

            for (CartItemComboLine comboLine : cartItem.getComboLines()) {
                OrderItemComboLine line = new OrderItemComboLine();
                line.setOrderItemId(savedItem.getId());
                line.setPizzaId(comboLine.getPizzaId());
                line.setPizzaVariantId(comboLine.getPizzaVariantId());
                line.setQuantity(comboLine.getQuantity());
                line.setPizzaName(comboLine.getPizzaName());
                line.setPizzaSize(comboLine.getPizzaSize());
                orderDao.insertComboLine(line);
            }
        }
    }

    private String generateOrderCode() {
        String datePart = ORDER_CODE_DATE.format(Instant.now().atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = "PC" + datePart + String.format("%04d", secureRandom.nextInt(10000));
            if (!orderDao.existsByOrderCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể tạo mã đơn hàng");
    }

    private String toAddressSnapshot(DeliveryAddressRequest address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không hợp lệ");
        }
    }

    private UUID resolveUserId(String userEmail) {
        return userDao.findByEmail(userEmail)
                .map(pizza_cheese.todo.domain.User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }

    private void failPendingPayment(Payment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.setStatus(PaymentStatus.FAILED);
        paymentDao.updateStatus(payment);
    }
}
