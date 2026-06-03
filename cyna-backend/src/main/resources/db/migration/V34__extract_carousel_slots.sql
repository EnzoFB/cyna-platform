-- Create a dedicated table for carousel slots.
-- Carousel ordering is orthogonal to the promotion concept: a promotion does not
-- "have" an order, its *slot in the carousel* does. Keeping this separate prevents
-- stale carousel_order data on promotions and removes the need for application-level
-- conflict checks (the unique constraint handles it at DB level).

CREATE TABLE product_schema.carousel_slots (
    id           UUID DEFAULT gen_random_uuid() NOT NULL,
    promotion_id UUID                           NOT NULL,
    slot_order   INT                            NOT NULL,

    CONSTRAINT pk_carousel_slots
        PRIMARY KEY (id),
    CONSTRAINT fk_carousel_slots_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES product_schema.promotions (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_carousel_slots_promotion
        UNIQUE (promotion_id),
    CONSTRAINT uq_carousel_slots_order
        UNIQUE (slot_order),
    CONSTRAINT chk_carousel_slots_order
        CHECK (slot_order >= 1)
);

-- Migrate existing carousel data from promotions table.
INSERT INTO product_schema.carousel_slots (promotion_id, slot_order)
SELECT id, carousel_order
FROM   product_schema.promotions
WHERE  show_in_carousel = TRUE
ORDER  BY carousel_order;

-- Drop carousel columns and the partial unique index from promotions.
DROP INDEX IF EXISTS product_schema.uq_promotions_visible_carousel_order;

ALTER TABLE product_schema.promotions
    DROP COLUMN IF EXISTS show_in_carousel,
    DROP COLUMN IF EXISTS carousel_order;
