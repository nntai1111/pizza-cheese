package pizza_cheese.todo.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import pizza_cheese.todo.dao.CouponDao;
import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.PaymentDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.dao.UserDao.UserDisplayInfo;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.dto.response.OrderResponse;

@Service
public class OrderResponseEnricher {

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;
    private final CouponDao couponDao;

    public OrderResponseEnricher(
            OrderDao orderDao,
            PaymentDao paymentDao,
            UserDao userDao,
            CouponDao couponDao) {
        this.orderDao = orderDao;
        this.paymentDao = paymentDao;
        this.userDao = userDao;
        this.couponDao = couponDao;
    }

    public List<OrderResponse> toListResponses(
            List<Order> orders,
            boolean includeItems,
            boolean includeKitchenStaff) {
        if (orders.isEmpty()) {
            return List.of();
        }

        if (includeItems) {
            orderDao.loadOrderItemsBatch(orders);
        }

        List<UUID> orderIds = orders.stream().map(Order::getId).toList();
        Map<UUID, Payment> payments = paymentDao.findLatestByOrderIds(orderIds);
        LookupContext lookup = buildLookupContext(orders, includeKitchenStaff);

        return orders.stream()
                .map(order -> {
                    Payment payment = payments.get(order.getId());
                    OrderResponse response = includeItems
                            ? OrderResponse.from(order, payment)
                            : OrderResponse.summary(order, payment);
                    applyLookup(response, order, lookup, includeKitchenStaff);
                    return response;
                })
                .toList();
    }

    public OrderResponse toDetailResponse(Order order, boolean includeKitchenStaff) {
        orderDao.loadOrderItems(order);
        Payment payment = paymentDao.findLatestByOrderId(order.getId()).orElse(null);
        OrderResponse response = OrderResponse.from(order, payment);
        LookupContext lookup = buildLookupContext(List.of(order), includeKitchenStaff);
        applyLookup(response, order, lookup, includeKitchenStaff);
        return response;
    }

    public OrderResponse enrichExisting(OrderResponse response, Order order, boolean includeKitchenStaff) {
        LookupContext lookup = buildLookupContext(List.of(order), includeKitchenStaff);
        applyLookup(response, order, lookup, includeKitchenStaff);
        return response;
    }

    private LookupContext buildLookupContext(List<Order> orders, boolean includeKitchenStaff) {
        Set<UUID> userIds = new HashSet<>();
        Set<UUID> couponIds = new HashSet<>();

        for (Order order : orders) {
            userIds.add(order.getUserId());
            if (includeKitchenStaff && order.getKitchenStaffId() != null) {
                userIds.add(order.getKitchenStaffId());
            }
            if (order.getCouponId() != null) {
                couponIds.add(order.getCouponId());
            }
        }

        return new LookupContext(
                userDao.findDisplayInfoByIds(userIds),
                couponDao.findCodesByIds(couponIds));
    }

    private void applyLookup(
            OrderResponse response,
            Order order,
            LookupContext lookup,
            boolean includeKitchenStaff) {
        UserDisplayInfo customer = lookup.users().get(order.getUserId());
        if (customer != null) {
            response.setCustomerName(customer.fullName());
            response.setCustomerEmail(customer.email());
        }

        if (includeKitchenStaff && order.getKitchenStaffId() != null) {
            UserDisplayInfo kitchenStaff = lookup.users().get(order.getKitchenStaffId());
            if (kitchenStaff != null) {
                response.setKitchenStaffName(kitchenStaff.fullName());
            }
        }

        if (order.getCouponId() != null) {
            response.setCouponCode(lookup.couponCodes().get(order.getCouponId()));
        }
    }

    private record LookupContext(
            Map<UUID, UserDisplayInfo> users,
            Map<UUID, String> couponCodes) {
    }
}
