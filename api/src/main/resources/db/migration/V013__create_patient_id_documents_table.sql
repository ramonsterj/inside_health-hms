-- Create patient_id_documents table for storing ID document files
CREATE TABLE patient_id_documents (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL UNIQUE REFERENCES patients(id),
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

-- Indexes
CREATE INDEX idx_patient_id_documents_patient_id ON patient_id_documents(patient_id);
CREATE INDEX idx_patient_id_documents_deleted_at ON patient_id_documents(deleted_at);
