-- Add max_slides column to offer_carousel_settings with default value of 5
ALTER TABLE product_schema.offer_carousel_settings
    ADD COLUMN max_slides INTEGER NOT NULL DEFAULT 5
        CONSTRAINT chk_max_slides CHECK (max_slides BETWEEN 1 AND 20);
