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
