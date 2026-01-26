-- Create admissions table for patient hospital admissions
CREATE TABLE admissions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    triage_code_id BIGINT NOT NULL REFERENCES triage_codes(id),
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    treating_physician_id BIGINT NOT NULL REFERENCES users(id),
    admission_date TIMESTAMP NOT NULL,
    discharge_date TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, DISCHARGED
    inventory TEXT,  -- Patient belongings
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_admissions_deleted_at ON admissions(deleted_at);
CREATE INDEX idx_admissions_patient_id ON admissions(patient_id);
CREATE INDEX idx_admissions_room_id ON admissions(room_id);
CREATE INDEX idx_admissions_status ON admissions(status);
CREATE INDEX idx_admissions_treating_physician_id ON admissions(treating_physician_id);
CREATE INDEX idx_admissions_admission_date ON admissions(admission_date);
CREATE INDEX idx_admissions_triage_code_id ON admissions(triage_code_id);

-- Composite index for room availability calculations
CREATE INDEX idx_admissions_room_status_active ON admissions(room_id, status) WHERE deleted_at IS NULL;
