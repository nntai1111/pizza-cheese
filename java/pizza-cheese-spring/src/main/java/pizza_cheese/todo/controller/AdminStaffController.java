package pizza_cheese.todo.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.dto.request.CreateStaffRequest;
import pizza_cheese.todo.dto.request.UpdateStaffRequest;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.exception.ApiException;
import pizza_cheese.todo.service.AdminStaffService;

@Tag(name = "Admin Staff", description = "Quản lý nhân viên")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    public AdminStaffController(AdminStaffService adminStaffService) {
        this.adminStaffService = adminStaffService;
    }

    @Operation(summary = "Danh sách nhân viên")
    @GetMapping
    public ResponseEntity<RestResponse<PageResponse<UserProfileResponse>>> getStaff(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(RestResponse.success(adminStaffService.getStaff(role, page, size)));
    }

    @Operation(summary = "Chi tiết nhân viên")
    @GetMapping("/{id}")
    public ResponseEntity<RestResponse<UserProfileResponse>> getStaffById(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(adminStaffService.getStaffById(id)));
    }

    @Operation(summary = "Tạo nhân viên")
    @PostMapping
    public ResponseEntity<RestResponse<UserProfileResponse>> createStaff(
            @Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestResponse.success(adminStaffService.createStaff(request)));
    }

    @Operation(summary = "Cập nhật nhân viên")
    @PutMapping("/{id}")
    public ResponseEntity<RestResponse<UserProfileResponse>> updateStaff(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(RestResponse.success(
                adminStaffService.updateStaff(id, request, jwt.getSubject())));
    }

    @Operation(summary = "Khóa / mở khóa nhân viên")
    @PutMapping("/{id}/active")
    public ResponseEntity<RestResponse<UserProfileResponse>> setActive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) {
            throw ApiException.badRequest("Thiếu trường active");
        }
        return ResponseEntity.ok(RestResponse.success(
                adminStaffService.setActive(id, active, jwt.getSubject())));
    }
}
