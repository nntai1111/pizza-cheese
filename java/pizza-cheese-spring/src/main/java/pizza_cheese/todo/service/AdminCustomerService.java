package pizza_cheese.todo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class AdminCustomerService {

    private final UserDao userDao;
    private final OrderDao orderDao;
    private final OrderResponseEnricher orderResponseEnricher;

    public AdminCustomerService(
            UserDao userDao,
            OrderDao orderDao,
            OrderResponseEnricher orderResponseEnricher) {
        this.userDao = userDao;
        this.orderDao = orderDao;
        this.orderResponseEnricher = orderResponseEnricher;
    }

    public PageResponse<UserProfileResponse> getCustomers(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = userDao.countCustomers();
        List<UserProfileResponse> content = userDao.findCustomersPage(safePage, safeSize).stream()
                .map(UserProfileResponse::from)
                .toList();
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public UserProfileResponse getCustomer(UUID id) {
        return UserProfileResponse.from(requireCustomer(id));
    }

    public PageResponse<OrderResponse> getCustomerOrders(UUID customerId, int page, int size) {
        requireCustomer(customerId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = orderDao.countByUserId(customerId);
        List<Order> orders = orderDao.findPageByUserId(customerId, safePage, safeSize);
        return PageResponse.of(
                orderResponseEnricher.toListResponses(orders, false, false),
                safePage,
                safeSize,
                total);
    }

    private User requireCustomer(UUID id) {
        User user = userDao.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng"));
        boolean isCustomer = user.getRoles().stream().anyMatch(role -> role == Role.CUSTOMER);
        if (!isCustomer) {
            throw ApiException.notFound("Không tìm thấy khách hàng");
        }
        return user;
    }
}
