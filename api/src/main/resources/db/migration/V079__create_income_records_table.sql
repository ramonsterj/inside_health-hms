-- V079: Income Records (cash and non-cash income to bank accounts)
CREATE TABLE income_records (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(30) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    income_date DATE NOT NULL,
    reference VARCHAR(100),
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    invoice_id BIGINT REFERENCES invoices(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_income_records_deleted_at ON income_records(deleted_at);
CREATE INDEX idx_income_records_income_date ON income_records(income_date);
CREATE INDEX idx_income_records_category ON income_records(category);
CREATE INDEX idx_income_records_bank_account_id ON income_records(bank_account_id);
CREATE INDEX idx_income_records_invoice_id ON income_records(invoice_id);
