CREATE SCHEMA IF NOT EXISTS product_schema;

CREATE TABLE IF NOT EXISTS product_schema.products (
    id                      UUID            DEFAULT gen_random_uuid(),
    name                    VARCHAR(200)    NOT NULL,
    category                VARCHAR(50)     NOT NULL,
    service_description     TEXT            NOT NULL,
    technical_description   TEXT            NOT NULL,
    monthly_price           NUMERIC(19,4)   NOT NULL,
    annual_price            NUMERIC(19,4)   NOT NULL,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    status                  VARCHAR(50)     NOT NULL DEFAULT 'DRAFT',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT ck_products_category CHECK (category IN ('SOC', 'EDR', 'XDR')),
    CONSTRAINT ck_products_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED')),
    CONSTRAINT ck_products_currency_format CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX IF NOT EXISTS idx_products_status ON product_schema.products (status);
CREATE INDEX IF NOT EXISTS idx_products_category ON product_schema.products (category);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON product_schema.products (created_at DESC);

CREATE OR REPLACE FUNCTION product_schema.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON product_schema.products
    FOR EACH ROW
    EXECUTE FUNCTION product_schema.update_updated_at();
