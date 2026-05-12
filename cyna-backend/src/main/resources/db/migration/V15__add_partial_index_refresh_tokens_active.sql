-- Partial index over active refresh tokens only.
--
-- Speeds up:
--   - bulk revocation by user (logout allDevices, change-password,
--     confirm-email-change, refresh-token reuse detection),
--   - per-user lookups when most rows are eventually revoked.
--
-- The base index idx_refresh_tokens_user_id (from V1) is kept — it still
-- serves queries that need to inspect revoked tokens (e.g. audit, cleanup).

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON user_schema.refresh_tokens (user_id)
    WHERE revoked = false;
