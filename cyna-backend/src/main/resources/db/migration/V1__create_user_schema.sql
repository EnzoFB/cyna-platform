CREATE SCHEMA IF NOT EXISTS user_schema;

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

CREATE OR REPLACE FUNCTION user_schema.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON user_schema.users
    FOR EACH ROW
    EXECUTE FUNCTION user_schema.update_updated_at();
