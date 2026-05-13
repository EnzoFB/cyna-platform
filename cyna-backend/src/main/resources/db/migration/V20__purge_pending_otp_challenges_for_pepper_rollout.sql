-- HMAC-pepper rollout for the login OTP hash.
--
-- Before this change the otp_hash column stored a plain SHA-256 of the raw
-- 6-digit code; after this change it stores an HMAC-SHA256 keyed on the
-- server-side pepper. Existing pending challenges (rows where consumed=false)
-- would never verify again because the raw codes — held only in the user's
-- mailbox — would now be hashed differently on the server side. Wiping them
-- forces those users to re-run /login (5-minute window in the worst case),
-- which is preferable to a silent permanent-failure UX.
--
-- Consumed and expired rows are kept for audit; their hashes are no longer
-- used for any verification so the format change is irrelevant to them.

DELETE FROM user_schema.login_otp_challenges
WHERE consumed = false;
