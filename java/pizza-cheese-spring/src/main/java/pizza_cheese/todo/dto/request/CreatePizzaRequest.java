package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreatePizzaRequest {

    @NotNull(message = "Danh mục không được để trống")
    private UUID categoryId;

    @NotBlank(message = "Tên pizza không được để trống")
    @Size(max = 150, message = "Tên pizza không được vượt quá 150 ký tự")
    private String name;

    @Size(max = 150, message = "Slug không được vượt quá 150 ký tự")
    private String slug;

    private String description;

    @NotNull(message = "Giá cơ bản không được để trống")
    @DecimalMin(value = "0.01", message = "Giá cơ bản phải lớn hơn 0")
    private BigDecimal basePrice;

    private Boolean isActive = true;

    @NotEmpty(message = "Phải có ít nhất một size pizza")
    @Valid
    private List<PizzaVariantRequest> variants;

    private List<UUID> toppingIds;

    @Valid
    private List<PizzaImageRequest> images;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<PizzaVariantRequest> getVariants() {
        return variants;
    }

    public void setVariants(List<PizzaVariantRequest> variants) {
        this.variants = variants;
    }

    public List<UUID> getToppingIds() {
        return toppingIds;
    }

    public void setToppingIds(List<UUID> toppingIds) {
        this.toppingIds = toppingIds;
    }

    public List<PizzaImageRequest> getImages() {
        return images;
    }

    public void setImages(List<PizzaImageRequest> images) {
        this.images = images;
    }
}
