package pizza_cheese.todo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.PaymentDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class CashierService {

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;
    private final CouponService couponService;

    public CashierService(
            OrderDao orderDao,
            PaymentDao paymentDao,
            UserDao userDao,
            CouponService couponService) {
        this.orderDao = orderDao;
        this.paymentDao = paymentDao;
        this.userDao = userDao;
        this.couponService = couponService;
    }

    public PageResponse<OrderResponse> getOrders(OrderStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = status == null ? orderDao.countAll() : orderDao.countByStatus(status);
        List<Order> orders = status == null
                ? orderDao.findPage(safePage, safeSize)
                : orderDao.findPageByStatus(status, safePage, safeSize);
        List<OrderResponse> content = orders.stream()
                .map(order -> enrich(
                        OrderResponse.summary(order, paymentDao.findLatestByOrderId(order.getId()).orElse(null)),
                        order))
                .toList();
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        Payment payment = paymentDao.findLatestByOrderId(orderId).orElse(null);
        return enrich(OrderResponse.from(order, payment), order);
    }

    @Transactional
    public OrderResponse confirmPayment(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        Payment payment = paymentDao.findLatestByOrderId(orderId)
                .orElseThrow(() -> ApiException.badRequest("Không tìm thấy thông tin thanh toán"));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            throw ApiException.badRequest("Không thể xác nhận thanh toán cho đơn đã hủy");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw ApiException.badRequest("Đơn đã được thanh toán");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("Không thể xác nhận thanh toán ở trạng thái hiện tại");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now);
        paymentDao.updateStatus(payment);

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderDao.updateStatus(orderId, OrderStatus.CONFIRMED);
            orderDao.insertStatusHistory(orderId, OrderStatus.CONFIRMED, staffId, "Thu ngan xac nhan thanh toan");
            order.setStatus(OrderStatus.CONFIRMED);
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            orderDao.insertStatusHistory(orderId, OrderStatus.CONFIRMED, staffId, "Thu ngan xac nhan thu tien");
        } else {
            throw ApiException.badRequest("Không thể xác nhận thanh toán ở trạng thái đơn hiện tại");
        }

        return enrich(OrderResponse.from(order, payment), order);
    }

    @Transactional
    public OrderResponse cancelOrder(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        Payment payment = paymentDao.findLatestByOrderId(orderId).orElse(null);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return enrich(OrderResponse.from(order, payment), order);
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            failPendingPayment(payment);
            orderDao.updateStatus(orderId, OrderStatus.CANCELLED);
            orderDao.insertStatusHistory(orderId, OrderStatus.CANCELLED, staffId, "Thu ngan huy don chua thanh toan");
            order.setStatus(OrderStatus.CANCELLED);
            return enrich(OrderResponse.from(order, payment), order);
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
            orderDao.insertStatusHistory(orderId, OrderStatus.CANCELLED, staffId, "Thu ngan huy don");
            order.setStatus(OrderStatus.CANCELLED);
            return enrich(OrderResponse.from(order, payment), order);
        }

        throw ApiException.badRequest("Không thể hủy đơn ở trạng thái hiện tại");
    }

    private void failPendingPayment(Payment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.setStatus(PaymentStatus.FAILED);
        paymentDao.updateStatus(payment);
    }

    private OrderResponse enrich(OrderResponse response, Order order) {
        userDao.findById(order.getUserId()).ifPresent(user -> applyCustomerInfo(response, user));
        if (order.getCouponId() != null) {
            response.setCouponCode(couponService.findCodeById(order.getCouponId()));
        }
        return response;
    }

    private void applyCustomerInfo(OrderResponse response, User user) {
        response.setCustomerName(user.getFullName());
        response.setCustomerEmail(user.getEmail());
    }

    private UUID resolveUserId(String userEmail) {
        return userDao.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }
}
