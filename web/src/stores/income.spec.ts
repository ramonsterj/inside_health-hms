import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useIncomeStore } from './income'
import api from '@/services/api'
import type { Income } from '@/types/treasury'
import { IncomeCategory } from '@/types/treasury'

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

const mockIncome: Income = {
  id: 1,
  description: 'Patient payment',
  category: IncomeCategory.PATIENT_PAYMENT,
  amount: 1500,
  incomeDate: '2026-03-10',
  reference: null,
  bankAccountId: 5,
  bankAccountName: 'Caja Chica',
  invoiceId: null,
  invoiceNumber: null,
  notes: null,
  createdAt: '2026-03-10T09:00:00',
  updatedAt: '2026-03-10T09:00:00',
  createdBy: null,
  updatedBy: null
}

const apiSuccess = <T>(data: T) => ({
  data: { success: true, data, message: null }
})

const pageSuccess = (items: Income[]) => ({
  data: {
    success: true,
    data: {
      content: items,
      page: { totalElements: items.length, totalPages: 1, size: 20, number: 0 }
    }
  }
})

describe('useIncomeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchIncomes populates incomes and totalIncomes', async () => {
    mockedApi.get.mockResolvedValueOnce(pageSuccess([mockIncome]))
    const store = useIncomeStore()

    await store.fetchIncomes()

    expect(store.incomes).toHaveLength(1)
    expect(store.totalIncomes).toBe(1)
    expect(store.incomes[0]!.description).toBe('Patient payment')
  })

  it('fetchIncomes passes filters as query params', async () => {
    mockedApi.get.mockResolvedValueOnce(pageSuccess([]))
    const store = useIncomeStore()

    await store.fetchIncomes(0, 20, { category: 'PATIENT_PAYMENT', search: 'patient' })

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/income',
      expect.objectContaining({
        params: expect.objectContaining({ category: 'PATIENT_PAYMENT', search: 'patient' })
      })
    )
  })

  it('fetchIncomes sets loading to false after completion', async () => {
    mockedApi.get.mockResolvedValueOnce(pageSuccess([]))
    const store = useIncomeStore()

    await store.fetchIncomes()

    expect(store.loading).toBe(false)
  })

  it('fetchIncomes sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useIncomeStore()

    await expect(store.fetchIncomes()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  it('fetchIncome sets currentIncome and returns it', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockIncome))
    const store = useIncomeStore()

    const result = await store.fetchIncome(1)

    expect(result).toEqual(mockIncome)
    expect(store.currentIncome).toEqual(mockIncome)
  })

  it('fetchIncome throws when income not found', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Not found' }
    })
    const store = useIncomeStore()

    await expect(store.fetchIncome(99)).rejects.toThrow('Not found')
  })

  it('createIncome posts data and returns created income', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess(mockIncome))
    const store = useIncomeStore()

    const result = await store.createIncome({
      description: 'Patient payment',
      category: IncomeCategory.PATIENT_PAYMENT,
      amount: 1500,
      incomeDate: '2026-03-10',
      bankAccountId: 5
    })

    expect(result).toEqual(mockIncome)
    expect(mockedApi.post).toHaveBeenCalledWith('/v1/treasury/income', expect.any(Object))
  })

  it('createIncome throws on failure', async () => {
    mockedApi.post.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Validation error' }
    })
    const store = useIncomeStore()

    await expect(
      store.createIncome({
        description: 'Test',
        category: IncomeCategory.OTHER_INCOME,
        amount: 100,
        incomeDate: '2026-03-01',
        bankAccountId: 5
      })
    ).rejects.toThrow('Validation error')
  })

  it('updateIncome sends PUT and updates currentIncome', async () => {
    const updated = { ...mockIncome, description: 'Insurance reimbursement', amount: 2000 }
    mockedApi.put.mockResolvedValueOnce(apiSuccess(updated))
    const store = useIncomeStore()

    const result = await store.updateIncome(1, {
      description: 'Insurance reimbursement',
      category: IncomeCategory.INSURANCE_PAYMENT,
      amount: 2000,
      incomeDate: '2026-03-10',
      bankAccountId: 5
    })

    expect(result.description).toBe('Insurance reimbursement')
    expect(store.currentIncome?.description).toBe('Insurance reimbursement')
    expect(mockedApi.put).toHaveBeenCalledWith('/v1/treasury/income/1', expect.any(Object))
  })

  it('deleteIncome sends DELETE request', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: true, data: null } })
    const store = useIncomeStore()

    await store.deleteIncome(1)

    expect(mockedApi.delete).toHaveBeenCalledWith('/v1/treasury/income/1')
  })

  it('deleteIncome throws on failure', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: false, message: 'Cannot delete' } })
    const store = useIncomeStore()

    await expect(store.deleteIncome(1)).rejects.toThrow('Cannot delete')
  })

  it('clearCurrentIncome sets currentIncome to null', () => {
    const store = useIncomeStore()
    store.currentIncome = mockIncome

    store.clearCurrentIncome()

    expect(store.currentIncome).toBeNull()
  })
})
