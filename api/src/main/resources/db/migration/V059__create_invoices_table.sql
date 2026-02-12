-- Invoices table for consolidated billing at discharge
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    charge_count INT NOT NULL DEFAULT 0,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_invoices_deleted_at ON invoices(deleted_at);
CREATE INDEX idx_invoices_admission_id ON invoices(admission_id);

-- One invoice per admission
CREATE UNIQUE INDEX idx_invoices_admission_unique
    ON invoices(admission_id) WHERE deleted_at IS NULL;

-- Now add the FK from patient_charges to invoices
ALTER TABLE patient_charges
    ADD CONSTRAINT fk_patient_charges_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id);
