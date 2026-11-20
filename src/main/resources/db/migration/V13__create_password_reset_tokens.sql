
CREATE TABLE password_reset_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    credential_id BIGINT NOT NULL,

    token_hash VARCHAR(64) NOT NULL UNIQUE,

    expires_at TIMESTAMPTZ NOT NULL,

    used_at TIMESTAMPTZ,

    invalidated_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_tokens_credential
        FOREIGN KEY (credential_id)
        REFERENCES merchant_credentials(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_password_reset_token_state
        CHECK (
            used_at IS NULL OR invalidated_at IS NULL
        )
);

CREATE INDEX idx_password_reset_tokens_credential
    ON password_reset_tokens (credential_id);

CREATE INDEX idx_password_reset_tokens_expiry
    ON password_reset_tokens (expires_at);
