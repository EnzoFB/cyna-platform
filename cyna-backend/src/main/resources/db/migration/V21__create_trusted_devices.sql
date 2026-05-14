-- "Trust this browser" device tokens.
--
-- After a successful OTP verification, the backend posts a long-lived
-- HttpOnly cookie (`device_token`) to the user's browser. Subsequent
-- /auth/login calls that present this cookie skip the OTP step and emit
-- the access + refresh tokens directly — same posture every SaaS uses
-- (Stripe, GitHub, Notion, etc.): MFA only on a never-seen browser.
--
-- Storage mirrors the other one-shot/credential tables in user_schema:
-- the raw token is sent to the browser, only its SHA-256 hash lives in
-- DB. Expiry is 30 days sliding — every successful trust hit updates
-- last_used_at and pushes expires_at forward, so an active user is
-- permanently trusted while a dormant one (>30d) gets re-challenged.

CREATE TABLE IF NOT EXISTS user_schema.trusted_devices (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    token_hash      VARCHAR(255)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_used_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    user_agent      TEXT            NULL,

    CONSTRAINT pk_trusted_devices PRIMARY KEY (id),
    CONSTRAINT fk_trusted_devices_user FOREIGN KEY (user_id)
        REFERENCES user_schema.users(id) ON DELETE CASCADE,
    CONSTRAINT uq_trusted_devices_hash UNIQUE (token_hash)
);

-- Listing trusted devices for a user (future "active devices" UI).
CREATE INDEX idx_trusted_devices_user_id
    ON user_schema.trusted_devices (user_id);

-- The UNIQUE constraint already provides an index on token_hash, which
-- covers the per-login lookup. Expiry is checked in the query, not in
-- the index predicate, since NOW() is mutable and can't appear in a
-- partial-index predicate.
