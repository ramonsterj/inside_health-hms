-- Create admission_consent_documents table for storing consent documents
CREATE TABLE admission_consent_documents (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL UNIQUE REFERENCES admissions(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_admission_consent_documents_deleted_at ON admission_consent_documents(deleted_at);
CREATE INDEX idx_admission_consent_documents_admission_id ON admission_consent_documents(admission_id);
