-- V076: Salary History (tracks salary changes for PAYROLL employees)
CREATE TABLE salary_history (
    id BIGSERIAL PRIMARY KEY,
    treasury_employee_id BIGINT NOT NULL REFERENCES treasury_employees(id),
    base_salary NUMERIC(12,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_salary_history_deleted_at ON salary_history(deleted_at);
CREATE INDEX idx_salary_history_treasury_employee_id ON salary_history(treasury_employee_id);
CREATE INDEX idx_salary_history_effective_from ON salary_history(effective_from);
-- At most one open (effective_to IS NULL) record per employee
CREATE UNIQUE INDEX idx_salary_history_open_record
    ON salary_history(treasury_employee_id)
    WHERE effective_to IS NULL AND deleted_at IS NULL;
