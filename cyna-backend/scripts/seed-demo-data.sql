\set ON_ERROR_STOP on

-- =====================================================
-- CYNA PLATFORM — Données de démo pour le dashboard (2026)
--
-- Génère un historique réaliste sur toute l'année 2026 :
--   · 1 000 clients
--   · 1 500 commandes (mix : 50% PAID · 25% FULFILLED · 15% CANCELLED · 5% PENDING · 5% CONFIRMED)
--   · ~1 125 abonnements (commandes PAID et FULFILLED uniquement)
--
-- Les dates sont redistribuées après insertion avec une courbe de
-- puissance pour un rendu plus naturel dans les graphiques.
--
-- =====================================================

BEGIN;

-- ── Fonction utilitaire : UUID déterministe à partir d'une clé ────────────────

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

-- ── Produits disponibles ──────────────────────────────────────────────────────

CREATE TEMP TABLE tmp_seed_products AS
SELECT
    p.id AS product_id,
    COALESCE(pt.name, c.name || ' ' || p.id::TEXT) AS product_name,
    c.name AS product_category,
    p.monthly_price,
    p.annual_price,
    p.currency,
    ROW_NUMBER() OVER (ORDER BY c.name, p.priority_level, p.id) AS product_rank
FROM product_schema.products p
JOIN product_schema.categories c ON c.id = p.category_id
LEFT JOIN product_schema.product_translations pt
    ON pt.product_id = p.id AND pt.locale = 'fr'
WHERE p.is_available = TRUE OR p.is_published = TRUE
ORDER BY c.name, p.priority_level, p.id
LIMIT 9;

DO $$
DECLARE
    available_product_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO available_product_count FROM tmp_seed_products;
    IF available_product_count < 1 THEN
        RAISE EXCEPTION 'Seed aborted: no products found in the database';
    END IF;
    RAISE NOTICE 'Seeding with % product(s)', available_product_count;
END $$;

-- ── Clients ───────────────────────────────────────────────────────────────────

CREATE TEMP TABLE tmp_seed_users AS
SELECT
    gs AS user_index,
    pg_temp.seed_uuid('seed-dash-2026-user', gs) AS user_id,
    format('seed-dash-2026-user-%s@cyna.test', lpad(gs::TEXT, 4, '0')) AS email,
    format('Client%s', lpad(gs::TEXT, 4, '0')) AS first_name,
    'Dashboard' AS last_name,
    'Seed Company' AS company,
    (TIMESTAMPTZ '2026-01-01 08:00:00+00'
        + (((gs - 1)::NUMERIC / 1000) * 360)::INT * INTERVAL '1 day'
        + ((gs % 6) * INTERVAL '4 hours')
    ) AS created_at
FROM generate_series(1, 1000) gs;

INSERT INTO user_schema.users (
    id, email, password_hash, first_name, last_name, company, role, status, created_at, updated_at
)
SELECT
    su.user_id, su.email, 'seed_dashboard_2026_password_hash',
    su.first_name, su.last_name, su.company,
    'CUSTOMER', 'ACTIVE', su.created_at, su.created_at
FROM tmp_seed_users su
ON CONFLICT (id) DO NOTHING;

-- ── Commandes ─────────────────────────────────────────────────────────────────

CREATE TEMP TABLE tmp_seed_orders AS
SELECT
    gs AS order_index,
    pg_temp.seed_uuid('seed-dash-2026-order', gs) AS order_id,
    su.user_id,
    CASE
        WHEN gs % 20 IN (0)                          THEN 'PENDING'
        WHEN gs % 20 IN (1)                          THEN 'CONFIRMED'
        WHEN gs % 20 IN (2,3,4,5,6,7,8,9,10,11)     THEN 'PAID'
        WHEN gs % 20 IN (12,13,14,15,16)             THEN 'FULFILLED'
        ELSE                                              'CANCELLED'
    END AS status,
    (TIMESTAMPTZ '2026-01-01 09:00:00+00'
        + (((gs - 1)::NUMERIC / 1500) * 360)::INT * INTERVAL '1 day'
        + ((gs % 8) * INTERVAL '3 hours')
    ) AS created_at
FROM generate_series(1, 1500) gs
JOIN tmp_seed_users su ON su.user_index = ((gs - 1) % 1000) + 1;

-- ── Lignes de commande ────────────────────────────────────────────────────────

CREATE TEMP TABLE tmp_seed_line_plan AS
SELECT
    gs AS line_index,
    CASE WHEN gs <= 1500 THEN gs ELSE gs - 1500 END AS order_index,
    pg_temp.seed_uuid('seed-dash-2026-order-line', gs) AS order_line_id,
    ((gs - 1) % (SELECT COUNT(*) FROM tmp_seed_products)) + 1 AS product_rank,
    1 + ((gs - 1) % 3) AS quantity,
    CASE WHEN gs % 2 = 0 THEN 'ANNUAL' ELSE 'MONTHLY' END AS billing_cycle
FROM generate_series(1, 2000) gs;

CREATE TEMP TABLE tmp_seed_order_lines AS
SELECT
    lp.line_index, lp.order_line_id,
    o.order_id, o.user_id, o.created_at AS order_created_at,
    p.product_id, p.product_name, p.product_category,
    lp.billing_cycle, lp.quantity,
    CASE WHEN lp.billing_cycle = 'ANNUAL' THEN p.annual_price ELSE p.monthly_price END AS unit_price,
    p.currency
FROM tmp_seed_line_plan lp
JOIN tmp_seed_orders o ON o.order_index = lp.order_index
JOIN tmp_seed_products p ON p.product_rank = lp.product_rank;

CREATE TEMP TABLE tmp_seed_order_totals AS
SELECT
    sol.order_id,
    round(SUM(sol.quantity * sol.unit_price)::NUMERIC, 4) AS subtotal_amount,
    MIN(sol.currency) AS currency
FROM tmp_seed_order_lines sol
GROUP BY sol.order_id;

INSERT INTO order_schema.orders (
    id, user_id, status, subtotal_amount, currency, created_at, updated_at
)
SELECT
    so.order_id, so.user_id, so.status,
    sot.subtotal_amount, sot.currency,
    so.created_at, so.created_at
FROM tmp_seed_orders so
JOIN tmp_seed_order_totals sot ON sot.order_id = so.order_id
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_schema.order_lines (
    id, order_id, product_id, product_name, product_category,
    billing_cycle, quantity, unit_price, currency, created_at, updated_at
)
SELECT
    sol.order_line_id, sol.order_id, sol.product_id, sol.product_name, sol.product_category,
    sol.billing_cycle, sol.quantity, sol.unit_price, sol.currency,
    sol.order_created_at, sol.order_created_at
FROM tmp_seed_order_lines sol
ON CONFLICT (id) DO NOTHING;

-- ── Abonnements ───────────────────────────────────────────────────────────────

CREATE TEMP TABLE tmp_seed_subscriptions AS
SELECT
    pg_temp.seed_uuid('seed-dash-2026-subscription', sol.line_index) AS subscription_id,
    sol.user_id, sol.order_id, sol.order_line_id,
    sol.product_id, sol.product_name, sol.product_category,
    sol.billing_cycle, sol.quantity, sol.unit_price, sol.currency,
    'ACTIVE' AS status,
    (sol.order_created_at + INTERVAL '1 day') AS start_at,
    CASE WHEN sol.billing_cycle = 'ANNUAL'
        THEN sol.order_created_at + INTERVAL '730 days'
        ELSE sol.order_created_at + INTERVAL '400 days'
    END AS end_at,
    CASE WHEN sol.billing_cycle = 'ANNUAL'
        THEN sol.order_created_at + INTERVAL '365 days'
        ELSE sol.order_created_at + INTERVAL '30 days'
    END AS next_billing_at,
    (sol.order_created_at + INTERVAL '1 day') AS created_at
FROM tmp_seed_order_lines sol
JOIN tmp_seed_orders so ON so.order_id = sol.order_id
WHERE so.status IN ('PAID', 'FULFILLED');

INSERT INTO subscription_schema.subscriptions (
    id, user_id, order_id, order_line_id,
    product_id, product_name, product_category,
    billing_cycle, status, quantity, unit_price, currency,
    start_at, end_at, next_billing_at,
    cancelled_at, auto_renew, auto_renew_notice_sent_at,
    stripe_subscription_id, stripe_schedule_id,
    created_at, updated_at
)
SELECT
    ss.subscription_id, ss.user_id, ss.order_id, ss.order_line_id,
    ss.product_id, ss.product_name, ss.product_category,
    ss.billing_cycle, ss.status, ss.quantity, ss.unit_price, ss.currency,
    ss.start_at, ss.end_at, ss.next_billing_at,
    NULL, TRUE, NULL, NULL, NULL,
    ss.created_at, ss.created_at
FROM tmp_seed_subscriptions ss
ON CONFLICT (id) DO NOTHING;

-- ── Rééquilibrage des dates (courbe de puissance) ─────────────────────────────
--
-- Les dates générées ci-dessus sont linéaires.
-- Ces UPDATE les redistribuent avec une courbe de puissance pour
-- un rendu plus naturel dans les graphiques du dashboard.

WITH bounds AS (
    SELECT
        TIMESTAMPTZ '2026-01-01 08:00:00+00' AS start_at,
        LEAST(NOW(), TIMESTAMPTZ '2026-12-31 23:59:00+00') AS end_at
),
ranked_users AS (
    SELECT
        u.id,
        ROW_NUMBER() OVER (ORDER BY u.id) - 1 AS rn,
        GREATEST(COUNT(*) OVER () - 1, 1) AS max_rn
    FROM user_schema.users u
    JOIN tmp_seed_users su ON su.user_id = u.id
)
UPDATE user_schema.users u
SET
    created_at = b.start_at + ((b.end_at - b.start_at) * POWER((ru.rn::double precision / ru.max_rn::double precision), 1.35)),
    updated_at = b.start_at + ((b.end_at - b.start_at) * POWER((ru.rn::double precision / ru.max_rn::double precision), 1.35))
FROM ranked_users ru, bounds b
WHERE u.id = ru.id;

WITH bounds AS (
    SELECT
        TIMESTAMPTZ '2026-01-01 09:00:00+00' AS start_at,
        LEAST(NOW(), TIMESTAMPTZ '2026-12-31 23:59:00+00') AS end_at
),
ranked_orders AS (
    SELECT
        o.id,
        ROW_NUMBER() OVER (ORDER BY o.id) - 1 AS rn,
        GREATEST(COUNT(*) OVER () - 1, 1) AS max_rn
    FROM order_schema.orders o
    JOIN tmp_seed_users su ON su.user_id = o.user_id
)
UPDATE order_schema.orders o
SET
    created_at = b.start_at + ((b.end_at - b.start_at) * POWER((ro.rn::double precision / ro.max_rn::double precision), 1.65)),
    updated_at = b.start_at + ((b.end_at - b.start_at) * POWER((ro.rn::double precision / ro.max_rn::double precision), 1.65))
FROM ranked_orders ro, bounds b
WHERE o.id = ro.id;

UPDATE order_schema.order_lines ol
SET
    created_at = o.created_at,
    updated_at = o.created_at
FROM order_schema.orders o
WHERE ol.order_id = o.id
  AND o.user_id IN (SELECT user_id FROM tmp_seed_users);

UPDATE subscription_schema.subscriptions s
SET
    created_at      = o.created_at + INTERVAL '1 day',
    updated_at      = o.created_at + INTERVAL '1 day',
    start_at        = o.created_at + INTERVAL '1 day',
    next_billing_at = CASE
        WHEN s.billing_cycle = 'ANNUAL' THEN o.created_at + INTERVAL '365 days'
        ELSE o.created_at + INTERVAL '30 days'
    END,
    end_at = CASE
        WHEN s.billing_cycle = 'ANNUAL' THEN o.created_at + INTERVAL '730 days'
        ELSE o.created_at + INTERVAL '400 days'
    END
FROM order_schema.orders o
WHERE s.order_id = o.id
  AND o.user_id IN (SELECT user_id FROM tmp_seed_users);

-- ── Vérification ──────────────────────────────────────────────────────────────

SELECT
    (SELECT COUNT(*) FROM user_schema.users u JOIN tmp_seed_users su ON su.user_id = u.id)             AS seeded_customers,
    (SELECT COUNT(*) FROM order_schema.orders o JOIN tmp_seed_orders so ON so.order_id = o.id)          AS seeded_orders,
    (SELECT COUNT(*) FROM order_schema.orders o JOIN tmp_seed_orders so ON so.order_id = o.id WHERE o.status = 'PAID')       AS paid,
    (SELECT COUNT(*) FROM order_schema.orders o JOIN tmp_seed_orders so ON so.order_id = o.id WHERE o.status = 'FULFILLED')  AS fulfilled,
    (SELECT COUNT(*) FROM order_schema.orders o JOIN tmp_seed_orders so ON so.order_id = o.id WHERE o.status = 'CANCELLED')  AS cancelled,
    (SELECT COUNT(*) FROM order_schema.orders o JOIN tmp_seed_orders so ON so.order_id = o.id WHERE o.status IN ('PENDING', 'CONFIRMED')) AS pending_confirmed,
    (SELECT COUNT(*) FROM subscription_schema.subscriptions s JOIN tmp_seed_subscriptions ss ON ss.subscription_id = s.id)  AS seeded_subscriptions;

COMMIT;
