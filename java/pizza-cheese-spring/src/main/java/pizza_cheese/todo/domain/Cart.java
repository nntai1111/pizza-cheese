package pizza_cheese.todo.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cart {

    private UUID id;
    private UUID userId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CartItem> items = new ArrayList<>();
}
