package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.Cart;
import pizza_cheese.todo.domain.CartItem;
import pizza_cheese.todo.domain.CartItemComboLine;
import pizza_cheese.todo.domain.CartItemTopping;
import pizza_cheese.todo.domain.LineItemType;
import pizza_cheese.todo.domain.PizzaSize;

@Getter
@Setter
public class CartResponse {

    private UUID id;
    private List<CartItemResponse> items;
    private int itemCount;
    private BigDecimal subtotal;

    public static CartResponse empty() {
        CartResponse response = new CartResponse();
        response.setItems(List.of());
        response.setItemCount(0);
        response.setSubtotal(BigDecimal.ZERO);
        return response;
    }

    public static CartResponse from(Cart cart) {
        if (cart == null) {
            return empty();
        }

        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        int itemCount = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setItems(items);
        response.setItemCount(itemCount);
        response.setSubtotal(subtotal);
        return response;
    }

    @Getter
    @Setter
    public static class CartItemResponse {

        private UUID id;
        private LineItemType itemType;
        private UUID pizzaId;
        private UUID pizzaVariantId;
        private UUID comboId;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String pizzaName;
        private String pizzaSlug;
        private PizzaSize pizzaSize;
        private String pizzaImageUrl;
        private String comboName;
        private String comboSlug;
        private String comboImageUrl;
        private List<CartItemToppingResponse> toppings;
        private List<CartItemComboLineResponse> comboLines;

        public static CartItemResponse from(CartItem item) {
            CartItemResponse response = new CartItemResponse();
            response.setId(item.getId());
            response.setItemType(item.getItemType());
            response.setPizzaId(item.getPizzaId());
            response.setPizzaVariantId(item.getPizzaVariantId());
            response.setComboId(item.getComboId());
            response.setQuantity(item.getQuantity());
            response.setUnitPrice(item.getUnitPrice());
            response.setLineTotal(item.getLineTotal());
            response.setPizzaName(item.getPizzaName());
            response.setPizzaSlug(item.getPizzaSlug());
            response.setPizzaSize(item.getPizzaSize());
            response.setPizzaImageUrl(item.getPizzaImageUrl());
            response.setComboName(item.getComboName());
            response.setComboSlug(item.getComboSlug());
            response.setComboImageUrl(item.getComboImageUrl());
            response.setToppings(item.getToppings().stream().map(CartItemToppingResponse::from).toList());
            response.setComboLines(item.getComboLines().stream().map(CartItemComboLineResponse::from).toList());
            return response;
        }
    }

    @Getter
    @Setter
    public static class CartItemToppingResponse {

        private UUID toppingId;
        private String toppingName;
        private BigDecimal price;

        public static CartItemToppingResponse from(CartItemTopping topping) {
            CartItemToppingResponse response = new CartItemToppingResponse();
            response.setToppingId(topping.getToppingId());
            response.setToppingName(topping.getToppingName());
            response.setPrice(topping.getPrice());
            return response;
        }
    }

    @Getter
    @Setter
    public static class CartItemComboLineResponse {

        private UUID pizzaId;
        private UUID pizzaVariantId;
        private int quantity;
        private String pizzaName;
        private PizzaSize pizzaSize;

        public static CartItemComboLineResponse from(CartItemComboLine line) {
            CartItemComboLineResponse response = new CartItemComboLineResponse();
            response.setPizzaId(line.getPizzaId());
            response.setPizzaVariantId(line.getPizzaVariantId());
            response.setQuantity(line.getQuantity());
            response.setPizzaName(line.getPizzaName());
            response.setPizzaSize(line.getPizzaSize());
            return response;
        }
    }
}
