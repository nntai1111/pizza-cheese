package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import pizza_cheese.todo.domain.PizzaSize;

public class PizzaVariantRequest {

    @NotNull(message = "Kích thước pizza không được để trống")
    private PizzaSize size;

    @NotNull(message = "Giá theo size không được để trống")
    @DecimalMin(value = "0.01", message = "Giá theo size phải lớn hơn 0")
    private BigDecimal price;

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
