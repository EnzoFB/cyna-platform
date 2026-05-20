-- =====================================================
-- V30: Introduce proper translation tables for products and categories.
-- All locale-specific display fields move out of the main tables into
-- dedicated child tables keyed by (entity_id, locale).
-- Existing data is migrated as 'fr' rows; the old columns are then dropped.
-- =====================================================

-- ── 1. product_schema.product_translations ────────────────────────────────────

CREATE TABLE product_schema.product_translations (
    product_id            UUID         NOT NULL
        REFERENCES product_schema.products(id) ON DELETE CASCADE,
    locale                VARCHAR(10)  NOT NULL,
    name                  VARCHAR(200) NOT NULL,
    service_description   TEXT         NOT NULL DEFAULT '',
    technical_description TEXT         NOT NULL DEFAULT '',
    highlight_points      JSONB        NOT NULL DEFAULT '[]',

    CONSTRAINT pk_product_translations PRIMARY KEY (product_id, locale)
);

-- Migrate existing FR data from main table
INSERT INTO product_schema.product_translations
    (product_id, locale, name, service_description, technical_description, highlight_points)
SELECT
    id,
    'fr',
    name,
    COALESCE(service_description, ''),
    COALESCE(technical_description, ''),
    COALESCE(highlight_points, '[]'::jsonb)
FROM product_schema.products;

-- Drop the now-redundant translatable columns from the main table
ALTER TABLE product_schema.products
    DROP COLUMN IF EXISTS name,
    DROP COLUMN IF EXISTS service_description,
    DROP COLUMN IF EXISTS technical_description,
    DROP COLUMN IF EXISTS highlight_points;

-- ── 2. product_schema.category_translations ───────────────────────────────────

CREATE TABLE product_schema.category_translations (
    category_id  UUID         NOT NULL
        REFERENCES product_schema.categories(id) ON DELETE CASCADE,
    locale       VARCHAR(10)  NOT NULL,
    full_name    VARCHAR(255) NOT NULL DEFAULT '',
    description  TEXT         NOT NULL DEFAULT '',

    CONSTRAINT pk_category_translations PRIMARY KEY (category_id, locale)
);

-- Migrate existing FR data from main table
INSERT INTO product_schema.category_translations
    (category_id, locale, full_name, description)
SELECT
    id,
    'fr',
    COALESCE(full_name, ''),
    COALESCE(description, '')
FROM product_schema.categories;

-- Drop the now-redundant translatable columns from the main table
ALTER TABLE product_schema.categories
    DROP COLUMN IF EXISTS full_name,
    DROP COLUMN IF EXISTS description;

-- ── 3. Performance indexes ────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_product_translations_locale
    ON product_schema.product_translations (product_id, locale);

CREATE INDEX IF NOT EXISTS idx_category_translations_locale
    ON product_schema.category_translations (category_id, locale);
