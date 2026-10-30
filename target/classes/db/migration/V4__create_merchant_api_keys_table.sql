CREATE TABLE merchant_api_keys(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    key_hash VARCHAR(64) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_api_key_merchant
        FOREIGN KEY (merchant_id)
        REFERENCES merchants(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_api_key_merchant
        UNIQUE (merchant_id),

    CONSTRAINT uq_api_key_hash
        UNIQUE(key_hash),

    CONSTRAINT chk_api_key_hash_length
        CHECK (LENGTH(key_hash)=64)
);