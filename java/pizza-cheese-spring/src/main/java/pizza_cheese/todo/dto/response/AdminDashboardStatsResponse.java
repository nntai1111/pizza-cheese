package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.dto.CodedEnumValue;

@Getter
@Setter
public class AdminDashboardStatsResponse {

    private LocalDate from;
    private LocalDate to;
    private Summary summary;
    private PeriodCompare previousPeriod;
    private List<DailyPoint> series;
    private List<TopItem> topItems;

    @Getter
    @Setter
    public static class Summary {
        private long ordersCreated;
        private long ordersCompleted;
        private BigDecimal revenueCompleted;
        private BigDecimal collected;
        /** Trung bình giá trị đơn hoàn thành. */
        private BigDecimal avgOrderValue;
    }

    @Getter
    @Setter
    public static class PeriodCompare {
        private LocalDate from;
        private LocalDate to;
        private long ordersCompleted;
        private BigDecimal revenueCompleted;
        private BigDecimal collected;
        /** % thay đổi doanh thu vs kỳ trước (null nếu kỳ trước = 0). */
        private BigDecimal revenueChangePercent;
        private BigDecimal collectedChangePercent;
        private BigDecimal ordersChangePercent;
    }

    @Getter
    @Setter
    public static class DailyPoint {
        private LocalDate date;
        private BigDecimal revenueCompleted;
        private BigDecimal collected;
        private long ordersCompleted;

        public static DailyPoint of(
                LocalDate date,
                BigDecimal revenueCompleted,
                BigDecimal collected,
                long ordersCompleted) {
            DailyPoint point = new DailyPoint();
            point.setDate(date);
            point.setRevenueCompleted(revenueCompleted != null ? revenueCompleted : BigDecimal.ZERO);
            point.setCollected(collected != null ? collected : BigDecimal.ZERO);
            point.setOrdersCompleted(ordersCompleted);
            return point;
        }
    }

    @Getter
    @Setter
    public static class TopItem {
        private String name;
        private CodedEnumValue itemType;
        private long quantity;
        private BigDecimal revenue;

        public static TopItem of(String name, CodedEnumValue itemType, long quantity, BigDecimal revenue) {
            TopItem item = new TopItem();
            item.setName(name);
            item.setItemType(itemType);
            item.setQuantity(quantity);
            item.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);
            return item;
        }
    }
}
