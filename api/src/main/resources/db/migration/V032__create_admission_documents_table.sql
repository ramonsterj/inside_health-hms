-- Create admission_documents table for multi-document upload system
CREATE TABLE admission_documents (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    document_type_id BIGINT NOT NULL REFERENCES document_types(id),
    display_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_admission_documents_deleted_at ON admission_documents(deleted_at);
CREATE INDEX idx_admission_documents_admission_id ON admission_documents(admission_id);
CREATE INDEX idx_admission_documents_document_type_id ON admission_documents(document_type_id);
