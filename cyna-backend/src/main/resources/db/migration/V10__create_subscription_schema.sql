CREATE SCHEMA IF NOT EXISTS subscription_schema;

CREATE TABLE IF NOT EXISTS subscription_schema.subscriptions (
    id                UUID            DEFAULT gen_random_uuid(),
    user_id           UUID            NOT NULL,
    order_id          UUID            NOT NULL,
    product_id        UUID            NOT NULL,
    product_name      VARCHAR(200)    NOT NULL,
    product_category  VARCHAR(200)    NOT NULL,
    billing_cycle     VARCHAR(20)     NOT NULL,
    status            VARCHAR(20)     NOT NULL,
    quantity          INTEGER         NOT NULL,
    unit_price        NUMERIC(19,4)   NOT NULL,
    currency          VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    start_at          TIMESTAMPTZ     NOT NULL,
    end_at            TIMESTAMPTZ     NOT NULL,
    next_billing_at   TIMESTAMPTZ     NOT NULL,
    cancelled_at      TIMESTAMPTZ     NULL,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT ck_subscriptions_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('PENDING', 'ACTIVE', 'PAUSED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_subscriptions_quantity CHECK (quantity >= 1 AND quantity <= 99),
    CONSTRAINT ck_subscriptions_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscription_schema.subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_order_id ON subscription_schema.subscriptions (order_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscription_schema.subscriptions (status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_created_at_desc ON subscription_schema.subscriptions (created_at DESC);
