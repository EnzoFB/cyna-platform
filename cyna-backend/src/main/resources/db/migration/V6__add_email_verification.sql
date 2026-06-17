-- ─────────────────────────────────────────────────────────────────────────────
-- Email verification at registration (CDC §X/§XI)
--
-- Accounts created via self-service registration start in
-- PENDING_VERIFICATION and cannot authenticate until the user clicks the
-- unique link mailed at registration (valid 24h). This migration:
--   (a) widens the users.status CHECK constraint to allow PENDING_VERIFICATION,
--   (b) creates the email_verification_tokens table (mirrors
--       password_reset_tokens: hashed single-use token, 24h TTL).
-- ─────────────────────────────────────────────────────────────────────────────

-- ── (a) users.status — allow PENDING_VERIFICATION ─────────────────────────────

ALTER TABLE user_schema.users
    DROP CONSTRAINT IF EXISTS ck_users_status;

ALTER TABLE user_schema.users
    ADD CONSTRAINT ck_users_status
    CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'INACTIVE', 'ANONYMIZED'));

-- ── (b) email_verification_tokens ─────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.email_verification_tokens (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255)    NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    consumed        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id
    ON user_schema.email_verification_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_active
    ON user_schema.email_verification_tokens (user_id)
    WHERE consumed = false;
