export enum BankAccountType {
  CHECKING = 'CHECKING',
  SAVINGS = 'SAVINGS',
  PETTY_CASH = 'PETTY_CASH'
}

export enum ExpenseCategory {
  SUPPLIES = 'SUPPLIES',
  UTILITIES = 'UTILITIES',
  MAINTENANCE = 'MAINTENANCE',
  EQUIPMENT = 'EQUIPMENT',
  SERVICES = 'SERVICES',
  PAYROLL = 'PAYROLL',
  OTHER = 'OTHER'
}

export enum ExpenseStatus {
  PENDING = 'PENDING',
  PARTIALLY_PAID = 'PARTIALLY_PAID',
  PAID = 'PAID',
  CANCELLED = 'CANCELLED'
}

export interface UserSummary {
  id: number
  username: string
  firstName: string | null
  lastName: string | null
}

export interface BankAccount {
  id: number
  name: string
  bankName: string | null
  maskedAccountNumber: string | null
  accountType: BankAccountType
  currency: string
  openingBalance: number
  bookBalance: number
  isPettyCash: boolean
  active: boolean
  notes: string | null
  createdAt: string | null
  updatedAt: string | null
  createdBy: UserSummary | null
  updatedBy: UserSummary | null
}

export interface CreateBankAccountRequest {
  name: string
  bankName?: string | null
  accountNumber?: string | null
  accountType: BankAccountType
  currency?: string
  openingBalance?: number
  notes?: string | null
}

export type UpdateBankAccountRequest = CreateBankAccountRequest & { active?: boolean }

export interface ExpensePayment {
  id: number
  expenseId: number
  amount: number
  paymentDate: string
  bankAccountId: number
  bankAccountName: string
  maskedAccountNumber: string | null
  reference: string | null
  notes: string | null
  createdAt: string | null
}

export interface Expense {
  id: number
  supplierName: string
  category: ExpenseCategory
  description: string | null
  amount: number
  expenseDate: string
  invoiceNumber: string
  invoiceDocumentPath: string | null
  status: ExpenseStatus
  isOverdue: boolean
  dueDate: string | null
  paidAmount: number
  remainingAmount: number
  notes: string | null
  createdAt: string | null
  updatedAt: string | null
  createdBy: UserSummary | null
  updatedBy: UserSummary | null
}

export interface CreateExpenseRequest {
  supplierName: string
  category: ExpenseCategory
  description?: string | null
  amount: number
  expenseDate: string
  invoiceNumber: string
  dueDate?: string | null
  isPaid: boolean
  paymentDate?: string | null
  bankAccountId?: number | null
  notes?: string | null
}

export interface UpdateExpenseRequest {
  supplierName: string
  category: ExpenseCategory
  description?: string | null
  amount: number
  expenseDate: string
  invoiceNumber: string
  dueDate?: string | null
  notes?: string | null
}

export interface RecordPaymentRequest {
  amount: number
  paymentDate: string
  bankAccountId: number
  reference?: string | null
  notes?: string | null
}

// --- Phase 2: Employees, Income, Payroll ---

export enum EmployeeType {
  PAYROLL = 'PAYROLL',
  CONTRACTOR = 'CONTRACTOR',
  DOCTOR = 'DOCTOR'
}

export enum DoctorFeeArrangement {
  HOSPITAL_BILLED = 'HOSPITAL_BILLED',
  EXTERNAL = 'EXTERNAL'
}

export enum PayrollPeriod {
  JANUARY = 'JANUARY',
  FEBRUARY = 'FEBRUARY',
  MARCH = 'MARCH',
  APRIL = 'APRIL',
  MAY = 'MAY',
  JUNE = 'JUNE',
  JULY = 'JULY',
  AUGUST = 'AUGUST',
  SEPTEMBER = 'SEPTEMBER',
  OCTOBER = 'OCTOBER',
  NOVEMBER = 'NOVEMBER',
  DECEMBER = 'DECEMBER',
  BONO_14 = 'BONO_14',
  AGUINALDO = 'AGUINALDO'
}

export enum PayrollStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  CANCELLED = 'CANCELLED'
}

export enum IncomeCategory {
  PATIENT_PAYMENT = 'PATIENT_PAYMENT',
  INSURANCE_PAYMENT = 'INSURANCE_PAYMENT',
  DONATION = 'DONATION',
  GOVERNMENT_SUBSIDY = 'GOVERNMENT_SUBSIDY',
  OTHER_INCOME = 'OTHER_INCOME'
}

export enum EmployeePaymentType {
  PAYROLL_ENTRY = 'PAYROLL_ENTRY',
  CONTRACTOR_PAYMENT = 'CONTRACTOR_PAYMENT',
  DOCTOR_FEE_SETTLEMENT = 'DOCTOR_FEE_SETTLEMENT'
}

export interface TreasuryEmployee {
  id: number
  fullName: string
  employeeType: EmployeeType
  taxId: string | null
  position: string | null
  baseSalary: number | null
  contractedRate: number | null
  doctorFeeArrangement: DoctorFeeArrangement | null
  hospitalCommissionPct: number
  hireDate: string | null
  terminationDate: string | null
  terminationReason: string | null
  active: boolean
  userId: number | null
  notes: string | null
  indemnizacionLiability: number | null
  createdAt: string | null
  updatedAt: string | null
  createdBy: UserSummary | null
  updatedBy: UserSummary | null
}

export interface SalaryHistory {
  id: number
  employeeId: number
  baseSalary: number
  effectiveFrom: string
  effectiveTo: string | null
  notes: string | null
  createdAt: string | null
}

export interface PayrollEntry {
  id: number
  employeeId: number
  employeeName: string
  year: number
  period: PayrollPeriod
  periodLabel: string
  baseSalary: number
  grossAmount: number
  dueDate: string
  status: PayrollStatus
  paidDate: string | null
  expenseId: number | null
  notes: string | null
  createdAt: string | null
}

export interface Income {
  id: number
  description: string
  category: IncomeCategory
  amount: number
  incomeDate: string
  reference: string | null
  bankAccountId: number
  bankAccountName: string
  invoiceId: number | null
  invoiceNumber: string | null
  notes: string | null
  createdAt: string | null
  updatedAt: string | null
  createdBy: UserSummary | null
  updatedBy: UserSummary | null
}

export interface IndemnizacionResult {
  employeeId: number
  employeeName: string
  baseSalary: number
  hireDate: string
  daysWorked: number
  liability: number
  asOfDate: string
}

export interface EmployeePaymentHistory {
  type: EmployeePaymentType
  amount: number
  date: string
  reference: string | null
  status: string
  relatedEntityId: number
  createdAt: string | null
}

export interface CreateTreasuryEmployeeRequest {
  fullName: string
  employeeType: EmployeeType
  taxId?: string | null
  position?: string | null
  baseSalary?: number | null
  contractedRate?: number | null
  doctorFeeArrangement?: DoctorFeeArrangement | null
  hospitalCommissionPct?: number
  hireDate?: string | null
  userId?: number | null
  notes?: string | null
}

export interface UpdateTreasuryEmployeeRequest {
  fullName: string
  taxId?: string | null
  position?: string | null
  contractedRate?: number | null
  doctorFeeArrangement?: DoctorFeeArrangement | null
  hospitalCommissionPct?: number
  hireDate?: string | null
  userId?: number | null
  notes?: string | null
}

export interface UpdateSalaryRequest {
  newSalary: number
  effectiveFrom: string
  notes?: string | null
}

export interface RecordContractorPaymentRequest {
  amount: number
  paymentDate: string
  invoiceNumber: string
  bankAccountId?: number | null
  notes?: string | null
}

export interface TerminateEmployeeRequest {
  terminationDate: string
  terminationReason?: string | null
  cancelPendingPayroll?: boolean
}

export interface GeneratePayrollScheduleRequest {
  year: number
}

export interface RecordPayrollPaymentRequest {
  paymentDate: string
  bankAccountId: number
  notes?: string | null
}

// --- Phase 3: Doctor Fees ---

export enum DoctorFeeStatus {
  PENDING = 'PENDING',
  INVOICED = 'INVOICED',
  PAID = 'PAID'
}

export enum DoctorFeeBillingType {
  HOSPITAL_BILLED = 'HOSPITAL_BILLED',
  EXTERNAL = 'EXTERNAL'
}

export interface DoctorFee {
  id: number
  treasuryEmployeeId: number
  employeeName: string
  patientChargeId: number | null
  billingType: DoctorFeeBillingType
  grossAmount: number
  commissionPct: number
  netAmount: number
  status: DoctorFeeStatus
  doctorInvoiceNumber: string | null
  invoiceDocumentPath: string | null
  expenseId: number | null
  feeDate: string
  description: string | null
  notes: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface DoctorFeeSummary {
  employeeId: number
  employeeName: string
  totalFees: number
  totalGross: number
  totalNet: number
  totalCommission: number
  pendingCount: number
  invoicedCount: number
  paidCount: number
}

export interface CreateDoctorFeeRequest {
  patientChargeId?: number | null
  billingType: DoctorFeeBillingType
  grossAmount: number
  commissionPct?: number | null
  feeDate: string
  description?: string | null
  notes?: string | null
}

export interface UpdateDoctorFeeStatusRequest {
  status: DoctorFeeStatus
  doctorInvoiceNumber: string
}

export interface SettleDoctorFeeRequest {
  bankAccountId: number
  paymentDate: string
  notes?: string | null
}

export interface InvoiceSummary {
  id: number
  invoiceNumber: string
  patientName: string
  totalAmount: number
}

export interface CreateIncomeRequest {
  description: string
  category: IncomeCategory
  amount: number
  incomeDate: string
  reference?: string | null
  bankAccountId: number
  invoiceId?: number | null
  notes?: string | null
}

export interface UpdateIncomeRequest {
  description: string
  category: IncomeCategory
  amount: number
  incomeDate: string
  reference?: string | null
  bankAccountId: number
  invoiceId?: number | null
  notes?: string | null
}
