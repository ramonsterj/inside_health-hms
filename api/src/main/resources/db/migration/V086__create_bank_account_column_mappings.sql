-- V086: Column mappings for bank statement parsing
CREATE TABLE bank_account_column_mappings (
    id BIGSERIAL PRIMARY KEY,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    file_type VARCHAR(10) NOT NULL DEFAULT 'XLSX',
    has_header BOOLEAN NOT NULL DEFAULT TRUE,
    date_column VARCHAR(50) NOT NULL,
    description_column VARCHAR(50),
    reference_column VARCHAR(50),
    debit_column VARCHAR(50) NOT NULL,
    credit_column VARCHAR(50) NOT NULL,
    balance_column VARCHAR(50),
    date_format VARCHAR(30) NOT NULL DEFAULT 'dd/MM/yyyy',
    skip_rows INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_account_column_mappings_deleted_at ON bank_account_column_mappings(deleted_at);
CREATE UNIQUE INDEX uq_bank_account_column_mappings_account
    ON bank_account_column_mappings(bank_account_id)
    WHERE deleted_at IS NULL;
