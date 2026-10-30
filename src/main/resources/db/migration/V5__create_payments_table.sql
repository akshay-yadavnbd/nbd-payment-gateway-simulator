CREATE TABLE payments(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id VARCHAR(40) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    checkout_token VARCHAR(64) NOT NULL UNIQUE,
    amount_paise BIGINT NOT  NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    error_code VARCHAR(50),
    settled_amount_paise BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_merchant
        FOREIGN KEY (merchant_id)
        REFERENCES merchants(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_payment_amount
        CHECK (amount_paise > 0),

    CONSTRAINT chk_payment_currency
        CHECK (currency = 'INR'),

    CONSTRAINT chk_payment_status
        CHECK(
            status IN (
                'PENDING',
                'SUCCESS',
                'FAILED',
                'REFUNDED'
            )
        ),

    CONSTRAINT chk_payment_settlement_consistency
        CHECK(
            (
                status = 'SUCCESS'
                AND settled_amount_paise = amount_paise
                AND error_code IS NULL
            )
            OR
            (
                status IN ('PENDING', 'FAILED', 'REFUNDED')
                AND settled_amount_paise = 0
            )
        )
);

CREATE INDEX idx_payments_merchant_id
ON payments (merchant_id);

CREATE INDEX idx_payments_created_at
ON payments (created_at);