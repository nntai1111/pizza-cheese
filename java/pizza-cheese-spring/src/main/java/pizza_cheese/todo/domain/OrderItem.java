package pizza_cheese.todo.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItem {

    private UUID id;
    private UUID orderId;
    private LineItemType itemType;
    private UUID pizzaId;
    private UUID pizzaVariantId;
    private UUID comboId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    private String pizzaName;
    private String pizzaSlug;
    private PizzaSize pizzaSize;
    private String pizzaImageUrl;
    private String comboName;
    private String comboSlug;
    private String comboImageUrl;

    private List<OrderItemTopping> toppings = new ArrayList<>();
    private List<OrderItemComboLine> comboLines = new ArrayList<>();
}
