# Feature: Treasury & Expense Management Module

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-03-17 | @ramonster | Initial draft |
| 1.1 | 2026-03-17 | @ramonster | Add compensation tracking, doctor fee billing, bank statement reconciliation |
| 1.2 | 2026-03-17 | @ramonster | Add employee tenure tracking and indemnización liability calculation |
| 1.3 | 2026-03-17 | Codex | Resolve implementation gaps, align API/schema/validation, add indemnización and reconciliation details |
| 1.4 | 2026-03-17 | @ramonster | Invoice scan at creation, suggested-only auto-match, contractor payments as employee payments, INVOICED doctor fee status, salary history mechanism, petty cash income |

---

## Overview

A treasury module that tracks all hospital financial operations: expense registration (paid via bank account or petty cash), income recording from patient billing, invoice management with deferred payment support, bank account management, employee/contractor/doctor compensation tracking and payroll, employee tenure tracking with indemnización liability provisioning, doctor fee billing and payment, bank statement upload with automated reconciliation, and financial reporting with monthly/weekly payment planning.

---

## Use Case / User Story

1. **As an admin staff member**, I want to register every hospital expense with its corresponding invoice so that we maintain a complete financial record.

2. **As an admin staff member**, I want to specify whether an expense was paid from a specific bank account or from petty cash so that we know exactly where money is going.

3. **As an admin staff member**, I want to register an invoice for a later payment (accounts payable) so that we can track upcoming financial obligations.

4. **As an admin staff member**, I want to record income payments and specify which bank account receives the deposit so that we can reconcile bank balances.

5. **As an admin staff member**, I want to see a monthly report of all payments (expenses and income) so that I can review the hospital's financial activity.

6. **As an admin staff member**, I want to see upcoming payments due per week/month so that we can plan cash flow safely.

7. **As an admin staff member**, I want to track employee payments, distinguishing between invoice-based contractors and payroll employees (14 payments/year) so that we have a unified view of all outflows.

8. **As an admin staff member**, I want to maintain a registry of all paid staff (employees, contractors, and doctors) with their compensation details so that I know exactly how much each person earns.

9. **As an admin staff member**, I want to track doctor fee billing — sometimes the hospital bills doctor fees on their behalf and pays them against their invoices, and sometimes it happens outside the hospital — so that we have a complete picture of doctor compensation.

10. **As an admin staff member**, I want to upload bank statements (XLSX or CSV) and have the system cross-reference them against recorded payments and expenses so that I can reconcile our books quickly and spot discrepancies.

11. **As an admin staff member**, I want to track how long each payroll employee has been working for the hospital so that I know exactly how much indemnización liability we have accumulated in case of dismissal.

---

## Authorization / Role Access

| Action | Required Role(s) | Permission | Notes |
|--------|------------------|------------|-------|
| View bank accounts | ADMIN, ADMINISTRATIVE_STAFF | `treasury:read` | |
| Manage bank accounts | ADMIN, ADMINISTRATIVE_STAFF | `treasury:configure` | Create/edit/deactivate |
| View expenses | ADMIN, ADMINISTRATIVE_STAFF | `treasury:read` | |
| Create/edit expenses | ADMIN, ADMINISTRATIVE_STAFF | `treasury:write` | |
| Delete expenses | ADMIN, ADMINISTRATIVE_STAFF | `treasury:delete` | Soft delete only |
| View income records | ADMIN, ADMINISTRATIVE_STAFF | `treasury:read` | |
| Create/edit income | ADMIN, ADMINISTRATIVE_STAFF | `treasury:write` | |
| View payables (pending) | ADMIN, ADMINISTRATIVE_STAFF | `treasury:read` | |
| Mark payable as paid | ADMIN, ADMINISTRATIVE_STAFF | `treasury:write` | |
| View employees | ADMIN, ADMINISTRATIVE_STAFF | `treasury:read` | |
| Manage employees | ADMIN, ADMINISTRATIVE_STAFF | `treasury:configure` | Create/edit employee records |
| Record employee payments | ADMIN, ADMINISTRATIVE_STAFF | `treasury:write` | |
| View employee tenure & indemnización | ADMIN, ADMINISTRATIVE_STAFF | `treasury:read` | |
| View doctor fee records | ADMIN, ADMINISTRATIVE_STAFF | `treasury:read` | |
| Manage doctor fees | ADMIN, ADMINISTRATIVE_STAFF | `treasury:write` | Bill, record payments |
| Upload bank statements | ADMIN, ADMINISTRATIVE_STAFF | `treasury:reconcile` | Upload XLSX/CSV |
| View reconciliation results | ADMIN, ADMINISTRATIVE_STAFF | `treasury:reconcile` | Match/unmatch transactions |
| View financial reports | ADMIN, ADMINISTRATIVE_STAFF | `treasury:report` | Monthly summaries, upcoming payments |

**Notes:**
- This module is restricted to `ADMIN` and `ADMINISTRATIVE_STAFF` roles — it deals with sensitive financial data.
- All operations are audited via BaseEntity (createdBy, updatedBy).

---

## Functional Requirements

### Bank Account Management
- CRUD for hospital bank accounts (name, bank name, account number, account type, currency, opening balance, active status).
- A special system-level "Petty Cash" account is seeded automatically.
- Bank accounts can be deactivated but not hard-deleted (soft delete).
- Account numbers should be partially masked in list views (show last 4 digits only).
- Current account balance is a computed "book balance" derived from `opening_balance + income - expense payments`; it is not entered manually after account creation.

### Expense Registration
- Every expense must have:
  - A supplier/vendor name (free text).
  - An expense category (enum: `SUPPLIES`, `UTILITIES`, `MAINTENANCE`, `EQUIPMENT`, `SERVICES`, `PAYROLL`, `OTHER`).
  - An optional description.
  - An amount.
  - An expense date.
  - A payment source when paid: reference to a bank account OR petty cash.
  - An invoice number or system-generated payroll reference. For manually entered expenses, the invoice number is required. For payroll-generated salary expenses, the system generates a reference value.
  - An optional invoice document attachment (scan or photo of the physical invoice). This can be uploaded at the time of expense creation (as a multipart request) or added/replaced later via a dedicated endpoint. For auditing purposes, uploading the invoice scan is strongly encouraged.
- Expenses can be registered as either:
  - **Pending payable**: no money has left the account yet.
  - **Already paid**: the system creates the expense plus an initial `ExpensePayment` record in the same transaction.
- All cash movements for expenses are represented by `ExpensePayment` rows, including the initial payment when an expense is created as paid.
- When creating or recording a payment, the payment date and bank account must be recorded.
- The persisted expense statuses are `PENDING`, `PARTIALLY_PAID`, `PAID`, and `CANCELLED`.

### Accounts Payable (Deferred Payments)
- Invoices registered for later payment must include a due date.
- Payables have persisted statuses: `PENDING`, `PARTIALLY_PAID`, `PAID`, `CANCELLED`.
- `OVERDUE` is a computed display state, not a stored status. Any payable with remaining balance and `due_date < CURRENT_DATE` is displayed as overdue.
- Partial payments are supported: record multiple payments against a single payable.
- Each payment against a payable records: amount, date, bank account used, and a reference/note.

### Income Registration
- Record income payments received from patients (linked to existing billing invoices when applicable).
- Each income record includes: amount, date, optional source description, bank account credited, and optional reference to a patient billing invoice.
- Income categories: `PATIENT_PAYMENT`, `INSURANCE`, `GOVERNMENT`, `DONATION`, `OTHER`.
- The destination account can be any active bank account **including the petty cash account**, to represent cash receipts (e.g., a patient paying in cash at the front desk).

### Employee & Contractor Compensation Tracking
- Maintain a financial registry of all paid personnel (separate from the User entity — not all employees are system users, and not all users are paid employees).
- Employee record includes: full name, employee type (`CONTRACTOR`, `PAYROLL`, or `DOCTOR`), tax ID (NIT), position/role description, compensation details, active status, and optional link to a system User.
- **Compensation details** are stored per employee:
  - **Payroll employees**: base salary (monthly), which drives the 14-payment schedule.
  - **Contractors**: agreed rate or fee (for reference/reporting; actual amounts come from their invoices).
  - **Doctors**: fee arrangement details (see Doctor Fee Billing below).
- **Contractors**: submit invoices; each payment is recorded via `POST /api/v1/treasury/employees/{id}/payments`, which automatically creates a linked `Expense` record with category `PAYROLL`. This ensures contractor payments appear in team cost reporting alongside payroll employees. The contractor's invoice scan should be attached to the generated expense record.
- **Payroll employees**: 14 payments per year (12 monthly + Bono 14 + Aguinaldo). Track each payment period and status.
- Generate a payroll schedule at the start of each year for payroll employees (14 entries).
- Each payroll entry records: period (month or bonus type), due date, amount, payment date, bank account, and status (`PENDING`, `PAID`).
- **All employee types** (contractor, payroll, doctor) have a unified payment history accessible via `GET /api/v1/treasury/employees/{id}/payments`, enabling a single view of total personnel cost per person.
- Employee list view shows: name, type, position, compensation amount, payment status summary (e.g., "8/14 paid" for payroll, total YTD for contractors).

### Employee Tenure & Indemnización Tracking
- **Applies to**: `PAYROLL` employees only (contractors and doctors are excluded — they have no labor relationship).
- Each payroll employee record tracks: `hire_date` (required), `termination_date` (null while active), `termination_reason` (optional), `active` (employment status), and `base_salary` (current monthly salary used for liability calculation).
- **Tenure calculation**: Computed dynamically as the difference between `hire_date` and the current date (or `termination_date` if terminated). Displayed as years, months, and days.
- **Indemnización calculation** (Guatemalan labor law):
  - One month's salary per year of service (proportional for partial years).
  - Formula: `(base_salary / 365) × days_worked` — equivalent to `base_salary × (days_worked / 365)`.
  - Uses the employee's **current** base salary (`base_salary`), not a historical average.
  - Recalculated dynamically on every query — no stored value (always up to date with current salary).
- **Salary history**: When a payroll employee's base salary is updated, the service layer automatically: (1) closes the current open `salary_history` record by setting its `effective_to = today`, (2) creates a new `salary_history` record with `effective_from = today` and no `effective_to`, and (3) updates all future unpaid `payroll_entries` for that employee to reflect the new amount. This history is for audit purposes only and does not affect the indemnización calculation (which always uses current salary per Guatemalan law).
- **Indemnización liability report**: Shows all active payroll employees (`active = TRUE` and `termination_date IS NULL`) with hire date, tenure, current salary, and calculated indemnización amount. Includes a total liability sum for the hospital.
- **Employee detail view**: Shows tenure prominently, with the indemnización amount calculated in real time.
- **Termination flow**: When an employee is terminated, admin records the termination date and reason, the system calculates the final indemnización amount as of that date, and the employee's `active` flag is set to `FALSE`. The employee record remains in the system for historical reporting; termination is not a delete operation.

### Doctor Fee Billing & Payment
- Doctors who treat patients at the hospital may have their fees billed through the hospital or handled externally.
- Each doctor in the treasury employee registry can have a fee arrangement: `HOSPITAL_BILLED` (hospital collects from patient and pays doctor) or `EXTERNAL` (doctor handles billing independently, tracked for reference only).
- **Hospital-billed flow**:
  1. Doctor fees are captured as part of patient charges (existing billing module, charge type `SERVICE` or `PROCEDURE`).
  2. A `DoctorFee` record links the patient charge to the doctor's treasury employee record, tracking: patient charge reference, gross amount, hospital commission/percentage (if any), net amount payable to doctor. Status starts as `PENDING`.
  3. When the doctor submits their physical invoice, admin records the doctor's invoice number and uploads a copy of the invoice to the system. Advancing the `DoctorFee` status to `INVOICED` via `PUT /api/v1/treasury/doctor-fees/{id}/status` is only allowed when the invoice number is present and an invoice document has already been uploaded or is included in the same workflow. No financial transaction is created at this step; it only signals that the invoice has been received and payment is pending.
  4. Admin settles the fee by creating a settlement expense (category `SERVICES`) linked to the `DoctorFee` record. This is done via `POST /api/v1/treasury/doctor-fees/{id}/settle`, which creates the expense, copies the doctor's invoice number into the linked expense `invoice_number`, associates the stored invoice document, and transitions the fee status to `PAID` in one transaction.
  5. In v1, partial doctor-fee payouts are not supported: one `DoctorFee` is either `PENDING`, `INVOICED`, or settled by exactly one linked expense (`PAID`).
- **Uniqueness rule**: A hospital-billed patient charge can be linked to at most one non-deleted `DoctorFee` record.
- **External flow**: Admin can register a doctor fee record marked as `EXTERNAL` for tracking purposes — no hospital expense or income is generated, but the record is visible in the doctor's history for complete records.
- **Doctor fee summary report**: Per doctor, show total fees billed, hospital commission retained, net payable, amount paid, and outstanding balance.

### Bank Statement Reconciliation
- Admin can upload bank statements as XLSX or CSV files for any non-petty-cash bank account.
- **Upload & parsing**:
  - Supported formats: XLSX (.xlsx) and CSV (.csv).
  - The system parses each row into: transaction date, description, reference number, debit amount, credit amount, balance (columns are configurable per bank account since different banks use different formats).
  - Column mapping is saved per bank account so subsequent uploads auto-detect the format.
  - Uploaded statements are stored as files following the existing file storage pattern.
- **Statement record**: Each upload creates a `BankStatement` record with: bank account, upload date, file path, period start/end dates, row count, reconciliation status.
- **Auto-matching**: After upload, the system attempts to find candidate matches for each statement row against recorded `ExpensePayment` rows (debits) and `Income` records (credits). Matching criteria: exact amount plus date within ±3 days tolerance. Auto-match results are **suggestions only** — the system sets the row status to `SUGGESTED` and highlights the candidate record, but does not confirm the match. An admin must review each suggestion and explicitly confirm or reject it.
- **Match status lifecycle**: `UNMATCHED` → `SUGGESTED` (auto-match found a candidate) → `MATCHED` (admin confirmed) or back to `UNMATCHED` (admin rejected the suggestion). Admin can also manually propose a match from `UNMATCHED` directly to `MATCHED`.
- **Reconciliation UI**:
  - Shows statement rows side-by-side with matched/suggested/unmatched treasury cash-movement records.
  - Confirmed matches highlighted green; suggested (pending review) highlighted blue; unmatched statement rows in yellow; unmatched treasury records in red.
  - Admin can confirm a suggestion, reject a suggestion (resets to `UNMATCHED`), manually match, unmatch, or mark a statement row as "acknowledged" only when the row is genuinely non-ledger (e.g., duplicate import row, opening-balance carryover already reflected in `opening_balance`, or informational/non-transaction row).
  - Admin can create a new paid expense or income record directly from an unmatched statement row.
- **Reconciliation statuses**: `PENDING` (just uploaded), `IN_PROGRESS` (admin has started reviewing), `COMPLETED` (all rows addressed — confirmed matched, manually matched, or acknowledged).
- Statement rows that remain unmatched or suggested (unreviewed) after reconciliation are flagged for review.
- `ACKNOWLEDGED` rows do **not** affect `book_balance`; they are only for non-ledger rows. Any current-period debit/credit that represents a real cash movement must be converted into an `Expense` or `Income` record before reconciliation is completed.

### Financial Reports
- **Monthly Payment Report**: All expenses and income grouped by month, with totals per category and overall net (income - expenses).
- **Upcoming Payments Report**: All pending payables and payroll due within a configurable window (default: next 7 days, next 30 days), sorted by due date.
- **Bank Account Summary**: Opening balance, computed book balance, last reconciled statement balance (if any), and recent transaction history per bank account. Any difference between book and statement balances after reconciliation should only come from rows still awaiting review, not from `ACKNOWLEDGED` rows.
- **Employee Payment History**: All payments made to a specific employee within a date range.
- **Employee Compensation Summary**: Overview of all personnel showing type, compensation, YTD payments, and pending amounts.
- **Doctor Fee Summary**: Per-doctor breakdown of fees billed, commission retained, net payable, paid, and outstanding.
- **Indemnización Liability Report**: All active payroll employees with hire date, tenure, current salary, calculated indemnización, and total hospital liability.
- **Reconciliation Summary**: Per bank account, show last reconciliation date, number of unmatched items, and reconciliation coverage percentage.
- Reports support date range filtering where applicable.

---

## Acceptance Criteria / Scenarios

### Bank Accounts
- When an admin creates a bank account with valid data, the system returns 201 Created with the account details.
- When an admin lists bank accounts, account numbers are masked (only last 4 digits visible).
- The system-seeded "Petty Cash" account cannot be deleted.

### Expenses
- When an admin creates an expense with `isPaid = true`, `paymentDate`, and a bank account, the system records the expense with status `PAID` and creates an initial `ExpensePayment` row in the same transaction.
- When an admin creates an expense without payment, the system records it with status `PENDING` and a due date.
- When an admin attempts to create a manual expense without an invoice number, the system returns 400 Bad Request.
- When an admin creates an expense and includes an invoice scan file in the multipart request, the file is stored and the `invoice_document_path` is set on the returned record.
- When an admin uploads an invoice scan via the dedicated endpoint after creation, the file is stored and the expense record is updated.

### Accounts Payable
- When an admin views pending payables, items with remaining balance past their due date show a computed display state of `OVERDUE`.
- When an admin records a partial payment against a payable, the status changes to `PARTIALLY_PAID` and the remaining balance is updated.
- When the sum of all payments against a payable equals or exceeds the total amount, the status changes to `PAID`.

### Income
- When an admin records an income payment linked to a patient billing invoice, the system validates the invoice exists.
- When an admin lists income records filtered by date range, only records within the range are returned.

### Employee & Contractor Compensation
- When an admin creates a payroll employee with a base salary, the system can generate 14 payment periods for a given year.
- When an admin records a contractor payment via `POST /api/v1/treasury/employees/{id}/payments`, the system creates an `Expense` record (category `PAYROLL`) linked to the employee and returns it in the response.
- When an admin views an employee's payment history via `GET /api/v1/treasury/employees/{id}/payments`, all payments are listed regardless of employee type (payroll entries, contractor payments, or doctor fee settlements), with totals.
- When an admin views the employee list, each entry shows the employee type, compensation amount, and YTD payment summary.
- When an admin updates an employee's base salary, the service closes the current salary history record, opens a new one, and updates all future unpaid payroll entries — all in one transaction.
- When payroll is generated for a year, each entry has the correct due date (monthly: last day of month, Bono 14: July 15, Aguinaldo: December 15).

### Employee Tenure & Indemnización
- When an admin creates a payroll employee with a hire date of 2024-03-17 and current date is 2026-03-17, the system shows tenure as "2 years, 0 months, 0 days".
- When a payroll employee has a base salary of Q5,000 and 2 years of tenure, the indemnización is calculated as Q10,000 (Q5,000 × 2).
- When a payroll employee has a base salary of Q5,000 and 1 year 6 months (548 days) of tenure, the indemnización is calculated as Q5,000 × (548/365) = Q7,506.85.
- When an admin changes an employee's salary from Q5,000 to Q6,000, a salary history record is created with the previous salary and date range, and the indemnización recalculates using Q6,000.
- When an admin terminates an employee, the system calculates the final indemnización as of the termination date, locks the tenure calculation, and sets `active = false`.
- When an admin views the indemnización liability report, all active payroll employees (`active = true` and `termination_date IS NULL`) are listed with their calculated amounts and a grand total is shown.
- Contractors and doctors do not appear in tenure or indemnización views.

### Doctor Fee Billing
- When an admin registers a doctor with type `DOCTOR` and arrangement `HOSPITAL_BILLED`, doctor fees can be linked to patient charges.
- When a doctor fee record is created linking a patient charge, the system calculates the net payable after hospital commission.
- When a doctor fee is marked as `EXTERNAL`, no expense or income records are created but the record appears in the doctor's history.
- When an admin uploads a doctor invoice document via `POST .../invoice-document`, the file is stored and the doctor fee record is updated.
- When an admin advances a doctor fee to `INVOICED` status via `PUT .../status`, the request must include the doctor's invoice number and the doctor fee must already have an uploaded invoice document (or upload it in the same workflow). No financial record is created — only the status changes.
- When an admin attempts an invalid status transition (e.g., `PAID` → `PENDING`), the system returns 400 Bad Request.
- When an admin settles a doctor fee via `POST .../settle`, exactly one linked expense is created using the stored `doctor_invoice_number`, the invoice document is associated to that expense, and the doctor fee status changes to `PAID` in the same transaction.
- When an admin attempts to create a second hospital-billed doctor fee for the same patient charge, the system returns 409 Conflict.
- When an admin views a doctor's fee summary, total billed, commission retained, net payable, paid, and outstanding amounts are shown.

### Bank Statement Reconciliation
- When an admin uploads a valid XLSX file for a bank account, the system parses transaction rows and creates a statement record.
- When an admin uploads a valid CSV file, the system parses it using the saved column mapping for that bank account.
- When an admin attempts to upload a bank statement for the seeded petty cash account, the system returns 400 Bad Request.
- When column mapping doesn't exist for a bank account, the system prompts the admin to map columns on first upload.
- After upload, auto-match runs and sets candidate rows to `SUGGESTED` status; no rows are automatically confirmed as `MATCHED`.
- When an admin views the reconciliation screen, confirmed matches show green, suggested (pending review) show blue, unmatched statement rows show yellow, unmatched treasury records show red.
- When an admin confirms a suggested match, the row status changes to `MATCHED`.
- When an admin rejects a suggested match, the row status resets to `UNMATCHED`.
- When an admin manually matches an unmatched row to a specific treasury record, the row status changes to `MATCHED`.
- When an admin attempts to acknowledge a current-period debit or credit row that represents a real cash movement, the system returns 400 Bad Request and instructs the admin to create an expense or income record instead.
- When an admin clicks "create expense" from an unmatched debit statement row, the system creates a paid expense plus initial expense payment pre-filled from the statement row.
- When an admin clicks "create income" from an unmatched credit statement row, the system creates an income record pre-filled from the statement row.
- When all rows in a statement are in a terminal state (MATCHED or ACKNOWLEDGED), the statement status changes to `COMPLETED`. Rows in `SUGGESTED` status block completion — admin must confirm or reject all suggestions first.

### Reports
- When an admin requests a monthly report for a specific month, the system returns categorized expenses, income, and net balance.
- When an admin requests the upcoming payments report, all pending payables and payroll items within the window are returned sorted by due date.
- When an admin requests the indemnización liability report, all active payroll employees (`active = true` and `termination_date IS NULL`) are returned with tenure, current salary, calculated liability, and a grand total.

---

## Non-Functional Requirements

- **Security**: All inputs must be validated; monetary amounts must use `BigDecimal` (never floating point). Account numbers must be encrypted or masked in responses.
- **Audit**: All financial operations are automatically audited via BaseEntity fields.
- **Performance**: Report queries should respond within 500ms for up to 12 months of data.
- **Data Integrity**: Payments and expenses use database transactions to ensure consistency.
- **File Upload**: Bank statement files limited to 10MB max. Only XLSX and CSV extensions accepted (validated server-side).

---

## API Contract

### Bank Accounts

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/treasury/bank-accounts` | - | `ApiResponse<List<BankAccountResponse>>` | Yes | List all bank accounts |
| GET | `/api/v1/treasury/bank-accounts/{id}` | - | `ApiResponse<BankAccountResponse>` | Yes | Get bank account by ID |
| POST | `/api/v1/treasury/bank-accounts` | `CreateBankAccountRequest` | `ApiResponse<BankAccountResponse>` | Yes | Create bank account |
| PUT | `/api/v1/treasury/bank-accounts/{id}` | `UpdateBankAccountRequest` | `ApiResponse<BankAccountResponse>` | Yes | Update bank account |
| DELETE | `/api/v1/treasury/bank-accounts/{id}` | - | `ApiResponse<Unit>` | Yes | Soft delete bank account |

### Expenses

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/treasury/expenses` | Query params | `ApiResponse<PageResponse<ExpenseResponse>>` | Yes | List expenses (filterable) |
| GET | `/api/v1/treasury/expenses/{id}` | - | `ApiResponse<ExpenseResponse>` | Yes | Get expense by ID |
| POST | `/api/v1/treasury/expenses` | `multipart (CreateExpenseRequest + optional file)` | `ApiResponse<ExpenseResponse>` | Yes | Create expense; invoice scan can be attached at creation time |
| PUT | `/api/v1/treasury/expenses/{id}` | `UpdateExpenseRequest` | `ApiResponse<ExpenseResponse>` | Yes | Update expense |
| DELETE | `/api/v1/treasury/expenses/{id}` | - | `ApiResponse<Unit>` | Yes | Soft delete expense |
| POST | `/api/v1/treasury/expenses/{id}/invoice-document` | `multipart (file)` | `ApiResponse<ExpenseResponse>` | Yes | Upload/replace invoice scan after creation |
| POST | `/api/v1/treasury/expenses/{id}/payments` | `RecordPaymentRequest` | `ApiResponse<ExpensePaymentResponse>` | Yes | Record payment against payable |
| GET | `/api/v1/treasury/expenses/{id}/payments` | - | `ApiResponse<List<ExpensePaymentResponse>>` | Yes | List payments for expense |

### Income

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/treasury/income` | Query params | `ApiResponse<PageResponse<IncomeResponse>>` | Yes | List income records |
| GET | `/api/v1/treasury/income/{id}` | - | `ApiResponse<IncomeResponse>` | Yes | Get income by ID |
| POST | `/api/v1/treasury/income` | `CreateIncomeRequest` | `ApiResponse<IncomeResponse>` | Yes | Record income |
| PUT | `/api/v1/treasury/income/{id}` | `UpdateIncomeRequest` | `ApiResponse<IncomeResponse>` | Yes | Update income |
| DELETE | `/api/v1/treasury/income/{id}` | - | `ApiResponse<Unit>` | Yes | Soft delete income |

### Employees (Financial)

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/treasury/employees` | Query params | `ApiResponse<PageResponse<TreasuryEmployeeResponse>>` | Yes | List employees |
| GET | `/api/v1/treasury/employees/{id}` | - | `ApiResponse<TreasuryEmployeeResponse>` | Yes | Get employee by ID |
| POST | `/api/v1/treasury/employees` | `CreateTreasuryEmployeeRequest` | `ApiResponse<TreasuryEmployeeResponse>` | Yes | Create employee |
| PUT | `/api/v1/treasury/employees/{id}` | `UpdateTreasuryEmployeeRequest` | `ApiResponse<TreasuryEmployeeResponse>` | Yes | Update employee |
| DELETE | `/api/v1/treasury/employees/{id}` | - | `ApiResponse<Unit>` | Yes | Soft delete employee |
| POST | `/api/v1/treasury/employees/{id}/payroll/{year}` | - | `ApiResponse<List<PayrollEntryResponse>>` | Yes | Generate payroll schedule for year |
| GET | `/api/v1/treasury/employees/{id}/payroll` | `?year=` | `ApiResponse<List<PayrollEntryResponse>>` | Yes | List payroll entries for employee/year |
| POST | `/api/v1/treasury/payroll/{entryId}/pay` | `RecordPayrollPaymentRequest` | `ApiResponse<PayrollEntryResponse>` | Yes | Pay payroll entry and create linked expense |
| GET | `/api/v1/treasury/employees/{id}/payments` | Query params | `ApiResponse<PageResponse<EmployeePaymentResponse>>` | Yes | Get employee payment history |
| POST | `/api/v1/treasury/employees/{id}/payments` | `RecordEmployeePaymentRequest` | `ApiResponse<EmployeePaymentResponse>` | Yes | Record contractor or non-payroll employee payment |

### Doctor Fees

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/treasury/employees/{id}/doctor-fees` | Query params | `ApiResponse<PageResponse<DoctorFeeResponse>>` | Yes | List doctor fee records |
| POST | `/api/v1/treasury/employees/{id}/doctor-fees` | `CreateDoctorFeeRequest` | `ApiResponse<DoctorFeeResponse>` | Yes | Record a doctor fee |
| PUT | `/api/v1/treasury/doctor-fees/{id}` | `UpdateDoctorFeeRequest` | `ApiResponse<DoctorFeeResponse>` | Yes | Update doctor fee record |
| DELETE | `/api/v1/treasury/doctor-fees/{id}` | - | `ApiResponse<Unit>` | Yes | Soft delete doctor fee |
| POST | `/api/v1/treasury/doctor-fees/{id}/invoice-document` | `multipart (file)` | `ApiResponse<DoctorFeeResponse>` | Yes | Upload/replace doctor invoice scan |
| PUT | `/api/v1/treasury/doctor-fees/{id}/status` | `UpdateDoctorFeeStatusRequest` | `ApiResponse<DoctorFeeResponse>` | Yes | Manually advance status (e.g., PENDING → INVOICED when doctor submits invoice number and invoice copy is on file) |
| POST | `/api/v1/treasury/doctor-fees/{id}/settle` | `SettleDoctorFeeRequest` | `ApiResponse<DoctorFeeResponse>` | Yes | Create settlement expense and mark fee PAID in one transaction |
| GET | `/api/v1/treasury/employees/{id}/doctor-fee-summary` | `?from=&to=` | `ApiResponse<DoctorFeeSummaryResponse>` | Yes | Doctor fee summary report |

### Bank Statement Reconciliation

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/treasury/bank-accounts/{id}/statements` | - | `ApiResponse<List<BankStatementResponse>>` | Yes | List uploaded statements |
| POST | `/api/v1/treasury/bank-accounts/{id}/statements` | `multipart (file)` | `ApiResponse<BankStatementResponse>` | Yes | Upload bank statement (XLSX/CSV, non-petty-cash only) |
| GET | `/api/v1/treasury/statements/{id}` | - | `ApiResponse<BankStatementDetailResponse>` | Yes | Get statement with parsed rows |
| DELETE | `/api/v1/treasury/statements/{id}` | - | `ApiResponse<Unit>` | Yes | Soft delete statement |
| GET | `/api/v1/treasury/bank-accounts/{id}/column-mapping` | - | `ApiResponse<ColumnMappingResponse>` | Yes | Get saved column mapping |
| PUT | `/api/v1/treasury/bank-accounts/{id}/column-mapping` | `UpdateColumnMappingRequest` | `ApiResponse<ColumnMappingResponse>` | Yes | Save/update column mapping |
| POST | `/api/v1/treasury/statements/{id}/auto-match` | - | `ApiResponse<ReconciliationResultResponse>` | Yes | Run auto-matching; sets candidate rows to SUGGESTED status |
| PUT | `/api/v1/treasury/statement-rows/{id}/confirm-match` | - | `ApiResponse<StatementRowResponse>` | Yes | Confirm a SUGGESTED match → MATCHED |
| PUT | `/api/v1/treasury/statement-rows/{id}/reject-match` | - | `ApiResponse<StatementRowResponse>` | Yes | Reject a SUGGESTED match → back to UNMATCHED |
| PUT | `/api/v1/treasury/statement-rows/{id}/match` | `ManualMatchRequest` | `ApiResponse<StatementRowResponse>` | Yes | Manually match an UNMATCHED row to a specific record |
| PUT | `/api/v1/treasury/statement-rows/{id}/unmatch` | - | `ApiResponse<StatementRowResponse>` | Yes | Unmatch a MATCHED row → back to UNMATCHED |
| PUT | `/api/v1/treasury/statement-rows/{id}/acknowledge` | `AcknowledgeRequest` | `ApiResponse<StatementRowResponse>` | Yes | Acknowledge unmatched row only when it is non-ledger (e.g., duplicate import row) |
| POST | `/api/v1/treasury/statement-rows/{id}/create-expense` | `CreateExpenseFromStatementRowRequest` | `ApiResponse<ExpenseResponse>` | Yes | Create paid expense from unmatched debit row |
| POST | `/api/v1/treasury/statement-rows/{id}/create-income` | `CreateIncomeFromStatementRowRequest` | `ApiResponse<IncomeResponse>` | Yes | Create income from unmatched credit row |

### Reports

| Method | Endpoint | Request DTO | Response DTO | Auth | Description |
|--------|----------|-------------|--------------|------|-------------|
| GET | `/api/v1/treasury/reports/monthly` | `?year=&month=` | `ApiResponse<MonthlyReportResponse>` | Yes | Monthly financial summary |
| GET | `/api/v1/treasury/reports/upcoming-payments` | `?days=30` | `ApiResponse<UpcomingPaymentsResponse>` | Yes | Upcoming payment obligations |
| GET | `/api/v1/treasury/reports/bank-summary` | - | `ApiResponse<List<BankAccountSummaryResponse>>` | Yes | Bank account balances summary |
| GET | `/api/v1/treasury/reports/compensation-summary` | `?year=` | `ApiResponse<CompensationSummaryResponse>` | Yes | Employee compensation overview |
| GET | `/api/v1/treasury/reports/indemnizacion-liability` | `?asOf=` | `ApiResponse<IndemnizacionLiabilityReportResponse>` | Yes | Payroll indemnización liability report |
| GET | `/api/v1/treasury/reports/reconciliation-summary` | - | `ApiResponse<List<ReconciliationSummaryResponse>>` | Yes | Reconciliation status per bank account |

---

## Database Changes

### New Entities

| Entity | Table | Extends | Description |
|--------|-------|---------|-------------|
| `BankAccount` | `bank_accounts` | `BaseEntity` | Hospital bank accounts and petty cash |
| `SalaryHistory` | `salary_history` | `BaseEntity` | Payroll salary history for indemnización audit |
| `Expense` | `expenses` | `BaseEntity` | Expense records with invoice reference |
| `ExpensePayment` | `expense_payments` | `BaseEntity` | Payments against pending expenses |
| `Income` | `income_records` | `BaseEntity` | Income/revenue entries |
| `TreasuryEmployee` | `treasury_employees` | `BaseEntity` | Employee/contractor/doctor financial records |
| `PayrollEntry` | `payroll_entries` | `BaseEntity` | Individual payroll payment periods |
| `DoctorFee` | `doctor_fees` | `BaseEntity` | Doctor fee records linked to patient charges |
| `BankStatement` | `bank_statements` | `BaseEntity` | Uploaded bank statement metadata |
| `BankStatementRow` | `bank_statement_rows` | `BaseEntity` | Parsed rows from bank statements |
| `BankAccountColumnMapping` | `bank_account_column_mappings` | `BaseEntity` | Column mapping config per bank account |

### New Migrations

| Migration | Description |
|-----------|-------------|
| `V074__create_bank_accounts_table.sql` | Bank accounts table with petty cash seed |
| `V075__create_treasury_employees_table.sql` | Employee financial registry |
| `V076__create_salary_history_table.sql` | Payroll salary history for indemnización audit |
| `V077__create_expenses_table.sql` | Expenses table with category enum and payable tracking |
| `V078__create_expense_payments_table.sql` | Partial payments against expenses |
| `V079__create_income_records_table.sql` | Income records with optional billing invoice link |
| `V080__create_payroll_entries_table.sql` | Payroll schedule entries (14/year) |
| `V081__create_doctor_fees_table.sql` | Doctor fee records with patient charge link |
| `V082__create_bank_statements_table.sql` | Bank statement uploads and parsed rows |
| `V083__create_bank_account_column_mappings_table.sql` | Column mapping configuration per bank account |
| `V084__seed_treasury_permissions.sql` | Treasury permissions and role assignments |

### Schema

```sql
-- V074: Bank Accounts
CREATE TABLE bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    bank_name VARCHAR(100),
    account_number VARCHAR(50),
    account_type VARCHAR(30) NOT NULL DEFAULT 'CHECKING',  -- CHECKING, SAVINGS, PETTY_CASH
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

-- V075: Treasury Employees (employees, contractors, and doctors)
CREATE TABLE treasury_employees (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    employee_type VARCHAR(20) NOT NULL,  -- CONTRACTOR, PAYROLL, DOCTOR
    tax_id VARCHAR(50),  -- NIT
    position VARCHAR(100),
    base_salary NUMERIC(12,2),  -- monthly salary for PAYROLL employees
    contracted_rate NUMERIC(12,2),  -- agreed rate for CONTRACTOR (reference only)
    doctor_fee_arrangement VARCHAR(20),  -- HOSPITAL_BILLED, EXTERNAL (for DOCTOR type only)
    hospital_commission_pct NUMERIC(5,2) DEFAULT 0,  -- % hospital retains from doctor fees
    hire_date DATE,  -- required for PAYROLL employees
    termination_date DATE,
    termination_reason VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,  -- employment status; set FALSE on termination
    user_id BIGINT REFERENCES users(id),  -- optional link to system user
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

-- V076: Salary History
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

-- V077: Expenses
CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    supplier_name VARCHAR(255) NOT NULL,
    category VARCHAR(30) NOT NULL,  -- SUPPLIES, UTILITIES, MAINTENANCE, EQUIPMENT, SERVICES, PAYROLL, OTHER
    description TEXT,
    amount NUMERIC(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    invoice_document_path VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PARTIALLY_PAID, PAID, CANCELLED
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

-- V079: Income Records
CREATE TABLE income_records (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(30) NOT NULL,  -- PATIENT_PAYMENT, INSURANCE, GOVERNMENT, DONATION, OTHER
    description TEXT,
    amount NUMERIC(12,2) NOT NULL,
    income_date DATE NOT NULL,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    billing_invoice_id BIGINT REFERENCES invoices(id),
    reference VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_income_records_deleted_at ON income_records(deleted_at);
CREATE INDEX idx_income_records_category ON income_records(category);
CREATE INDEX idx_income_records_income_date ON income_records(income_date);
CREATE INDEX idx_income_records_bank_account_id ON income_records(bank_account_id);
CREATE INDEX idx_income_records_billing_invoice_id ON income_records(billing_invoice_id);

-- V080: Payroll Entries
CREATE TABLE payroll_entries (
    id BIGSERIAL PRIMARY KEY,
    treasury_employee_id BIGINT NOT NULL REFERENCES treasury_employees(id),
    year INT NOT NULL,
    period_type VARCHAR(20) NOT NULL,  -- MONTHLY, BONO_14, AGUINALDO
    period_month INT,  -- 1-12 for MONTHLY, NULL for bonuses
    due_date DATE NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PAID
    payment_date DATE,
    bank_account_id BIGINT REFERENCES bank_accounts(id),
    expense_id BIGINT REFERENCES expenses(id),  -- link to expense record when paid
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_payroll_entries_deleted_at ON payroll_entries(deleted_at);
CREATE INDEX idx_payroll_entries_treasury_employee_id ON payroll_entries(treasury_employee_id);
CREATE INDEX idx_payroll_entries_year ON payroll_entries(year);
CREATE INDEX idx_payroll_entries_status ON payroll_entries(status);
CREATE INDEX idx_payroll_entries_due_date ON payroll_entries(due_date);
CREATE UNIQUE INDEX idx_payroll_entries_unique_period
    ON payroll_entries(treasury_employee_id, year, period_type, period_month)
    WHERE deleted_at IS NULL;

-- V081: Doctor Fees
CREATE TABLE doctor_fees (
    id BIGSERIAL PRIMARY KEY,
    treasury_employee_id BIGINT NOT NULL REFERENCES treasury_employees(id),
    patient_charge_id BIGINT REFERENCES patient_charges(id),  -- link to billing charge (NULL for EXTERNAL)
    admission_id BIGINT REFERENCES admissions(id),  -- for context
    fee_type VARCHAR(20) NOT NULL,  -- HOSPITAL_BILLED, EXTERNAL
    gross_amount NUMERIC(12,2) NOT NULL,
    commission_pct NUMERIC(5,2) NOT NULL DEFAULT 0,
    commission_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(12,2) NOT NULL,  -- gross - commission
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, INVOICED, PAID
    doctor_invoice_number VARCHAR(100),  -- required before status can move to INVOICED
    doctor_invoice_document_path VARCHAR(500),  -- uploaded copy of doctor's invoice
    expense_id BIGINT REFERENCES expenses(id),  -- link to expense when paid
    fee_date DATE NOT NULL,
    description TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_doctor_fees_deleted_at ON doctor_fees(deleted_at);
CREATE INDEX idx_doctor_fees_treasury_employee_id ON doctor_fees(treasury_employee_id);
CREATE INDEX idx_doctor_fees_patient_charge_id ON doctor_fees(patient_charge_id);
CREATE INDEX idx_doctor_fees_admission_id ON doctor_fees(admission_id);
CREATE INDEX idx_doctor_fees_status ON doctor_fees(status);
CREATE INDEX idx_doctor_fees_fee_date ON doctor_fees(fee_date);
CREATE UNIQUE INDEX idx_doctor_fees_unique_patient_charge
    ON doctor_fees(patient_charge_id)
    WHERE deleted_at IS NULL AND fee_type = 'HOSPITAL_BILLED' AND patient_charge_id IS NOT NULL;

-- V082: Bank Statements
CREATE TABLE bank_statements (
    id BIGSERIAL PRIMARY KEY,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(10) NOT NULL,  -- XLSX, CSV
    period_start DATE,
    period_end DATE,
    row_count INT NOT NULL DEFAULT 0,
    matched_count INT NOT NULL DEFAULT 0,
    unmatched_count INT NOT NULL DEFAULT 0,
    acknowledged_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, COMPLETED
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_statements_deleted_at ON bank_statements(deleted_at);
CREATE INDEX idx_bank_statements_bank_account_id ON bank_statements(bank_account_id);
CREATE INDEX idx_bank_statements_status ON bank_statements(status);

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
    match_status VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED',  -- UNMATCHED, SUGGESTED, MATCHED, ACKNOWLEDGED
    matched_expense_payment_id BIGINT REFERENCES expense_payments(id),
    matched_income_id BIGINT REFERENCES income_records(id),
    acknowledge_reason VARCHAR(255),  -- e.g., "duplicate import row", "opening balance carryover"
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_statement_rows_deleted_at ON bank_statement_rows(deleted_at);
CREATE INDEX idx_bank_statement_rows_bank_statement_id ON bank_statement_rows(bank_statement_id);
CREATE INDEX idx_bank_statement_rows_match_status ON bank_statement_rows(match_status);
CREATE INDEX idx_bank_statement_rows_transaction_date ON bank_statement_rows(transaction_date);

-- V083: Bank Account Column Mappings
CREATE TABLE bank_account_column_mappings (
    id BIGSERIAL PRIMARY KEY,
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    date_column VARCHAR(100) NOT NULL,
    description_column VARCHAR(100),
    reference_column VARCHAR(100),
    debit_column VARCHAR(100),
    credit_column VARCHAR(100),
    balance_column VARCHAR(100),
    date_format VARCHAR(50) NOT NULL DEFAULT 'yyyy-MM-dd',
    has_header_row BOOLEAN NOT NULL DEFAULT TRUE,
    skip_rows INT NOT NULL DEFAULT 0,  -- rows to skip before header/data
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_account_column_mappings_deleted_at ON bank_account_column_mappings(deleted_at);
CREATE UNIQUE INDEX idx_bank_account_column_mappings_bank_account
    ON bank_account_column_mappings(bank_account_id)
    WHERE deleted_at IS NULL;
```

### Index Requirements

- [x] `deleted_at` - Required on all tables for soft delete queries
- [x] Foreign keys - All FK columns indexed
- [x] `status` - On expenses, payroll_entries, doctor_fees, and bank_statements for filtering
- [x] `expense_date`, `income_date`, `due_date` - Date-based report queries
- [x] `category` - On expenses and income for report grouping
- [x] Unique constraint on payroll periods per employee/year
- [x] Unique constraint on hospital-billed doctor fee per patient charge
- [x] Unique constraint on column mapping per bank account
- [x] `match_status` and `transaction_date` on statement rows for reconciliation queries
- [x] `fee_date` and `status` on doctor fees for reporting
- [x] `effective_from` on salary history for audit lookups

---

## Frontend Changes

### Components

| Component | Location | Description |
|-----------|----------|-------------|
| `BankAccountList.vue` | `src/views/treasury/` | List/manage bank accounts |
| `BankAccountForm.vue` | `src/components/treasury/` | Create/edit bank account dialog |
| `ExpenseList.vue` | `src/views/treasury/` | List expenses with filtering by status/category/date |
| `ExpenseForm.vue` | `src/components/treasury/` | Create/edit expense with invoice upload |
| `ExpensePaymentDialog.vue` | `src/components/treasury/` | Record payment against payable |
| `IncomeList.vue` | `src/views/treasury/` | List income records |
| `IncomeForm.vue` | `src/components/treasury/` | Create/edit income record |
| `EmployeeList.vue` | `src/views/treasury/` | List treasury employees |
| `EmployeeForm.vue` | `src/components/treasury/` | Create/edit employee |
| `EmployeePayrollView.vue` | `src/views/treasury/` | View/manage payroll entries for employee |
| `DoctorFeeList.vue` | `src/views/treasury/` | Doctor fee records with summary |
| `DoctorFeeForm.vue` | `src/components/treasury/` | Create/edit doctor fee record |
| `BankStatementList.vue` | `src/views/treasury/` | List uploaded statements per bank account |
| `BankStatementUpload.vue` | `src/components/treasury/` | Upload dialog with column mapping |
| `ReconciliationView.vue` | `src/views/treasury/` | Side-by-side reconciliation UI |
| `ColumnMappingForm.vue` | `src/components/treasury/` | Configure column mapping for bank account |
| `BankSummaryReport.vue` | `src/views/treasury/` | Bank balances and recent transaction history |
| `MonthlyReport.vue` | `src/views/treasury/` | Monthly financial summary report |
| `UpcomingPayments.vue` | `src/views/treasury/` | Upcoming payments planning view |
| `CompensationSummary.vue` | `src/views/treasury/` | Employee compensation overview report |
| `IndemnizacionLiabilityReport.vue` | `src/views/treasury/` | Payroll indemnización liability report |
| `ReconciliationSummary.vue` | `src/views/treasury/` | Reconciliation status by bank account |
| `TreasuryDashboard.vue` | `src/views/treasury/` | Overview: bank balances, pending payables count, upcoming due |

### Pinia Stores

| Store | Location | Description |
|-------|----------|-------------|
| `useBankAccountStore` | `src/stores/bankAccount.ts` | Bank account CRUD |
| `useExpenseStore` | `src/stores/expense.ts` | Expense CRUD + payments |
| `useIncomeStore` | `src/stores/income.ts` | Income CRUD |
| `useTreasuryEmployeeStore` | `src/stores/treasuryEmployee.ts` | Employee + payroll management |
| `useDoctorFeeStore` | `src/stores/doctorFee.ts` | Doctor fee CRUD + summary |
| `useBankStatementStore` | `src/stores/bankStatement.ts` | Statement upload, reconciliation |
| `useTreasuryReportStore` | `src/stores/treasuryReport.ts` | Report data fetching |

### Routes

| Path | Component | Auth Required | Roles |
|------|-----------|---------------|-------|
| `/treasury` | `TreasuryDashboard` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/bank-accounts` | `BankAccountList` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/expenses` | `ExpenseList` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/income` | `IncomeList` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/employees` | `EmployeeList` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/employees/:id/payroll` | `EmployeePayrollView` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/employees/:id/doctor-fees` | `DoctorFeeList` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/bank-accounts/:id/statements` | `BankStatementList` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/statements/:id/reconcile` | `ReconciliationView` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/reports/bank-summary` | `BankSummaryReport` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/reports/monthly` | `MonthlyReport` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/reports/upcoming` | `UpcomingPayments` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/reports/compensation` | `CompensationSummary` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/reports/indemnizacion` | `IndemnizacionLiabilityReport` | Yes | ADMIN, ADMINISTRATIVE_STAFF |
| `/treasury/reports/reconciliation` | `ReconciliationSummary` | Yes | ADMIN, ADMINISTRATIVE_STAFF |

### Validation (Zod Schemas)

```typescript
// src/schemas/treasury.ts
export const createBankAccountSchema = z.object({
  name: z.string().min(1).max(100),
  bankName: z.string().max(100).optional(),
  accountNumber: z.string().max(50).optional(),
  accountType: z.enum(['CHECKING', 'SAVINGS']),
  currency: z.string().length(3).default('GTQ'),
  openingBalance: z.number().min(0).default(0),
  notes: z.string().optional(),
})

export const createExpenseSchema = z.object({
  supplierName: z.string().min(1).max(255),
  category: z.enum(['SUPPLIES', 'UTILITIES', 'MAINTENANCE', 'EQUIPMENT', 'SERVICES', 'PAYROLL', 'OTHER']),
  description: z.string().optional(),
  amount: z.number().positive(),
  expenseDate: z.string(), // ISO date
  invoiceNumber: z.string().min(1).max(100),
  dueDate: z.string().optional(),
  paymentDate: z.string().optional(),
  bankAccountId: z.number().optional(),
  isPaid: z.boolean().default(false),
  notes: z.string().optional(),
}).superRefine((data, ctx) => {
  if (!data.isPaid && !data.dueDate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['dueDate'],
      message: 'dueDate is required when the expense is not paid',
    })
  }

  if (data.isPaid && !data.paymentDate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['paymentDate'],
      message: 'paymentDate is required when isPaid is true',
    })
  }

  if (data.isPaid && !data.bankAccountId) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['bankAccountId'],
      message: 'bankAccountId is required when isPaid is true',
    })
  }
})

export const createIncomeSchema = z.object({
  category: z.enum(['PATIENT_PAYMENT', 'INSURANCE', 'GOVERNMENT', 'DONATION', 'OTHER']),
  description: z.string().optional(),
  amount: z.number().positive(),
  incomeDate: z.string(),
  bankAccountId: z.number().positive(),
  billingInvoiceId: z.number().optional(),
  reference: z.string().max(255).optional(),
  notes: z.string().optional(),
})

export const createTreasuryEmployeeSchema = z.object({
  fullName: z.string().min(1).max(255),
  employeeType: z.enum(['CONTRACTOR', 'PAYROLL', 'DOCTOR']),
  taxId: z.string().max(50).optional(),
  position: z.string().max(100).optional(),
  baseSalary: z.number().positive().optional(),          // for PAYROLL
  contractedRate: z.number().positive().optional(),       // for CONTRACTOR
  doctorFeeArrangement: z.enum(['HOSPITAL_BILLED', 'EXTERNAL']).optional(),  // for DOCTOR
  hospitalCommissionPct: z.number().min(0).max(100).optional(),              // for DOCTOR
  hireDate: z.string().optional(),
  terminationDate: z.string().optional(),
  terminationReason: z.string().max(255).optional(),
  active: z.boolean().default(true),
  userId: z.number().optional(),
  notes: z.string().optional(),
}).superRefine((data, ctx) => {
  if (data.employeeType === 'PAYROLL' && !data.baseSalary) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['baseSalary'],
      message: 'baseSalary is required for PAYROLL employees',
    })
  }

  if (data.employeeType === 'PAYROLL' && !data.hireDate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['hireDate'],
      message: 'hireDate is required for PAYROLL employees',
    })
  }

  if (data.employeeType === 'CONTRACTOR' && !data.contractedRate) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['contractedRate'],
      message: 'contractedRate is required for CONTRACTOR employees',
    })
  }

  if (data.employeeType === 'DOCTOR' && !data.doctorFeeArrangement) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['doctorFeeArrangement'],
      message: 'doctorFeeArrangement is required for DOCTOR employees',
    })
  }

  if (data.terminationDate && !data.terminationReason) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['terminationReason'],
      message: 'terminationReason is required when terminationDate is provided',
    })
  }

  if (data.terminationDate && data.active !== false) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['active'],
      message: 'active must be false when terminationDate is provided',
    })
  }
})

export const createDoctorFeeSchema = z.object({
  patientChargeId: z.number().optional(),   // link to billing charge (NULL for EXTERNAL)
  admissionId: z.number().optional(),
  feeType: z.enum(['HOSPITAL_BILLED', 'EXTERNAL']),
  grossAmount: z.number().positive(),
  commissionPct: z.number().min(0).max(100).default(0),
  doctorInvoiceNumber: z.string().max(100).optional(),  // can be captured at creation or later when invoiced
  feeDate: z.string(),
  description: z.string().optional(),
  notes: z.string().optional(),
}).superRefine((data, ctx) => {
  if (data.feeType === 'HOSPITAL_BILLED' && !data.patientChargeId) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['patientChargeId'],
      message: 'patientChargeId is required for HOSPITAL_BILLED fees',
    })
  }

  if (data.feeType === 'EXTERNAL' && data.patientChargeId) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['patientChargeId'],
      message: 'patientChargeId must be omitted for EXTERNAL fees',
    })
  }
})

export const recordPaymentSchema = z.object({
  amount: z.number().positive(),
  paymentDate: z.string(),
  bankAccountId: z.number().positive(),
  reference: z.string().max(255).optional(),
  notes: z.string().optional(),
})

export const recordPayrollPaymentSchema = z.object({
  paymentDate: z.string(),
  bankAccountId: z.number().positive(),
  notes: z.string().optional(),
})

export const columnMappingSchema = z.object({
  dateColumn: z.string().min(1),
  descriptionColumn: z.string().optional(),
  referenceColumn: z.string().optional(),
  debitColumn: z.string().optional(),
  creditColumn: z.string().optional(),
  balanceColumn: z.string().optional(),
  dateFormat: z.string().default('yyyy-MM-dd'),
  hasHeaderRow: z.boolean().default(true),
  skipRows: z.number().min(0).default(0),
})

export const manualMatchSchema = z.object({
  expensePaymentId: z.number().optional(),
  incomeId: z.number().optional(),
}).superRefine((data, ctx) => {
  const selected = [data.expensePaymentId, data.incomeId].filter(Boolean).length

  if (selected !== 1) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: 'Exactly one target record must be selected',
    })
  }
})

export const acknowledgeRowSchema = z.object({
  reason: z.string().min(1).max(255),
})

export const updateDoctorFeeStatusSchema = z.object({
  status: z.enum(['PENDING', 'INVOICED', 'PAID']),
  doctorInvoiceNumber: z.string().max(100).optional(),
}).superRefine((data, ctx) => {
  if (data.status === 'INVOICED' && !data.doctorInvoiceNumber) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['doctorInvoiceNumber'],
      message: 'doctorInvoiceNumber is required when moving a doctor fee to INVOICED',
    })
  }
})

export const createExpenseFromStatementRowSchema = z.object({
  supplierName: z.string().min(1).max(255),
  category: z.enum(['SUPPLIES', 'UTILITIES', 'MAINTENANCE', 'EQUIPMENT', 'SERVICES', 'PAYROLL', 'OTHER']),
  invoiceNumber: z.string().min(1).max(100),
  description: z.string().optional(),
  notes: z.string().optional(),
})

export const createIncomeFromStatementRowSchema = z.object({
  category: z.enum(['PATIENT_PAYMENT', 'INSURANCE', 'GOVERNMENT', 'DONATION', 'OTHER']),
  description: z.string().optional(),
  billingInvoiceId: z.number().optional(),
  notes: z.string().optional(),
})
```

---

## Implementation Notes

- **Currency**: Default currency is GTQ (Guatemalan Quetzal). The `currency` field on bank accounts allows for future multi-currency support but all amounts are assumed GTQ for now.
- **Book balance**: `book_balance` is derived as `opening_balance + sum(income_records) - sum(expense_payments)` per bank account. `ACKNOWLEDGED` rows never change this value because they are restricted to non-ledger items only. The latest statement `balance` is shown separately for reconciliation purposes.
- **Payroll schedule**: Bono 14 is due in July, Aguinaldo in December. When generating the yearly payroll schedule, set due dates accordingly (Bono 14: July 15, Aguinaldo: December 15, Monthly: last day of each month).
- **Expense ↔ Payroll link**: When paying a payroll entry, the system should automatically create a corresponding expense record with category `PAYROLL` and link it to the treasury employee. The payroll entry references this expense via `expense_id`.
- **Payroll expense reference**: When a payroll payment creates an expense, the service generates `invoice_number = PAYROLL-{employeeId}-{year}-{period}` so the expense record satisfies the reference requirement even though no vendor invoice exists.
- **Expense cash movement**: `ExpensePayment` is the source of truth for money leaving a bank account or petty cash. If an expense is created with `isPaid = true`, the service creates an initial `ExpensePayment` immediately.
- **Income ↔ Billing integration**: The `billing_invoice_id` on income records is an optional FK to the existing `invoices` table, allowing traceability from patient billing to treasury income.
- **File storage**: Invoice document attachments (scans/photos) follow the existing pattern at `{base-path}/treasury/expenses/{expenseId}/{uuid}_{filename}`. When a file is included in the `POST /api/v1/treasury/expenses` multipart request, it is stored immediately after the expense record is persisted.
- **OVERDUE detection**: Rather than a scheduler, determine overdue display state at query time: any expense with remaining balance and `due_date < CURRENT_DATE` is considered overdue. The API response DTO exposes this as a computed field.
- **Petty cash**: The seeded petty cash account has `is_petty_cash = TRUE` and cannot be deleted. It does not require bank_name or account_number. Petty cash can receive income (cash receipts) but cannot have bank statements uploaded.
- **Petty cash statements**: Bank statement upload is not available for petty cash accounts.
- **Contractor payment flow**: When `POST /api/v1/treasury/employees/{id}/payments` is called for a `CONTRACTOR` employee, the service creates an `Expense` record (category `PAYROLL`, `supplier_name = employee full name`, `invoice_number` from the request) and links it to the employee via `treasury_employee_id`. The response includes the created expense reference. This is the only path for recording contractor payments — do not create contractor expenses directly via `POST /api/v1/treasury/expenses`.
- **Doctor fee status transitions**: `PENDING` → `INVOICED` (admin manually sets when doctor submits invoice, via `PUT .../status`) → `PAID` (via `POST .../settle`, which creates the linked expense atomically). Moving to `INVOICED` requires `doctor_invoice_number` plus an uploaded invoice document on the `DoctorFee`. Only valid forward transitions are allowed; the service must reject invalid status changes.
- **Doctor fee invoice document**: Store the uploaded invoice copy at `{base-path}/treasury/doctor-fees/{doctorFeeId}/{uuid}_{filename}`. When the fee is settled, copy or reference that same stored file on the linked expense so audit users can find the supporting document from either record.
- **Doctor fee uniqueness**: Enforce at most one non-deleted `HOSPITAL_BILLED` `DoctorFee` per `patient_charge_id`.
- **Doctor fee calculation**: When creating a doctor fee with `HOSPITAL_BILLED` type, `net_amount = gross_amount - (gross_amount * commission_pct / 100)`. The `commission_pct` defaults to the doctor's `hospital_commission_pct` but can be overridden per fee.
- **Doctor types**: The `DOCTOR` employee type uses `doctor_fee_arrangement` to distinguish billing flow. Doctors with `EXTERNAL` arrangement can still have fee records logged for tracking, but no financial transactions are created.
- **Doctor fee settlement scope**: In v1, each `DoctorFee` can be settled by at most one linked expense. Partial payouts to a doctor are out of scope for this iteration.
- **Salary history mechanism**: The service layer (not a DB trigger) manages salary history. On every `PUT /api/v1/treasury/employees/{id}` call where `base_salary` changes: (1) close the current open `salary_history` record (`effective_to = today`), (2) insert a new `salary_history` record (`effective_from = today`, `effective_to = null`), (3) update `amount` on all future `PENDING` payroll entries for this employee. These three steps must execute in a single transaction.
- **Termination persistence**: Setting `termination_date` on a payroll employee must also set `active = FALSE` in the same transaction so active-list queries and indemnización reports stay correct.
- **Bank statement parsing**: Use Apache POI for XLSX parsing and OpenCSV for CSV parsing. Both are well-supported Spring Boot compatible libraries.
- **Column mapping**: Saved per bank account since each bank has different statement formats. For XLSX, column values are the header names (strings). For CSV without headers, column values are zero-based numeric indices stored as strings (e.g., `"0"`, `"3"`). The `date_format` field handles locale-specific date parsing.
- **Auto-matching algorithm**: For each statement row, find candidate treasury records by: (1) exact amount match, (2) date within ±3 days, (3) optional reference/description fuzzy match. Debit rows match `ExpensePayment`; credit rows match `Income`. When a single strong candidate is found, set `match_status = SUGGESTED` and store the candidate FK. Admin must explicitly confirm (`PUT .../confirm-match`) or reject (`PUT .../reject-match`). Rows with multiple candidates or no candidates remain `UNMATCHED`.
- **Reconciliation file storage**: Uploaded statement files are stored at `{base-path}/treasury/statements/{bankAccountId}/{uuid}_{filename}`.

---

## QA Checklist

### Backend
- [ ] All functional requirements implemented
- [ ] All entities extend `BaseEntity`
- [ ] All entities have `@SQLRestriction("deleted_at IS NULL")`
- [ ] DTOs used in controllers (no entity exposure)
- [ ] All monetary amounts use `BigDecimal`
- [ ] Input validation in place
- [ ] Account numbers masked in responses
- [ ] Petty cash account cannot be deleted
- [ ] Petty cash accounts reject bank statement uploads
- [ ] Partial payment logic correctly updates expense status and paid_amount
- [ ] Creating a paid expense also creates an initial `ExpensePayment`
- [ ] Payroll schedule generates exactly 14 entries (12 monthly + Bono 14 + Aguinaldo)
- [ ] Payroll schedule assigns correct due dates
- [ ] Salary history row created and previous record closed when base salary changes
- [ ] Future unpaid payroll entries updated in same transaction as salary change
- [ ] Terminating a payroll employee sets `active = false` in the same transaction and removes them from active liability reporting
- [ ] Doctor fee net amount calculation correct (gross - commission)
- [ ] Doctor fee arrangement types (HOSPITAL_BILLED, EXTERNAL) behave correctly
- [ ] Doctor fee cannot move to `INVOICED` without `doctor_invoice_number` and an uploaded invoice copy
- [ ] Doctor fee status transitions enforced (invalid transitions return 400)
- [ ] Doctor fee settlement (`POST .../settle`) creates exactly one linked expense and marks fee PAID atomically
- [ ] One hospital-billed `DoctorFee` per patient charge enforced (duplicate returns 409)
- [ ] Contractor payments recorded via employee payment endpoint create linked expense automatically
- [ ] Invoice scan upload supported both at creation (multipart) and via dedicated endpoint
- [ ] Doctor invoice upload supported and linked expense reuses stored invoice metadata on settlement
- [ ] XLSX and CSV bank statement parsing works with various formats
- [ ] Column mapping saved and reused for subsequent uploads (string for XLSX headers, numeric string index for headerless CSV)
- [ ] Auto-match sets rows to SUGGESTED, not MATCHED
- [ ] Confirm-match transitions SUGGESTED → MATCHED
- [ ] Reject-match transitions SUGGESTED → UNMATCHED
- [ ] SUGGESTED rows block statement COMPLETED status
- [ ] ACKNOWLEDGED rows allowed only for non-ledger items; current-period cash movements must be created as expense/income
- [ ] Manual match/unmatch/acknowledge updates row and statement counts
- [ ] Statement status transitions (PENDING → IN_PROGRESS → COMPLETED) correct
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (Testcontainers)
- [ ] Detekt passes (no new violations)
- [ ] OWASP dependency-check passes

### Frontend
- [ ] All components created and functional
- [ ] Pinia stores implemented
- [ ] Routes configured with proper guards (`ADMIN`, `ADMINISTRATIVE_STAFF`)
- [ ] Form validation with VeeValidate + Zod
- [ ] Account numbers masked in list views
- [ ] Invoice document upload working
- [ ] Employee form shows conditional fields based on type (salary/rate/fee arrangement)
- [ ] Doctor fee form with commission calculation preview
- [ ] Bank statement upload with column mapping configuration
- [ ] Reconciliation UI with color-coded match status (green=matched, blue=suggested, yellow=unmatched statement row, red=unmatched treasury record)
- [ ] Create expense/income from unmatched statement row
- [ ] Bank summary report view
- [ ] Compensation summary report view
- [ ] Indemnización liability report view
- [ ] Monthly report with chart/table view
- [ ] Upcoming payments view with week/month toggle
- [ ] Reconciliation summary report view
- [ ] Error handling implemented
- [ ] ESLint/oxlint passes
- [ ] i18n keys added for all user-facing text
- [ ] Unit tests written and passing (Vitest)

### E2E Tests (Playwright)
- [ ] Bank account CRUD flow
- [ ] Expense creation and payment flow
- [ ] Accounts payable partial payment flow
- [ ] Income registration flow
- [ ] Employee creation and payroll generation
- [ ] Monthly report filtering
- [ ] Upcoming payments view
- [ ] Doctor fee creation and payment flow
- [ ] Indemnización liability report
- [ ] Bank statement upload and reconciliation flow
- [ ] Column mapping configuration on first upload
- [ ] Manual match/unmatch in reconciliation view
- [ ] Compensation summary report
- [ ] Permission denial for users without treasury access

### General
- [ ] API contract documented
- [ ] Database migrations tested
- [ ] Feature documentation updated
- [ ] Reviewed by project owner

---

## Documentation Updates Required

### Must Update

- [ ] **[CLAUDE.md](../../CLAUDE.md)**
  - Add Treasury Module to "Implemented Features"
  - Update migration count (V074-V084)
- [ ] **[ARCHITECTURE.md](../architecture/ARCHITECTURE.md)** (if exists)
  - Add treasury module to architecture overview

### Code Documentation

- [ ] **Entity KDoc comments** for complex financial logic
- [ ] **Service layer** - Document payroll generation and payment recording flows

---

## Related Docs/Commits/Issues

- Related feature: [Hospital Billing System](./hospital-billing-system.md) — income records can link to billing invoices
- Related feature: [Inventory Module](./inventory-module.md) — expenses for supplies may reference inventory
