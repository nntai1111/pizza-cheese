package pizza_cheese.todo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import pizza_cheese.todo.dto.request.CreateToppingRequest;
import pizza_cheese.todo.dto.request.UpdateToppingRequest;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.dto.response.ToppingResponse;
import pizza_cheese.todo.service.ToppingService;

@Tag(name = "Topping", description = "Quản lý topping")
@RestController
@RequestMapping("/api/v1")
public class ToppingController {

    private final ToppingService toppingService;

    public ToppingController(ToppingService toppingService) {
        this.toppingService = toppingService;
    }

    @Operation(summary = "Danh sách topping")
    @GetMapping("/toppings")
    public ResponseEntity<RestResponse<List<ToppingResponse>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(RestResponse.success(toppingService.findAll(activeOnly)));
    }

    @Operation(summary = "Chi tiết topping")
    @GetMapping("/toppings/{id}")
    public ResponseEntity<RestResponse<ToppingResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(toppingService.findById(id)));
    }

    @Operation(summary = "Tạo topping")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/toppings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ToppingResponse>> create(@Valid @RequestBody CreateToppingRequest request) {
        return ResponseEntity.ok(RestResponse.success(toppingService.create(request)));
    }

    @Operation(summary = "Cập nhật topping")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/admin/toppings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ToppingResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateToppingRequest request) {
        return ResponseEntity.ok(RestResponse.success(toppingService.update(id, request)));
    }

    @Operation(summary = "Xóa topping (vô hiệu hóa)")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/toppings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable UUID id) {
        toppingService.delete(id);
        return ResponseEntity.ok(RestResponse.success(null));
    }
}
