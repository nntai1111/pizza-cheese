package pizza_cheese.todo.domain;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComboItem {

    private UUID comboId;
    private UUID pizzaId;
    private UUID pizzaVariantId;
    private PizzaSize pizzaSize;
    private String pizzaName;
    private String pizzaSlug;
    private int quantity;
}
