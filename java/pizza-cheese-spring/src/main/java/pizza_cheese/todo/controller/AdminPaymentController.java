package pizza_cheese.todo.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import pizza_cheese.todo.domain.PaymentMethod;
import pizza_cheese.todo.domain.PaymentStatus;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.PaymentResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.AdminPaymentService;

@Tag(name = "Admin Payments", description = "Đối soát thanh toán")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @Operation(summary = "Danh sách thanh toán")
    @GetMapping
    public ResponseEntity<RestResponse<PageResponse<PaymentResponse>>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RestResponse.success(
                adminPaymentService.getPayments(status, method, from, to, page, size)));
    }

    @Operation(summary = "Chi tiết thanh toán")
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<PaymentResponse>> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(adminPaymentService.getPayment(id)));
    }
}
