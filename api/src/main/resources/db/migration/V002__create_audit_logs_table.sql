-- Audit Logs table for tracking entity changes
-- This table stores all CREATE, UPDATE, DELETE operations on audited entities

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for querying by user
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);

-- Index for querying by entity (type + id)
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- Index for filtering by action type
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- Index for time-based queries (most common: recent logs)
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);

-- Comment on table
COMMENT ON TABLE audit_logs IS 'Immutable audit trail of all entity changes';
COMMENT ON COLUMN audit_logs.action IS 'CREATE, UPDATE, or DELETE';
COMMENT ON COLUMN audit_logs.old_values IS 'JSON snapshot of entity before change (UPDATE/DELETE only)';
COMMENT ON COLUMN audit_logs.new_values IS 'JSON snapshot of entity after change (CREATE/UPDATE only)';
