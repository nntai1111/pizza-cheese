-- Combo: mỗi pizza trong combo phải có size (variant) để bếp biết làm món gì.
-- Cart/order: snapshot chi tiết combo tại thời điểm đặt (không phụ thuộc menu sau này).

ALTER TABLE combo_items
    ADD COLUMN pizza_variant_id UUID REFERENCES pizza_variants (id);

UPDATE combo_items ci
SET pizza_variant_id = (
    SELECT pv.id
    FROM pizza_variants pv
    WHERE pv.pizza_id = ci.pizza_id
    ORDER BY pv.size
    LIMIT 1
)
WHERE pizza_variant_id IS NULL;

ALTER TABLE combo_items
    ALTER COLUMN pizza_variant_id SET NOT NULL;

ALTER TABLE combo_items DROP CONSTRAINT combo_items_pkey;
ALTER TABLE combo_items
    ADD PRIMARY KEY (combo_id, pizza_id, pizza_variant_id);

-- Chi tiết combo trong giỏ (snapshot khi khách thêm combo)
CREATE TABLE cart_item_combo_lines (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_item_id     UUID        NOT NULL REFERENCES cart_items (id) ON DELETE CASCADE,
    pizza_id         UUID        NOT NULL REFERENCES pizzas (id),
    pizza_variant_id UUID        NOT NULL REFERENCES pizza_variants (id),
    quantity         INT         NOT NULL CHECK (quantity > 0),
    pizza_name       VARCHAR(150) NOT NULL,
    pizza_size       pizza_size  NOT NULL
);

CREATE INDEX idx_cart_item_combo_lines_cart_item ON cart_item_combo_lines (cart_item_id);

-- Chi tiết combo trong đơn hàng (bếp đọc từ đây)
CREATE TABLE order_item_combo_lines (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id    UUID        NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    pizza_id         UUID        NOT NULL REFERENCES pizzas (id),
    pizza_variant_id UUID        NOT NULL REFERENCES pizza_variants (id),
    quantity         INT         NOT NULL CHECK (quantity > 0),
    pizza_name       VARCHAR(150) NOT NULL,
    pizza_size       pizza_size  NOT NULL
);

CREATE INDEX idx_order_item_combo_lines_order_item ON order_item_combo_lines (order_item_id);
