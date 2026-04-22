ALTER TABLE user_schema.users
    ADD COLUMN IF NOT EXISTS company VARCHAR(255) NULL;

CREATE TABLE IF NOT EXISTS user_schema.email_change_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    new_email   VARCHAR(255) NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
