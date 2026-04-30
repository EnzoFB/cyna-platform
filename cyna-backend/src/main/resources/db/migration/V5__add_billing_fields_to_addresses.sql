ALTER TABLE user_schema.addresses
    ADD COLUMN first_name  VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN last_name   VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN company     VARCHAR(200),
    ADD COLUMN vat_number  VARCHAR(50);
