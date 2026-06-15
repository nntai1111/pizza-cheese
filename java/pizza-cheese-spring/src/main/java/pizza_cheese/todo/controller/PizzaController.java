package pizza_cheese.todo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pizza_cheese.todo.dto.request.CreatePizzaRequest;
import pizza_cheese.todo.dto.request.UpdatePizzaRequest;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.PizzaResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.PizzaService;

@Tag(name = "Pizza", description = "Quản lý menu pizza")
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1")
public class PizzaController {

    private final PizzaService pizzaService;

    public PizzaController(PizzaService pizzaService) {
        this.pizzaService = pizzaService;
    }

    @Operation(summary = "Danh sách pizza (phân trang)")
    @GetMapping("/pizzas")
    public ResponseEntity<RestResponse<PageResponse<PizzaResponse>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(RestResponse.success(pizzaService.findPage(activeOnly, categoryId, page, size)));
    }

    @Operation(summary = "Chi tiết pizza")
    @GetMapping("/pizzas/{id}")
    public ResponseEntity<RestResponse<PizzaResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(pizzaService.findById(id)));
    }

    @Operation(summary = "Tạo pizza mới (JSON)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/admin/pizzas", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<PizzaResponse>> createJson(@Valid @RequestBody CreatePizzaRequest request) {
        return ResponseEntity.ok(RestResponse.success(pizzaService.create(request)));
    }

    @Operation(summary = "Tạo pizza mới (multipart + upload ảnh)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/admin/pizzas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<PizzaResponse>> createMultipart(
            @Valid @RequestPart("request") CreatePizzaRequest request,
            @RequestPart(value = "mainImage", required = false) @Schema(type = "string", format = "binary") MultipartFile mainImage,
            @RequestPart(value = "images", required = false) @Schema(type = "string", format = "binary") List<MultipartFile> images) {
        return ResponseEntity.ok(RestResponse.success(pizzaService.create(
                request,
                mainImage,
                images != null ? images : List.of())));
    }

    @Operation(summary = "Cập nhật pizza (JSON)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = "/admin/pizzas/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<PizzaResponse>> updateJson(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePizzaRequest request) {
        return ResponseEntity.ok(RestResponse.success(pizzaService.update(id, request)));
    }

    @Operation(summary = "Cập nhật pizza (multipart + upload ảnh)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = "/admin/pizzas/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<PizzaResponse>> updateMultipart(
            @PathVariable UUID id,
            @Valid @RequestPart("request") UpdatePizzaRequest request,
            @RequestPart(value = "mainImage", required = false) @Schema(type = "string", format = "binary") MultipartFile mainImage,
            @RequestPart(value = "images", required = false) @Schema(type = "string", format = "binary") List<MultipartFile> images) {
        return ResponseEntity.ok(RestResponse.success(pizzaService.update(
                id,
                request,
                mainImage,
                images != null ? images : List.of())));
    }

    @Operation(summary = "Xóa pizza (vô hiệu hóa)")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/pizzas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable UUID id) {
        pizzaService.delete(id);
        return ResponseEntity.ok(RestResponse.success(null));
    }
}
