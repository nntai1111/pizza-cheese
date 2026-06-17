package pizza_cheese.todo.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pizza_cheese.todo.dto.request.AddComboToCartRequest;
import pizza_cheese.todo.dto.request.AddPizzaToCartRequest;
import pizza_cheese.todo.dto.request.UpdateCartItemRequest;
import pizza_cheese.todo.dto.response.CartResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.service.CartService;

@Tag(name = "Cart", description = "Giỏ hàng khách hàng")
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "Lấy giỏ hàng hiện tại")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<RestResponse<CartResponse>> getCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(RestResponse.success(cartService.getCart(jwt.getSubject())));
    }

    @Operation(summary = "Thêm pizza vào giỏ hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/items/pizza")
    public ResponseEntity<RestResponse<CartResponse>> addPizza(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddPizzaToCartRequest request) {
        return ResponseEntity.ok(RestResponse.success(cartService.addPizza(jwt.getSubject(), request)));
    }

    @Operation(summary = "Thêm combo vào giỏ hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/items/combo")
    public ResponseEntity<RestResponse<CartResponse>> addCombo(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddComboToCartRequest request) {
        return ResponseEntity.ok(RestResponse.success(cartService.addCombo(jwt.getSubject(), request)));
    }

    @Operation(summary = "Cập nhật số lượng món trong giỏ")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/items/{id}")
    public ResponseEntity<RestResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(RestResponse.success(
                cartService.updateItemQuantity(jwt.getSubject(), id, request)));
    }

    @Operation(summary = "Xóa món khỏi giỏ hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/items/{id}")
    public ResponseEntity<RestResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        return ResponseEntity.ok(RestResponse.success(cartService.removeItem(jwt.getSubject(), id)));
    }

    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping
    public ResponseEntity<RestResponse<CartResponse>> clearCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(RestResponse.success(cartService.clearCart(jwt.getSubject())));
    }
}
