-- name: findAll
SELECT id, code, description, discount_type, discount_value, min_order_value, max_discount,
       start_date, end_date, usage_limit, used_count, per_user_limit,
       is_active AS active, created_at, updated_at
FROM coupons
WHERE (:activeOnly = FALSE OR is_active = TRUE)
ORDER BY created_at DESC

-- name: findById
SELECT id, code, description, discount_type, discount_value, min_order_value, max_discount,
       start_date, end_date, usage_limit, used_count, per_user_limit,
       is_active AS active, created_at, updated_at
FROM coupons
WHERE id = :id

-- name: findByCode
SELECT id, code, description, discount_type, discount_value, min_order_value, max_discount,
       start_date, end_date, usage_limit, used_count, per_user_limit,
       is_active AS active, created_at, updated_at
FROM coupons
WHERE UPPER(code) = UPPER(:code)

-- name: count
SELECT COUNT(*) FROM coupons

-- name: insert
INSERT INTO coupons (
    id, code, description, discount_type, discount_value, min_order_value, max_discount,
    start_date, end_date, usage_limit, used_count, per_user_limit, is_active, created_at, updated_at
)
VALUES (
    :id, :code, :description, :discountType, :discountValue, :minOrderValue, :maxDiscount,
    :startDate, :endDate, :usageLimit, :usedCount, :perUserLimit, :active, :createdAt, :updatedAt
)

-- name: update
UPDATE coupons
SET code = :code,
    description = :description,
    discount_type = :discountType,
    discount_value = :discountValue,
    min_order_value = :minOrderValue,
    max_discount = :maxDiscount,
    start_date = :startDate,
    end_date = :endDate,
    usage_limit = :usageLimit,
    per_user_limit = :perUserLimit,
    is_active = :active,
    updated_at = :updatedAt
WHERE id = :id

-- name: deactivate
UPDATE coupons SET is_active = FALSE, updated_at = :updatedAt WHERE id = :id

-- name: incrementUsedCount
UPDATE coupons
SET used_count = used_count + 1, updated_at = :updatedAt
WHERE id = :id
  AND (usage_limit IS NULL OR used_count < usage_limit)

-- name: insertUsage
INSERT INTO coupon_usages (id, coupon_id, user_id, order_id, used_at)
VALUES (:id, :couponId, :userId, :orderId, :usedAt)

-- name: countUsagesByUser
SELECT COUNT(*)
FROM coupon_usages
WHERE coupon_id = :couponId AND user_id = :userId
