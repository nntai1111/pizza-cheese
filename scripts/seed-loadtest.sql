-- Load-test seed for Docker Postgres only.
-- Targets (~1M+ transactional rows):
--   10k users, 10k addresses, 30 pizzas, 300k orders, ~600k items, 300k payments
--
-- Usage (from repo root, Docker up + Flyway already applied):
--   .\scripts\seed-loadtest.ps1
--
-- Safe to re-run: clears previous loadtest_* data first (keeps staff users from DataInitializer).

BEGIN;

-- Tunables
-- change these if you want a smaller smoke run first
-- \set users 10000
-- (psql variables below)

DO $$
DECLARE
  v_users          INT := 10000;
  v_orders         INT := 300000;
  v_pizzas         INT := 30;
  v_customer_role  UUID;
  v_pwd            TEXT;
  v_cat_ids        UUID[];
  v_variant_ids    UUID[];
  v_user_ids       UUID[];
  v_addr_ids       UUID[];
BEGIN
  SELECT id INTO v_customer_role FROM roles WHERE name = 'CUSTOMER';
  IF v_customer_role IS NULL THEN
    RAISE EXCEPTION 'CUSTOMER role missing — run Flyway first';
  END IF;

  -- BCrypt for password "123456" (Spring BCryptPasswordEncoder)
  v_pwd := '$2a$10$6Pm0buXCQsl/SV8.rtnYeeieyOEyIOAJ5veFU0Gmu6RW1hQwnu6b.';

  -- Ensure at least one staff-like login exists for manual testing
  IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@gmail.com') THEN
    INSERT INTO users (username, email, password_hash, full_name, is_email_verified, is_active)
    VALUES ('admin', 'admin@gmail.com', v_pwd, 'Admin User', TRUE, TRUE);
    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u CROSS JOIN roles r
    WHERE u.email = 'admin@gmail.com' AND r.name = 'ADMIN';
  END IF;

  RAISE NOTICE 'Cleaning previous load-test rows...';

  DELETE FROM payments p
  USING orders o
  WHERE p.order_id = o.id AND o.order_code LIKE 'LT%';

  DELETE FROM order_items oi
  USING orders o
  WHERE oi.order_id = o.id AND o.order_code LIKE 'LT%';

  DELETE FROM order_status_history h
  USING orders o
  WHERE h.order_id = o.id AND o.order_code LIKE 'LT%';

  DELETE FROM orders WHERE order_code LIKE 'LT%';

  DELETE FROM addresses a
  USING users u
  WHERE a.user_id = u.id AND u.username LIKE 'loaduser_%';

  DELETE FROM user_roles ur
  USING users u
  WHERE ur.user_id = u.id AND u.username LIKE 'loaduser_%';

  DELETE FROM users WHERE username LIKE 'loaduser_%';

  DELETE FROM pizza_variants pv
  USING pizzas p
  WHERE pv.pizza_id = p.id AND p.slug LIKE 'load-pizza-%';

  DELETE FROM pizzas WHERE slug LIKE 'load-pizza-%';
  DELETE FROM categories WHERE slug LIKE 'load-cat-%';

  RAISE NOTICE 'Seeding catalog...';

  WITH c AS (
    INSERT INTO categories (name, slug, description, sort_order)
    SELECT
      'Load Cat ' || i,
      'load-cat-' || i,
      'Load-test category',
      i
    FROM generate_series(1, 4) AS i
    RETURNING id
  )
  SELECT array_agg(id) INTO v_cat_ids FROM c;

  WITH p AS (
    INSERT INTO pizzas (category_id, name, slug, description, base_price)
    SELECT
      v_cat_ids[1 + ((i - 1) % array_length(v_cat_ids, 1))],
      'Load Pizza ' || i,
      'load-pizza-' || i,
      'Seed pizza for load test',
      (99000 + (i % 10) * 10000)::numeric
    FROM generate_series(1, v_pizzas) AS i
    RETURNING id, base_price
  ),
  v AS (
    INSERT INTO pizza_variants (pizza_id, size, price)
    SELECT
      p.id,
      s.size_code,
      round(p.base_price * s.mult, 2)
    FROM p
    CROSS JOIN (VALUES (1, 1.0), (2, 1.2), (3, 1.4)) AS s(size_code, mult)
    RETURNING id
  )
  SELECT array_agg(id) INTO v_variant_ids FROM v;

  RAISE NOTICE 'Seeding % users + addresses...', v_users;

  WITH u AS (
    INSERT INTO users (username, email, password_hash, full_name, phone, is_email_verified, is_active)
    SELECT
      'loaduser_' || i,
      'loaduser_' || i || '@example.com',
      v_pwd,
      'Load User ' || i,
      '09' || lpad((i % 100000000)::text, 8, '0'),
      TRUE,
      TRUE
    FROM generate_series(1, v_users) AS i
    RETURNING id
  )
  SELECT array_agg(id) INTO v_user_ids FROM u;

  INSERT INTO user_roles (user_id, role_id)
  SELECT uid, v_customer_role FROM unnest(v_user_ids) AS uid;

  WITH a AS (
    INSERT INTO addresses (user_id, address_line1, ward, district, city, is_default)
    SELECT
      v_user_ids[i],
      i || ' Nguyen Hue',
      'Ben Nghe',
      'District 1',
      'Ho Chi Minh',
      TRUE
    FROM generate_series(1, v_users) AS i
    RETURNING id
  )
  SELECT array_agg(id) INTO v_addr_ids FROM a;

  RAISE NOTICE 'Seeding % orders (this may take a few minutes)...', v_orders;

  -- Create orders in one shot; store ids via temp table for items/payments
  CREATE TEMP TABLE tmp_seed_orders (
    seq INT PRIMARY KEY,
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    address_id UUID NOT NULL,
    status SMALLINT NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    final_amount NUMERIC(12,2) NOT NULL,
    payment_method SMALLINT NOT NULL
  ) ON COMMIT DROP;

  INSERT INTO tmp_seed_orders (seq, id, user_id, address_id, status, total_amount, final_amount, payment_method)
  SELECT
    i,
    gen_random_uuid(),
    v_user_ids[1 + ((i - 1) % v_users)],
    v_addr_ids[1 + ((i - 1) % v_users)],
    (ARRAY[2,3,4,5,6,7,8])[1 + ((i - 1) % 7)], -- skip PENDING_PAYMENT mostly
    150000 + ((i % 20) * 10000),
    150000 + ((i % 20) * 10000),
    (ARRAY[1,2])[1 + ((i - 1) % 2)]
  FROM generate_series(1, v_orders) AS i;

  INSERT INTO orders (
    id, order_code, user_id, address_id, status,
    total_amount, discount_amount, final_amount,
    payment_method_selected, delivery_address_snapshot, created_at, updated_at
  )
  SELECT
    t.id,
    'LT' || lpad(t.seq::text, 18, '0'), -- VARCHAR(20), unique
    t.user_id,
    t.address_id,
    t.status,
    t.total_amount,
    0,
    t.final_amount,
    t.payment_method,
    jsonb_build_object(
      'addressLine1', 'Seed address',
      'city', 'Ho Chi Minh'
    ),
    NOW() - ((t.seq % 90) || ' days')::interval - ((t.seq % 86400) || ' seconds')::interval,
    NOW() - ((t.seq % 90) || ' days')::interval
  FROM tmp_seed_orders t;

  RAISE NOTICE 'Seeding order_items (~2 per order)...';

  INSERT INTO order_items (order_id, item_type, pizza_id, pizza_variant_id, combo_id, quantity, unit_price, line_total)
  SELECT
    t.id,
    1, -- PIZZA
    pv.pizza_id,
    pv.id,
    NULL,
    1 + ((t.seq + line_no) % 2),
    pv.price,
    pv.price * (1 + ((t.seq + line_no) % 2))
  FROM tmp_seed_orders t
  CROSS JOIN generate_series(0, 1) AS line_no
  JOIN pizza_variants pv ON pv.id = v_variant_ids[1 + ((t.seq + line_no - 1) % array_length(v_variant_ids, 1))];

  RAISE NOTICE 'Seeding payments...';

  INSERT INTO payments (order_id, payment_method, amount, status, paid_at, created_at, updated_at)
  SELECT
    t.id,
    t.payment_method,
    t.final_amount,
    CASE WHEN t.status = 8 THEN 3 ELSE 2 END, -- FAILED if cancelled-ish else PAID
    CASE WHEN t.status = 8 THEN NULL ELSE NOW() - ((t.seq % 90) || ' days')::interval END,
    NOW() - ((t.seq % 90) || ' days')::interval,
    NOW() - ((t.seq % 90) || ' days')::interval
  FROM tmp_seed_orders t;

  RAISE NOTICE 'Done. users=% orders=% variants=%', v_users, v_orders, array_length(v_variant_ids, 1);
END $$;

COMMIT;

-- Summary
SELECT 'users_load' AS metric, count(*)::bigint AS n FROM users WHERE username LIKE 'loaduser_%'
UNION ALL
SELECT 'addresses', count(*) FROM addresses a JOIN users u ON u.id = a.user_id WHERE u.username LIKE 'loaduser_%'
UNION ALL
SELECT 'pizzas_load', count(*) FROM pizzas WHERE slug LIKE 'load-pizza-%'
UNION ALL
SELECT 'orders_LT', count(*) FROM orders WHERE order_code LIKE 'LT%'
UNION ALL
SELECT 'order_items', count(*) FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.order_code LIKE 'LT%'
UNION ALL
SELECT 'payments', count(*) FROM payments p JOIN orders o ON o.id = p.order_id WHERE o.order_code LIKE 'LT%';
