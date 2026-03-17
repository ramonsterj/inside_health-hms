-- V078: Expense Payments (actual money movement for expenses)
CREATE TABLE expense_payments (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses(id),
    amount NUMERIC(12,2) NOT NULL,
    payment_date DATE NOT NULL,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    reference VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_expense_payments_deleted_at ON expense_payments(deleted_at);
CREATE INDEX idx_expense_payments_expense_id ON expense_payments(expense_id);
CREATE INDEX idx_expense_payments_bank_account_id ON expense_payments(bank_account_id);
CREATE INDEX idx_expense_payments_payment_date ON expense_payments(payment_date);
