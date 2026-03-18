-- V080: Payroll Entries (generated schedule; one row per employee per pay period)
CREATE TABLE payroll_entries (
    id BIGSERIAL PRIMARY KEY,
    treasury_employee_id BIGINT NOT NULL REFERENCES treasury_employees(id),
    year INT NOT NULL,
    period VARCHAR(20) NOT NULL,
    period_label VARCHAR(50) NOT NULL,
    base_salary NUMERIC(12,2) NOT NULL,
    gross_amount NUMERIC(12,2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_date DATE,
    expense_id BIGINT REFERENCES expenses(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_payroll_entries_deleted_at ON payroll_entries(deleted_at);
CREATE INDEX idx_payroll_entries_treasury_employee_id ON payroll_entries(treasury_employee_id);
CREATE INDEX idx_payroll_entries_status ON payroll_entries(status);
CREATE INDEX idx_payroll_entries_year ON payroll_entries(year);
CREATE INDEX idx_payroll_entries_due_date ON payroll_entries(due_date);
-- Prevent duplicate entries per employee/year/period
CREATE UNIQUE INDEX idx_payroll_entries_unique
    ON payroll_entries(treasury_employee_id, year, period)
    WHERE deleted_at IS NULL;
