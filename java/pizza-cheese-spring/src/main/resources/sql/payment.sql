-- name: insert
INSERT INTO payments (
    id, order_id, payment_method, amount, transaction_id,
    status, payment_url, callback_data, paid_at, created_at, updated_at
)
VALUES (
    :id, :orderId, CAST(:paymentMethod AS payment_method), :amount, :transactionId,
    CAST(:status AS payment_status), :paymentUrl, CAST(:callbackData AS jsonb), :paidAt, :createdAt, :updatedAt
)

-- name: findById
SELECT id, order_id, payment_method, amount, transaction_id,
       status, payment_url, callback_data::text AS callback_data, paid_at, created_at, updated_at
FROM payments
WHERE id = :id

-- name: findLatestByOrderId
SELECT id, order_id, payment_method, amount, transaction_id,
       status, payment_url, callback_data::text AS callback_data, paid_at, created_at, updated_at
FROM payments
WHERE order_id = :orderId
ORDER BY created_at DESC
LIMIT 1

-- name: findByTransactionId
SELECT id, order_id, payment_method, amount, transaction_id,
       status, payment_url, callback_data::text AS callback_data, paid_at, created_at, updated_at
FROM payments
WHERE transaction_id = :transactionId
ORDER BY created_at DESC
LIMIT 1

-- name: updateStatus
UPDATE payments
SET status = CAST(:status AS payment_status),
    callback_data = CAST(:callbackData AS jsonb),
    paid_at = :paidAt,
    transaction_id = COALESCE(:transactionId, transaction_id),
    updated_at = :updatedAt
WHERE id = :id

-- name: updatePaymentUrl
UPDATE payments
SET payment_url = :paymentUrl,
    updated_at = :updatedAt
WHERE id = :id
