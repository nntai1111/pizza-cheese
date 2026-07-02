package pizza_cheese.todo.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.dto.request.CreateOrderRequest;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.CashierService;
import pizza_cheese.todo.service.OrderService;

@Tag(name = "Cashier", description = "Quản lý đơn hàng tại quầy")

@RestController
@RequestMapping("/api/v1/cashier/orders")
public class CashierController {

    private final CashierService cashierService;
    private final OrderService orderService;

    public CashierController(CashierService cashierService, OrderService orderService) {
        this.cashierService = cashierService;
        this.orderService = orderService;
    }

    @Operation(summary = "Danh sách đơn hàng (phân trang)")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    public ResponseEntity<RestResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RestResponse.success(cashierService.getOrders(status, page, size)));
    }

    @Operation(summary = "Tạo đơn hàng tại quầy")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<RestResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(RestResponse.success(
                orderService.createOrder(jwt.getSubject(), request, httpRequest)));
    }

    @Operation(summary = "Chi tiết đơn hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    public ResponseEntity<RestResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(cashierService.getOrder(id)));
    }

    @Operation(summary = "Xác nhận đã thu tiền / chuyển khoản")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<RestResponse<OrderResponse>> confirmPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(cashierService.confirmPayment(jwt.getSubject(), id)));
    }

    @Operation(summary = "Hủy đơn hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<RestResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(cashierService.cancelOrder(jwt.getSubject(), id)));
    }
}
