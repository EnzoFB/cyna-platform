-- ============================================================
-- V14 : Multi-product mixed-cycle checkout
--
-- Move from "one Stripe Subscription per Order (single cycle)" to
-- "one Stripe Subscription per OrderLine (own cycle)". Checkout switches
-- from a default_incomplete + PaymentIntent flow to a SetupIntent +
-- server-side multi-sub creation flow.
-- ============================================================

-- ---- payments: SetupIntent columns for the new checkout flow ----
ALTER TABLE payment_schema.payments
    ADD COLUMN IF NOT EXISTS stripe_setup_intent_id            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_setup_intent_client_secret VARCHAR(500);

-- Legacy single-sub columns (stripe_payment_intent_id, stripe_subscription_id,
-- stripe_schedule_id, stripe_client_secret) stay in place — used to read
-- payments created before V14. New checkouts populate the SetupIntent columns
-- and leave the legacy ones NULL.

CREATE INDEX IF NOT EXISTS idx_payments_stripe_setup_intent
    ON payment_schema.payments (stripe_setup_intent_id);

-- ---- subscriptions: link back to the specific OrderLine ----
-- Required so each local subscription is provisionable independently of the
-- other lines in the same order (different cycle, different cancel state…).
ALTER TABLE subscription_schema.subscriptions
    ADD COLUMN IF NOT EXISTS order_line_id UUID;

-- Order-line uniqueness only enforced for rows seeded post-V14. Older rows are
-- left with NULL order_line_id; if the column is ever required NOT NULL, a
-- separate back-fill migration will be needed.
CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_order_line
    ON subscription_schema.subscriptions (order_line_id)
    WHERE order_line_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_order_line
    ON subscription_schema.subscriptions (order_line_id);
