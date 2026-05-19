CREATE TABLE IF NOT EXISTS product_schema.promotions (
    id                UUID          NOT NULL DEFAULT gen_random_uuid(),
    product_id        UUID          NOT NULL,
    discount_percent  INTEGER       NOT NULL,
    marketing_text_fr TEXT          NOT NULL,
    marketing_text_en TEXT          NOT NULL,
    start_at          TIMESTAMPTZ   NOT NULL,
    end_at            TIMESTAMPTZ   NOT NULL,
    is_enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT fk_promotions_product
        FOREIGN KEY (product_id) REFERENCES product_schema.products (id) ON DELETE CASCADE,
    CONSTRAINT ck_promotions_discount_percent
        CHECK (discount_percent BETWEEN 1 AND 100),
    CONSTRAINT ck_promotions_window
        CHECK (start_at < end_at)
);

CREATE INDEX IF NOT EXISTS idx_promotions_product_id
    ON product_schema.promotions (product_id);

CREATE INDEX IF NOT EXISTS idx_promotions_enabled_window
    ON product_schema.promotions (is_enabled, start_at, end_at);

CREATE TRIGGER trg_promotions_updated_at
    BEFORE UPDATE ON product_schema.promotions
    FOR EACH ROW
    EXECUTE FUNCTION product_schema.update_updated_at();
