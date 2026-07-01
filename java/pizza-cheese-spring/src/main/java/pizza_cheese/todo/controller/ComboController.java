package pizza_cheese.todo.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pizza_cheese.todo.dto.request.CreateComboRequest;
import pizza_cheese.todo.dto.request.UpdateComboRequest;
import pizza_cheese.todo.dto.response.ComboResponse;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.ComboService;

@Tag(name = "Combo", description = "Quản lý combo pizza")

@RestController
@RequestMapping("/api/v1")
public class ComboController {

    @Autowired
    private ComboService comboService;

    // public ComboController(ComboService comboService) {
    // this.comboService = comboService;
    // }

    @Operation(summary = "Danh sách combo (phân trang)")
    @GetMapping("/combos")
    public ResponseEntity<RestResponse<PageResponse<ComboResponse>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(RestResponse.success(comboService.findPage(activeOnly, page, size)));
    }

    @Operation(summary = "Chi tiết combo")
    @GetMapping("/combos/{id}")
    public ResponseEntity<RestResponse<ComboResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(comboService.findById(id)));
    }

    @Operation(summary = "Tạo combo mới (JSON)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/admin/combos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ComboResponse>> createJson(@Valid @RequestBody CreateComboRequest request) {
        return ResponseEntity.ok(RestResponse.success(comboService.create(request)));
    }

    @Operation(summary = "Tạo combo mới (multipart + upload ảnh)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/admin/combos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ComboResponse>> createMultipart(
            @Valid @RequestPart("request") CreateComboRequest request,
            @RequestPart(value = "image", required = false) @Schema(type = "string", format = "binary") MultipartFile image) {
        return ResponseEntity.ok(RestResponse.success(comboService.create(request, image)));
    }

    @Operation(summary = "Cập nhật combo (JSON)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = "/admin/combos/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ComboResponse>> updateJson(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComboRequest request) {
        return ResponseEntity.ok(RestResponse.success(comboService.update(id, request)));
    }

    @Operation(summary = "Cập nhật combo (multipart + upload ảnh)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = "/admin/combos/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<ComboResponse>> updateMultipart(
            @PathVariable UUID id,
            @Valid @RequestPart("request") UpdateComboRequest request,
            @RequestPart(value = "image", required = false) @Schema(type = "string", format = "binary") MultipartFile image) {
        return ResponseEntity.ok(RestResponse.success(comboService.update(id, request, image)));
    }

    @Operation(summary = "Xóa combo (vô hiệu hóa)")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/combos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> delete(@PathVariable UUID id) {
        comboService.delete(id);
        return ResponseEntity.ok(RestResponse.success(null));
    }
}
