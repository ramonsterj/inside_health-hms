-- Create patients table for New Patient Intake feature
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    age INTEGER NOT NULL CHECK (age >= 0 AND age <= 150),
    sex VARCHAR(10) NOT NULL,
    gender VARCHAR(50) NOT NULL,
    marital_status VARCHAR(20) NOT NULL,
    religion VARCHAR(100) NOT NULL,
    education_level VARCHAR(20) NOT NULL,
    occupation VARCHAR(100) NOT NULL,
    address VARCHAR(500) NOT NULL,
    email VARCHAR(255) NOT NULL,
    id_document_number VARCHAR(50),
    notes TEXT,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

-- Indexes for search and performance
CREATE INDEX idx_patients_last_name ON patients(last_name);
CREATE INDEX idx_patients_first_name ON patients(first_name);
CREATE INDEX idx_patients_id_document_number ON patients(id_document_number);
CREATE INDEX idx_patients_deleted_at ON patients(deleted_at);
