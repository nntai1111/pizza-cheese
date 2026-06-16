package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.PizzaSize;

@Getter
@Setter
public class PizzaVariantRequest {

    @NotNull(message = "Kích thước pizza không được để trống")
    private PizzaSize size;

    @NotNull(message = "Giá theo size không được để trống")
    @DecimalMin(value = "0.01", message = "Giá theo size phải lớn hơn 0")
    private BigDecimal price;
}
