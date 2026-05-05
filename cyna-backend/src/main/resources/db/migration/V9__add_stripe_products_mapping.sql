-- ============================================================
-- V6 : Persistent mapping {cyna_product_id → stripe_product_id}
-- Avoids creating duplicate Stripe Products on backend restart
-- or across multiple instances.
-- ============================================================

CREATE TABLE payment_schema.stripe_products (
    cyna_product_id    UUID         PRIMARY KEY,
    stripe_product_id  VARCHAR(255) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stripe_products_cyna
        FOREIGN KEY (cyna_product_id) REFERENCES product_schema.products (id),
    CONSTRAINT uq_stripe_products_stripe_id
        UNIQUE (stripe_product_id)
);
