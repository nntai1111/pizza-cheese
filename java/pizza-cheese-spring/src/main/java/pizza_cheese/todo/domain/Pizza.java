package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Pizza {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    private String name;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private boolean active = true;
    private List<PizzaVariant> variants = new ArrayList<>();
    private List<Topping> toppings = new ArrayList<>();
    private List<UUID> toppingIds = new ArrayList<>();
    private List<PizzaImage> images = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
