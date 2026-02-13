CREATE TABLE medication_administrations (
    id BIGSERIAL PRIMARY KEY,
    medical_order_id BIGINT NOT NULL REFERENCES medical_orders(id),
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(1000),
    administered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_medication_administration_status
        CHECK (status IN ('GIVEN', 'MISSED', 'REFUSED', 'HELD'))
);

CREATE INDEX idx_medication_administrations_deleted_at ON medication_administrations(deleted_at);
CREATE INDEX idx_medication_administrations_medical_order_id ON medication_administrations(medical_order_id);
CREATE INDEX idx_medication_administrations_admission_id ON medication_administrations(admission_id);
CREATE INDEX idx_medication_administrations_status ON medication_administrations(status);
CREATE INDEX idx_medication_administrations_administered_at ON medication_administrations(administered_at);
