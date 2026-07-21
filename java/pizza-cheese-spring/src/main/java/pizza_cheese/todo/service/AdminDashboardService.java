package pizza_cheese.todo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.PaymentDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.dto.CodedEnumValue;
import pizza_cheese.todo.dto.response.AdminDashboardResponse;
import pizza_cheese.todo.dto.response.AdminDashboardResponse.Kpis;
import pizza_cheese.todo.dto.response.AdminDashboardResponse.StatusCount;
import pizza_cheese.todo.dto.response.AdminDashboardStatsResponse;
import pizza_cheese.todo.dto.response.AdminDashboardStatsResponse.DailyPoint;
import pizza_cheese.todo.dto.response.AdminDashboardStatsResponse.PeriodCompare;
import pizza_cheese.todo.dto.response.AdminDashboardStatsResponse.Summary;
import pizza_cheese.todo.dto.response.AdminDashboardStatsResponse.TopItem;
import pizza_cheese.todo.exception.ApiException;

@Service
public class AdminDashboardService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_STATS_RANGE_DAYS = 90;
    private static final int TOP_ITEMS_LIMIT = 5;

    private static final List<OrderStatus> INCOMPLETE_STATUSES = List.of(
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.OUT_FOR_DELIVERY);

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final UserDao userDao;

    public AdminDashboardService(OrderDao orderDao, PaymentDao paymentDao, UserDao userDao) {
        this.orderDao = orderDao;
        this.paymentDao = paymentDao;
        this.userDao = userDao;
    }

    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        LocalDateTime generatedAt = LocalDateTime.now(APP_ZONE);

        List<StatusCount> incompleteByStatus = new ArrayList<>(INCOMPLETE_STATUSES.size());
        long ordersIncomplete = 0L;
        for (OrderStatus status : INCOMPLETE_STATUSES) {
            long count = orderDao.countByStatus(status);
            ordersIncomplete += count;
            incompleteByStatus.add(StatusCount.of(CodedEnumValue.from(status), count));
        }

        Kpis kpis = new Kpis();
        kpis.setOrdersCompletedToday(orderDao.countFiltered(OrderStatus.COMPLETED, from, to));
        kpis.setOrdersIncomplete(ordersIncomplete);
        kpis.setCollectedToday(paymentDao.sumPaidAmountBetween(from, to));
        kpis.setRevenueCompletedToday(orderDao.sumFinalAmountFiltered(OrderStatus.COMPLETED, from, to));
        kpis.setCustomersTotal(userDao.countCustomers());
        kpis.setCustomersNewToday(userDao.countCustomersCreatedBetween(from, to));

        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setGeneratedAt(generatedAt);
        response.setKpis(kpis);
        response.setIncompleteByStatus(incompleteByStatus);
        return response;
    }

    public AdminDashboardStatsResponse getStats(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate to = toDate != null ? toDate : today;
        LocalDate from = fromDate != null ? fromDate : to.minusDays(6);

        if (from.isAfter(to)) {
            throw ApiException.badRequest("from không được sau to");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_STATS_RANGE_DAYS) {
            throw ApiException.badRequest("Khoảng ngày tối đa " + MAX_STATS_RANGE_DAYS + " ngày");
        }

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);
        LocalDateTime prevFromDt = prevFrom.atStartOfDay();
        LocalDateTime prevToDt = prevTo.plusDays(1).atStartOfDay();

        Map<LocalDate, OrderDao.DailyAmountRow> revenueByDay = new HashMap<>();
        for (OrderDao.DailyAmountRow row : orderDao.sumCompletedRevenueByDay(fromDt, toDt)) {
            revenueByDay.put(row.day(), row);
        }
        Map<LocalDate, BigDecimal> collectedByDay = new HashMap<>();
        for (PaymentDao.DailyCollectedRow row : paymentDao.sumCollectedByDay(fromDt, toDt)) {
            collectedByDay.put(row.day(), row.amount());
        }

        List<DailyPoint> series = new ArrayList<>((int) days);
        BigDecimal revenueCompleted = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        long ordersCompleted = 0L;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            OrderDao.DailyAmountRow revenueRow = revenueByDay.get(day);
            BigDecimal dayRevenue = revenueRow != null ? revenueRow.amount() : BigDecimal.ZERO;
            long dayOrders = revenueRow != null ? revenueRow.count() : 0L;
            BigDecimal dayCollected = collectedByDay.getOrDefault(day, BigDecimal.ZERO);
            series.add(DailyPoint.of(day, dayRevenue, dayCollected, dayOrders));
            revenueCompleted = revenueCompleted.add(dayRevenue);
            collected = collected.add(dayCollected);
            ordersCompleted += dayOrders;
        }

        long ordersCreated = orderDao.countFiltered(null, fromDt, toDt);
        BigDecimal avgOrderValue = ordersCompleted == 0
                ? BigDecimal.ZERO
                : revenueCompleted.divide(BigDecimal.valueOf(ordersCompleted), 0, RoundingMode.HALF_UP);

        Summary summary = new Summary();
        summary.setOrdersCreated(ordersCreated);
        summary.setOrdersCompleted(ordersCompleted);
        summary.setRevenueCompleted(revenueCompleted);
        summary.setCollected(collected);
        summary.setAvgOrderValue(avgOrderValue);

        BigDecimal prevRevenue = orderDao.sumFinalAmountFiltered(OrderStatus.COMPLETED, prevFromDt, prevToDt);
        BigDecimal prevCollected = paymentDao.sumPaidAmountBetween(prevFromDt, prevToDt);
        long prevOrdersCompleted = orderDao.countFiltered(OrderStatus.COMPLETED, prevFromDt, prevToDt);

        PeriodCompare previousPeriod = new PeriodCompare();
        previousPeriod.setFrom(prevFrom);
        previousPeriod.setTo(prevTo);
        previousPeriod.setOrdersCompleted(prevOrdersCompleted);
        previousPeriod.setRevenueCompleted(prevRevenue);
        previousPeriod.setCollected(prevCollected);
        previousPeriod.setRevenueChangePercent(percentChange(revenueCompleted, prevRevenue));
        previousPeriod.setCollectedChangePercent(percentChange(collected, prevCollected));
        previousPeriod.setOrdersChangePercent(percentChange(
                BigDecimal.valueOf(ordersCompleted),
                BigDecimal.valueOf(prevOrdersCompleted)));

        List<TopItem> topItems = orderDao.findTopSellingItems(fromDt, toDt, TOP_ITEMS_LIMIT).stream()
                .map(row -> TopItem.of(
                        row.name(),
                        CodedEnumValue.from(row.itemType()),
                        row.quantity(),
                        row.revenue()))
                .toList();

        AdminDashboardStatsResponse response = new AdminDashboardStatsResponse();
        response.setFrom(from);
        response.setTo(to);
        response.setSummary(summary);
        response.setPreviousPeriod(previousPeriod);
        response.setSeries(series);
        response.setTopItems(topItems);
        return response;
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }
}
