package pizza_cheese.todo.controller;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pizza_cheese.todo.dto.response.AdminDashboardResponse;
import pizza_cheese.todo.dto.response.AdminDashboardStatsResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.AdminDashboardService;

@Tag(name = "Admin", description = "API dành cho quản trị viên")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    public AdminController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @Operation(summary = "Dashboard admin — KPI và đơn chưa hoàn thành")
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<AdminDashboardResponse>> dashboard() {
        return ResponseEntity.ok(RestResponse.success(adminDashboardService.getDashboard()));
    }

    @Operation(summary = "Thống kê dashboard theo khoảng ngày")
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<AdminDashboardStatsResponse>> dashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(RestResponse.success(adminDashboardService.getStats(from, to)));
    }
}
