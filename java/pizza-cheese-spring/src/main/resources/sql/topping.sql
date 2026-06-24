-- name: findAll
SELECT id, name, price, is_active AS active, created_at, updated_at
FROM toppings
WHERE (:activeOnly = FALSE OR is_active = TRUE)
ORDER BY name

-- name: findById
SELECT id, name, price, is_active AS active, created_at, updated_at
FROM toppings
WHERE id = :id

-- name: findByIds
SELECT id, name, price, is_active AS active, created_at, updated_at
FROM toppings
WHERE id IN (:ids)

-- name: insert
INSERT INTO toppings (id, name, price, is_active, created_at, updated_at)
VALUES (:id, :name, :price, :isActive, :createdAt, :updatedAt)

-- name: update
UPDATE toppings
SET name = :name,
    price = :price,
    is_active = :isActive,
    updated_at = :updatedAt
WHERE id = :id

-- name: deactivate
UPDATE toppings SET is_active = FALSE, updated_at = :updatedAt WHERE id = :id
