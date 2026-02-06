-- Create vital_signs table
CREATE TABLE vital_signs (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    systolic_bp INTEGER NOT NULL,
    diastolic_bp INTEGER NOT NULL,
    heart_rate INTEGER NOT NULL,
    respiratory_rate INTEGER NOT NULL,
    temperature DECIMAL(4,1) NOT NULL,
    oxygen_saturation INTEGER NOT NULL,
    other VARCHAR(1000),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_vital_signs_admission_id FOREIGN KEY (admission_id) REFERENCES admissions(id),
    CONSTRAINT fk_vital_signs_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_vital_signs_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_vital_signs_bp CHECK (systolic_bp > diastolic_bp)
);

CREATE INDEX idx_vital_signs_admission_id ON vital_signs(admission_id);
CREATE INDEX idx_vital_signs_recorded_at ON vital_signs(recorded_at);
CREATE INDEX idx_vital_signs_deleted_at ON vital_signs(deleted_at);
