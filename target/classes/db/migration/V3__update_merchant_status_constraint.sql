ALTER TABLE merchants
DROP CONSTRAINT chk_merchant_status;

UPDATE merchants
SET status = 'SUSPENDED',
    updated_at = CURRENT_TIMESTAMP

WHERE status = 'INACTIVE';



ALTER TABLE merchants
ADD CONSTRAINT chk_merchant_status
CHECK (
    status IN (
        'PENDING',
        'ACTIVE',
        'REJECTED',
        'SUSPENDED'
    )
);