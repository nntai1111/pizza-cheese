package pizza_cheese.todo.domain;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PizzaImage {

    private UUID id;
    private UUID pizzaId;
    private String imageUrl;
    private boolean main;
    private int sortOrder;
}
