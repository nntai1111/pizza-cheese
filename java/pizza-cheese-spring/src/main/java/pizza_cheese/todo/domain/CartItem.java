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
public class CartItem {

    private UUID id;
    private UUID cartId;
    private LineItemType itemType;
    private UUID pizzaId;
    private UUID pizzaVariantId;
    private UUID comboId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String pizzaName;
    private String pizzaSlug;
    private PizzaSize pizzaSize;
    private String pizzaImageUrl;
    private String comboName;
    private String comboSlug;
    private String comboImageUrl;

    private List<CartItemTopping> toppings = new ArrayList<>();
    private List<CartItemComboLine> comboLines = new ArrayList<>();
}
