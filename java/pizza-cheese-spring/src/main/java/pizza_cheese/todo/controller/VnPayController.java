package pizza_cheese.todo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.OrderService;
import pizza_cheese.todo.service.VnPayService;

@Tag(name = "Payment", description = "Thanh toán VNPay")
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/payments/vnpay")
public class VnPayController {

    private final VnPayService vnPayService;
    private final OrderService orderService;

    public VnPayController(VnPayService vnPayService, OrderService orderService) {
        this.vnPayService = vnPayService;
        this.orderService = orderService;
    }

    @Operation(summary = "VNPay IPN callback")
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> ipn(HttpServletRequest request) {
        Map<String, String> params = VnPayService.toParamMap(request);
        Map<String, String> response = vnPayService.handleIpn(params);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "VNPay return URL - redirect về frontend")
    @GetMapping("/return")
    public RedirectView returnUrl(HttpServletRequest request) {
        Map<String, String> params = VnPayService.toParamMap(request);
        try {
            vnPayService.handleReturn(params);
        } catch (RuntimeException ignored) {
            // Frontend hiển thị trạng thái dựa trên query params
        }

        String query = request.getQueryString();
        String redirectTarget = "http://localhost:4200/customer/payment/return";
        if (query != null && !query.isBlank()) {
            redirectTarget += "?" + query;
        }
        return new RedirectView(redirectTarget);
    }

    @Operation(summary = "Tra cứu đơn sau thanh toán (theo txnRef)")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RestResponse<OrderResponse>> paymentStatus(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("txnRef") String txnRef) {
        return ResponseEntity.ok(RestResponse.success(
                orderService.getOrderByPaymentTxnRef(jwt.getSubject(), txnRef)));
    }
}
