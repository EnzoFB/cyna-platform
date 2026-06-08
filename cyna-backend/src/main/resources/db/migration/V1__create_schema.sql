-- =====================================================
-- CYNA PLATFORM — CONSOLIDATED SCHEMA
-- =====================================================

-- =====================================================
-- SCHEMAS
-- =====================================================

CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS cart_schema;
CREATE SCHEMA IF NOT EXISTS subscription_schema;
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE SCHEMA IF NOT EXISTS dashboard_schema;


-- =====================================================
-- FONCTION updated_at (partagée par tous les triggers)
-- =====================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =====================================================
-- USER_SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS user_schema.users (
    id              UUID            DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    company         VARCHAR(255),
    role            VARCHAR(50)     NOT NULL DEFAULT 'CUSTOMER',
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role   CHECK (role   IN ('CUSTOMER', 'ADMIN', 'SUPPORT')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ANONYMIZED'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON user_schema.users (email);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON user_schema.users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── refresh_tokens ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.refresh_tokens (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    token_hash      VARCHAR(255)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens     PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES user_schema.users(id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON user_schema.refresh_tokens (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON user_schema.refresh_tokens (user_id)
    WHERE revoked = false;

-- ── email_change_tokens ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.email_change_tokens (
    id          UUID            PRIMARY KEY,
    user_id     UUID            NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    new_email   VARCHAR(255)    NOT NULL,
    token       VARCHAR(255)    NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ── addresses ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.addresses (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    label        VARCHAR(255) NOT NULL,
    address      VARCHAR(255) NOT NULL,
    address2     VARCHAR(255),
    zip_code     VARCHAR(20)  NOT NULL,
    city         VARCHAR(100) NOT NULL,
    region       VARCHAR(100) NOT NULL,
    country_code VARCHAR(2)   NOT NULL,
    phone        VARCHAR(50)  NOT NULL,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    first_name   VARCHAR(100) NOT NULL DEFAULT '',
    last_name    VARCHAR(100) NOT NULL DEFAULT '',
    company      VARCHAR(200),
    vat_number   VARCHAR(50),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_addresses_updated_at
    BEFORE UPDATE ON user_schema.addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── login_otp_challenges ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.login_otp_challenges (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    otp_hash        VARCHAR(255)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    consumed        BOOLEAN         NOT NULL DEFAULT FALSE,
    attempts        INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    consumed_at     TIMESTAMPTZ,

    CONSTRAINT pk_login_otp_challenges PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_login_otp_challenges_user_id
    ON user_schema.login_otp_challenges (user_id);
CREATE INDEX IF NOT EXISTS idx_login_otp_challenges_expires_at
    ON user_schema.login_otp_challenges (expires_at);
CREATE INDEX IF NOT EXISTS idx_login_otp_challenges_consumed
    ON user_schema.login_otp_challenges (consumed);
CREATE INDEX IF NOT EXISTS idx_login_otp_challenges_user_active
    ON user_schema.login_otp_challenges (user_id)
    WHERE consumed = false;

-- ── password_reset_tokens ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.password_reset_tokens (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255)    NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    consumed        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON user_schema.password_reset_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_active
    ON user_schema.password_reset_tokens (user_id)
    WHERE consumed = false;

-- ── trusted_devices ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.trusted_devices (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255)    NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_used_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    user_agent      TEXT,

    CONSTRAINT pk_trusted_devices PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_trusted_devices_user_id
    ON user_schema.trusted_devices (user_id);

-- ── user_consent_log ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_schema.user_consent_log (
    id            UUID         DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    action        VARCHAR(64)  NOT NULL,
    label_version VARCHAR(32)  NOT NULL,
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(512),
    given_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_consent_log PRIMARY KEY (id),
    CONSTRAINT ck_user_consent_log_action CHECK (action IN ('TERMS_AND_PRIVACY'))
);

CREATE INDEX IF NOT EXISTS idx_user_consent_log_user_id
    ON user_schema.user_consent_log (user_id);


-- =====================================================
-- PRODUCT_SCHEMA
-- =====================================================

-- ── categories ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.categories (
    id              UUID            DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    image           BYTEA,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_categories     PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_categories_name   ON product_schema.categories (name);
CREATE INDEX IF NOT EXISTS idx_categories_active ON product_schema.categories (is_active);

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON product_schema.categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── category_translations ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.category_translations (
    category_id  UUID         NOT NULL REFERENCES product_schema.categories(id) ON DELETE CASCADE,
    locale       VARCHAR(10)  NOT NULL,
    full_name    VARCHAR(255) NOT NULL DEFAULT '',
    description  TEXT         NOT NULL DEFAULT '',

    CONSTRAINT pk_category_translations PRIMARY KEY (category_id, locale)
);

CREATE INDEX IF NOT EXISTS idx_category_translations_locale
    ON product_schema.category_translations (category_id, locale);

-- ── products ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.products (
    id                      UUID            DEFAULT gen_random_uuid(),
    category_id             UUID            NOT NULL,
    monthly_price           NUMERIC(19,4)   NOT NULL,
    annual_price            NUMERIC(19,4)   NOT NULL,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    is_published            BOOLEAN         NOT NULL DEFAULT FALSE,
    is_available            BOOLEAN         NOT NULL DEFAULT FALSE,
    priority_level          INTEGER         NOT NULL DEFAULT 0,
    free_trial_days         INTEGER         NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products          PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_schema.categories(id),
    CONSTRAINT ck_products_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_products_priority_level  CHECK (priority_level >= 0)
);

CREATE INDEX IF NOT EXISTS idx_products_category_id    ON product_schema.products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_is_published   ON product_schema.products (is_published);
CREATE INDEX IF NOT EXISTS idx_products_priority_level ON product_schema.products (priority_level DESC);
CREATE INDEX IF NOT EXISTS idx_products_created_at     ON product_schema.products (created_at DESC);

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON product_schema.products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── product_translations ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.product_translations (
    product_id            UUID         NOT NULL REFERENCES product_schema.products(id) ON DELETE CASCADE,
    locale                VARCHAR(10)  NOT NULL,
    name                  VARCHAR(200) NOT NULL,
    service_description   TEXT         NOT NULL DEFAULT '',
    technical_description TEXT         NOT NULL DEFAULT '',
    highlight_points      JSONB        NOT NULL DEFAULT '[]',

    CONSTRAINT pk_product_translations PRIMARY KEY (product_id, locale)
);

CREATE INDEX IF NOT EXISTS idx_product_translations_locale
    ON product_schema.product_translations (product_id, locale);

-- ── product_images ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.product_images (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL,
    image_data      BYTEA           NOT NULL,
    mime_type       VARCHAR(50)     NOT NULL DEFAULT 'image/jpeg',
    display_order   INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_product_images          PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product  FOREIGN KEY (product_id)
        REFERENCES product_schema.products(id) ON DELETE CASCADE,
    CONSTRAINT ck_product_images_display_order CHECK (display_order >= 0),
    CONSTRAINT uq_product_images_product_order UNIQUE (product_id, display_order)
);

CREATE INDEX IF NOT EXISTS idx_product_images_product_id
    ON product_schema.product_images (product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_display_order
    ON product_schema.product_images (display_order);

CREATE TRIGGER trg_product_images_updated_at
    BEFORE UPDATE ON product_schema.product_images
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── promotions ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.promotions (
    id                UUID          NOT NULL DEFAULT gen_random_uuid(),
    product_id        UUID          NOT NULL,
    discount_percent  INTEGER       NOT NULL,
    start_at          TIMESTAMPTZ   NOT NULL,
    end_at            TIMESTAMPTZ   NOT NULL,
    is_enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT fk_promotions_product
        FOREIGN KEY (product_id) REFERENCES product_schema.products(id) ON DELETE CASCADE,
    CONSTRAINT ck_promotions_discount_percent CHECK (discount_percent BETWEEN 1 AND 100),
    CONSTRAINT ck_promotions_window           CHECK (start_at < end_at)
);

CREATE INDEX IF NOT EXISTS idx_promotions_product_id
    ON product_schema.promotions (product_id);
CREATE INDEX IF NOT EXISTS idx_promotions_enabled_window
    ON product_schema.promotions (is_enabled, start_at, end_at);

CREATE TRIGGER trg_promotions_updated_at
    BEFORE UPDATE ON product_schema.promotions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── promotion_translations ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.promotion_translations (
    promotion_id   UUID        NOT NULL REFERENCES product_schema.promotions(id) ON DELETE CASCADE,
    locale         VARCHAR(10) NOT NULL,
    marketing_text TEXT        NOT NULL DEFAULT '',

    CONSTRAINT pk_promotion_translations PRIMARY KEY (promotion_id, locale)
);

CREATE INDEX IF NOT EXISTS idx_promotion_translations_promotion_locale
    ON product_schema.promotion_translations (promotion_id, locale);

-- ── offer_carousel_settings ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.offer_carousel_settings (
    id          SMALLINT    NOT NULL,
    max_slides  INTEGER     NOT NULL DEFAULT 5,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_offer_carousel_settings          PRIMARY KEY (id),
    CONSTRAINT ck_offer_carousel_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_max_slides CHECK (max_slides BETWEEN 1 AND 20)
);

CREATE TRIGGER trg_offer_carousel_settings_updated_at
    BEFORE UPDATE ON product_schema.offer_carousel_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── offer_carousel_settings_translations ──────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.offer_carousel_settings_translations (
    settings_id  SMALLINT    NOT NULL REFERENCES product_schema.offer_carousel_settings(id) ON DELETE CASCADE,
    locale       VARCHAR(10) NOT NULL,
    fixed_text   TEXT        NOT NULL DEFAULT '',

    CONSTRAINT pk_offer_carousel_settings_translations PRIMARY KEY (settings_id, locale)
);

CREATE INDEX IF NOT EXISTS idx_offer_carousel_settings_translations_settings_locale
    ON product_schema.offer_carousel_settings_translations (settings_id, locale);

-- ── carousel_slots ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_schema.carousel_slots (
    id           UUID DEFAULT gen_random_uuid() NOT NULL,
    promotion_id UUID                           NOT NULL,
    slot_order   INT                            NOT NULL,

    CONSTRAINT pk_carousel_slots           PRIMARY KEY (id),
    CONSTRAINT fk_carousel_slots_promotion
        FOREIGN KEY (promotion_id) REFERENCES product_schema.promotions(id) ON DELETE CASCADE,
    CONSTRAINT uq_carousel_slots_promotion UNIQUE (promotion_id),
    CONSTRAINT uq_carousel_slots_order     UNIQUE (slot_order),
    CONSTRAINT chk_carousel_slots_order    CHECK (slot_order >= 1)
);


-- =====================================================
-- CART_SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS cart_schema.carts (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NULL,
    guest_token     VARCHAR(255)    NULL,
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT ck_carts_status CHECK (status IN ('ACTIVE', 'CHECKED_OUT')),
    CONSTRAINT ck_carts_owner_xor CHECK (
        (user_id IS NOT NULL AND guest_token IS NULL)
        OR
        (user_id IS NULL AND guest_token IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_carts_active_user
    ON cart_schema.carts (user_id)
    WHERE status = 'ACTIVE' AND user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_carts_active_guest
    ON cart_schema.carts (guest_token)
    WHERE status = 'ACTIVE' AND guest_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_carts_status     ON cart_schema.carts (status);
CREATE INDEX IF NOT EXISTS idx_carts_updated_at ON cart_schema.carts (updated_at DESC);

CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON cart_schema.carts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── cart_lines ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cart_schema.cart_lines (
    id                  UUID            DEFAULT gen_random_uuid(),
    cart_id             UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    product_name        VARCHAR(200)    NOT NULL,
    product_category    VARCHAR(50)     NOT NULL,
    billing_cycle       VARCHAR(50)     NOT NULL,
    quantity            INTEGER         NOT NULL,

    CONSTRAINT pk_cart_lines          PRIMARY KEY (id),
    CONSTRAINT fk_cart_lines_carts    FOREIGN KEY (cart_id) REFERENCES cart_schema.carts(id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_lines_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_cart_lines_quantity      CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT uq_cart_lines_product_cycle UNIQUE (cart_id, product_id, billing_cycle)
);

CREATE INDEX IF NOT EXISTS idx_cart_lines_product_id ON cart_schema.cart_lines (product_id);
CREATE INDEX IF NOT EXISTS idx_cart_lines_cart_id    ON cart_schema.cart_lines (cart_id);


-- =====================================================
-- ORDER_SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS order_schema.orders (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    subtotal_amount NUMERIC(19,4)   NOT NULL,
    vat_amount      NUMERIC(19,4)   NOT NULL,
    total_amount    NUMERIC(19,4)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_orders_status          CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'FULFILLED', 'CANCELLED')),
    CONSTRAINT ck_orders_subtotal_amount CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_orders_vat_amount      CHECK (vat_amount >= 0),
    CONSTRAINT ck_orders_total_amount    CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id         ON order_schema.orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status          ON order_schema.orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc ON order_schema.orders (created_at DESC);

CREATE TRIGGER set_updated_at_orders
    BEFORE UPDATE ON order_schema.orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── order_lines ───────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS order_schema.order_lines (
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID            NOT NULL,
    product_id       UUID            NOT NULL,
    product_name     VARCHAR(200)    NOT NULL,
    product_category VARCHAR(200)    NOT NULL,
    billing_cycle    VARCHAR(20)     NOT NULL,
    quantity         INTEGER         NOT NULL,
    unit_price       NUMERIC(19,4)   NOT NULL,
    currency         VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_lines_order         FOREIGN KEY (order_id) REFERENCES order_schema.orders(id) ON DELETE CASCADE,
    CONSTRAINT ck_order_lines_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_order_lines_quantity      CHECK (quantity >= 1 AND quantity <= 99),
    CONSTRAINT ck_order_lines_unit_price    CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_lines_order_id ON order_schema.order_lines (order_id);


-- =====================================================
-- SUBSCRIPTION_SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS subscription_schema.subscriptions (
    id                        UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id                   UUID            NOT NULL,
    order_id                  UUID            NOT NULL,
    product_id                UUID            NOT NULL,
    product_name              VARCHAR(200)    NOT NULL,
    product_category          VARCHAR(200)    NOT NULL,
    order_line_id             UUID,
    billing_cycle             VARCHAR(20)     NOT NULL,
    status                    VARCHAR(20)     NOT NULL,
    quantity                  INTEGER         NOT NULL,
    unit_price                NUMERIC(19,4)   NOT NULL,
    currency                  VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    start_at                  TIMESTAMPTZ     NOT NULL,
    end_at                    TIMESTAMPTZ     NOT NULL,
    next_billing_at           TIMESTAMPTZ     NOT NULL,
    cancelled_at              TIMESTAMPTZ,
    auto_renew                BOOLEAN         NOT NULL DEFAULT TRUE,
    auto_renew_notice_sent_at TIMESTAMPTZ,
    stripe_subscription_id    VARCHAR(255),
    stripe_schedule_id        VARCHAR(255),
    created_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_subscriptions        PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_user   FOREIGN KEY (user_id)    REFERENCES user_schema.users(id),
    CONSTRAINT fk_subscriptions_order  FOREIGN KEY (order_id)   REFERENCES order_schema.orders(id),
    CONSTRAINT fk_subscriptions_product FOREIGN KEY (product_id) REFERENCES product_schema.products(id),
    CONSTRAINT ck_subscriptions_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_subscriptions_status        CHECK (status IN ('PENDING', 'ACTIVE', 'PAST_DUE', 'PAUSED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_subscriptions_quantity      CHECK (quantity >= 1 AND quantity <= 99),
    CONSTRAINT ck_subscriptions_unit_price    CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id          ON subscription_schema.subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_order_id         ON subscription_schema.subscriptions (order_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status           ON subscription_schema.subscriptions (status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_created_at_desc  ON subscription_schema.subscriptions (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_subscriptions_end_at           ON subscription_schema.subscriptions (end_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_auto_renew_notice
    ON subscription_schema.subscriptions (auto_renew, auto_renew_notice_sent_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_stripe_subscription
    ON subscription_schema.subscriptions (stripe_subscription_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_order_line
    ON subscription_schema.subscriptions (order_line_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_order_line
    ON subscription_schema.subscriptions (order_line_id)
    WHERE order_line_id IS NOT NULL;

CREATE TRIGGER set_updated_at_subscriptions
    BEFORE UPDATE ON subscription_schema.subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- =====================================================
-- PAYMENT_SCHEMA
-- =====================================================

-- ── stripe_customers ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS payment_schema.stripe_customers (
    user_id            UUID         PRIMARY KEY,
    stripe_customer_id VARCHAR(255) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stripe_customers_user FOREIGN KEY (user_id) REFERENCES user_schema.users(id)
);

-- ── stripe_products ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS payment_schema.stripe_products (
    cyna_product_id    UUID         PRIMARY KEY,
    stripe_product_id  VARCHAR(255) NOT NULL UNIQUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stripe_products_product FOREIGN KEY (cyna_product_id) REFERENCES product_schema.products(id)
);

-- ── payments ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS payment_schema.payments (
    id                                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                          UUID           NOT NULL,
    user_id                           UUID           NOT NULL,
    status                            VARCHAR(50)    NOT NULL,
    amount                            NUMERIC(19,4)  NOT NULL,
    currency                          VARCHAR(3)     NOT NULL DEFAULT 'EUR',
    stripe_payment_intent_id          VARCHAR(255),
    stripe_client_secret              VARCHAR(500),
    stripe_subscription_id            VARCHAR(255),
    stripe_schedule_id                VARCHAR(255),
    stripe_setup_intent_id            VARCHAR(255),
    stripe_setup_intent_client_secret VARCHAR(500),
    created_at                        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at                        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payments_order     FOREIGN KEY (order_id) REFERENCES order_schema.orders(id),
    CONSTRAINT fk_payments_user      FOREIGN KEY (user_id)  REFERENCES user_schema.users(id),
    CONSTRAINT ck_payments_status    CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
    CONSTRAINT uq_payments_stripe_intent UNIQUE (stripe_payment_intent_id)
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id
    ON payment_schema.payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id
    ON payment_schema.payments (user_id);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_intent
    ON payment_schema.payments (stripe_payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_subscription
    ON payment_schema.payments (stripe_subscription_id);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_setup_intent
    ON payment_schema.payments (stripe_setup_intent_id);

CREATE TRIGGER set_updated_at_payments
    BEFORE UPDATE ON payment_schema.payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── processed_stripe_events ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS payment_schema.processed_stripe_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    event_type   VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_stripe_events_processed_at
    ON payment_schema.processed_stripe_events (processed_at DESC);

-- ── payment_consent_log ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS payment_schema.payment_consent_log (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL REFERENCES user_schema.users(id) ON DELETE CASCADE,
    action                   VARCHAR(64)  NOT NULL,
    label_version            VARCHAR(32)  NOT NULL,
    stripe_payment_method_id VARCHAR(255),
    ip_address               VARCHAR(64),
    user_agent               VARCHAR(512),
    given_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_payment_consent_log_action CHECK (action IN ('SAVE_CARD_AT_CHECKOUT'))
);

CREATE INDEX IF NOT EXISTS idx_payment_consent_log_user_id
    ON payment_schema.payment_consent_log (user_id);
CREATE INDEX IF NOT EXISTS idx_payment_consent_log_given_at_desc
    ON payment_schema.payment_consent_log (given_at DESC);


-- =====================================================
-- DASHBOARD_SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS dashboard_schema.dashboard_goal_settings (
    fiscal_year           INTEGER         NOT NULL,
    revenue_target_value  NUMERIC(19,4)   NOT NULL DEFAULT 0,
    clients_target_value  BIGINT          NOT NULL DEFAULT 0,
    monthly_revenue_goal  JSONB           NOT NULL DEFAULT '[]'::jsonb,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dashboard_goal_settings         PRIMARY KEY (fiscal_year),
    CONSTRAINT ck_dashboard_goal_settings_year    CHECK (fiscal_year >= 2000),
    CONSTRAINT ck_dashboard_goal_settings_revenue CHECK (revenue_target_value >= 0),
    CONSTRAINT ck_dashboard_goal_settings_clients CHECK (clients_target_value >= 0)
);

CREATE TRIGGER trg_dashboard_goal_settings_updated_at
    BEFORE UPDATE ON dashboard_schema.dashboard_goal_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
