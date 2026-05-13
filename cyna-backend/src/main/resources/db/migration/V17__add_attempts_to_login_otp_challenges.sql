-- Brute-force hardening for the login OTP challenge:
--   - attempts counter caps wrong-code submissions per challenge (locked at MAX).
--   - partial index speeds up the "invalidate previous active challenges" sweep
--     run on every /login (mirrors password_reset_tokens).

ALTER TABLE user_schema.login_otp_challenges
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_login_otp_challenges_user_active
    ON user_schema.login_otp_challenges (user_id)
    WHERE consumed = false;
