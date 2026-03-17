-- V077: Expenses
CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    supplier_name VARCHAR(255) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description TEXT,
    amount NUMERIC(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    invoice_document_path VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date DATE,
    paid_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    treasury_employee_id BIGINT REFERENCES treasury_employees(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_expenses_deleted_at ON expenses(deleted_at);
CREATE INDEX idx_expenses_status ON expenses(status);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);
CREATE INDEX idx_expenses_due_date ON expenses(due_date);
CREATE INDEX idx_expenses_treasury_employee_id ON expenses(treasury_employee_id);
CREATE INDEX idx_expenses_invoice_number ON expenses(invoice_number);
CREATE INDEX idx_expenses_supplier_name ON expenses(supplier_name);
