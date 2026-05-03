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
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION user_schema.update_address_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_addresses_updated_at
    BEFORE UPDATE ON user_schema.addresses
    FOR EACH ROW EXECUTE FUNCTION user_schema.update_address_updated_at();
