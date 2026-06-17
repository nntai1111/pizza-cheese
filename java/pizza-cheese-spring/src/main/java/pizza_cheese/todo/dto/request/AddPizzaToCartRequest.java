package pizza_cheese.todo.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPizzaToCartRequest {

    @NotNull(message = "Pizza không được để trống")
    private UUID pizzaId;

    @NotNull(message = "Size không được để trống")
    private UUID pizzaVariantId;

    private List<UUID> toppingIds;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
