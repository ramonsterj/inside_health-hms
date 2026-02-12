-- Patient charges table for billing module
-- Tracks every billable event (medication, room, procedure, lab, service, adjustment)
CREATE TABLE patient_charges (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    charge_type VARCHAR(30) NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
    total_amount DECIMAL(12,2) NOT NULL,
    charge_date DATE NOT NULL DEFAULT CURRENT_DATE,
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    room_id BIGINT REFERENCES rooms(id),
    invoice_id BIGINT, -- FK added in V059 after invoices table is created
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_patient_charges_deleted_at ON patient_charges(deleted_at);
CREATE INDEX idx_patient_charges_admission_id ON patient_charges(admission_id);
CREATE INDEX idx_patient_charges_charge_date ON patient_charges(charge_date);
CREATE INDEX idx_patient_charges_invoice_id ON patient_charges(invoice_id);
CREATE INDEX idx_patient_charges_charge_type ON patient_charges(charge_type);

-- Idempotency index for daily scheduler: prevent duplicate daily room charges
CREATE UNIQUE INDEX idx_patient_charges_daily_unique
    ON patient_charges(admission_id, charge_type, charge_date, COALESCE(room_id, 0))
    WHERE charge_type IN ('ROOM') AND deleted_at IS NULL;
