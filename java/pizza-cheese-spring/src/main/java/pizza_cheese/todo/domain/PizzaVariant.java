package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class PizzaVariant {

    private UUID id;
    private UUID pizzaId;
    private PizzaSize size;
    private BigDecimal price;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPizzaId() {
        return pizzaId;
    }

    public void setPizzaId(UUID pizzaId) {
        this.pizzaId = pizzaId;
    }

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
