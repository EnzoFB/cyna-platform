-- =====================================================
-- User consent log — RGPD Art. 7.1 (proof of consent)
-- =====================================================
-- Records the mandatory acceptance of the Terms of Service and Privacy Policy
-- at registration: which wording version the user saw, when, and from where.
-- Stored separately from the users row so the proof is an append-only
-- historical fact, never mutated by profile edits.
--
-- Retained for the legal proof-of-consent period (5 years in France), aligned
-- with the litigation prescription (Code civil Art. 2224).

CREATE TABLE user_schema.user_consent_log (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    action        VARCHAR(64)  NOT NULL,
    label_version VARCHAR(32)  NOT NULL,
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(512),
    given_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_consent_log_user
        FOREIGN KEY (user_id) REFERENCES user_schema.users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_consent_log_action
        CHECK (action IN ('TERMS_AND_PRIVACY'))
);

CREATE INDEX idx_user_consent_log_user_id
    ON user_schema.user_consent_log (user_id);
