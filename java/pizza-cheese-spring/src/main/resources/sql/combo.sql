-- name: findAll
SELECT id, name, slug, description, price, discount_percent, image_url, is_active AS active, created_at, updated_at
FROM combos
WHERE (:activeOnly = FALSE OR is_active = TRUE)
ORDER BY name

-- name: countAll
SELECT COUNT(*)
FROM combos
WHERE (:activeOnly = FALSE OR is_active = TRUE)

-- name: findPage
SELECT id, name, slug, description, price, discount_percent, image_url, is_active AS active, created_at, updated_at
FROM combos
WHERE (:activeOnly = FALSE OR is_active = TRUE)
ORDER BY name
LIMIT :limit OFFSET :offset

-- name: findById
SELECT id, name, slug, description, price, discount_percent, image_url, is_active AS active, created_at, updated_at
FROM combos
WHERE id = :id

-- name: existsBySlug
SELECT EXISTS (SELECT 1 FROM combos WHERE slug = :slug)

-- name: existsBySlugExcludingId
SELECT EXISTS (SELECT 1 FROM combos WHERE slug = :slug AND id != :id)

-- name: insert
INSERT INTO combos (id, name, slug, description, price, discount_percent, image_url, is_active, created_at, updated_at)
VALUES (:id, :name, :slug, :description, :price, :discountPercent, :imageUrl, :isActive, :createdAt, :updatedAt)

-- name: update
UPDATE combos
SET name = :name,
    slug = :slug,
    description = :description,
    price = :price,
    discount_percent = :discountPercent,
    image_url = :imageUrl,
    is_active = :isActive,
    updated_at = :updatedAt
WHERE id = :id

-- name: deactivate
UPDATE combos SET is_active = FALSE, updated_at = :updatedAt WHERE id = :id

-- name: findItemsByComboId
SELECT ci.combo_id,
       ci.pizza_id,
       ci.pizza_variant_id,
       ci.quantity,
       p.name AS pizza_name,
       p.slug AS pizza_slug,
       pv.size::text AS pizza_size
FROM combo_items ci
JOIN pizzas p ON p.id = ci.pizza_id
JOIN pizza_variants pv ON pv.id = ci.pizza_variant_id
WHERE ci.combo_id = :comboId
ORDER BY p.name, pv.size

-- name: deleteItemsByComboId
DELETE FROM combo_items WHERE combo_id = :comboId

-- name: insertItem
INSERT INTO combo_items (combo_id, pizza_id, pizza_variant_id, quantity)
VALUES (:comboId, :pizzaId, :pizzaVariantId, :quantity)
