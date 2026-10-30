
CREATE TABLE merchant_credentials (

    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    merchant_id BIGINT NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    failed_login_attempts INTEGER NOT NULL DEFAULT 0,

    locked_until TIMESTAMPTZ,

    password_changed_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_merchant_credentials_merchant
        FOREIGN KEY (merchant_id)
        REFERENCES merchants(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_merchant_credentials_merchant
        UNIQUE (merchant_id),

    CONSTRAINT chk_failed_login_attempts
        CHECK (failed_login_attempts >= 0),

    CONSTRAINT chk_password_hash_not_blank
        CHECK (LENGTH(TRIM(password_hash)) > 0)

);
