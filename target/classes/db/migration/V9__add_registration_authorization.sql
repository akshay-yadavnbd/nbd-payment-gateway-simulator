
ALTER TABLE email_verifications
ADD COLUMN registration_token_hash VARCHAR(64) UNIQUE;

ALTER TABLE email_verifications
ADD COLUMN registration_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE email_verifications
ADD COLUMN registration_consumed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE email_verifications
ADD CONSTRAINT chk_registration_authorization
CHECK (
    (
        registration_token_hash IS NULL
        AND registration_expires_at IS NULL
        AND registration_consumed_at IS NULL
    )
    OR
    (
        registration_token_hash IS NOT NULL
        AND registration_expires_at IS NOT NULL
    )
);

CREATE INDEX idx_email_verifications_registration_expiry
ON email_verifications (registration_expires_at)
WHERE registration_consumed_at IS NULL;
