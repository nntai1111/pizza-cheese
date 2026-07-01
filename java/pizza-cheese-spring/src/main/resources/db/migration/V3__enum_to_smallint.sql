-- Chuyển PostgreSQL ENUM sang SMALLINT (code tường minh, không dùng ordinal).

-- order_status: 1=PENDING_PAYMENT ... 9=REFUNDED
ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN status TYPE SMALLINT USING (
    CASE status::text
        WHEN 'PENDING_PAYMENT' THEN 1
        WHEN 'PAID' THEN 2
        WHEN 'CONFIRMED' THEN 3
        WHEN 'PREPARING' THEN 4
        WHEN 'READY' THEN 5
        WHEN 'OUT_FOR_DELIVERY' THEN 6
        WHEN 'DELIVERED' THEN 7
        WHEN 'CANCELLED' THEN 8
        WHEN 'REFUNDED' THEN 9
    END
);
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 1;

ALTER TABLE order_status_history ALTER COLUMN status TYPE SMALLINT USING (
    CASE status::text
        WHEN 'PENDING_PAYMENT' THEN 1
        WHEN 'PAID' THEN 2
        WHEN 'CONFIRMED' THEN 3
        WHEN 'PREPARING' THEN 4
        WHEN 'READY' THEN 5
        WHEN 'OUT_FOR_DELIVERY' THEN 6
        WHEN 'DELIVERED' THEN 7
        WHEN 'CANCELLED' THEN 8
        WHEN 'REFUNDED' THEN 9
    END
);

-- payment_method: 1=COD, 2=VNPAY, 3=MOMO, 4=STRIPE
ALTER TABLE orders ALTER COLUMN payment_method_selected TYPE SMALLINT USING (
    CASE payment_method_selected::text
        WHEN 'COD' THEN 1
        WHEN 'VNPAY' THEN 2
        WHEN 'MOMO' THEN 3
        WHEN 'STRIPE' THEN 4
    END
);

ALTER TABLE payments ALTER COLUMN payment_method TYPE SMALLINT USING (
    CASE payment_method::text
        WHEN 'COD' THEN 1
        WHEN 'VNPAY' THEN 2
        WHEN 'MOMO' THEN 3
        WHEN 'STRIPE' THEN 4
    END
);

-- payment_status: 1=PENDING, 2=PAID, 3=FAILED, 4=REFUNDED
ALTER TABLE payments ALTER COLUMN status DROP DEFAULT;
ALTER TABLE payments ALTER COLUMN status TYPE SMALLINT USING (
    CASE status::text
        WHEN 'PENDING' THEN 1
        WHEN 'PAID' THEN 2
        WHEN 'FAILED' THEN 3
        WHEN 'REFUNDED' THEN 4
    END
);
ALTER TABLE payments ALTER COLUMN status SET DEFAULT 1;

-- pizza_size: 1=SMALL, 2=MEDIUM, 3=LARGE
ALTER TABLE pizza_variants ALTER COLUMN size TYPE SMALLINT USING (
    CASE size::text
        WHEN 'SMALL' THEN 1
        WHEN 'MEDIUM' THEN 2
        WHEN 'LARGE' THEN 3
    END
);

ALTER TABLE cart_item_combo_lines ALTER COLUMN pizza_size TYPE SMALLINT USING (
    CASE pizza_size::text
        WHEN 'SMALL' THEN 1
        WHEN 'MEDIUM' THEN 2
        WHEN 'LARGE' THEN 3
    END
);

ALTER TABLE order_item_combo_lines ALTER COLUMN pizza_size TYPE SMALLINT USING (
    CASE pizza_size::text
        WHEN 'SMALL' THEN 1
        WHEN 'MEDIUM' THEN 2
        WHEN 'LARGE' THEN 3
    END
);

-- line_item_type: 1=PIZZA, 2=COMBO
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS cart_items_type_check;
ALTER TABLE cart_items ALTER COLUMN item_type TYPE SMALLINT USING (
    CASE item_type::text
        WHEN 'PIZZA' THEN 1
        WHEN 'COMBO' THEN 2
    END
);
ALTER TABLE cart_items ADD CONSTRAINT cart_items_type_check CHECK (
    (item_type = 1 AND pizza_id IS NOT NULL AND pizza_variant_id IS NOT NULL AND combo_id IS NULL)
    OR (item_type = 2 AND combo_id IS NOT NULL AND pizza_id IS NULL AND pizza_variant_id IS NULL)
);

ALTER TABLE order_items DROP CONSTRAINT IF EXISTS order_items_type_check;
ALTER TABLE order_items ALTER COLUMN item_type TYPE SMALLINT USING (
    CASE item_type::text
        WHEN 'PIZZA' THEN 1
        WHEN 'COMBO' THEN 2
    END
);
ALTER TABLE order_items ADD CONSTRAINT order_items_type_check CHECK (
    (item_type = 1 AND pizza_id IS NOT NULL AND pizza_variant_id IS NOT NULL AND combo_id IS NULL)
    OR (item_type = 2 AND combo_id IS NOT NULL AND pizza_id IS NULL AND pizza_variant_id IS NULL)
);

-- discount_type: 1=PERCENT, 2=FIXED
ALTER TABLE coupons ALTER COLUMN discount_type TYPE SMALLINT USING (
    CASE discount_type::text
        WHEN 'PERCENT' THEN 1
        WHEN 'FIXED' THEN 2
    END
);

DROP TYPE IF EXISTS order_status;
DROP TYPE IF EXISTS payment_status;
DROP TYPE IF EXISTS payment_method;
DROP TYPE IF EXISTS discount_type;
DROP TYPE IF EXISTS pizza_size;
DROP TYPE IF EXISTS line_item_type;
