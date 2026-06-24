package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.Topping;

@Getter
@Setter
public class ToppingResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
}
