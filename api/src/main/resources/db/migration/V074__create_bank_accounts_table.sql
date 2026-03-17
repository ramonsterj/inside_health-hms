-- V074: Bank Accounts
CREATE TABLE bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    bank_name VARCHAR(100),
    account_number VARCHAR(50),
    account_type VARCHAR(30) NOT NULL DEFAULT 'CHECKING',
    currency VARCHAR(3) NOT NULL DEFAULT 'GTQ',
    opening_balance NUMERIC(12,2) NOT NULL DEFAULT 0,
    is_petty_cash BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_accounts_deleted_at ON bank_accounts(deleted_at);
CREATE INDEX idx_bank_accounts_active ON bank_accounts(active);

-- Seed petty cash account
INSERT INTO bank_accounts (name, account_type, is_petty_cash, currency, opening_balance)
VALUES ('Caja Chica', 'PETTY_CASH', TRUE, 'GTQ', 0);
