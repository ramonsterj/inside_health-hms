-- Create emergency_contacts table for patient emergency contacts
CREATE TABLE emergency_contacts (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    name VARCHAR(200) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_emergency_contacts_patient_id ON emergency_contacts(patient_id);
CREATE INDEX idx_emergency_contacts_deleted_at ON emergency_contacts(deleted_at);
