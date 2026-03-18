# Treasury Module — Phased Implementation Plan

## Overview

The Treasury & Expense Management Module is broken into 5 self-contained phases, each deliverable independently. Phases 2, 3, and 4 are largely independent of each other once Phase 1 is done and can be developed in parallel.

| Phase | Summary | Depends On | Status |
|-------|---------|------------|--------|
| 1 — Bank Accounts + Expenses | Core financial records | — | ✅ Done |
| 2 — Income + Payroll | Revenue + personnel costs | Phase 1 | ✅ Done |
| 3 — Doctor Fees | Doctor compensation workflow | Phase 1 | |
| 4 — Reconciliation | Bank statement upload + matching | Phase 1 | |
| 5 — Dashboard + Reports | Aggregated views | Phases 1–4 | |

---

## Phase 1 — Bank Accounts + Expense Management ✅

**Scope**: The financial backbone. Everything else builds on this.

### Backend

- Migrations `V074`–`V078`: `bank_accounts`, `expenses`, `expense_payments`
- Migration `V084`: treasury permissions seed
- Entities: `BankAccount`, `Expense`, `ExpensePayment`
- Services: `BankAccountService`, `ExpenseService`
- Controllers: `BankAccountController`, `ExpenseController`

Key logic:
- Petty cash account seeded via migration, cannot be deleted
- Account number masking (last 4 digits) in responses
- Creating an expense with `isPaid=true` creates an `ExpensePayment` in the same transaction
- Partial payment logic updating `paid_amount` + status transitions (`PENDING` → `PARTIALLY_PAID` → `PAID`)
- `OVERDUE` computed at query time (not stored) — any expense with remaining balance and `due_date < CURRENT_DATE`
- Multipart expense creation with optional invoice scan upload
- Dedicated invoice scan upload/replace endpoint (`POST /expenses/{id}/invoice-document`)

### Frontend

- Types: `BankAccount`, `Expense`, `ExpensePayment`
- Stores: `useBankAccountStore`, `useExpenseStore`
- Views: `BankAccountList.vue`, `ExpenseList.vue`
- Components: `BankAccountForm.vue`, `ExpenseForm.vue`, `ExpensePaymentDialog.vue`
- Routes: `/treasury/bank-accounts`, `/treasury/expenses`
- Zod schemas: `createBankAccountSchema`, `createExpenseSchema`, `recordPaymentSchema`

---

## Phase 2 — Income + Employee/Payroll Compensation ✅

**Scope**: Revenue tracking and the full personnel cost picture.

### Backend

- Migrations `V075`, `V076`, `V079`, `V080`: `treasury_employees`, `salary_history`, `income_records`, `payroll_entries`
- Entities: `TreasuryEmployee`, `SalaryHistory`, `Income`, `PayrollEntry`
- Services: `IncomeService`, `TreasuryEmployeeService`
- Controllers: `IncomeController`, `TreasuryEmployeeController`

Key logic:
- Income can go to petty cash (cash receipts); optional FK to existing `invoices` table
- Payroll schedule generation: 14 entries/year (12 monthly + Bono 14 July 15 + Aguinaldo Dec 15)
- Salary update triggers (single transaction): close current `salary_history`, open new one, update future `PENDING` payroll entries
- Contractor payment via `POST .../employees/{id}/payments` → auto-creates `Expense` (category `PAYROLL`) with `invoice_number` from request
- Payroll payment via `POST .../payroll/{entryId}/pay` → auto-creates linked `Expense` with reference `PAYROLL-{employeeId}-{year}-{period}`
- Indemnización calculation: `base_salary × (days_worked / 365)`, computed dynamically — never stored
- Employee termination: sets `active=false` + `termination_date` in same transaction
- Unified payment history endpoint covering payroll entries and contractor payments per employee (doctor fee settlements deferred to Phase 3 — will be added to the payment history aggregation when `DoctorFee` entity exists)

### Frontend

- Types: `TreasuryEmployee`, `PayrollEntry`, `Income`, enums
- Stores: `useTreasuryEmployeeStore`, `useIncomeStore`
- Views: `IncomeList.vue`, `EmployeeList.vue`, `EmployeePayrollView.vue`
- Components: `IncomeForm.vue`, `EmployeeForm.vue` (conditional fields by type)
- Routes: `/treasury/income`, `/treasury/employees`, `/treasury/employees/:id/payroll`
- Zod schemas: `createIncomeSchema`, `createTreasuryEmployeeSchema`, `recordPayrollPaymentSchema`

---

## Phase 3 — Doctor Fee Billing

**Scope**: The doctor compensation workflow — the most complex status machine in the module.

### Backend

- Migration `V081`: `doctor_fees`
- Entity: `DoctorFee`
- Service: `DoctorFeeService`
- Controller: `DoctorFeeController`

Key logic:
- Fee calculation: `net_amount = gross_amount - (gross_amount × commission_pct / 100)`; defaults to doctor's `hospital_commission_pct`
- Status machine: `PENDING` → `INVOICED` (requires `doctor_invoice_number` + uploaded invoice doc) → `PAID` (via settle endpoint only)
- Invalid transitions return 400
- Settlement (`POST .../settle`): creates `Expense` (category `SERVICES`), copies `doctor_invoice_number` and invoice document reference → marks fee `PAID` atomically
- Uniqueness: at most one non-deleted `HOSPITAL_BILLED` `DoctorFee` per `patient_charge_id` (409 on duplicate)
- `EXTERNAL` fees: no financial records created, tracked for reference only
- Doctor invoice stored at `{base-path}/treasury/doctor-fees/{doctorFeeId}/{uuid}_{filename}`; referenced on linked expense at settlement
- Doctor fee summary report per doctor

### Frontend

- Types: `DoctorFee`, `DoctorFeeSummary`, enums
- Store: `useDoctorFeeStore`
- Views: `DoctorFeeList.vue`
- Components: `DoctorFeeForm.vue` (with commission preview calculation)
- Routes: `/treasury/employees/:id/doctor-fees`
- Zod schemas: `createDoctorFeeSchema`, `updateDoctorFeeStatusSchema`

---

## Phase 4 — Bank Statement Reconciliation

**Scope**: The most technically involved phase — file parsing and the reconciliation workflow.

### Backend

- Migrations `V082`, `V083`: `bank_statements`, `bank_statement_rows`, `bank_account_column_mappings`
- Entities: `BankStatement`, `BankStatementRow`, `BankAccountColumnMapping`
- Services: `BankStatementService`, `ReconciliationService`
- Controller: `BankStatementController`

Key logic:
- Upload blocked for petty cash accounts (400)
- XLSX parsing: Apache POI; CSV parsing: OpenCSV
- Column mapping saved per bank account; string header names for XLSX, numeric string indices for headerless CSV
- Statement files stored at `{base-path}/treasury/statements/{bankAccountId}/{uuid}_{filename}`
- Auto-match algorithm: exact amount + date within ±3 days; debits → `ExpensePayment`, credits → `Income`; single strong match → `SUGGESTED`; multiple or no candidates → `UNMATCHED`
- Auto-match sets `SUGGESTED` only — never auto-confirms to `MATCHED`
- Match status lifecycle: `UNMATCHED` → `SUGGESTED` → `MATCHED`/`UNMATCHED` (admin review), or `UNMATCHED` → `MATCHED` (manual)
- `ACKNOWLEDGED` only for non-ledger rows (e.g. duplicate import row, opening-balance carryover); current-period cash movements must become Expense/Income (400 otherwise)
- Create expense/income directly from an unmatched row (pre-fills from row data)
- Statement completes only when all rows are `MATCHED` or `ACKNOWLEDGED`; any `SUGGESTED` rows block completion
- Statement counters (`matched_count`, `unmatched_count`, `acknowledged_count`) updated on every row action

### Frontend

- Types: `BankStatement`, `BankStatementRow`, `ColumnMapping`, enums
- Store: `useBankStatementStore`
- Views: `BankStatementList.vue`, `ReconciliationView.vue`
- Components: `BankStatementUpload.vue`, `ColumnMappingForm.vue`
- Routes: `/treasury/bank-accounts/:id/statements`, `/treasury/statements/:id/reconcile`
- Reconciliation UI: color-coded rows (green=matched, blue=suggested, yellow=unmatched statement row, red=unmatched treasury record)

---

## Phase 5 — Treasury Dashboard + Financial Reports

**Scope**: Reporting layer and the main entry-point dashboard. Depends on all prior phases.

### Backend

- Service: `TreasuryReportService`
- Controller: `TreasuryReportController`

Reports:
- **Monthly payment report**: expenses + income grouped by category, net balance
- **Upcoming payments**: pending payables + payroll within configurable window (default 30 days), sorted by due date
- **Bank account summary**: opening balance, book balance `(opening_balance + income - expense_payments)`, last statement balance, recent transactions
- **Employee compensation summary**: type, compensation, YTD payments, pending amounts per person
- **Indemnización liability report**: all active PAYROLL employees (`active=true AND termination_date IS NULL`), hire date, tenure, current salary, calculated liability, grand total
- **Reconciliation summary**: per bank account — last reconciliation date, unmatched count, coverage %
- All reports support date range filtering where applicable

### Frontend

- Store: `useTreasuryReportStore`
- Views: `TreasuryDashboard.vue`, `BankSummaryReport.vue`, `MonthlyReport.vue`, `UpcomingPayments.vue`, `CompensationSummary.vue`, `IndemnizacionLiabilityReport.vue`, `ReconciliationSummary.vue`
- Dashboard: bank balances snapshot, pending payables count, next 7-day obligations
- Routes: `/treasury`, `/treasury/reports/bank-summary`, `/treasury/reports/monthly`, `/treasury/reports/upcoming`, `/treasury/reports/compensation`, `/treasury/reports/indemnizacion`, `/treasury/reports/reconciliation`
