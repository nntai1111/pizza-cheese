package pizza_cheese.todo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pizza_cheese.todo.dao.CartDao;
import pizza_cheese.todo.dao.ComboDao;
import pizza_cheese.todo.dao.PizzaDao;
import pizza_cheese.todo.dao.ToppingDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Cart;
import pizza_cheese.todo.domain.CartItem;
import pizza_cheese.todo.domain.CartItemComboLine;
import pizza_cheese.todo.domain.Combo;
import pizza_cheese.todo.domain.ComboItem;
import pizza_cheese.todo.domain.LineItemType;
import pizza_cheese.todo.domain.Pizza;
import pizza_cheese.todo.domain.PizzaVariant;
import pizza_cheese.todo.domain.Topping;
import pizza_cheese.todo.dto.request.AddComboToCartRequest;
import pizza_cheese.todo.dto.request.AddPizzaToCartRequest;
import pizza_cheese.todo.dto.request.UpdateCartItemRequest;
import pizza_cheese.todo.dto.response.CartResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class CartService {

    private final CartDao cartDao;
    private final UserDao userDao;
    private final PizzaDao pizzaDao;
    private final ComboDao comboDao;
    private final ToppingDao toppingDao;

    public CartService(
            CartDao cartDao,
            UserDao userDao,
            PizzaDao pizzaDao,
            ComboDao comboDao,
            ToppingDao toppingDao) {
        this.cartDao = cartDao;
        this.userDao = userDao;
        this.pizzaDao = pizzaDao;
        this.comboDao = comboDao;
        this.toppingDao = toppingDao;
    }

    public CartResponse getCart(String userEmail) {
        UUID userId = resolveUserId(userEmail);
        return cartDao.findByUserId(userId)
                .map(CartResponse::from)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addPizza(String userEmail, AddPizzaToCartRequest request) {
        UUID userId = resolveUserId(userEmail);
        Cart cart = getOrCreateCart(userId);

        Pizza pizza = pizzaDao.findById(request.getPizzaId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy pizza"));
        if (!pizza.isActive()) {
            throw new IllegalArgumentException("Pizza không còn hoạt động");
        }

        PizzaVariant variant = pizza.getVariants().stream()
                .filter(v -> v.getId().equals(request.getPizzaVariantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Size không hợp lệ cho pizza này"));

        List<UUID> toppingIds = request.getToppingIds() != null ? request.getToppingIds() : List.of();
        List<Topping> toppings = validateToppings(pizza, toppingIds);

        BigDecimal unitPrice = variant.getPrice().add(
                toppings.stream()
                        .map(Topping::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        int quantity = request.getQuantity();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        CartItem item = new CartItem();
        item.setCartId(cart.getId());
        item.setItemType(LineItemType.PIZZA);
        item.setPizzaId(pizza.getId());
        item.setPizzaVariantId(variant.getId());
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setLineTotal(lineTotal);

        CartItem saved = cartDao.insertItem(item);
        for (Topping topping : toppings) {
            cartDao.insertItemTopping(saved.getId(), topping.getId(), topping.getPrice());
        }

        cartDao.touchUpdatedAt(cart.getId());
        return reloadCart(userId);
    }

    @Transactional
    public CartResponse addCombo(String userEmail, AddComboToCartRequest request) {
        UUID userId = resolveUserId(userEmail);
        Cart cart = getOrCreateCart(userId);

        Combo combo = comboDao.findById(request.getComboId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy combo"));
        if (!combo.isActive()) {
            throw new IllegalArgumentException("Combo không còn hoạt động");
        }
        if (combo.getItems() == null || combo.getItems().isEmpty()) {
            throw new IllegalArgumentException("Combo không có pizza nào");
        }

        BigDecimal unitPrice = calculateComboUnitPrice(combo);
        int quantity = request.getQuantity();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        CartItem item = new CartItem();
        item.setCartId(cart.getId());
        item.setItemType(LineItemType.COMBO);
        item.setComboId(combo.getId());
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setLineTotal(lineTotal);

        CartItem saved = cartDao.insertItem(item);
        for (ComboItem comboItem : combo.getItems()) {
            CartItemComboLine line = new CartItemComboLine();
            line.setCartItemId(saved.getId());
            line.setPizzaId(comboItem.getPizzaId());
            line.setPizzaVariantId(comboItem.getPizzaVariantId());
            line.setQuantity(comboItem.getQuantity());
            line.setPizzaName(comboItem.getPizzaName());
            line.setPizzaSize(comboItem.getPizzaSize());
            cartDao.insertComboLine(line);
        }

        cartDao.touchUpdatedAt(cart.getId());
        return reloadCart(userId);
    }

    @Transactional
    public CartResponse updateItemQuantity(String userEmail, UUID itemId, UpdateCartItemRequest request) {
        UUID userId = resolveUserId(userEmail);
        CartItem item = findOwnedItem(userId, itemId);

        item.setQuantity(request.getQuantity());
        item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        cartDao.updateItemQuantity(item);
        cartDao.touchUpdatedAt(item.getCartId());

        return reloadCart(userId);
    }

    @Transactional
    public CartResponse removeItem(String userEmail, UUID itemId) {
        UUID userId = resolveUserId(userEmail);
        CartItem item = findOwnedItem(userId, itemId);

        cartDao.deleteItemById(item.getId());
        cartDao.touchUpdatedAt(item.getCartId());

        return reloadCart(userId);
    }

    @Transactional
    public CartResponse clearCart(String userEmail) {
        UUID userId = resolveUserId(userEmail);
        return cartDao.findByUserId(userId)
                .map(cart -> {
                    cartDao.deleteItemsByCartId(cart.getId());
                    cartDao.touchUpdatedAt(cart.getId());
                    return reloadCart(userId);
                })
                .orElseGet(CartResponse::empty);
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartDao.findByUserId(userId).orElseGet(() -> cartDao.createForUser(userId));
    }

    private CartResponse reloadCart(UUID userId) {
        return cartDao.findByUserId(userId)
                .map(CartResponse::from)
                .orElseGet(CartResponse::empty);
    }

    private CartItem findOwnedItem(UUID userId, UUID itemId) {
        CartItem item = cartDao.findItemById(itemId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy món trong giỏ hàng"));

        Cart cart = cartDao.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy món trong giỏ hàng"));

        if (!item.getCartId().equals(cart.getId())) {
            throw ApiException.notFound("Không tìm thấy món trong giỏ hàng");
        }

        return item;
    }

    private List<Topping> validateToppings(Pizza pizza, List<UUID> toppingIds) {
        if (toppingIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> uniqueIds = new HashSet<>(toppingIds);
        if (uniqueIds.size() != toppingIds.size()) {
            throw new IllegalArgumentException("Topping bị trùng lặp");
        }

        Set<UUID> allowedIds = new HashSet<>(pizza.getToppingIds());
        List<Topping> toppings = new ArrayList<>();

        for (UUID toppingId : toppingIds) {
            if (!allowedIds.contains(toppingId)) {
                throw new IllegalArgumentException("Topping không hợp lệ cho pizza này");
            }
            Topping topping = toppingDao.findById(toppingId)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy topping"));
            if (!topping.isActive()) {
                throw new IllegalArgumentException("Topping không còn hoạt động: " + topping.getName());
            }
            toppings.add(topping);
        }

        return toppings;
    }

    private BigDecimal calculateComboUnitPrice(Combo combo) {
        BigDecimal price = combo.getPrice();
        if (combo.getDiscountPercent() != null
                && combo.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    combo.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            price = price.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        }
        return price;
    }

    private UUID resolveUserId(String userEmail) {
        return userDao.findByEmail(userEmail)
                .map(pizza_cheese.todo.domain.User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }
}
