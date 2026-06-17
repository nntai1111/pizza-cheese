package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemTopping {

    private UUID cartItemId;
    private UUID toppingId;
    private String toppingName;
    private BigDecimal price;
}
