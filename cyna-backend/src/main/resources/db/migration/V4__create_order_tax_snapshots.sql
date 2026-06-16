-- Per-order VAT/TTC snapshot, captured once at payment time from the Stripe
-- checkout invoices (Stripe Tax: destination VAT + intra-EU B2B reverse charge).
--
-- Rationale: the order stores HT only (see V3) because Stripe is the
-- authoritative source for what was billed. Reading that VAT/TTC live from
-- Stripe on every confirmation-page view and email send was chatty (N invoice
-- reads per order, plus front-end polling) and drifted to the latest renewal
-- invoice for older orders. This table caches the figures at the moment of
-- payment — a single local read afterwards — while the Stripe invoice stays the
-- legal source of record.

CREATE TABLE IF NOT EXISTS payment_schema.order_tax_snapshots (
    order_id       UUID          PRIMARY KEY,
    user_id        UUID          NOT NULL,
    subtotal_ht    NUMERIC(19,4) NOT NULL,
    vat_amount     NUMERIC(19,4) NOT NULL,
    total_ttc      NUMERIC(19,4) NOT NULL,
    currency       VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    reverse_charge BOOLEAN       NOT NULL DEFAULT FALSE,
    captured_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_tax_snapshots_order FOREIGN KEY (order_id) REFERENCES order_schema.orders(id),
    CONSTRAINT fk_order_tax_snapshots_user  FOREIGN KEY (user_id)  REFERENCES user_schema.users(id)
);

CREATE INDEX IF NOT EXISTS idx_order_tax_snapshots_user_id
    ON payment_schema.order_tax_snapshots (user_id);
