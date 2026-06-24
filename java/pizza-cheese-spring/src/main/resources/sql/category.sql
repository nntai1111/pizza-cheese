-- name: findAll
SELECT id, name, slug, description, image_url, sort_order, is_active AS active, created_at, updated_at
FROM categories
WHERE (:activeOnly = FALSE OR is_active = TRUE)
ORDER BY sort_order, name

-- name: findById
SELECT id, name, slug, description, image_url, sort_order, is_active AS active, created_at, updated_at
FROM categories
WHERE id = :id

-- name: existsBySlug
SELECT EXISTS (SELECT 1 FROM categories WHERE slug = :slug)

-- name: existsBySlugExcludingId
SELECT EXISTS (SELECT 1 FROM categories WHERE slug = :slug AND id != :id)

-- name: insert
INSERT INTO categories (id, name, slug, description, image_url, sort_order, is_active, created_at, updated_at)
VALUES (:id, :name, :slug, :description, :imageUrl, :sortOrder, :isActive, :createdAt, :updatedAt)

-- name: update
UPDATE categories
SET name = :name,
    slug = :slug,
    description = :description,
    image_url = :imageUrl,
    sort_order = :sortOrder,
    is_active = :isActive,
    updated_at = :updatedAt
WHERE id = :id

-- name: deactivate
UPDATE categories SET is_active = FALSE, updated_at = :updatedAt WHERE id = :id
