package pizza_cheese.todo.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import pizza_cheese.todo.domain.OrderStatus;
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.KitchenService;

@Tag(name = "Kitchen", description = "Quản lý đơn hàng tại bếp")

@RestController
@RequestMapping("/api/v1/kitchen/orders")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @Operation(summary = "Danh sách đơn hàng cho bếp (phân trang)")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN')")
    public ResponseEntity<RestResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RestResponse.success(kitchenService.getOrders(status, page, size)));
    }

    @Operation(summary = "Chi tiết đơn hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN')")
    public ResponseEntity<RestResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(kitchenService.getOrder(id)));
    }

    @Operation(summary = "Nhận đơn và bắt đầu chế biến (CONFIRMED → PREPARING)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/start-preparing")
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<RestResponse<OrderResponse>> startPreparing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(kitchenService.startPreparing(jwt.getSubject(), id)));
    }

    @Operation(summary = "Hoàn thành chế biến (PREPARING → READY)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/mark-ready")
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<RestResponse<OrderResponse>> markReady(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(kitchenService.markReady(jwt.getSubject(), id)));
    }
}
