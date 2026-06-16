package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Topping {

    private UUID id;
    private String name;
    private BigDecimal price;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;
}
