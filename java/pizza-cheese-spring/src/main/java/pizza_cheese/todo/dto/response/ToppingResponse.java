package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import pizza_cheese.todo.domain.Topping;

public class ToppingResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static ToppingResponse from(Topping topping) {
        ToppingResponse response = new ToppingResponse();
        response.setId(topping.getId());
        response.setName(topping.getName());
        response.setPrice(topping.getPrice());
        response.setActive(topping.isActive());
        response.setCreatedAt(topping.getCreatedAt());
        response.setUpdatedAt(topping.getUpdatedAt());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
