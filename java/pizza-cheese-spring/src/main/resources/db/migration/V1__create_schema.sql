-- =============================================================================
-- Pizza Store — Full Schema (PostgreSQL + UUID)
-- Single migration for fresh database (Supabase / local).
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. ENUM TYPES
-- -----------------------------------------------------------------------------
CREATE TYPE order_status AS ENUM (
    'PENDING_PAYMENT', 'PAID', 'CONFIRMED',
    'PREPARING', 'READY', 'OUT_FOR_DELIVERY',
    'DELIVERED', 'CANCELLED', 'REFUNDED'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING', 'PAID', 'FAILED', 'REFUNDED'
);

CREATE TYPE payment_method AS ENUM (
    'COD', 'VNPAY', 'MOMO', 'STRIPE'
);

CREATE TYPE discount_type AS ENUM ('PERCENT', 'FIXED');

CREATE TYPE pizza_size AS ENUM ('SMALL', 'MEDIUM', 'LARGE');

CREATE TYPE line_item_type AS ENUM ('PIZZA', 'COMBO');

-- -----------------------------------------------------------------------------
-- 2. USERS & RBAC
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles (name, description) VALUES
    ('CUSTOMER', 'Dat hang, xem menu'),
    ('CASHIER',  'Xac nhan thanh toan va don hang'),
    ('KITCHEN',  'Che bien mon'),
    ('DELIVERY', 'Giao hang'),
    ('ADMIN',    'Quan tri toan he thong');

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username           VARCHAR(50)  NOT NULL UNIQUE,
    email              VARCHAR(100) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    full_name          VARCHAR(100),
    phone              VARCHAR(20),
    avatar_url         TEXT,
    is_email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone ON users (phone);

-- -----------------------------------------------------------------------------
-- 3. AUTH TOKENS
-- -----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    device_info TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE email_verification_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_user_id ON email_verification_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_user_id ON password_reset_tokens (user_id);

-- -----------------------------------------------------------------------------
-- 4. ADDRESSES
-- -----------------------------------------------------------------------------
CREATE TABLE addresses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    address_line1 TEXT         NOT NULL,
    address_line2 TEXT,
    ward          VARCHAR(100),
    district      VARCHAR(100),
    city          VARCHAR(100) NOT NULL,
    latitude      DECIMAL(10, 8),
    longitude     DECIMAL(11, 8),
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);

-- -----------------------------------------------------------------------------
-- 5. MENU & PRODUCT
-- -----------------------------------------------------------------------------
CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    image_url   TEXT,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE pizzas (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID           REFERENCES categories (id),
    name        VARCHAR(150)   NOT NULL,
    slug        VARCHAR(150)   NOT NULL UNIQUE,
    description TEXT,
    base_price  DECIMAL(12, 2) NOT NULL,
    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pizzas_category_id ON pizzas (category_id);
CREATE INDEX idx_pizzas_search ON pizzas
    USING gin (to_tsvector('simple', name || ' ' || coalesce(description, '')));

CREATE TABLE pizza_variants (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pizza_id UUID           NOT NULL REFERENCES pizzas (id) ON DELETE CASCADE,
    size     pizza_size     NOT NULL,
    price    DECIMAL(12, 2) NOT NULL,
    UNIQUE (pizza_id, size)
);

CREATE TABLE pizza_images (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pizza_id   UUID    NOT NULL REFERENCES pizzas (id) ON DELETE CASCADE,
    image_url  TEXT    NOT NULL,
    is_main    BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_pizza_images_pizza_id ON pizza_images (pizza_id);

CREATE TABLE toppings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100)   NOT NULL,
    price      DECIMAL(10, 2) NOT NULL,
    is_active  BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE pizza_toppings (
    pizza_id   UUID NOT NULL REFERENCES pizzas (id) ON DELETE CASCADE,
    topping_id UUID NOT NULL REFERENCES toppings (id) ON DELETE CASCADE,
    PRIMARY KEY (pizza_id, topping_id)
);

CREATE TABLE combos (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(150)   NOT NULL,
    slug             VARCHAR(150)   NOT NULL UNIQUE,
    description      TEXT,
    price            DECIMAL(12, 2) NOT NULL,
    discount_percent DECIMAL(5, 2),
    image_url        TEXT,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE combo_items (
    combo_id UUID NOT NULL REFERENCES combos (id) ON DELETE CASCADE,
    pizza_id UUID NOT NULL REFERENCES pizzas (id) ON DELETE CASCADE,
    quantity INT  NOT NULL DEFAULT 1 CHECK (quantity > 0),
    PRIMARY KEY (combo_id, pizza_id)
);

-- -----------------------------------------------------------------------------
-- 6. CART
-- -----------------------------------------------------------------------------
CREATE TABLE carts (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        REFERENCES users (id) ON DELETE CASCADE,
    session_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT carts_owner_check CHECK (
        (user_id IS NOT NULL AND session_id IS NULL)
        OR (user_id IS NULL AND session_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_carts_user_id ON carts (user_id) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX idx_carts_session_id ON carts (session_id) WHERE session_id IS NOT NULL;

CREATE TABLE cart_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id          UUID           NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    item_type        line_item_type NOT NULL,
    pizza_id         UUID           REFERENCES pizzas (id),
    pizza_variant_id UUID           REFERENCES pizza_variants (id),
    combo_id         UUID           REFERENCES combos (id),
    quantity         INT            NOT NULL CHECK (quantity > 0),
    unit_price       DECIMAL(12, 2) NOT NULL,
    line_total       DECIMAL(12, 2) NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT cart_items_type_check CHECK (
        (item_type = 'PIZZA' AND pizza_id IS NOT NULL AND pizza_variant_id IS NOT NULL AND combo_id IS NULL)
        OR (item_type = 'COMBO' AND combo_id IS NOT NULL AND pizza_id IS NULL AND pizza_variant_id IS NULL)
    )
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

CREATE TABLE cart_item_toppings (
    cart_item_id UUID           NOT NULL REFERENCES cart_items (id) ON DELETE CASCADE,
    topping_id   UUID           NOT NULL REFERENCES toppings (id),
    price        DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (cart_item_id, topping_id)
);

-- -----------------------------------------------------------------------------
-- 7. COUPONS
-- -----------------------------------------------------------------------------
CREATE TABLE coupons (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50)    NOT NULL UNIQUE,
    description     TEXT,
    discount_type   discount_type  NOT NULL,
    discount_value  DECIMAL(12, 2) NOT NULL,
    min_order_value DECIMAL(12, 2),
    max_discount    DECIMAL(12, 2),
    start_date      TIMESTAMPTZ,
    end_date        TIMESTAMPTZ,
    usage_limit     INT,
    used_count      INT            NOT NULL DEFAULT 0,
    per_user_limit  INT,
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 8. ORDERS
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code                VARCHAR(20)    NOT NULL UNIQUE,
    user_id                   UUID           NOT NULL REFERENCES users (id),
    address_id                UUID           REFERENCES addresses (id),
    status                    order_status   NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount              DECIMAL(12, 2) NOT NULL,
    discount_amount           DECIMAL(12, 2) NOT NULL DEFAULT 0,
    final_amount              DECIMAL(12, 2) NOT NULL,
    coupon_id                 UUID           REFERENCES coupons (id),
    payment_method_selected   payment_method,
    note                      TEXT,
    estimated_delivery_time   TIMESTAMPTZ,
    kitchen_staff_id          UUID           REFERENCES users (id),
    delivery_staff_id         UUID           REFERENCES users (id),
    delivery_address_snapshot JSONB          NOT NULL DEFAULT '{}',
    created_at                TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_order_code ON orders (order_code);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_kitchen_staff ON orders (kitchen_staff_id);
CREATE INDEX idx_orders_delivery_staff ON orders (delivery_staff_id);

CREATE TABLE order_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    item_type        line_item_type NOT NULL,
    pizza_id         UUID           REFERENCES pizzas (id),
    pizza_variant_id UUID           REFERENCES pizza_variants (id),
    combo_id         UUID           REFERENCES combos (id),
    quantity         INT            NOT NULL CHECK (quantity > 0),
    unit_price       DECIMAL(12, 2) NOT NULL,
    line_total       DECIMAL(12, 2) NOT NULL,
    CONSTRAINT order_items_type_check CHECK (
        (item_type = 'PIZZA' AND pizza_id IS NOT NULL AND pizza_variant_id IS NOT NULL AND combo_id IS NULL)
        OR (item_type = 'COMBO' AND combo_id IS NOT NULL AND pizza_id IS NULL AND pizza_variant_id IS NULL)
    )
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE order_item_toppings (
    order_item_id UUID           NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    topping_id    UUID           NOT NULL REFERENCES toppings (id),
    price         DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (order_item_id, topping_id)
);

CREATE TABLE order_status_history (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    status     order_status NOT NULL,
    changed_by UUID         REFERENCES users (id),
    note       TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);

-- -----------------------------------------------------------------------------
-- 9. PAYMENTS
-- -----------------------------------------------------------------------------
CREATE TABLE payments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    payment_method payment_method NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    transaction_id VARCHAR(100),
    status         payment_status NOT NULL DEFAULT 'PENDING',
    payment_url    TEXT,
    callback_data  JSONB,
    paid_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE coupon_usages (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id UUID        NOT NULL REFERENCES coupons (id),
    user_id   UUID        NOT NULL REFERENCES users (id),
    order_id  UUID        NOT NULL REFERENCES orders (id),
    used_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (coupon_id, order_id)
);

CREATE INDEX idx_coupon_usages_user_id ON coupon_usages (user_id);

-- -----------------------------------------------------------------------------
-- 10. REVIEWS & AUDIT
-- -----------------------------------------------------------------------------
CREATE TABLE reviews (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID        NOT NULL REFERENCES orders (id),
    user_id    UUID        NOT NULL REFERENCES users (id),
    pizza_id   UUID        NOT NULL REFERENCES pizzas (id),
    rating     INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (order_id, user_id, pizza_id)
);

CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type  VARCHAR(50) NOT NULL,
    entity_id    UUID        NOT NULL,
    action       VARCHAR(50) NOT NULL,
    performed_by UUID        REFERENCES users (id),
    old_values   JSONB,
    new_values   JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

-- -----------------------------------------------------------------------------
-- 11. UPDATED_AT TRIGGERS
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_addresses_updated_at
    BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_pizzas_updated_at
    BEFORE UPDATE ON pizzas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_toppings_updated_at
    BEFORE UPDATE ON toppings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_combos_updated_at
    BEFORE UPDATE ON combos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_cart_items_updated_at
    BEFORE UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_coupons_updated_at
    BEFORE UPDATE ON coupons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
