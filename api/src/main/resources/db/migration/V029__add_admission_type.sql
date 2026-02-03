-- Add admission type column with default for existing records
ALTER TABLE admissions
ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'HOSPITALIZATION';

-- Make room_id nullable (for ambulatory/procedure admissions)
ALTER TABLE admissions
ALTER COLUMN room_id DROP NOT NULL;

-- Create index on type for filtering
CREATE INDEX idx_admissions_type ON admissions(type);

-- Create composite index for common filter combinations (partial index excluding soft-deleted)
CREATE INDEX idx_admissions_status_type ON admissions(status, type) WHERE deleted_at IS NULL;
