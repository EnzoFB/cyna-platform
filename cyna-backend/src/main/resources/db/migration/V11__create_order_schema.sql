CREATE SCHEMA IF NOT EXISTS order_schema;

CREATE TABLE IF NOT EXISTS order_schema.orders (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    subtotal_amount NUMERIC(19,4)   NOT NULL,
    vat_amount      NUMERIC(19,4)   NOT NULL,
    total_amount    NUMERIC(19,4)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'FULFILLED', 'CANCELLED')),
    CONSTRAINT ck_orders_subtotal_amount CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_orders_vat_amount CHECK (vat_amount >= 0),
    CONSTRAINT ck_orders_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON order_schema.orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON order_schema.orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc ON order_schema.orders (created_at DESC);

CREATE TABLE IF NOT EXISTS order_schema.order_lines (
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID            NOT NULL,
    product_id       UUID            NOT NULL,
    product_name     VARCHAR(200)    NOT NULL,
    product_category VARCHAR(200)    NOT NULL,
    billing_cycle    VARCHAR(20)     NOT NULL,
    quantity         INTEGER         NOT NULL,
    unit_price       NUMERIC(19,4)   NOT NULL,
    currency         VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id)
        REFERENCES order_schema.orders(id) ON DELETE CASCADE,
    CONSTRAINT ck_order_lines_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_order_lines_quantity CHECK (quantity >= 1 AND quantity <= 99),
    CONSTRAINT ck_order_lines_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_lines_order_id ON order_schema.order_lines (order_id);
