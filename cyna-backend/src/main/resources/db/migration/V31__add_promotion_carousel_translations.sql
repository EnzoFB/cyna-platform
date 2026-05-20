-- ── promotion_translations ────────────────────────────────────────────────────
CREATE TABLE product_schema.promotion_translations (
    promotion_id   UUID        NOT NULL
        REFERENCES product_schema.promotions(id) ON DELETE CASCADE,
    locale         VARCHAR(10) NOT NULL,
    marketing_text TEXT        NOT NULL DEFAULT '',
    CONSTRAINT pk_promotion_translations PRIMARY KEY (promotion_id, locale)
);

-- Migrate existing FR rows
INSERT INTO product_schema.promotion_translations (promotion_id, locale, marketing_text)
SELECT id, 'fr', marketing_text_fr
FROM product_schema.promotions;

-- Migrate existing EN rows (fall back to FR text if EN was empty)
INSERT INTO product_schema.promotion_translations (promotion_id, locale, marketing_text)
SELECT id, 'en',
    CASE WHEN marketing_text_en <> '' THEN marketing_text_en ELSE marketing_text_fr END
FROM product_schema.promotions;

ALTER TABLE product_schema.promotions
    DROP COLUMN marketing_text_fr,
    DROP COLUMN marketing_text_en;

CREATE INDEX idx_promotion_translations_promotion_locale
    ON product_schema.promotion_translations (promotion_id, locale);

-- ── offer_carousel_settings_translations ─────────────────────────────────────
CREATE TABLE product_schema.offer_carousel_settings_translations (
    settings_id  SMALLINT    NOT NULL
        REFERENCES product_schema.offer_carousel_settings(id) ON DELETE CASCADE,
    locale       VARCHAR(10) NOT NULL,
    fixed_text   TEXT        NOT NULL DEFAULT '',
    CONSTRAINT pk_offer_carousel_settings_translations PRIMARY KEY (settings_id, locale)
);

-- Migrate existing FR rows
INSERT INTO product_schema.offer_carousel_settings_translations (settings_id, locale, fixed_text)
SELECT id, 'fr', fixed_text_fr
FROM product_schema.offer_carousel_settings;

-- Migrate existing EN rows (fall back to FR text if EN was empty)
INSERT INTO product_schema.offer_carousel_settings_translations (settings_id, locale, fixed_text)
SELECT id, 'en',
    CASE WHEN fixed_text_en <> '' THEN fixed_text_en ELSE fixed_text_fr END
FROM product_schema.offer_carousel_settings;

ALTER TABLE product_schema.offer_carousel_settings
    DROP COLUMN fixed_text_fr,
    DROP COLUMN fixed_text_en;

CREATE INDEX idx_offer_carousel_settings_translations_settings_locale
    ON product_schema.offer_carousel_settings_translations (settings_id, locale);
