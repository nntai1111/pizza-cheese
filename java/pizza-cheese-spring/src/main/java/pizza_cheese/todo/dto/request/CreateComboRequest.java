package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;
import java.util.List;

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
public class CreateComboRequest {

    @NotBlank(message = "Tên combo không được để trống")
    @Size(max = 150, message = "Tên combo không được vượt quá 150 ký tự")
    private String name;

    @Size(max = 150, message = "Slug không được vượt quá 150 ký tự")
    private String slug;

    private String description;

    @NotNull(message = "Giá combo không được để trống")
    @DecimalMin(value = "0.01", message = "Giá combo phải lớn hơn 0")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "Phần trăm giảm giá không được âm")
    private BigDecimal discountPercent;

    private String imageUrl;
    private Boolean isActive = true;

    @NotEmpty(message = "Combo phải có ít nhất một pizza")
    @Valid
    private List<ComboItemRequest> items;
}
