-- Add locale preference column to users table for i18n support
ALTER TABLE users ADD COLUMN locale_preference VARCHAR(10) DEFAULT NULL;

-- Add comment for documentation
COMMENT ON COLUMN users.locale_preference IS 'User preferred locale code (e.g., en, es)';
