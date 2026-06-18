package pizza_cheese.todo.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import pizza_cheese.todo.dto.request.CreateOrderRequest;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.OrderService;

@Tag(name = "Order", description = "Đặt hàng khách hàng")
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Tạo đơn hàng từ giỏ hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CASHIER')")
    public ResponseEntity<RestResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(RestResponse.success(
                orderService.createOrder(jwt.getSubject(), request, httpRequest)));
    }

    @Operation(summary = "Danh sách đơn hàng của tôi")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RestResponse<java.util.List<OrderResponse>>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(RestResponse.success(orderService.getMyOrders(jwt.getSubject())));
    }

    @Operation(summary = "Chi tiết đơn hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RestResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(orderService.getOrder(jwt.getSubject(), id)));
    }

    @Operation(summary = "Hủy đơn hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RestResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(orderService.cancelOrder(jwt.getSubject(), id)));
    }
}
