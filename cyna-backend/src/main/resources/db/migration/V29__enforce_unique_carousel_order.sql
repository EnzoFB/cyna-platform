WITH ordered_promotions AS (
    SELECT id,
           ROW_NUMBER() OVER (
               ORDER BY
                   COALESCE(carousel_order, 2147483647),
                   updated_at,
                   id
               ) AS normalized_order
    FROM product_schema.promotions
    WHERE show_in_carousel = TRUE
)
UPDATE product_schema.promotions promotions
SET carousel_order = ordered_promotions.normalized_order
FROM ordered_promotions
WHERE promotions.id = ordered_promotions.id
  AND promotions.carousel_order IS DISTINCT FROM ordered_promotions.normalized_order;

CREATE UNIQUE INDEX IF NOT EXISTS uq_promotions_visible_carousel_order
    ON product_schema.promotions (carousel_order)
    WHERE show_in_carousel = TRUE;
