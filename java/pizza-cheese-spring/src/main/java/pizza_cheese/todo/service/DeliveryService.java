package pizza_cheese.todo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.PaymentDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.exception.ApiException;
import pizza_cheese.todo.realtime.OrderRealtimePublisher;

@Service
public class DeliveryService {

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;
    private final OrderResponseEnricher orderResponseEnricher;
    private final OrderRealtimePublisher orderRealtimePublisher;

    public DeliveryService(
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

    public PageResponse<OrderResponse> getOrders(
            String staffEmail,
            OrderStatus status,
            int page,
            int size,
            LocalDateTime updatedSince) {
        UUID staffId = resolveUserId(staffEmail);
        boolean admin = isAdmin();

        if (updatedSince != null) {
            List<Order> changes = orderDao.findUpdatedSince(updatedSince, 200).stream()
                    .filter(order -> isDeliveryChangeVisible(order, staffId, status, admin))
                    .toList();
            long total = countVisible(status, staffId, admin);
            List<OrderResponse> content = orderResponseEnricher.toListResponses(changes, true, false, true);
            return PageResponse.incremental(content, total);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = countVisible(status, staffId, admin);
        List<Order> orders = findVisiblePage(status, staffId, admin, safePage, safeSize);
        List<OrderResponse> content = orderResponseEnricher.toListResponses(orders, true, false, true);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public OrderResponse getOrder(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        assertCanView(order, staffId);
        return orderResponseEnricher.toDetailResponse(order, false, true);
    }

    @Transactional
    public OrderResponse startDelivery(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            if (staffId.equals(order.getDeliveryStaffId())) {
                return orderResponseEnricher.toDetailResponse(order, false, true);
            }
            throw ApiException.badRequest("Đơn đang được giao bởi nhân viên khác");
        }

        if (order.getStatus() != OrderStatus.READY) {
            throw ApiException.badRequest("Chỉ có thể nhận đơn ở trạng thái sẵn sàng giao");
        }

        if (!orderDao.claimForDelivery(orderId, staffId)) {
            throw ApiException.conflict("Đơn đã được nhân viên giao hàng khác nhận");
        }

        orderDao.insertStatusHistory(orderId, OrderStatus.OUT_FOR_DELIVERY, staffId, "Shipper bat dau giao hang");
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDeliveryStaffId(staffId);

        OrderResponse response = orderResponseEnricher.toDetailResponse(order, false, true);
        orderRealtimePublisher.publishDelivery(order);
        return response;
    }

    @Transactional
    public OrderResponse markDelivered(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            if (!isAdmin() && order.getDeliveryStaffId() != null && !staffId.equals(order.getDeliveryStaffId())) {
                throw ApiException.forbidden("Không có quyền xem đơn của shipper khác");
            }
            markPaymentPaidIfPending(orderId);
            return orderResponseEnricher.toDetailResponse(order, false, true);
        }

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw ApiException.badRequest("Chỉ có thể hoàn thành đơn đang giao");
        }

        if (order.getDeliveryStaffId() != null && !staffId.equals(order.getDeliveryStaffId())) {
            throw ApiException.badRequest("Chỉ nhân viên đang giao đơn này mới có thể đánh dấu đã giao");
        }

        if (!orderDao.updateStatusIfCurrent(orderId, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.COMPLETED)) {
            throw ApiException.conflict("Không thể cập nhật trạng thái đơn, vui lòng thử lại");
        }

        orderDao.insertStatusHistory(orderId, OrderStatus.COMPLETED, staffId, "Shipper giao hang thanh cong");
        order.setStatus(OrderStatus.COMPLETED);
        markPaymentPaidIfPending(orderId);

        OrderResponse response = orderResponseEnricher.toDetailResponse(order, false, true);
        orderRealtimePublisher.publishDelivery(order);
        return response;
    }

    private List<Order> findVisiblePage(OrderStatus status, UUID staffId, boolean admin, int page, int size) {
        if (admin) {
            return status == null
                    ? orderDao.findPage(page, size)
                    : orderDao.findPageByStatus(status, page, size);
        }
        if (status == null) {
            return orderDao.findPageForDeliveryStaff(staffId, page, size);
        }
        if (status == OrderStatus.READY) {
            return orderDao.findPageByStatus(status, page, size);
        }
        if (status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.COMPLETED) {
            return orderDao.findPageByStatusAndDeliveryStaff(status, staffId, page, size);
        }
        return List.of();
    }

    private long countVisible(OrderStatus status, UUID staffId, boolean admin) {
        if (admin) {
            return status == null ? orderDao.countAll() : orderDao.countByStatus(status);
        }
        if (status == null) {
            return orderDao.countForDeliveryStaff(staffId);
        }
        if (status == OrderStatus.READY) {
            return orderDao.countByStatus(status);
        }
        if (status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.COMPLETED) {
            return orderDao.countByStatusAndDeliveryStaff(status, staffId);
        }
        return 0L;
    }

    /**
     * Incremental: shared READY pool for everyone; claimed orders only for owner;
     * also emit claimed-by-others updates so peers can drop them from READY.
     */
    private boolean isDeliveryChangeVisible(Order order, UUID staffId, OrderStatus filter, boolean admin) {
        if (admin) {
            return true;
        }
        if (order.getStatus() == OrderStatus.READY) {
            return filter == null || filter == OrderStatus.READY;
        }
        if (staffId.equals(order.getDeliveryStaffId())) {
            return true;
        }
        // Peer claimed/finished: so READY / ALL can remove the order from shared pool
        return filter == null || filter == OrderStatus.READY;
    }

    private void assertCanView(Order order, UUID staffId) {
        if (isAdmin() || order.getStatus() == OrderStatus.READY) {
            return;
        }
        if (order.getDeliveryStaffId() != null && !staffId.equals(order.getDeliveryStaffId())) {
            throw ApiException.forbidden("Không có quyền xem đơn của shipper khác");
        }
    }

    private void markPaymentPaidIfPending(UUID orderId) {
        Payment payment = paymentDao.findLatestByOrderId(orderId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentDao.updateStatus(payment);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private UUID resolveUserId(String userEmail) {
        return userDao.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }
}
