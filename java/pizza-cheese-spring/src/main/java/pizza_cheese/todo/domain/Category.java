package pizza_cheese.todo.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Category {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private int sortOrder;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;
}
