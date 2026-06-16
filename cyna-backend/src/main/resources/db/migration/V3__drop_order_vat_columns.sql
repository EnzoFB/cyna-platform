-- Drop the VAT/TTC columns from `orders`.
--
-- Rationale: Stripe is the authoritative source for what was actually billed
-- (Stripe Tax computes the destination VAT, B2B reverse charge, etc.). Storing
-- a hardcoded 20% locally created a divergence with the Stripe invoice — a
-- German customer was charged 19% by Stripe but seen as 20% in our DB. The
-- order now stores HT only; downstream consumers (account history, admin
-- dashboard, confirmation email) defer to the Stripe invoice for the
-- authoritative TTC/VAT amounts.

ALTER TABLE order_schema.orders
    DROP CONSTRAINT IF EXISTS ck_orders_vat_amount,
    DROP CONSTRAINT IF EXISTS ck_orders_total_amount,
    DROP COLUMN IF EXISTS vat_amount,
    DROP COLUMN IF EXISTS total_amount;
