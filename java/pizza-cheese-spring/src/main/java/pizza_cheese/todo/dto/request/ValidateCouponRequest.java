package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateCouponRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    private String code;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @DecimalMin(value = "0", message = "Giá trị đơn hàng không được âm")
    private BigDecimal orderAmount;
}
