-- =====================================================
-- RGPD Art. 17 — ANONYMIZED user status
-- =====================================================
-- When an erasure request targets an account that carries a legally-retained
-- transactional footprint (orders/invoices: French Code de commerce L123-22 =
-- 10 years), the account cannot be hard-deleted. It is scrubbed of direct
-- identifiers and moved to status ANONYMIZED — a terminal state that can never
-- authenticate again. The row survives only as a non-identifying FK anchor for
-- the retained accounting records.
--
-- No new column: the existing status + updated_at already convey "anonymized
-- and when". We only widen the CHECK constraint to admit the new value.

ALTER TABLE user_schema.users DROP CONSTRAINT ck_users_status;

ALTER TABLE user_schema.users
    ADD CONSTRAINT ck_users_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'ANONYMIZED'));
