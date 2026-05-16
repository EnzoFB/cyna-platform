-- =====================================================
-- Drop the dead local payment-method cache
-- =====================================================
-- Stripe is the single source of truth for saved cards: the listing reads
-- them live (PaymentGatewayPort.listPaymentMethods) and the Customer Portal
-- owns add/remove/update/default. The payment_schema.saved_payment_methods
-- table (added in V13) had become write-only — nothing read it — so it is
-- removed along with its webhook-sync code. No data migration: every card
-- already lives in Stripe; RGPD consent proof is kept separately in
-- payment_schema.payment_consent_log.

DROP TABLE IF EXISTS payment_schema.saved_payment_methods CASCADE;
