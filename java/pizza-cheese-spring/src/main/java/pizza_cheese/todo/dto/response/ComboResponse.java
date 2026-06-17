package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.Combo;
import pizza_cheese.todo.domain.ComboItem;
import pizza_cheese.todo.domain.PizzaSize;

@Getter
@Setter
public class ComboResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPercent;
    private String imageUrl;
    private boolean active;
    private List<ComboItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;

    public static ComboResponse from(Combo combo) {
        ComboResponse response = new ComboResponse();
        response.setId(combo.getId());
        response.setName(combo.getName());
        response.setSlug(combo.getSlug());
        response.setDescription(combo.getDescription());
        response.setPrice(combo.getPrice());
        response.setDiscountPercent(combo.getDiscountPercent());
        response.setImageUrl(combo.getImageUrl());
        response.setActive(combo.isActive());
        response.setItems(combo.getItems().stream().map(ComboItemResponse::from).toList());
        response.setCreatedAt(combo.getCreatedAt());
        response.setUpdatedAt(combo.getUpdatedAt());
        return response;
    }

    @Getter
    @Setter
    public static class ComboItemResponse {

        private UUID pizzaId;
        private UUID pizzaVariantId;
        private PizzaSize pizzaSize;
        private String pizzaName;
        private String pizzaSlug;
        private int quantity;

        public static ComboItemResponse from(ComboItem item) {
            ComboItemResponse response = new ComboItemResponse();
            response.setPizzaId(item.getPizzaId());
            response.setPizzaVariantId(item.getPizzaVariantId());
            response.setPizzaSize(item.getPizzaSize());
            response.setPizzaName(item.getPizzaName());
            response.setPizzaSlug(item.getPizzaSlug());
            response.setQuantity(item.getQuantity());
            return response;
        }
    }
}
