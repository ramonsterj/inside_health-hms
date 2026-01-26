-- V026__create_admission_consulting_physicians_table.sql
-- Creates junction table for consulting physicians (interconsultas) on admissions

CREATE TABLE admission_consulting_physicians (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    physician_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(500),
    requested_date DATE,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

-- Partial unique constraint: same physician can only be added once per admission (excluding soft-deleted)
CREATE UNIQUE INDEX uk_admission_consulting_physician
    ON admission_consulting_physicians(admission_id, physician_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_acp_deleted_at ON admission_consulting_physicians(deleted_at);
CREATE INDEX idx_acp_admission_id ON admission_consulting_physicians(admission_id);
CREATE INDEX idx_acp_physician_id ON admission_consulting_physicians(physician_id);
