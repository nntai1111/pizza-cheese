package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateToppingRequest {

    @NotBlank(message = "Tên topping không được để trống")
    @Size(max = 100, message = "Tên topping không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Giá topping không được để trống")
    @DecimalMin(value = "0", message = "Giá topping không được âm")
    private BigDecimal price;

    private Boolean isActive = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
