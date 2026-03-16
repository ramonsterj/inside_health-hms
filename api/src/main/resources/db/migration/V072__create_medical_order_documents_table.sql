-- Create medical_order_documents table for attaching files to medical orders (e.g., lab results)

CREATE TABLE medical_order_documents (
    id BIGSERIAL PRIMARY KEY,
    medical_order_id BIGINT NOT NULL REFERENCES medical_orders(id),
    display_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_medical_order_documents_medical_order_id ON medical_order_documents(medical_order_id);
CREATE INDEX idx_medical_order_documents_deleted_at ON medical_order_documents(deleted_at);
