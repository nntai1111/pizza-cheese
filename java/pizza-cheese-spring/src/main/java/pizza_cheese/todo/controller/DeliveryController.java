package pizza_cheese.todo.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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
import pizza_cheese.todo.service.DeliveryService;

@Tag(name = "Delivery", description = "Quản lý giao hàng")

@RestController
@RequestMapping("/api/v1/delivery/orders")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Operation(summary = "Danh sách đơn hàng cho shipper (phân trang; dùng updatedSince để chỉ lấy đơn mới/đổi)")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    @PreAuthorize("hasAnyRole('DELIVERY', 'ADMIN')")
    public ResponseEntity<RestResponse<PageResponse<OrderResponse>>> getOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedSince,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RestResponse.success(
                deliveryService.getOrders(jwt.getSubject(), status, page, size, updatedSince)));
    }

    @Operation(summary = "Chi tiết đơn hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DELIVERY', 'ADMIN')")
    public ResponseEntity<RestResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(deliveryService.getOrder(jwt.getSubject(), id)));
    }

    @Operation(summary = "Nhận đơn và bắt đầu giao (READY → OUT_FOR_DELIVERY)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/start-delivery")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<RestResponse<OrderResponse>> startDelivery(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(deliveryService.startDelivery(jwt.getSubject(), id)));
    }

    @Operation(summary = "Hoàn thành giao hàng (OUT_FOR_DELIVERY → DELIVERED)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{id}/mark-delivered")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<RestResponse<OrderResponse>> markDelivered(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(deliveryService.markDelivered(jwt.getSubject(), id)));
    }
}
