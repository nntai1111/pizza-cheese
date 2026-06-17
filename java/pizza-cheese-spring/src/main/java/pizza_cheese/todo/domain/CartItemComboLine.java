package pizza_cheese.todo.domain;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemComboLine {

    private UUID id;
    private UUID cartItemId;
    private UUID pizzaId;
    private UUID pizzaVariantId;
    private int quantity;
    private String pizzaName;
    private PizzaSize pizzaSize;
}
