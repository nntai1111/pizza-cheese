package pizza_cheese.todo.controller;

import java.util.UUID;

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
import pizza_cheese.todo.dto.response.OrderResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.service.AdminCustomerService;

@Tag(name = "Admin Customers", description = "Quản lý khách hàng")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/v1/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    @Operation(summary = "Danh sách khách hàng")
    @GetMapping
    public ResponseEntity<RestResponse<PageResponse<UserProfileResponse>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RestResponse.success(adminCustomerService.getCustomers(page, size)));
    }

    @Operation(summary = "Chi tiết khách hàng")
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<UserProfileResponse>> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(adminCustomerService.getCustomer(id)));
    }

    @Operation(summary = "Lịch sử đơn hàng của khách")
    @GetMapping("/{id}/orders")
    public ResponseEntity<RestResponse<PageResponse<OrderResponse>>> getCustomerOrders(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RestResponse.success(adminCustomerService.getCustomerOrders(id, page, size)));
    }
}
