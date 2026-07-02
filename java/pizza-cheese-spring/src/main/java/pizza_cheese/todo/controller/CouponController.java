package pizza_cheese.todo.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pizza_cheese.todo.dto.request.CreateCouponRequest;
import pizza_cheese.todo.dto.request.UpdateCouponRequest;
import pizza_cheese.todo.dto.request.ValidateCouponRequest;
import pizza_cheese.todo.dto.response.AvailableCouponResponse;
import pizza_cheese.todo.dto.response.CouponResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.dto.response.ValidateCouponResponse;
import pizza_cheese.todo.service.CouponService;

@Tag(name = "Coupon", description = "Quản lý mã giảm giá")
@RestController
@RequestMapping("/api/v1")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "Kiểm tra mã giảm giá")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/coupons/validate")
    public ResponseEntity<RestResponse<ValidateCouponResponse>> validate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ValidateCouponRequest request) {
        return ResponseEntity.ok(RestResponse.success(
                ValidateCouponResponse.from(
                        couponService.applyCoupon(jwt.getSubject(), request.getCode(), request.getOrderAmount()))));
    }

    @Operation(summary = "Danh sách mã giảm giá có thể dùng cho đơn hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/coupons/available")
    public ResponseEntity<RestResponse<List<AvailableCouponResponse>>> listAvailable(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam BigDecimal orderAmount) {
        return ResponseEntity.ok(RestResponse.success(
                couponService.findAvailableCoupons(jwt.getSubject(), orderAmount)));
    }

    @Operation(summary = "Danh sách coupon (admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<List<CouponResponse>>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(RestResponse.success(couponService.findAll(activeOnly)));
    }

    @Operation(summary = "Chi tiết coupon (admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<CouponResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(couponService.findById(id)));
    }

    @Operation(summary = "Tạo coupon")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<CouponResponse>> create(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(RestResponse.success(couponService.create(request)));
    }

    @Operation(summary = "Cập nhật coupon")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<CouponResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCouponRequest request) {
        return ResponseEntity.ok(RestResponse.success(couponService.update(id, request)));
    }

    @Operation(summary = "Xóa coupon (vô hiệu hóa)")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable UUID id) {
        couponService.delete(id);
        return ResponseEntity.ok(RestResponse.success(null));
    }
}
