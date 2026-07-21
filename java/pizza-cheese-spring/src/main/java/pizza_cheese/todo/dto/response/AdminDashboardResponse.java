package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.dto.CodedEnumValue;

@Getter
@Setter
public class AdminDashboardResponse {

    private LocalDateTime generatedAt;
    private Kpis kpis;
    private List<StatusCount> incompleteByStatus;

    @Getter
    @Setter
    public static class Kpis {
        /** Đơn COMPLETED tạo trong hôm nay. */
        private long ordersCompletedToday;
        /** Tổng đơn chưa kết thúc (không gồm COMPLETED/CANCELLED/REFUNDED). */
        private long ordersIncomplete;
        /** Tiền đã thu hôm nay (Payment PAID theo paid_at). */
        private BigDecimal collectedToday;
        /** Doanh thu đơn hoàn thành hôm nay (SUM final_amount COMPLETED hôm nay). */
        private BigDecimal revenueCompletedToday;
        private long customersTotal;
        private long customersNewToday;
    }

    @Getter
    @Setter
    public static class StatusCount {
        private CodedEnumValue status;
        private long count;

        public static StatusCount of(CodedEnumValue status, long count) {
            StatusCount item = new StatusCount();
            item.setStatus(status);
            item.setCount(count);
            return item;
        }
    }
}
