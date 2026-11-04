
CREATE TABLE payment_attempts (

    id BIGSERIAL PRIMARY KEY,

    attempt_id VARCHAR(50) NOT NULL UNIQUE,

    payment_id BIGINT NOT NULL,

    attempt_number SMALLINT NOT NULL,

    amount_paise BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    error_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_attempts_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id),

    CONSTRAINT uq_payment_attempt_number
        UNIQUE (payment_id, attempt_number),

    CONSTRAINT chk_attempt_number
        CHECK (attempt_number BETWEEN 1 AND 5),

    CONSTRAINT chk_attempt_amount
        CHECK (amount_paise > 0),

    CONSTRAINT chk_attempt_status
        CHECK (status IN (
            'SUCCESS',
            'FAILED',
            'AUTHENTICATION_FAILED'
        ))
);

CREATE INDEX idx_payment_attempts_payment_id
    ON payment_attempts(payment_id);