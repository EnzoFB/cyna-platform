CREATE TABLE IF NOT EXISTS user_schema.login_otp_challenges (
    id              UUID            DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    otp_hash        VARCHAR(255)    NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    consumed        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    consumed_at     TIMESTAMPTZ     NULL,

    CONSTRAINT pk_login_otp_challenges PRIMARY KEY (id),
    CONSTRAINT fk_login_otp_challenges_user FOREIGN KEY (user_id)
        REFERENCES user_schema.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_login_otp_challenges_user_id
    ON user_schema.login_otp_challenges (user_id);

CREATE INDEX IF NOT EXISTS idx_login_otp_challenges_expires_at
    ON user_schema.login_otp_challenges (expires_at);

CREATE INDEX IF NOT EXISTS idx_login_otp_challenges_consumed
    ON user_schema.login_otp_challenges (consumed);
