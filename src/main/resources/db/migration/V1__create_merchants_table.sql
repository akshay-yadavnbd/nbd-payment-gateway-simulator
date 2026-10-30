
CREATE TABLE merchants (

    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    merchant_id VARCHAR(40) NOT NULL UNIQUE,

    business_name VARCHAR(150) NOT NULL,

    email VARCHAR(254) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_merchant_status
        CHECK (
            status IN (
                'PENDING',
                'ACTIVE',
                'INACTIVE',
                'SUSPENDED'
            )
        ),

    CONSTRAINT chk_merchant_business_name
        CHECK (LENGTH(TRIM(business_name)) > 0),

    CONSTRAINT chk_merchant_email
        CHECK (LENGTH(TRIM(email)) > 0)

);

CREATE UNIQUE INDEX uq_merchants_email_lower
ON merchants (LOWER(email));
