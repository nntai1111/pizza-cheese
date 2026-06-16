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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
