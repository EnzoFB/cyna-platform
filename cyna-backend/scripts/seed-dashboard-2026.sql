-- Seed data for Admin Dashboard tests (2026 only)
-- - 1000 customers
-- - 1500 orders
-- - 2000 ACTIVE subscriptions
-- - Uses the first 9 available products (3 per category expected)
-- - Does NOT insert dashboard goals
--
-- Idempotent behavior:
-- deterministic UUIDs + ON CONFLICT DO NOTHING.

BEGIN;

CREATE OR REPLACE FUNCTION pg_temp.seed_uuid(seed_key TEXT, seed_index INTEGER)
RETURNS UUID
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT (
        SUBSTRING(md5(seed_key || ':' || seed_index::TEXT), 1, 8) || '-' ||
        SUBSTRING(md5(seed_key || ':' || seed_index::TEXT), 9, 4) || '-' ||
        SUBSTRING(md5(seed_key || ':' || seed_index::TEXT), 13, 4) || '-' ||
        SUBSTRING(md5(seed_key || ':' || seed_index::TEXT), 17, 4) || '-' ||
        SUBSTRING(md5(seed_key || ':' || seed_index::TEXT), 21, 12)
    )::UUID
$$;

CREATE TEMP TABLE tmp_seed_products ON COMMIT DROP AS
SELECT
    p.id AS product_id,
    COALESCE(pt.name, c.name || ' ' || p.id::TEXT) AS product_name,
    c.name AS product_category,
    p.monthly_price,
    p.annual_price,
    p.currency,
    ROW_NUMBER() OVER (ORDER BY c.name, p.priority_level, p.id) AS product_rank
FROM product_schema.products p
JOIN product_schema.categories c
    ON c.id = p.category_id
LEFT JOIN product_schema.product_translations pt
    ON pt.product_id = p.id
   AND pt.locale = 'fr'
WHERE p.is_available = TRUE
ORDER BY c.name, p.priority_level, p.id
LIMIT 9;

DO $$
DECLARE
    available_product_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO available_product_count FROM tmp_seed_products;
    IF available_product_count < 9 THEN
        RAISE EXCEPTION 'Seed aborted: expected at least 9 available products, found %', available_product_count;
    END IF;
END $$;

CREATE TEMP TABLE tmp_seed_users ON COMMIT DROP AS
SELECT
    gs AS user_index,
    pg_temp.seed_uuid('seed-dash-2026-user', gs) AS user_id,
    format('seed-dash-2026-user-%s@cyna.test', lpad(gs::TEXT, 4, '0')) AS email,
    format('Client%s', lpad(gs::TEXT, 4, '0')) AS first_name,
    'Dashboard' AS last_name,
    'Seed Company' AS company,
    (TIMESTAMPTZ '2026-01-01 08:00:00+00' + (gs - 1) * INTERVAL '3 hours') AS created_at
FROM generate_series(1, 1000) gs;

INSERT INTO user_schema.users (
    id, email, password_hash, first_name, last_name, company, role, status, created_at, updated_at
)
SELECT
    su.user_id,
    su.email,
    'seed_dashboard_2026_password_hash',
    su.first_name,
    su.last_name,
    su.company,
    'CUSTOMER',
    'ACTIVE',
    su.created_at,
    su.created_at
FROM tmp_seed_users su
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE tmp_seed_orders ON COMMIT DROP AS
SELECT
    gs AS order_index,
    pg_temp.seed_uuid('seed-dash-2026-order', gs) AS order_id,
    su.user_id,
    CASE WHEN gs % 4 = 0 THEN 'FULFILLED' ELSE 'PAID' END AS status,
    (TIMESTAMPTZ '2026-01-01 09:00:00+00' + (gs - 1) * INTERVAL '2 hours') AS created_at
FROM generate_series(1, 1500) gs
JOIN tmp_seed_users su
    ON su.user_index = ((gs - 1) % 1000) + 1;

CREATE TEMP TABLE tmp_seed_line_plan ON COMMIT DROP AS
SELECT
    gs AS line_index,
    CASE WHEN gs <= 1500 THEN gs ELSE gs - 1500 END AS order_index,
    pg_temp.seed_uuid('seed-dash-2026-order-line', gs) AS order_line_id,
    ((gs - 1) % 9) + 1 AS product_rank,
    1 + ((gs - 1) % 3) AS quantity,
    CASE WHEN gs % 2 = 0 THEN 'ANNUAL' ELSE 'MONTHLY' END AS billing_cycle
FROM generate_series(1, 2000) gs;

CREATE TEMP TABLE tmp_seed_order_lines ON COMMIT DROP AS
SELECT
    lp.line_index,
    lp.order_line_id,
    o.order_id,
    o.user_id,
    o.created_at AS order_created_at,
    p.product_id,
    p.product_name,
    p.product_category,
    lp.billing_cycle,
    lp.quantity,
    CASE
        WHEN lp.billing_cycle = 'ANNUAL' THEN p.annual_price
        ELSE p.monthly_price
    END AS unit_price,
    p.currency
FROM tmp_seed_line_plan lp
JOIN tmp_seed_orders o
    ON o.order_index = lp.order_index
JOIN tmp_seed_products p
    ON p.product_rank = lp.product_rank;

CREATE TEMP TABLE tmp_seed_order_totals ON COMMIT DROP AS
SELECT
    sol.order_id,
    round(SUM(sol.quantity * sol.unit_price)::NUMERIC, 4) AS subtotal_amount,
    round((SUM(sol.quantity * sol.unit_price) * 0.20)::NUMERIC, 4) AS vat_amount,
    round((SUM(sol.quantity * sol.unit_price) * 1.20)::NUMERIC, 4) AS total_amount,
    MIN(sol.currency) AS currency
FROM tmp_seed_order_lines sol
GROUP BY sol.order_id;

INSERT INTO order_schema.orders (
    id, user_id, status, subtotal_amount, vat_amount, total_amount, currency, created_at, updated_at
)
SELECT
    so.order_id,
    so.user_id,
    so.status,
    sot.subtotal_amount,
    sot.vat_amount,
    sot.total_amount,
    sot.currency,
    so.created_at,
    so.created_at
FROM tmp_seed_orders so
JOIN tmp_seed_order_totals sot
    ON sot.order_id = so.order_id
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_schema.order_lines (
    id, order_id, product_id, product_name, product_category, billing_cycle, quantity, unit_price, currency, created_at, updated_at
)
SELECT
    sol.order_line_id,
    sol.order_id,
    sol.product_id,
    sol.product_name,
    sol.product_category,
    sol.billing_cycle,
    sol.quantity,
    sol.unit_price,
    sol.currency,
    sol.order_created_at,
    sol.order_created_at
FROM tmp_seed_order_lines sol
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE tmp_seed_subscriptions ON COMMIT DROP AS
SELECT
    pg_temp.seed_uuid('seed-dash-2026-subscription', sol.line_index) AS subscription_id,
    sol.user_id,
    sol.order_id,
    sol.order_line_id,
    sol.product_id,
    sol.product_name,
    sol.product_category,
    sol.billing_cycle,
    'ACTIVE' AS status,
    sol.quantity,
    sol.unit_price,
    sol.currency,
    (sol.order_created_at + INTERVAL '1 day') AS start_at,
    CASE
        WHEN sol.billing_cycle = 'ANNUAL' THEN sol.order_created_at + INTERVAL '730 days'
        ELSE sol.order_created_at + INTERVAL '400 days'
    END AS end_at,
    CASE
        WHEN sol.billing_cycle = 'ANNUAL' THEN sol.order_created_at + INTERVAL '365 days'
        ELSE sol.order_created_at + INTERVAL '30 days'
    END AS next_billing_at,
    (sol.order_created_at + INTERVAL '1 day') AS created_at
FROM tmp_seed_order_lines sol;

INSERT INTO subscription_schema.subscriptions (
    id,
    user_id,
    order_id,
    order_line_id,
    product_id,
    product_name,
    product_category,
    billing_cycle,
    status,
    quantity,
    unit_price,
    currency,
    start_at,
    end_at,
    next_billing_at,
    cancelled_at,
    auto_renew,
    auto_renew_notice_sent_at,
    stripe_subscription_id,
    stripe_schedule_id,
    created_at,
    updated_at
)
SELECT
    ss.subscription_id,
    ss.user_id,
    ss.order_id,
    ss.order_line_id,
    ss.product_id,
    ss.product_name,
    ss.product_category,
    ss.billing_cycle,
    ss.status,
    ss.quantity,
    ss.unit_price,
    ss.currency,
    ss.start_at,
    ss.end_at,
    ss.next_billing_at,
    NULL,
    TRUE,
    NULL,
    NULL,
    NULL,
    ss.created_at,
    ss.created_at
FROM tmp_seed_subscriptions ss
ON CONFLICT (id) DO NOTHING;

-- Quick verification
SELECT
    (SELECT COUNT(*) FROM user_schema.users u JOIN tmp_seed_users su ON su.user_id = u.id) AS seeded_customers_2026,
    (SELECT COUNT(*) FROM order_schema.orders o JOIN tmp_seed_orders so ON so.order_id = o.id) AS seeded_orders_2026,
    (SELECT COUNT(*) FROM subscription_schema.subscriptions s JOIN tmp_seed_subscriptions ss ON ss.subscription_id = s.id) AS seeded_active_subscriptions_2026;

COMMIT;
