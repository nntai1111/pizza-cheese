package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaImage;
import pizza_cheese.todo.domain.PizzaSize;
import pizza_cheese.todo.domain.PizzaVariant;

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CategorySummary getCategory() {
        return category;
    }

    public void setCategory(CategorySummary category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<PizzaVariantResponse> getVariants() {
        return variants;
    }

    public void setVariants(List<PizzaVariantResponse> variants) {
        this.variants = variants;
    }

    public List<ToppingResponse> getToppings() {
        return toppings;
    }

    public void setToppings(List<ToppingResponse> toppings) {
        this.toppings = toppings;
    }

    public List<PizzaImageResponse> getImages() {
        return images;
    }

    public void setImages(List<PizzaImageResponse> images) {
        this.images = images;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class CategorySummary {

        private UUID id;
        private String name;
        private String slug;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }

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

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public PizzaSize getSize() {
            return size;
        }

        public void setSize(PizzaSize size) {
            this.size = size;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

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

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public boolean isMain() {
            return main;
        }

        public void setMain(boolean main) {
            this.main = main;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
