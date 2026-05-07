-- ============================================================
-- V12 : Saved payment methods
-- Stores Stripe PaymentMethod metadata per user so the app can
-- list, select and delete cards without going through the portal.
-- Stripe is the source of truth for card data; we only keep
-- display metadata (brand, last4, expiry) + the Stripe PM id.
-- ============================================================

CREATE TABLE payment_schema.saved_payment_methods (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL
        CONSTRAINT fk_saved_pm_user REFERENCES user_schema.users(id) ON DELETE CASCADE,
    stripe_payment_method_id VARCHAR(255) NOT NULL,
    brand                    VARCHAR(50)  NOT NULL,
    last4                    VARCHAR(4)   NOT NULL,
    exp_month                VARCHAR(2)   NOT NULL,
    exp_year                 VARCHAR(4)   NOT NULL,
    holder_name              VARCHAR(255),
    is_default               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One Stripe PM id can only be attached to one row
CREATE UNIQUE INDEX idx_saved_pm_stripe_id
    ON payment_schema.saved_payment_methods(stripe_payment_method_id);

-- Fast lookup by user
CREATE INDEX idx_saved_pm_user_id
    ON payment_schema.saved_payment_methods(user_id);

-- Enforce at most one default per user at the DB level
CREATE UNIQUE INDEX idx_saved_pm_user_default
    ON payment_schema.saved_payment_methods(user_id)
    WHERE is_default = TRUE;

-- Reuse the function defined in V6
CREATE TRIGGER set_updated_at_saved_payment_methods
    BEFORE UPDATE ON payment_schema.saved_payment_methods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
