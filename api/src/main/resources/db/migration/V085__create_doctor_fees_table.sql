CREATE TABLE doctor_fees (
    id                    BIGSERIAL PRIMARY KEY,
    treasury_employee_id  BIGINT       NOT NULL REFERENCES treasury_employees(id),
    patient_charge_id     BIGINT       REFERENCES patient_charges(id),
    billing_type          VARCHAR(20)  NOT NULL,
    gross_amount          NUMERIC(12,2) NOT NULL,
    commission_pct        NUMERIC(5,2)  NOT NULL,
    net_amount            NUMERIC(12,2) NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    doctor_invoice_number VARCHAR(100),
    invoice_document_path VARCHAR(500),
    expense_id            BIGINT       REFERENCES expenses(id),
    fee_date              DATE         NOT NULL,
    description           VARCHAR(500),
    notes                 TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT,
    updated_by            BIGINT,
    deleted_at            TIMESTAMP
);

CREATE INDEX idx_doctor_fees_deleted_at ON doctor_fees(deleted_at);
CREATE INDEX idx_doctor_fees_treasury_employee_id ON doctor_fees(treasury_employee_id);
CREATE INDEX idx_doctor_fees_patient_charge_id ON doctor_fees(patient_charge_id);
CREATE INDEX idx_doctor_fees_status ON doctor_fees(status);
CREATE INDEX idx_doctor_fees_expense_id ON doctor_fees(expense_id);

-- At most one non-deleted HOSPITAL_BILLED fee per patient_charge_id
CREATE UNIQUE INDEX uq_doctor_fees_charge_hospital
    ON doctor_fees(patient_charge_id)
    WHERE billing_type = 'HOSPITAL_BILLED' AND deleted_at IS NULL;
