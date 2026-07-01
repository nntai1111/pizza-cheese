-- name: findByUserId
SELECT id, user_id, created_at, updated_at
FROM carts
WHERE user_id = :userId

-- name: insert
INSERT INTO carts (id, user_id, session_id, created_at, updated_at)
VALUES (:id, :userId, NULL, :createdAt, :updatedAt)

-- name: touchUpdatedAt
UPDATE carts SET updated_at = :updatedAt WHERE id = :id

-- name: findItemsByCartId
SELECT ci.id,
       ci.cart_id,
       ci.item_type,
       ci.pizza_id,
       ci.pizza_variant_id,
       ci.combo_id,
       ci.quantity,
       ci.unit_price,
       ci.line_total,
       ci.created_at,
       ci.updated_at,
       p.name AS pizza_name,
       p.slug AS pizza_slug,
       pv.size AS pizza_size,
       (SELECT pi.image_url
        FROM pizza_images pi
        WHERE pi.pizza_id = p.id AND pi.is_main = TRUE
        ORDER BY pi.sort_order
        LIMIT 1) AS pizza_image_url,
       c.name AS combo_name,
       c.slug AS combo_slug,
       c.image_url AS combo_image_url
FROM cart_items ci
LEFT JOIN pizzas p ON p.id = ci.pizza_id
LEFT JOIN pizza_variants pv ON pv.id = ci.pizza_variant_id
LEFT JOIN combos c ON c.id = ci.combo_id
WHERE ci.cart_id = :cartId
ORDER BY ci.created_at

-- name: findToppingsByCartItemId
SELECT cit.cart_item_id,
       cit.topping_id,
       cit.price,
       t.name AS topping_name
FROM cart_item_toppings cit
JOIN toppings t ON t.id = cit.topping_id
WHERE cit.cart_item_id = :cartItemId
ORDER BY t.name

-- name: findComboLinesByCartItemId
SELECT id,
       cart_item_id,
       pizza_id,
       pizza_variant_id,
       quantity,
       pizza_name,
       pizza_size
FROM cart_item_combo_lines
WHERE cart_item_id = :cartItemId
ORDER BY pizza_name, pizza_size

-- name: findItemById
SELECT id, cart_id, item_type, pizza_id, pizza_variant_id, combo_id, quantity, unit_price, line_total, created_at, updated_at
FROM cart_items
WHERE id = :id

-- name: insertItem
INSERT INTO cart_items (id, cart_id, item_type, pizza_id, pizza_variant_id, combo_id, quantity, unit_price, line_total, created_at, updated_at)
VALUES (:id, :cartId, :itemType, :pizzaId, :pizzaVariantId, :comboId, :quantity, :unitPrice, :lineTotal, :createdAt, :updatedAt)

-- name: insertItemTopping
INSERT INTO cart_item_toppings (cart_item_id, topping_id, price)
VALUES (:cartItemId, :toppingId, :price)

-- name: insertComboLine
INSERT INTO cart_item_combo_lines (id, cart_item_id, pizza_id, pizza_variant_id, quantity, pizza_name, pizza_size)
VALUES (:id, :cartItemId, :pizzaId, :pizzaVariantId, :quantity, :pizzaName, :pizzaSize)

-- name: updateItemQuantity
UPDATE cart_items
SET quantity = :quantity,
    line_total = :lineTotal,
    updated_at = :updatedAt
WHERE id = :id

-- name: deleteItemById
DELETE FROM cart_items WHERE id = :id

-- name: deleteItemsByCartId
DELETE FROM cart_items WHERE cart_id = :cartId
