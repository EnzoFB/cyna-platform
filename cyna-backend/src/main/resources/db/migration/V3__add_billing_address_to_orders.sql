ALTER TABLE order_schema.orders
    ADD COLUMN IF NOT EXISTS billing_address_line1 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS billing_city          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS billing_zip_code      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS billing_country_code  VARCHAR(3);
