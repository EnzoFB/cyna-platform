-- Remove seed data that stored SVG URL paths (incompatible with binary storage)
DELETE FROM product_schema.product_images;

-- Replace image_url TEXT with binary storage columns
ALTER TABLE product_schema.product_images
    DROP COLUMN image_url;

ALTER TABLE product_schema.product_images
    ADD COLUMN image_data BYTEA        NOT NULL,
    ADD COLUMN mime_type  VARCHAR(50)  NOT NULL DEFAULT 'image/jpeg';
