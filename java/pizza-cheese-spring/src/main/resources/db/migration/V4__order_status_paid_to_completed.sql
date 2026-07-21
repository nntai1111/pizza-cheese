-- Legacy OrderStatus.PAID (code 2) was never a real terminal state; remap to CONFIRMED.
-- COMPLETED keeps code 7 (formerly DELIVERED).

UPDATE orders SET status = 3 WHERE status = 2;
UPDATE order_status_history SET status = 3 WHERE status = 2;
