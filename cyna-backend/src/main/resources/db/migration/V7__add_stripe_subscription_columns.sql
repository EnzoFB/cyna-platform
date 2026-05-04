-- ============================================================
-- V4 : Stripe subscription tracking columns
-- ============================================================

-- payments: link to the Stripe Subscription and SubscriptionSchedule
ALTER TABLE payment_schema.payments
    ADD COLUMN stripe_subscription_id VARCHAR(255),
    ADD COLUMN stripe_schedule_id     VARCHAR(255);

CREATE INDEX idx_payments_stripe_subscription
    ON payment_schema.payments (stripe_subscription_id);

-- subscriptions: link to the Stripe Subscription and SubscriptionSchedule
ALTER TABLE subscription_schema.subscriptions
    ADD COLUMN stripe_subscription_id VARCHAR(255),
    ADD COLUMN stripe_schedule_id     VARCHAR(255);

CREATE INDEX idx_subscriptions_stripe_subscription
    ON subscription_schema.subscriptions (stripe_subscription_id);
