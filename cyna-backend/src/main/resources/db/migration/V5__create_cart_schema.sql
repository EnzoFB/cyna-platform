CREATE SCHEMA IF NOT EXISTS cart_schema;

CREATE TABLE IF NOT EXISTS cart_schema.carts (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NULL,
    guest_token     VARCHAR(255)    NULL,
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT ck_carts_status CHECK (status IN ('ACTIVE', 'CHECKED_OUT')),
    CONSTRAINT ck_carts_owner_xor CHECK (
        (user_id IS NOT NULL AND guest_token IS NULL)
        OR
        (user_id IS NULL AND guest_token IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS cart_schema.cart_lines (
    id                  UUID            DEFAULT gen_random_uuid(),
    cart_id             UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    product_name        VARCHAR(200)    NOT NULL,
    product_category    VARCHAR(50)     NOT NULL,
    billing_cycle       VARCHAR(50)     NOT NULL,
    quantity            INTEGER         NOT NULL,

    CONSTRAINT pk_cart_lines PRIMARY KEY (id),
    CONSTRAINT fk_cart_lines_carts FOREIGN KEY (cart_id)
        REFERENCES cart_schema.carts (id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_lines_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_cart_lines_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT uq_cart_lines_product_cycle UNIQUE (cart_id, product_id, billing_cycle)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_carts_active_user
    ON cart_schema.carts (user_id)
    WHERE status = 'ACTIVE' AND user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_carts_active_guest
    ON cart_schema.carts (guest_token)
    WHERE status = 'ACTIVE' AND guest_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_carts_status ON cart_schema.carts (status);
CREATE INDEX IF NOT EXISTS idx_carts_updated_at ON cart_schema.carts (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_cart_lines_product_id ON cart_schema.cart_lines (product_id);
CREATE INDEX IF NOT EXISTS idx_cart_lines_cart_id ON cart_schema.cart_lines (cart_id);

CREATE OR REPLACE FUNCTION cart_schema.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON cart_schema.carts
    FOR EACH ROW
    EXECUTE FUNCTION cart_schema.update_updated_at();
