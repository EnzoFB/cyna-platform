-- =====================================================
-- Payment consent log — RGPD Art. 7.1 (proof of consent)
-- =====================================================
-- Every time a user explicitly consents to the persistence of their payment
-- method (checkbox "Reuse this card" at checkout), we record the act with
-- enough metadata to prove the consent later. Stored separately from
-- saved_payment_methods so that the proof survives card deletion.
--
-- Retained for the legal period (5 years minimum in France for proof of
-- consent under RGPD; aligned with PCI-DSS log retention guidelines).

CREATE TABLE payment_schema.payment_consent_log (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    action          VARCHAR(64)  NOT NULL,
    -- Identifies which version of the consent label / wording was shown to
    -- the user. If we ever change the checkbox text, bump the version so old
    -- proofs stay tied to the wording the user actually saw.
    label_version   VARCHAR(32)  NOT NULL,
    -- Stripe PaymentMethod id when applicable — null for actions that don't
    -- relate to a specific card (e.g. blanket withdrawals).
    stripe_payment_method_id VARCHAR(255),
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    given_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payment_consent_log_user
        FOREIGN KEY (user_id) REFERENCES user_schema.users (id) ON DELETE CASCADE,
    CONSTRAINT ck_payment_consent_log_action
        CHECK (action IN ('SAVE_CARD_AT_CHECKOUT'))
);

CREATE INDEX idx_payment_consent_log_user_id
    ON payment_schema.payment_consent_log (user_id);

CREATE INDEX idx_payment_consent_log_given_at_desc
    ON payment_schema.payment_consent_log (given_at DESC);
