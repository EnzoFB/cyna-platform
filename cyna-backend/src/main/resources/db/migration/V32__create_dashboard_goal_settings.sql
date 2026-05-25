CREATE SCHEMA IF NOT EXISTS dashboard_schema;

CREATE TABLE IF NOT EXISTS dashboard_schema.dashboard_goal_settings (
    fiscal_year           INTEGER         NOT NULL,
    revenue_target_value  NUMERIC(19,4)   NOT NULL DEFAULT 0,
    clients_target_value  BIGINT          NOT NULL DEFAULT 0,
    monthly_revenue_goal  JSONB           NOT NULL DEFAULT '[]'::jsonb,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_dashboard_goal_settings PRIMARY KEY (fiscal_year),
    CONSTRAINT ck_dashboard_goal_settings_year CHECK (fiscal_year >= 2000),
    CONSTRAINT ck_dashboard_goal_settings_revenue_target CHECK (revenue_target_value >= 0),
    CONSTRAINT ck_dashboard_goal_settings_clients_target CHECK (clients_target_value >= 0)
);

CREATE OR REPLACE FUNCTION dashboard_schema.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_dashboard_goal_settings_updated_at ON dashboard_schema.dashboard_goal_settings;
CREATE TRIGGER trg_dashboard_goal_settings_updated_at
    BEFORE UPDATE ON dashboard_schema.dashboard_goal_settings
    FOR EACH ROW
    EXECUTE FUNCTION dashboard_schema.update_updated_at();
