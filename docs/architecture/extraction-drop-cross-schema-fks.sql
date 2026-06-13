-- ============================================================================
-- Cross-schema foreign-key teardown — run ONLY when splitting a module out of
-- the monolith into its own database.
--
-- DO NOT place this under src/main/resources/db/migration: it must NOT run on
-- the monolith, where these constraints are kept as an integrity safety net.
-- The application already upholds these references through module APIs (write-
-- side validation) and domain events (deletion/anonymization), so dropping the
-- constraints is safe at extraction time.
--
-- Each statement is idempotent (DROP ... IF EXISTS). Drop only the constraints
-- that cross the boundary of the module you are extracting.
-- ============================================================================

-- subscription_schema.subscriptions → user / order / product
ALTER TABLE subscription_schema.subscriptions
    DROP CONSTRAINT IF EXISTS fk_subscriptions_user;
ALTER TABLE subscription_schema.subscriptions
    DROP CONSTRAINT IF EXISTS fk_subscriptions_order;
ALTER TABLE subscription_schema.subscriptions
    DROP CONSTRAINT IF EXISTS fk_subscriptions_product;

-- payment_schema.stripe_customers → user
ALTER TABLE payment_schema.stripe_customers
    DROP CONSTRAINT IF EXISTS fk_stripe_customers_user;

-- payment_schema.stripe_products → product
ALTER TABLE payment_schema.stripe_products
    DROP CONSTRAINT IF EXISTS fk_stripe_products_product;

-- payment_schema.payments → order / user
ALTER TABLE payment_schema.payments
    DROP CONSTRAINT IF EXISTS fk_payments_order;
ALTER TABLE payment_schema.payments
    DROP CONSTRAINT IF EXISTS fk_payments_user;

-- payment_schema.payment_consent_log → user (inline FK, auto-named by Postgres)
ALTER TABLE payment_schema.payment_consent_log
    DROP CONSTRAINT IF EXISTS payment_consent_log_user_id_fkey;
