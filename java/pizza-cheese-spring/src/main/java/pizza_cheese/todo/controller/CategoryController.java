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
import pizza_cheese.todo.dto.request.CreateCategoryRequest;
import pizza_cheese.todo.dto.request.UpdateCategoryRequest;
import pizza_cheese.todo.dto.response.CategoryResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.CategoryService;

@Tag(name = "Category", description = "Quản lý danh mục pizza")
@RestController
@RequestMapping("/api/v1")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Danh sách danh mục")
    @GetMapping("/categories")
    public ResponseEntity<RestResponse<List<CategoryResponse>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(RestResponse.success(categoryService.findAll(activeOnly)));
    }

    @Operation(summary = "Chi tiết danh mục")
    @GetMapping("/categories/{id}")
    public ResponseEntity<RestResponse<CategoryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(categoryService.findById(id)));
    }

    @Operation(summary = "Tạo danh mục")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(RestResponse.success(categoryService.create(request)));
    }

    @Operation(summary = "Cập nhật danh mục")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<CategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(RestResponse.success(categoryService.update(id, request)));
    }

    @Operation(summary = "Xóa danh mục (vô hiệu hóa)")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(RestResponse.success(null));
    }
}
