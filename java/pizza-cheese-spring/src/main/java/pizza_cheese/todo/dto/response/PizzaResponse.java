package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaImage;
import pizza_cheese.todo.domain.PizzaSize;
import pizza_cheese.todo.domain.PizzaVariant;

@Getter
@Setter
public class PizzaResponse {

    private UUID id;
    private CategorySummary category;
    private String name;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private boolean active;
    private List<PizzaVariantResponse> variants;
    private List<ToppingResponse> toppings;
    private List<PizzaImageResponse> images;
    private Instant createdAt;
    private Instant updatedAt;

    public static PizzaResponse from(Pizza pizza) {
        PizzaResponse response = new PizzaResponse();
        response.setId(pizza.getId());
        if (pizza.getCategoryId() != null) {
            CategorySummary category = new CategorySummary();
            category.setId(pizza.getCategoryId());
            category.setName(pizza.getCategoryName());
            category.setSlug(pizza.getCategorySlug());
            response.setCategory(category);
        }
        response.setName(pizza.getName());
        response.setSlug(pizza.getSlug());
        response.setDescription(pizza.getDescription());
        response.setBasePrice(pizza.getBasePrice());
        response.setActive(pizza.isActive());
        response.setVariants(pizza.getVariants().stream().map(PizzaVariantResponse::from).toList());
        response.setToppings(pizza.getToppings().stream().map(ToppingResponse::from).toList());
        response.setImages(pizza.getImages().stream().map(PizzaImageResponse::from).toList());
        response.setCreatedAt(pizza.getCreatedAt());
        response.setUpdatedAt(pizza.getUpdatedAt());
        return response;
    }

    @Getter
    @Setter
    public static class CategorySummary {

        private UUID id;
        private String name;
        private String slug;
    }

    @Getter
    @Setter
    public static class PizzaVariantResponse {

        private UUID id;
        private PizzaSize size;
        private BigDecimal price;

        public static PizzaVariantResponse from(PizzaVariant variant) {
            PizzaVariantResponse response = new PizzaVariantResponse();
            response.setId(variant.getId());
            response.setSize(variant.getSize());
            response.setPrice(variant.getPrice());
            return response;
        }
    }

    @Getter
    @Setter
    public static class PizzaImageResponse {

        private UUID id;
        private String imageUrl;
        private boolean main;
        private int sortOrder;

        public static PizzaImageResponse from(PizzaImage image) {
            PizzaImageResponse response = new PizzaImageResponse();
            response.setId(image.getId());
            response.setImageUrl(image.getImageUrl());
            response.setMain(image.isMain());
            response.setSortOrder(image.getSortOrder());
            return response;
        }
    }
}
