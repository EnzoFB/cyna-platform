ALTER TABLE IF EXISTS product_schema.products
    ADD COLUMN IF NOT EXISTS priority VARCHAR(50) NOT NULL DEFAULT 'NORMALE';

ALTER TABLE product_schema.products
    DROP CONSTRAINT IF EXISTS ck_products_priority;

ALTER TABLE product_schema.products
    ADD CONSTRAINT ck_products_priority
        CHECK (priority IN ('NORMALE', 'MOYENNE', 'HAUTE'));

CREATE INDEX IF NOT EXISTS idx_products_priority ON product_schema.products (priority);
