package pizza_cheese.todo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import pizza_cheese.todo.dao.PaymentDao;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.PaymentResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class AdminPaymentService {

    private final PaymentDao paymentDao;

    public AdminPaymentService(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
    }

    public PageResponse<PaymentResponse> getPayments(
            PaymentStatus status,
            PaymentMethod method,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        long total = paymentDao.countFiltered(status, method, from, to);
        List<PaymentResponse> content = paymentDao.findPageFiltered(status, method, from, to, safePage, safeSize);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public PaymentResponse getPayment(UUID id) {
        return paymentDao.findAdminById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thanh toán"));
    }
}
