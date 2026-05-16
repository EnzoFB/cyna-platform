-- =====================================================
-- Webhook idempotency — Stripe at-least-once delivery
-- =====================================================
-- Stripe guarantees *at-least-once* webhook delivery: the same event
-- (evt_...) can be delivered multiple times (network retry, timeout, manual
-- resend). This table is the dedup ledger: the handler claims an event id
-- atomically before processing; a duplicate delivery finds the row already
-- present and is skipped. Combined with the already-idempotent domain
-- operations, this makes the payment webhook flow safe under redelivery.
--
-- Kept in payment_schema (the module that owns Stripe integration). Rows are
-- cheap (one short id + timestamp) and serve as an audit trail of every
-- webhook actually processed.

CREATE TABLE payment_schema.processed_stripe_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    event_type   VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_stripe_events_processed_at
    ON payment_schema.processed_stripe_events (processed_at DESC);
