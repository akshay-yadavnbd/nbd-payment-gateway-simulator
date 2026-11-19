-- Ensure merchant return URL column exists.
ALTER TABLE merchants
ADD COLUMN IF NOT EXISTS return_url VARCHAR(1000);