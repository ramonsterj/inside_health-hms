-- V087: Bank statements and statement rows for reconciliation
CREATE TABLE bank_statements (
    id BIGSERIAL PRIMARY KEY,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    statement_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    total_rows INT NOT NULL DEFAULT 0,
    matched_count INT NOT NULL DEFAULT 0,
    unmatched_count INT NOT NULL DEFAULT 0,
    acknowledged_count INT NOT NULL DEFAULT 0,
    suggested_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_statements_deleted_at ON bank_statements(deleted_at);
CREATE INDEX idx_bank_statements_bank_account_id ON bank_statements(bank_account_id);
CREATE INDEX idx_bank_statements_status ON bank_statements(status);
CREATE INDEX idx_bank_statements_statement_date ON bank_statements(statement_date);

CREATE TABLE bank_statement_rows (
    id BIGSERIAL PRIMARY KEY,
    bank_statement_id BIGINT NOT NULL REFERENCES bank_statements(id),
    row_number INT NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(500),
    reference VARCHAR(255),
    debit_amount NUMERIC(12,2),
    credit_amount NUMERIC(12,2),
    balance NUMERIC(12,2),
    match_status VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED',
    matched_entity_type VARCHAR(20),
    matched_entity_id BIGINT,
    acknowledged_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_statement_rows_deleted_at ON bank_statement_rows(deleted_at);
CREATE INDEX idx_bank_statement_rows_bank_statement_id ON bank_statement_rows(bank_statement_id);
CREATE INDEX idx_bank_statement_rows_match_status ON bank_statement_rows(match_status);
CREATE INDEX idx_bank_statement_rows_transaction_date ON bank_statement_rows(transaction_date);
CREATE UNIQUE INDEX uq_bank_statement_rows_statement_row
    ON bank_statement_rows(bank_statement_id, row_number)
    WHERE deleted_at IS NULL;
