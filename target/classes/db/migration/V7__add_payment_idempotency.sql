ALTER TABLE payments
ADD COLUMN idempotency_key VARCHAR(100);

-- Step 2: Create merchant-specific unique index
CREATE UNIQUE INDEX uq_payment_merchant_idempotency
ON payments (merchant_id, idempotency_key)
WHERE idempotency_key IS NOT NULL; 