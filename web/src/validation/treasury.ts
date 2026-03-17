import { z } from 'zod'
import { BankAccountType, ExpenseCategory } from '@/types/treasury'

export const createBankAccountSchema = z.object({
  name: z
    .string()
    .min(1, 'validation.treasury.bankAccount.name.required')
    .max(100, 'validation.treasury.bankAccount.name.max'),
  bankName: z.string().max(100).optional().or(z.literal('')),
  accountNumber: z.string().max(50).optional().or(z.literal('')),
  accountType: z.nativeEnum(BankAccountType, {
    required_error: 'validation.treasury.bankAccount.accountType.required'
  }),
  currency: z.string().length(3).default('GTQ'),
  openingBalance: z
    .number()
    .min(0, 'validation.treasury.bankAccount.openingBalance.min')
    .default(0),
  notes: z.string().optional().or(z.literal(''))
})

export const updateBankAccountSchema = createBankAccountSchema.extend({
  active: z.boolean().default(true)
})

export const createExpenseSchema = z
  .object({
    supplierName: z.string().min(1, 'validation.treasury.expense.supplierName.required').max(255),
    category: z.nativeEnum(ExpenseCategory, {
      required_error: 'validation.treasury.expense.category.required'
    }),
    description: z.string().optional().or(z.literal('')),
    amount: z
      .number({ required_error: 'validation.treasury.expense.amount.required' })
      .positive('validation.treasury.expense.amount.positive'),
    expenseDate: z.string().min(1, 'validation.treasury.expense.expenseDate.required'),
    invoiceNumber: z.string().min(1, 'validation.treasury.expense.invoiceNumber.required').max(100),
    dueDate: z.string().optional().or(z.literal('')),
    isPaid: z.boolean().default(false),
    paymentDate: z.string().optional().or(z.literal('')),
    bankAccountId: z.number().positive().optional().nullable(),
    notes: z.string().optional().or(z.literal(''))
  })
  .superRefine((data, ctx) => {
    if (!data.isPaid && !data.dueDate) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['dueDate'],
        message: 'validation.treasury.expense.dueDate.required'
      })
    }
    if (data.isPaid && !data.paymentDate) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['paymentDate'],
        message: 'validation.treasury.expense.paymentDate.required'
      })
    }
    if (data.isPaid && !data.bankAccountId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['bankAccountId'],
        message: 'validation.treasury.expense.bankAccountId.required'
      })
    }
  })

export const recordPaymentSchema = z.object({
  amount: z
    .number({ required_error: 'validation.treasury.payment.amount.required' })
    .positive('validation.treasury.payment.amount.positive'),
  paymentDate: z.string().min(1, 'validation.treasury.payment.paymentDate.required'),
  bankAccountId: z
    .number({
      required_error: 'validation.treasury.payment.bankAccountId.required'
    })
    .positive(),
  reference: z.string().max(255).optional().or(z.literal('')),
  notes: z.string().optional().or(z.literal(''))
})

export type CreateBankAccountFormData = z.infer<typeof createBankAccountSchema>
export type UpdateBankAccountFormData = z.infer<typeof updateBankAccountSchema>
export type CreateExpenseFormData = z.infer<typeof createExpenseSchema>
export type RecordPaymentFormData = z.infer<typeof recordPaymentSchema>
