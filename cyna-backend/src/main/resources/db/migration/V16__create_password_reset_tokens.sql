-- Stores one-shot, time-limited tokens for the forgot-password flow.
-- The raw token only ever appears in the email sent to the user; the DB
-- holds the SHA-256 hash, same pattern as refresh_tokens.

CREATE TABLE IF NOT EXISTS user_schema.password_reset_tokens (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    token_hash      VARCHAR(255)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    consumed        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id)
        REFERENCES user_schema.users(id) ON DELETE CASCADE,
    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON user_schema.password_reset_tokens (user_id);

-- Partial index over still-usable tokens — speeds up cleanup of prior
-- pending tokens when a new reset is requested for the same user.
CREATE INDEX idx_password_reset_tokens_user_active
    ON user_schema.password_reset_tokens (user_id)
    WHERE consumed = false;
