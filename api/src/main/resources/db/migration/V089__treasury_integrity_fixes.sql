-- V089: Treasury integrity fixes
-- 1. Add non_ledger flag to bank_statement_rows for acknowledge validation
-- 2. Add period_start, period_end, ending_balance to bank_statements
-- 3. Support PENDING status in bank_statements lifecycle

-- Bank statement rows: non_ledger flag for acknowledged rows
ALTER TABLE bank_statement_rows
    ADD COLUMN non_ledger BOOLEAN NOT NULL DEFAULT FALSE;

-- Bank statements: period and ending balance tracking
ALTER TABLE bank_statements
    ADD COLUMN period_start DATE,
    ADD COLUMN period_end DATE,
    ADD COLUMN ending_balance NUMERIC(12,2);
