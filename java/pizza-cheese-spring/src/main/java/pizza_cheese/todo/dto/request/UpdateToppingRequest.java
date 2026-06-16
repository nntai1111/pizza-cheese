package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateToppingRequest {

    @Size(max = 100, message = "Tên topping không được vượt quá 100 ký tự")
    private String name;

    @DecimalMin(value = "0", message = "Giá topping không được âm")
    private BigDecimal price;

    private Boolean isActive;
}
