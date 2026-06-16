package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PizzaVariant {

    private UUID id;
    private UUID pizzaId;
    private PizzaSize size;
    private BigDecimal price;
}
