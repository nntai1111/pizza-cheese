-- name: findAll
SELECT p.id, p.category_id, p.name, p.slug, p.description, p.base_price, p.is_active,
       p.created_at, p.updated_at,
       c.name AS category_name, c.slug AS category_slug
FROM pizzas p
LEFT JOIN categories c ON c.id = p.category_id
WHERE (:activeOnly = FALSE OR p.is_active = TRUE)
  AND (:filterByCategory = FALSE OR p.category_id = :categoryId)
ORDER BY p.created_at DESC

-- name: countAll
SELECT COUNT(*)
FROM pizzas p
WHERE (:activeOnly = FALSE OR p.is_active = TRUE)
  AND (:filterByCategory = FALSE OR p.category_id = :categoryId)

-- name: findPage
SELECT p.id, p.category_id, p.name, p.slug, p.description, p.base_price, p.is_active,
       p.created_at, p.updated_at,
       c.name AS category_name, c.slug AS category_slug
FROM pizzas p
LEFT JOIN categories c ON c.id = p.category_id
WHERE (:activeOnly = FALSE OR p.is_active = TRUE)
  AND (:filterByCategory = FALSE OR p.category_id = :categoryId)
ORDER BY p.created_at DESC
LIMIT :limit OFFSET :offset

-- name: findById
SELECT p.id, p.category_id, p.name, p.slug, p.description, p.base_price, p.is_active,
       p.created_at, p.updated_at,
       c.name AS category_name, c.slug AS category_slug
FROM pizzas p
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.id = :id

-- name: existsBySlug
SELECT EXISTS (SELECT 1 FROM pizzas WHERE slug = :slug)

-- name: existsBySlugExcludingId
SELECT EXISTS (SELECT 1 FROM pizzas WHERE slug = :slug AND id != :id)

-- name: insert
INSERT INTO pizzas (id, category_id, name, slug, description, base_price, is_active, created_at, updated_at)
VALUES (:id, :categoryId, :name, :slug, :description, :basePrice, :isActive, :createdAt, :updatedAt)

-- name: update
UPDATE pizzas
SET category_id = :categoryId,
    name = :name,
    slug = :slug,
    description = :description,
    base_price = :basePrice,
    is_active = :isActive,
    updated_at = :updatedAt
WHERE id = :id

-- name: deactivate
UPDATE pizzas SET is_active = FALSE, updated_at = :updatedAt WHERE id = :id

-- name: findVariantsByPizzaId
SELECT id, pizza_id, size, price
FROM pizza_variants
WHERE pizza_id = :pizzaId
ORDER BY size

-- name: deleteVariantsByPizzaId
DELETE FROM pizza_variants WHERE pizza_id = :pizzaId

-- name: insertVariant
INSERT INTO pizza_variants (id, pizza_id, size, price)
VALUES (:id, :pizzaId, CAST(:size AS pizza_size), :price)

-- name: findToppingsByPizzaId
SELECT t.id, t.name, t.price, t.is_active, t.created_at, t.updated_at
FROM pizza_toppings pt
JOIN toppings t ON t.id = pt.topping_id
WHERE pt.pizza_id = :pizzaId
ORDER BY t.name

-- name: deleteToppingsByPizzaId
DELETE FROM pizza_toppings WHERE pizza_id = :pizzaId

-- name: insertPizzaTopping
INSERT INTO pizza_toppings (pizza_id, topping_id)
VALUES (:pizzaId, :toppingId)

-- name: findImagesByPizzaId
SELECT id, pizza_id, image_url, is_main, sort_order
FROM pizza_images
WHERE pizza_id = :pizzaId
ORDER BY sort_order

-- name: deleteImagesByPizzaId
DELETE FROM pizza_images WHERE pizza_id = :pizzaId

-- name: insertImage
INSERT INTO pizza_images (id, pizza_id, image_url, is_main, sort_order)
VALUES (:id, :pizzaId, :imageUrl, :isMain, :sortOrder)
