-- Add changed_fields column to audit_logs table
-- This column stores a JSON array of field names that were modified during an UPDATE operation
-- For sensitive fields (like passwordHash), the field name is recorded but not the value

ALTER TABLE audit_logs
ADD COLUMN changed_fields JSONB;

-- Add a comment explaining the column's purpose
COMMENT ON COLUMN audit_logs.changed_fields IS 'JSON array of field names that were modified (includes sensitive field names without values)';
