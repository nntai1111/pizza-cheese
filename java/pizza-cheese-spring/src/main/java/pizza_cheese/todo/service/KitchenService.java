package pizza_cheese.todo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class KitchenService {

    private final OrderDao orderDao;
    private final UserDao userDao;
    private final OrderResponseEnricher orderResponseEnricher;

    public KitchenService(
            OrderDao orderDao,
            UserDao userDao,
            OrderResponseEnricher orderResponseEnricher) {
        this.orderDao = orderDao;
        this.userDao = userDao;
        this.orderResponseEnricher = orderResponseEnricher;
    }

    public PageResponse<OrderResponse> getOrders(OrderStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = status == null ? orderDao.countAll() : orderDao.countByStatus(status);
        List<Order> orders = status == null
                ? orderDao.findPage(safePage, safeSize)
                : orderDao.findPageByStatus(status, safePage, safeSize);
        List<OrderResponse> content = orderResponseEnricher.toListResponses(orders, true, true);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        return orderResponseEnricher.toDetailResponse(order, true);
    }

    @Transactional
    public OrderResponse startPreparing(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.PREPARING) {
            if (staffId.equals(order.getKitchenStaffId())) {
                return orderResponseEnricher.toDetailResponse(order, true);
            }
            throw ApiException.badRequest("Đơn đang được chế biến bởi nhân viên khác");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.badRequest("Chỉ có thể nhận đơn ở trạng thái đã xác nhận");
        }

        if (!orderDao.claimForPreparing(orderId, staffId)) {
            throw ApiException.conflict("Đơn đã được nhân viên bếp khác nhận");
        }

        orderDao.insertStatusHistory(orderId, OrderStatus.PREPARING, staffId, "Bep bat dau che bien");
        order.setStatus(OrderStatus.PREPARING);
        order.setKitchenStaffId(staffId);

        return orderResponseEnricher.toDetailResponse(order, true);
    }

    @Transactional
    public OrderResponse markReady(String staffEmail, UUID orderId) {
        UUID staffId = resolveUserId(staffEmail);
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.READY) {
            return orderResponseEnricher.toDetailResponse(order, true);
        }

        if (order.getStatus() != OrderStatus.PREPARING) {
            throw ApiException.badRequest("Chỉ có thể hoàn thành đơn đang chế biến");
        }

        if (!orderDao.updateStatusIfCurrent(orderId, OrderStatus.PREPARING, OrderStatus.READY)) {
            throw ApiException.conflict("Không thể cập nhật trạng thái đơn, vui lòng thử lại");
        }

        orderDao.insertStatusHistory(orderId, OrderStatus.READY, staffId, "Bep hoan thanh mon");
        order.setStatus(OrderStatus.READY);

        return orderResponseEnricher.toDetailResponse(order, true);
    }

    private UUID resolveUserId(String userEmail) {
        return userDao.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }
}
