import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useExpenseStore } from './expense'
import api from '@/services/api'
import type { Expense, ExpensePayment } from '@/types/treasury'
import { ExpenseCategory, ExpenseStatus } from '@/types/treasury'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  put: Mock
  delete: Mock
}

const mockExpense: Expense = {
  id: 1,
  supplierName: 'Proveedor SA',
  category: ExpenseCategory.SUPPLIES,
  description: null,
  amount: 500,
  expenseDate: '2026-03-01',
  invoiceNumber: 'INV-001',
  invoiceDocumentPath: null,
  status: ExpenseStatus.PENDING,
  isOverdue: false,
  dueDate: '2026-04-01',
  paidAmount: 0,
  remainingAmount: 500,
  notes: null,
  createdAt: '2026-03-01T10:00:00',
  updatedAt: '2026-03-01T10:00:00',
  createdBy: null,
  updatedBy: null
}

const mockPayment: ExpensePayment = {
  id: 1,
  expenseId: 1,
  amount: 200,
  paymentDate: '2026-03-10',
  bankAccountId: 5,
  bankAccountName: 'Banco Industrial',
  maskedAccountNumber: '****1234',
  reference: 'TRF-001',
  notes: null,
  createdAt: '2026-03-10T09:00:00'
}

const apiSuccess = <T>(data: T) => ({
  data: { success: true, data, message: null }
})

const pageSuccess = (items: Expense[]) => ({
  data: {
    success: true,
    data: {
      content: items,
      page: { totalElements: items.length, totalPages: 1, size: 20, number: 0 }
    }
  }
})

describe('useExpenseStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchExpenses populates expenses and totalExpenses', async () => {
    mockedApi.get.mockResolvedValueOnce(pageSuccess([mockExpense]))
    const store = useExpenseStore()

    await store.fetchExpenses()

    expect(store.expenses).toHaveLength(1)
    expect(store.totalExpenses).toBe(1)
    expect(store.expenses[0]!.supplierName).toBe('Proveedor SA')
  })

  it('fetchExpenses passes filters as query params', async () => {
    mockedApi.get.mockResolvedValueOnce(pageSuccess([]))
    const store = useExpenseStore()

    await store.fetchExpenses(0, 20, { status: 'PENDING', search: 'sup' })

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/expenses',
      expect.objectContaining({
        params: expect.objectContaining({ status: 'PENDING', search: 'sup' })
      })
    )
  })

  it('fetchExpenses sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useExpenseStore()

    await expect(store.fetchExpenses()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  it('fetchExpense sets currentExpense and returns it', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockExpense))
    const store = useExpenseStore()

    const result = await store.fetchExpense(1)

    expect(result).toEqual(mockExpense)
    expect(store.currentExpense).toEqual(mockExpense)
  })

  it('fetchExpense throws when expense not found', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Not found' }
    })
    const store = useExpenseStore()

    await expect(store.fetchExpense(99)).rejects.toThrow('Not found')
  })

  it('createExpense posts data and returns created expense', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess(mockExpense))
    const store = useExpenseStore()

    const result = await store.createExpense({
      supplierName: 'Proveedor SA',
      category: ExpenseCategory.SUPPLIES,
      amount: 500,
      expenseDate: '2026-03-01',
      invoiceNumber: 'INV-001',
      dueDate: '2026-04-01',
      isPaid: false
    })

    expect(result).toEqual(mockExpense)
    expect(mockedApi.post).toHaveBeenCalledWith('/v1/treasury/expenses', expect.any(Object))
  })

  it('createExpense uploads invoice file when provided', async () => {
    mockedApi.post
      .mockResolvedValueOnce(apiSuccess(mockExpense)) // create
      .mockResolvedValueOnce(
        apiSuccess({ ...mockExpense, invoiceDocumentPath: 'path/to/invoice.pdf' })
      ) // upload
    const store = useExpenseStore()
    const file = new File(['content'], 'invoice.pdf', { type: 'application/pdf' })

    const result = await store.createExpense(
      {
        supplierName: 'S',
        category: ExpenseCategory.SUPPLIES,
        amount: 100,
        expenseDate: '2026-03-01',
        invoiceNumber: 'INV',
        isPaid: false
      },
      file
    )

    expect(mockedApi.post).toHaveBeenCalledTimes(2)
    expect(result.invoiceDocumentPath).toBe('path/to/invoice.pdf')
  })

  it('updateExpense sends PUT and updates currentExpense', async () => {
    const updated = { ...mockExpense, supplierName: 'Updated Supplier' }
    mockedApi.put.mockResolvedValueOnce(apiSuccess(updated))
    const store = useExpenseStore()

    const result = await store.updateExpense(1, {
      supplierName: 'Updated Supplier',
      category: ExpenseCategory.SUPPLIES,
      amount: 500,
      expenseDate: '2026-03-01',
      invoiceNumber: 'INV-001'
    })

    expect(result.supplierName).toBe('Updated Supplier')
    expect(store.currentExpense?.supplierName).toBe('Updated Supplier')
  })

  it('deleteExpense sends DELETE request', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: true, data: null } })
    const store = useExpenseStore()

    await store.deleteExpense(1)

    expect(mockedApi.delete).toHaveBeenCalledWith('/v1/treasury/expenses/1')
  })

  it('deleteExpense throws on failure', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: false, message: 'Cannot delete' } })
    const store = useExpenseStore()

    await expect(store.deleteExpense(1)).rejects.toThrow('Cannot delete')
  })

  it('recordPayment posts payment data and returns payment', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess(mockPayment))
    const store = useExpenseStore()

    const result = await store.recordPayment(1, {
      amount: 200,
      paymentDate: '2026-03-10',
      bankAccountId: 5
    })

    expect(result).toEqual(mockPayment)
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/expenses/1/payments',
      expect.any(Object)
    )
  })

  it('fetchPayments populates payments array', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockPayment]))
    const store = useExpenseStore()

    await store.fetchPayments(1)

    expect(store.payments).toHaveLength(1)
    expect(store.payments[0]!.amount).toBe(200)
  })

  it('uploadInvoiceDocument posts multipart and returns updated expense', async () => {
    const updated = { ...mockExpense, invoiceDocumentPath: 'treasury/expenses/1/uuid_invoice.pdf' }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(updated))
    const store = useExpenseStore()
    const file = new File(['pdf content'], 'invoice.pdf', { type: 'application/pdf' })

    const result = await store.uploadInvoiceDocument(1, file)

    expect(result.invoiceDocumentPath).toBe('treasury/expenses/1/uuid_invoice.pdf')
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/expenses/1/invoice-document',
      expect.any(FormData),
      expect.objectContaining({ headers: { 'Content-Type': 'multipart/form-data' } })
    )
  })

  it('clearCurrentExpense sets currentExpense to null', () => {
    const store = useExpenseStore()
    store.currentExpense = mockExpense

    store.clearCurrentExpense()

    expect(store.currentExpense).toBeNull()
  })
})
