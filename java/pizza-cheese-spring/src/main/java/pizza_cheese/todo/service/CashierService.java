package pizza_cheese.todo.service;

import java.time.LocalDate;
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
import pizza_cheese.todo.realtime.OrderRealtimePublisher;

@Service
public class CashierService {

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;
    private final OrderResponseEnricher orderResponseEnricher;
    private final OrderRealtimePublisher orderRealtimePublisher;

    public CashierService(
            OrderDao orderDao,
            PaymentDao paymentDao,
            UserDao userDao,
            OrderResponseEnricher orderResponseEnricher,
            OrderRealtimePublisher orderRealtimePublisher) {
        this.orderDao = orderDao;
        this.paymentDao = paymentDao;
        this.userDao = userDao;
        this.orderResponseEnricher = orderResponseEnricher;
        this.orderRealtimePublisher = orderRealtimePublisher;
    }

    public PageResponse<OrderResponse> getOrders(OrderStatus status, int page, int size) {
        return getOrders(status, null, null, page, size);
    }

    public PageResponse<OrderResponse> getOrders(
            OrderStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        if (from == null && to == null && status == null) {
            long total = orderDao.countAll();
            List<Order> orders = orderDao.findPage(safePage, safeSize);
            return PageResponse.of(
                    orderResponseEnricher.toListResponses(orders, false, false),
                    safePage,
                    safeSize,
                    total);
        }

        if (from == null && to == null && status != null) {
            long total = orderDao.countByStatus(status);
            List<Order> orders = orderDao.findPageByStatus(status, safePage, safeSize);
            return PageResponse.of(
                    orderResponseEnricher.toListResponses(orders, false, false),
                    safePage,
                    safeSize,
                    total);
        }

        long total = orderDao.countFiltered(status, from, to);
        List<Order> orders = orderDao.findPageFiltered(status, from, to, safePage, safeSize);
        return PageResponse.of(
                orderResponseEnricher.toListResponses(orders, false, false),
                safePage,
                safeSize,
                total);
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        return orderResponseEnricher.toDetailResponse(order, false);
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
            orderRealtimePublisher.publishKitchen(order);
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            orderDao.insertStatusHistory(orderId, OrderStatus.CONFIRMED, staffId, "Thu ngan xac nhan thu tien");
        } else {
            throw ApiException.badRequest("Không thể xác nhận thanh toán ở trạng thái đơn hiện tại");
        }

        return orderResponseEnricher.toDetailResponse(order, false);
    }

    @Transactional
    public OrderResponse cancelOrder(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        Payment payment = paymentDao.findLatestByOrderId(orderId).orElse(null);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return orderResponseEnricher.toDetailResponse(order, false);
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            failPendingPayment(payment);
            orderDao.updateStatus(orderId, OrderStatus.CANCELLED);
            orderDao.insertStatusHistory(orderId, OrderStatus.CANCELLED, staffId, "Nhan vien huy don chua thanh toan");
            order.setStatus(OrderStatus.CANCELLED);
            return orderResponseEnricher.toDetailResponse(order, false);
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
            orderDao.insertStatusHistory(orderId, OrderStatus.CANCELLED, staffId, "Nhan vien huy don");
            order.setStatus(OrderStatus.CANCELLED);
            return orderResponseEnricher.toDetailResponse(order, false);
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

    private UUID resolveUserId(String userEmail) {
        return userDao.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }
}
