-- ============================================================
-- V5 : Add PAST_DUE status for subscriptions
-- Used when a renewal payment fails (Stripe dunning in progress).
-- ============================================================

ALTER TABLE subscription_schema.subscriptions
    DROP CONSTRAINT ck_subscriptions_status;

ALTER TABLE subscription_schema.subscriptions
    ADD CONSTRAINT ck_subscriptions_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'PAST_DUE', 'PAUSED', 'CANCELLED', 'EXPIRED'));
