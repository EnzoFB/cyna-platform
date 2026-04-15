-- Backfill full_name for categories seeded before V8 added the column.
UPDATE product_schema.categories
SET full_name = name
WHERE full_name IS NULL;

-- Now enforce NOT NULL so future inserts always provide full_name.
ALTER TABLE product_schema.categories
    ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE product_schema.categories
    ALTER COLUMN full_name SET DEFAULT '';
