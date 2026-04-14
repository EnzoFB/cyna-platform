-- =========================
-- CATEGORY
-- =========================
ALTER TABLE product_schema.categories
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

-- =========================
-- PRODUCTS
-- =========================

-- 1. Supprimer contraintes
ALTER TABLE product_schema.products
DROP CONSTRAINT IF EXISTS ck_products_category;

ALTER TABLE product_schema.products
DROP CONSTRAINT IF EXISTS ck_products_status;

-- =========================
-- 2. CATEGORY_ID (migration propre)
-- =========================

-- ajouter colonne nullable
ALTER TABLE product_schema.products
    ADD COLUMN IF NOT EXISTS category_id UUID;

-- migration des données
UPDATE product_schema.products p
SET category_id = c.id
    FROM product_schema.categories c
WHERE p.category = c.name;

-- maintenant seulement → NOT NULL
ALTER TABLE product_schema.products
    ALTER COLUMN category_id SET NOT NULL;

-- FK
ALTER TABLE product_schema.products
    ADD CONSTRAINT fk_products_category
        FOREIGN KEY (category_id)
            REFERENCES product_schema.categories(id);

-- supprimer ancienne colonne
ALTER TABLE product_schema.products
DROP COLUMN category;

-- =========================
-- 3. STATUS → is_published
-- =========================

ALTER TABLE product_schema.products
    ADD COLUMN IF NOT EXISTS is_published BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE product_schema.products
SET is_published = (status = 'PUBLISHED');

ALTER TABLE product_schema.products
DROP COLUMN status;

-- =========================
-- 4. AVAILABILITY → BOOLEAN
-- =========================

ALTER TABLE product_schema.products
    ADD COLUMN IF NOT EXISTS is_available BOOLEAN NOT NULL DEFAULT FALSE;

-- =========================
-- 5. PRIORITY → INTEGER
-- =========================

ALTER TABLE product_schema.products
    ADD COLUMN IF NOT EXISTS priority_level INTEGER NOT NULL DEFAULT 0;

UPDATE product_schema.products
SET priority_level = CASE priority
                         WHEN 'NORMALE' THEN 1
                         WHEN 'MOYENNE' THEN 5
                         WHEN 'HAUTE' THEN 10
                         ELSE 0
    END;

ALTER TABLE product_schema.products
DROP CONSTRAINT IF EXISTS ck_products_priority;

ALTER TABLE product_schema.products
DROP COLUMN priority;

-- =========================
-- 6. INDEX
-- =========================

CREATE INDEX IF NOT EXISTS idx_products_category_id
    ON product_schema.products (category_id);

CREATE INDEX IF NOT EXISTS idx_products_is_published
    ON product_schema.products (is_published);

CREATE INDEX IF NOT EXISTS idx_products_priority_level
    ON product_schema.products (priority_level DESC);
