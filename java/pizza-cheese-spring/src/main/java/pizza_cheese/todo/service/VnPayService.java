package pizza_cheese.todo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import pizza_cheese.todo.config.VnPayProperties;
import pizza_cheese.todo.dao.OrderDao;
import pizza_cheese.todo.dao.PaymentDao;
import pizza_cheese.todo.domain.Order;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.domain.Payment;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.exception.InvalidPaymentSignatureException;
import pizza_cheese.todo.exception.OrderNotFoundException;
import pizza_cheese.todo.util.VnPayUtil;

@Service
public class VnPayService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayProperties vnPayProperties;
    private final PaymentDao paymentDao;
    private final OrderDao orderDao;
    private final ObjectMapper objectMapper;

    public VnPayService(
            VnPayProperties vnPayProperties,
            PaymentDao paymentDao,
            OrderDao orderDao,
            ObjectMapper objectMapper) {
        this.vnPayProperties = vnPayProperties;
        this.paymentDao = paymentDao;
        this.orderDao = orderDao;
        this.objectMapper = objectMapper;
    }

    public String createPaymentUrl(Order order, Payment payment, String clientIp) {
        Map<String, String> params = new LinkedHashMap<>();
        Instant now = Instant.now();
        Instant expire = now.plusSeconds(15 * 60);

        params.put("vnp_Version", vnPayProperties.getVersion());
        params.put("vnp_Command", vnPayProperties.getCommand());
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", toVnpayAmount(payment.getAmount()));
        params.put("vnp_CurrCode", vnPayProperties.getCurrCode());
        params.put("vnp_TxnRef", payment.getTransactionId());
        params.put("vnp_OrderInfo", "Thanh toan don " + order.getOrderCode());
        params.put("vnp_OrderType", vnPayProperties.getOrderType());
        params.put("vnp_Locale", vnPayProperties.getLocale());
        params.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        params.put("vnp_IpAddr", normalizeClientIp(clientIp));
        params.put("vnp_CreateDate", VNPAY_DATE_FORMAT.format(now.atZone(VN_ZONE)));
        params.put("vnp_ExpireDate", VNPAY_DATE_FORMAT.format(expire.atZone(VN_ZONE)));

        Map<String, String> sortedParams = new TreeMap<>(params);
        String paymentUrl = VnPayUtil.buildPaymentUrl(
                vnPayProperties.getPayUrl(), sortedParams, vnPayProperties.getHashSecret());

        payment.setPaymentUrl(paymentUrl);
        paymentDao.updatePaymentUrl(payment.getId(), paymentUrl);
        return paymentUrl;
    }

    private static String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        if ("0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
            return "127.0.0.1";
        }
        if (clientIp.contains(":")) {
            return "127.0.0.1";
        }
        return clientIp;
    }

    @Transactional
    public Map<String, String> handleIpn(Map<String, String> params) {
        Map<String, String> response = new LinkedHashMap<>();

        if (!VnPayUtil.verifySignature(params, vnPayProperties.getHashSecret())) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid signature");
            return response;
        }

        String txnRef = params.get("vnp_TxnRef");
        Payment payment = paymentDao.findByTransactionId(txnRef)
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy giao dịch thanh toán"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;
        }

        Order order = orderDao.findById(payment.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng"));

        String responseCode = params.get("vnp_ResponseCode");
        String callbackJson = toJson(params);

        if ("00".equals(responseCode) && isAmountValid(order, params)) {
            markPaymentSuccess(payment, order, params, callbackJson);
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setCallbackData(callbackJson);
        paymentDao.updateStatus(payment);

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    @Transactional
    public void handleReturn(Map<String, String> params) {
        if (!VnPayUtil.verifySignature(params, vnPayProperties.getHashSecret())) {
            throw new InvalidPaymentSignatureException("Chữ ký VNPay không hợp lệ");
        }

        String txnRef = params.get("vnp_TxnRef");
        Payment payment = paymentDao.findByTransactionId(txnRef)
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy giao dịch thanh toán"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        Order order = orderDao.findById(payment.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng"));

        String responseCode = params.get("vnp_ResponseCode");
        String callbackJson = toJson(params);

        if ("00".equals(responseCode) && isAmountValid(order, params)) {
            markPaymentSuccess(payment, order, params, callbackJson);
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setCallbackData(callbackJson);
        paymentDao.updateStatus(payment);
    }

    public static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static Map<String, String> toParamMap(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    private void markPaymentSuccess(Payment payment, Order order, Map<String, String> params, String callbackJson) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        payment.setCallbackData(callbackJson);
        paymentDao.updateStatus(payment);

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderDao.updateStatus(order.getId(), OrderStatus.CONFIRMED);
            orderDao.insertStatusHistory(order.getId(), OrderStatus.CONFIRMED, null, "Thanh toan VNPay thanh cong");
        }
    }

    private boolean isAmountValid(Order order, Map<String, String> params) {
        String vnpAmount = params.get("vnp_Amount");
        if (vnpAmount == null) {
            return false;
        }
        try {
            long expected = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
            return expected == Long.parseLong(vnpAmount);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String toVnpayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
