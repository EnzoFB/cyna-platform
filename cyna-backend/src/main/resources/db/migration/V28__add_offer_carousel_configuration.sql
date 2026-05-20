ALTER TABLE product_schema.promotions
    ADD COLUMN IF NOT EXISTS show_in_carousel BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS carousel_order INTEGER;

ALTER TABLE product_schema.promotions
    DROP CONSTRAINT IF EXISTS ck_promotions_carousel_order;

ALTER TABLE product_schema.promotions
    ADD CONSTRAINT ck_promotions_carousel_order
        CHECK (
            (show_in_carousel = FALSE AND carousel_order IS NULL)
            OR (show_in_carousel = TRUE AND carousel_order IS NOT NULL AND carousel_order >= 1)
        );

CREATE INDEX IF NOT EXISTS idx_promotions_carousel_order
    ON product_schema.promotions (show_in_carousel, carousel_order);

CREATE TABLE IF NOT EXISTS product_schema.offer_carousel_settings (
    id            SMALLINT    NOT NULL,
    fixed_text_fr TEXT        NOT NULL,
    fixed_text_en TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_offer_carousel_settings PRIMARY KEY (id),
    CONSTRAINT ck_offer_carousel_settings_singleton CHECK (id = 1)
);

INSERT INTO product_schema.offer_carousel_settings (id, fixed_text_fr, fixed_text_en)
VALUES (1, '', '')
ON CONFLICT (id) DO NOTHING;

DROP TRIGGER IF EXISTS trg_offer_carousel_settings_updated_at ON product_schema.offer_carousel_settings;

CREATE TRIGGER trg_offer_carousel_settings_updated_at
    BEFORE UPDATE ON product_schema.offer_carousel_settings
    FOR EACH ROW
    EXECUTE FUNCTION product_schema.update_updated_at();
