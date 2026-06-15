package pizza_cheese.todo.domain;

import java.util.UUID;

public class PizzaImage {

    private UUID id;
    private UUID pizzaId;
    private String imageUrl;
    private boolean main;
    private int sortOrder;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
