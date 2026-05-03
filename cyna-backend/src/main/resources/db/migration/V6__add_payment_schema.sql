-- ============================================================
-- V3 : Module paiement — tables payments et stripe_customers
-- ============================================================

-- Fonction trigger updated_at (créée ici si absente des migrations précédentes)
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Table des paiements
CREATE TABLE payment_schema.payments (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                 UUID         NOT NULL,
    user_id                  UUID         NOT NULL,
    status                   VARCHAR(50)  NOT NULL
        CONSTRAINT ck_payments_status
            CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
    amount                   NUMERIC(19, 4) NOT NULL,
    currency                 VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    stripe_payment_intent_id VARCHAR(255),
    stripe_client_secret     VARCHAR(500),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES order_schema.orders (id),
    CONSTRAINT fk_payments_user
        FOREIGN KEY (user_id)  REFERENCES user_schema.users (id),
    CONSTRAINT uq_payments_stripe_intent
        UNIQUE (stripe_payment_intent_id)
);

-- Table de correspondance utilisateur ↔ client Stripe
-- Stockée dans payment_schema pour garder la dépendance Stripe
-- isolée du module User
CREATE TABLE payment_schema.stripe_customers (
    user_id            UUID         PRIMARY KEY,
    stripe_customer_id VARCHAR(255) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stripe_customers_user
        FOREIGN KEY (user_id) REFERENCES user_schema.users (id)
);

-- Index de recherche
CREATE INDEX idx_payments_order_id
    ON payment_schema.payments (order_id);

CREATE INDEX idx_payments_user_id
    ON payment_schema.payments (user_id);

CREATE INDEX idx_payments_stripe_intent
    ON payment_schema.payments (stripe_payment_intent_id);

-- Trigger updated_at (réutilise la fonction définie en V1)
CREATE TRIGGER set_updated_at_payments
    BEFORE UPDATE ON payment_schema.payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
