BEGIN;

CREATE TEMP TABLE tmp_seed_users_ids ON COMMIT DROP AS
SELECT id
FROM user_schema.users
WHERE email LIKE 'seed-dash-2026-user-%@cyna.test';

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
    JOIN tmp_seed_users_ids su ON su.id = u.id
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
    JOIN tmp_seed_users_ids su ON su.id = o.user_id
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
  AND o.user_id IN (SELECT id FROM tmp_seed_users_ids);

UPDATE subscription_schema.subscriptions s
SET
    created_at = o.created_at + INTERVAL '1 day',
    updated_at = o.created_at + INTERVAL '1 day',
    start_at = o.created_at + INTERVAL '1 day',
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
  AND o.user_id IN (SELECT id FROM tmp_seed_users_ids);

COMMIT;
