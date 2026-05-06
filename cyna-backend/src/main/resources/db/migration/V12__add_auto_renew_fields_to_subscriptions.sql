ALTER TABLE subscription_schema.subscriptions
    ADD COLUMN IF NOT EXISTS auto_renew BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE subscription_schema.subscriptions
    ADD COLUMN IF NOT EXISTS auto_renew_notice_sent_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_end_at
    ON subscription_schema.subscriptions (end_at);

CREATE INDEX IF NOT EXISTS idx_subscriptions_auto_renew_notice
    ON subscription_schema.subscriptions (auto_renew, auto_renew_notice_sent_at);
