-- =====================================================
-- CYNA PLATFORM - COMPLETE DATABASE SCHEMA
-- Consolidated migration - All tables, constraints, 
-- indexes, triggers, and initial data
-- =====================================================

-- =====================================================
-- SECTION 1: USER SCHEMA
-- =====================================================

CREATE SCHEMA IF NOT EXISTS user_schema;

-- Users table
CREATE TABLE IF NOT EXISTS user_schema.users (
    id              UUID            DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    role            VARCHAR(50)     NOT NULL DEFAULT 'CUSTOMER',
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN', 'SUPPORT')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_users_email ON user_schema.users (email);

-- Function for updating updated_at timestamp
CREATE OR REPLACE FUNCTION user_schema.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for users table
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON user_schema.users
    FOR EACH ROW
    EXECUTE FUNCTION user_schema.update_updated_at();

-- Refresh tokens table
CREATE TABLE IF NOT EXISTS user_schema.refresh_tokens (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    token_hash      VARCHAR(255)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES user_schema.users(id),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON user_schema.refresh_tokens (user_id);

-- =====================================================
-- SECTION 2: PRODUCT SCHEMA
-- =====================================================

CREATE SCHEMA IF NOT EXISTS product_schema;

-- Categories table
CREATE TABLE IF NOT EXISTS product_schema.categories (
    id              UUID            DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(255)    NOT NULL DEFAULT '',
    description     TEXT            NOT NULL DEFAULT '',
    image           BYTEA           NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE INDEX idx_categories_name ON product_schema.categories (name);
CREATE INDEX idx_categories_active ON product_schema.categories (is_active);

-- Function for updating updated_at timestamp
CREATE OR REPLACE FUNCTION product_schema.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for categories table
CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON product_schema.categories
    FOR EACH ROW
    EXECUTE FUNCTION product_schema.update_updated_at();

-- Products table
CREATE TABLE IF NOT EXISTS product_schema.products (
    id                      UUID            DEFAULT gen_random_uuid(),
    name                    VARCHAR(200)    NOT NULL,
    category_id             UUID            NOT NULL,
    service_description     TEXT            NOT NULL,
    technical_description   TEXT            NOT NULL,
    monthly_price           NUMERIC(19,4)   NOT NULL,
    annual_price            NUMERIC(19,4)   NOT NULL,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    is_published            BOOLEAN         NOT NULL DEFAULT FALSE,
    is_available            BOOLEAN         NOT NULL DEFAULT FALSE,
    priority_level          INTEGER         NOT NULL DEFAULT 0,
    free_trial_days         INTEGER         NOT NULL DEFAULT 0,
    highlight_points        JSONB           NOT NULL DEFAULT '[]',
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_schema.categories(id),
    CONSTRAINT ck_products_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_products_priority_level CHECK (priority_level >= 0)
);

CREATE INDEX idx_products_category_id ON product_schema.products (category_id);
CREATE INDEX idx_products_is_published ON product_schema.products (is_published);
CREATE INDEX idx_products_priority_level ON product_schema.products (priority_level DESC);
CREATE INDEX idx_products_created_at ON product_schema.products (created_at DESC);

-- Trigger for products table
CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON product_schema.products
    FOR EACH ROW
    EXECUTE FUNCTION product_schema.update_updated_at();

-- =====================================================
-- SECTION 3: CART SCHEMA
-- =====================================================

CREATE SCHEMA IF NOT EXISTS cart_schema;

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

CREATE TABLE IF NOT EXISTS cart_schema.cart_lines (
    id                  UUID            DEFAULT gen_random_uuid(),
    cart_id             UUID            NOT NULL,
    product_id          UUID            NOT NULL,
    product_name        VARCHAR(200)    NOT NULL,
    product_category    VARCHAR(50)     NOT NULL,
    billing_cycle       VARCHAR(50)     NOT NULL,
    quantity            INTEGER         NOT NULL,

    CONSTRAINT pk_cart_lines PRIMARY KEY (id),
    CONSTRAINT fk_cart_lines_carts FOREIGN KEY (cart_id)
        REFERENCES cart_schema.carts (id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_lines_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_cart_lines_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT uq_cart_lines_product_cycle UNIQUE (cart_id, product_id, billing_cycle)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_carts_active_user
    ON cart_schema.carts (user_id)
    WHERE status = 'ACTIVE' AND user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_carts_active_guest
    ON cart_schema.carts (guest_token)
    WHERE status = 'ACTIVE' AND guest_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_carts_status ON cart_schema.carts (status);
CREATE INDEX IF NOT EXISTS idx_carts_updated_at ON cart_schema.carts (updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_cart_lines_product_id ON cart_schema.cart_lines (product_id);
CREATE INDEX IF NOT EXISTS idx_cart_lines_cart_id ON cart_schema.cart_lines (cart_id);

-- Function for updating updated_at timestamp in cart_schema
CREATE OR REPLACE FUNCTION cart_schema.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for carts table
CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON cart_schema.carts
    FOR EACH ROW
    EXECUTE FUNCTION cart_schema.update_updated_at();

-- =====================================================
-- SECTION 4: SUBSCRIPTION SCHEMA
-- =====================================================

CREATE SCHEMA IF NOT EXISTS subscription_schema;

CREATE TABLE IF NOT EXISTS subscription_schema.subscriptions (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID            NOT NULL,
    order_id          UUID            NOT NULL,
    product_id        UUID            NOT NULL,
    product_name      VARCHAR(200)    NOT NULL,
    product_category  VARCHAR(200)    NOT NULL,
    billing_cycle     VARCHAR(20)     NOT NULL,
    status            VARCHAR(20)     NOT NULL,
    quantity          INTEGER         NOT NULL,
    unit_price        NUMERIC(19,4)   NOT NULL,
    currency          VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    start_at          TIMESTAMPTZ     NOT NULL,
    end_at            TIMESTAMPTZ     NOT NULL,
    next_billing_at   TIMESTAMPTZ     NOT NULL,
    cancelled_at      TIMESTAMPTZ     NULL,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT ck_subscriptions_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('PENDING', 'ACTIVE', 'PAUSED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_subscriptions_quantity CHECK (quantity >= 1 AND quantity <= 99),
    CONSTRAINT ck_subscriptions_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscription_schema.subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_order_id ON subscription_schema.subscriptions (order_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscription_schema.subscriptions (status);
CREATE INDEX IF NOT EXISTS idx_subscriptions_created_at_desc ON subscription_schema.subscriptions (created_at DESC);

-- =====================================================
-- SECTION 5: ORDER SCHEMA
-- =====================================================

CREATE SCHEMA IF NOT EXISTS order_schema;

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

    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PAID', 'FULFILLED', 'CANCELLED')),
    CONSTRAINT ck_orders_subtotal_amount CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_orders_vat_amount CHECK (vat_amount >= 0),
    CONSTRAINT ck_orders_total_amount CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON order_schema.orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON order_schema.orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc ON order_schema.orders (created_at DESC);

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

    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id)
        REFERENCES order_schema.orders(id) ON DELETE CASCADE,
    CONSTRAINT ck_order_lines_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    CONSTRAINT ck_order_lines_quantity CHECK (quantity >= 1 AND quantity <= 99),
    CONSTRAINT ck_order_lines_unit_price CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_lines_order_id ON order_schema.order_lines (order_id);

-- =====================================================
-- SECTION 6: INITIAL DATA SEEDING
-- =====================================================

-- Insert categories
INSERT INTO product_schema.categories (id, name, full_name, description, is_active)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'SOC',
        'Security Operations Center',
        'Centre opérationnel dédié à la surveillance, la détection et la réponse aux incidents de sécurité en temps réel.',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'EDR',
        'Endpoint Detection & Response',
        'Solution de sécurité permettant de surveiller, détecter et répondre aux menaces sur les postes de travail et serveurs.',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        'XDR',
        'Extended Detection & Response',
        'Plateforme unifiée de détection et de réponse couvrant plusieurs couches de sécurité (endpoint, réseau, cloud).',
        TRUE
    )
ON CONFLICT (id) DO NOTHING;

-- Insert products with all final columns
INSERT INTO product_schema.products (
    id,
    name,
    category_id,
    service_description,
    technical_description,
    monthly_price,
    annual_price,
    currency,
    is_published,
    is_available,
    priority_level,
    free_trial_days,
    highlight_points
)
VALUES
-- SOC products
(
    '10000000-0000-0000-0000-000000000001',
    'SOC Starter',
    '00000000-0000-0000-0000-000000000001',
    'Supervision de sécurité 24/7 pour petites entreprises avec alerting de base',
    'SIEM mutualisé, collecte logs, alertes temps réel, dashboard basique',
    99.99,
    999.99,
    'EUR',
    TRUE,
    TRUE,
    1,
    14,
    '["Protection en temps réel","Mises à jour automatiques","Support 24/7"]'
),
(
    '10000000-0000-0000-0000-000000000002',
    'SOC Advanced',
    '00000000-0000-0000-0000-000000000001',
    'SOC avancé avec analystes dédiés et réponse aux incidents',
    'SIEM dédié, playbooks automatisés, SOAR, intégration API, SLA 24/7',
    299.99,
    2999.99,
    'EUR',
    TRUE,
    TRUE,
    5,
    14,
    '["Protection en temps réel","Mises à jour automatiques","Support 24/7"]'
),
(
    '10000000-0000-0000-0000-000000000003',
    'SOC Enterprise',
    '00000000-0000-0000-0000-000000000001',
    'SOC complet avec équipe dédiée et threat intelligence',
    'SIEM + SOAR + Threat Intel + hunting proactif + intégration SI complexe',
    799.99,
    7999.99,
    'EUR',
    TRUE,
    FALSE,
    10,
    14,
    '["Protection en temps réel","Mises à jour automatiques","Support 24/7"]'
),

-- EDR products
(
    '20000000-0000-0000-0000-000000000001',
    'EDR Essential',
    '00000000-0000-0000-0000-000000000002',
    'Protection des endpoints avec détection comportementale',
    'Agent léger, détection malware, isolation machine, console cloud',
    4.99,
    49.99,
    'EUR',
    TRUE,
    TRUE,
    1,
    30,
    '["Détection avancée des menaces","Réponse automatisée","Analyse comportementale"]'
),
(
    '20000000-0000-0000-0000-000000000002',
    'EDR Professional',
    '00000000-0000-0000-0000-000000000002',
    'EDR avancé avec réponse automatisée',
    'Détection comportementale avancée, remédiation auto, forensic tools',
    9.99,
    99.99,
    'EUR',
    TRUE,
    TRUE,
    5,
    30,
    '["Détection avancée des menaces","Réponse automatisée","Analyse comportementale"]'
),
(
    '20000000-0000-0000-0000-000000000003',
    'EDR Elite',
    '00000000-0000-0000-0000-000000000002',
    'Protection endpoint premium avec threat hunting',
    'EDR + threat hunting + sandboxing + intégration SIEM',
    14.99,
    149.99,
    'EUR',
    TRUE,
    FALSE,
    10,
    30,
    '["Détection avancée des menaces","Réponse automatisée","Analyse comportementale"]'
),

-- XDR products
(
    '30000000-0000-0000-0000-000000000001',
    'XDR Core',
    '00000000-0000-0000-0000-000000000003',
    'Corrélation des événements sécurité multi-sources',
    'Collecte logs, corrélation basique, dashboard centralisé',
    199.99,
    1999.99,
    'EUR',
    TRUE,
    TRUE,
    5,
    30,
    '["Protection réseau étendue","Corrélation multi-sources","Tableau de bord unifié"]'
),
(
    '30000000-0000-0000-0000-000000000002',
    'XDR Advanced',
    '00000000-0000-0000-0000-000000000003',
    'XDR avec intelligence avancée et détection automatisée',
    'Corrélation IA, détection anomalies, intégration EDR + SIEM',
    399.99,
    3999.99,
    'EUR',
    TRUE,
    TRUE,
    10,
    30,
    '["Protection réseau étendue","Corrélation multi-sources","Tableau de bord unifié"]'
),
(
    '30000000-0000-0000-0000-000000000003',
    'XDR Ultimate',
    '00000000-0000-0000-0000-000000000003',
    'Plateforme XDR complète avec automatisation et orchestration',
    'XDR + SOAR + IA + playbooks avancés + réponse automatisée complète',
    699.99,
    6999.99,
    'EUR',
    TRUE,
    FALSE,
    10,
    30,
    '["Protection réseau étendue","Corrélation multi-sources","Tableau de bord unifié"]'
)
ON CONFLICT (id) DO NOTHING;
