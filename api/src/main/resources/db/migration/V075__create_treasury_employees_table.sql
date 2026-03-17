-- V075: Treasury Employees (employees, contractors, and doctors)
-- Table created here; service/controller implemented in Phase 2
CREATE TABLE treasury_employees (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    employee_type VARCHAR(20) NOT NULL,
    tax_id VARCHAR(50),
    position VARCHAR(100),
    base_salary NUMERIC(12,2),
    contracted_rate NUMERIC(12,2),
    doctor_fee_arrangement VARCHAR(20),
    hospital_commission_pct NUMERIC(5,2) DEFAULT 0,
    hire_date DATE,
    termination_date DATE,
    termination_reason VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BIGINT REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_treasury_employees_deleted_at ON treasury_employees(deleted_at);
CREATE INDEX idx_treasury_employees_employee_type ON treasury_employees(employee_type);
CREATE INDEX idx_treasury_employees_active ON treasury_employees(active);
CREATE INDEX idx_treasury_employees_user_id ON treasury_employees(user_id);
