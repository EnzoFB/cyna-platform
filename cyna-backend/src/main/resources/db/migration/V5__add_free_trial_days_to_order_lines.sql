-- Snapshot the product's free-trial length onto each order line at order time.
--
-- Rationale: the trial length lives on the product (products.free_trial_days) and
-- can be changed by admins in the back-office at any moment. An order line is an
-- immutable record of what the customer bought, so — exactly like unit_price — the
-- trial granted must be frozen on the line at checkout, not re-read from the
-- (possibly since-changed) product when the Stripe subscription is created. The
-- payment finalize step reads this value to set Stripe's trial_period_days.
--
-- Default 0 = "no trial" so pre-existing lines keep their (non-trial) semantics.

ALTER TABLE order_schema.order_lines
    ADD COLUMN IF NOT EXISTS free_trial_days INTEGER NOT NULL DEFAULT 0;

ALTER TABLE order_schema.order_lines
    ADD CONSTRAINT ck_order_lines_free_trial_days CHECK (free_trial_days >= 0);
