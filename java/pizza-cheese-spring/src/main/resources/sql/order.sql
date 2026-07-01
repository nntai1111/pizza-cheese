-- name: insert
INSERT INTO orders (
    id, order_code, user_id, address_id, status,
    total_amount, discount_amount, final_amount, coupon_id,
    payment_method_selected, note, estimated_delivery_time,
    kitchen_staff_id, delivery_staff_id, delivery_address_snapshot,
    created_at, updated_at
)
VALUES (
    :id, :orderCode, :userId, :addressId, :status,
    :totalAmount, :discountAmount, :finalAmount, :couponId,
    :paymentMethodSelected, :note, :estimatedDeliveryTime,
    :kitchenStaffId, :deliveryStaffId, CAST(:deliveryAddressSnapshot AS jsonb),
    :createdAt, :updatedAt
)

-- name: findById
SELECT id, order_code, user_id, address_id, status,
       total_amount, discount_amount, final_amount, coupon_id,
       payment_method_selected, note, estimated_delivery_time,
       kitchen_staff_id, delivery_staff_id, delivery_address_snapshot::text AS delivery_address_snapshot,
       created_at, updated_at
FROM orders
WHERE id = :id

-- name: findByIdAndUserId
SELECT id, order_code, user_id, address_id, status,
       total_amount, discount_amount, final_amount, coupon_id,
       payment_method_selected, note, estimated_delivery_time,
       kitchen_staff_id, delivery_staff_id, delivery_address_snapshot::text AS delivery_address_snapshot,
       created_at, updated_at
FROM orders
WHERE id = :id AND user_id = :userId

-- name: findByUserId
SELECT id, order_code, user_id, address_id, status,
       total_amount, discount_amount, final_amount, coupon_id,
       payment_method_selected, note, estimated_delivery_time,
       kitchen_staff_id, delivery_staff_id, delivery_address_snapshot::text AS delivery_address_snapshot,
       created_at, updated_at
FROM orders
WHERE user_id = :userId
ORDER BY created_at DESC

-- name: findAll
SELECT id, order_code, user_id, address_id, status,
       total_amount, discount_amount, final_amount, coupon_id,
       payment_method_selected, note, estimated_delivery_time,
       kitchen_staff_id, delivery_staff_id, delivery_address_snapshot::text AS delivery_address_snapshot,
       created_at, updated_at
FROM orders
ORDER BY created_at DESC

-- name: countAll
SELECT COUNT(*) FROM orders

-- name: findPage
SELECT id, order_code, user_id, address_id, status,
       total_amount, discount_amount, final_amount, coupon_id,
       payment_method_selected, note, estimated_delivery_time,
       kitchen_staff_id, delivery_staff_id, delivery_address_snapshot::text AS delivery_address_snapshot,
       created_at, updated_at
FROM orders
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset

-- name: findByStatus
SELECT id, order_code, user_id, address_id, status,
       total_amount, discount_amount, final_amount, coupon_id,
       payment_method_selected, note, estimated_delivery_time,
       kitchen_staff_id, delivery_staff_id, delivery_address_snapshot::text AS delivery_address_snapshot,
       created_at, updated_at
FROM orders
WHERE status = :status
ORDER BY created_at DESC

-- name: countByStatus
SELECT COUNT(*) FROM orders WHERE status = :status

-- name: findPageByStatus
SELECT id, order_code, user_id, address_id, status,
       total_amount, discount_amount, final_amount, coupon_id,
       payment_method_selected, note, estimated_delivery_time,
       kitchen_staff_id, delivery_staff_id, delivery_address_snapshot::text AS delivery_address_snapshot,
       created_at, updated_at
FROM orders
WHERE status = :status
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset

-- name: existsByOrderCode
SELECT COUNT(*) FROM orders WHERE order_code = :orderCode

-- name: updateStatus
UPDATE orders
SET status = :status,
    updated_at = :updatedAt
WHERE id = :id

-- name: insertStatusHistory
INSERT INTO order_status_history (id, order_id, status, changed_by, note, created_at)
VALUES (:id, :orderId, :status, :changedBy, :note, :createdAt)

-- name: insertItem
INSERT INTO order_items (
    id, order_id, item_type, pizza_id, pizza_variant_id, combo_id,
    quantity, unit_price, line_total
)
VALUES (
    :id, :orderId, :itemType, :pizzaId, :pizzaVariantId, :comboId,
    :quantity, :unitPrice, :lineTotal
)

-- name: insertItemTopping
INSERT INTO order_item_toppings (order_item_id, topping_id, price)
VALUES (:orderItemId, :toppingId, :price)

-- name: insertComboLine
INSERT INTO order_item_combo_lines (id, order_item_id, pizza_id, pizza_variant_id, quantity, pizza_name, pizza_size)
VALUES (:id, :orderItemId, :pizzaId, :pizzaVariantId, :quantity, :pizzaName, :pizzaSize)

-- name: findItemsByOrderId
SELECT oi.id,
       oi.order_id,
       oi.item_type,
       oi.pizza_id,
       oi.pizza_variant_id,
       oi.combo_id,
       oi.quantity,
       oi.unit_price,
       oi.line_total,
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
FROM order_items oi
LEFT JOIN pizzas p ON p.id = oi.pizza_id
LEFT JOIN pizza_variants pv ON pv.id = oi.pizza_variant_id
LEFT JOIN combos c ON c.id = oi.combo_id
WHERE oi.order_id = :orderId
ORDER BY oi.id

-- name: findToppingsByOrderItemId
SELECT oit.order_item_id,
       oit.topping_id,
       oit.price,
       t.name AS topping_name
FROM order_item_toppings oit
JOIN toppings t ON t.id = oit.topping_id
WHERE oit.order_item_id = :orderItemId
ORDER BY t.name

-- name: findComboLinesByOrderItemId
SELECT id,
       order_item_id,
       pizza_id,
       pizza_variant_id,
       quantity,
       pizza_name,
       pizza_size
FROM order_item_combo_lines
WHERE order_item_id = :orderItemId
ORDER BY pizza_name, pizza_size
