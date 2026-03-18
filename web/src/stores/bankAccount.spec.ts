import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBankAccountStore } from './bankAccount'
import api from '@/services/api'
import type { BankAccount } from '@/types/treasury'
import { BankAccountType } from '@/types/treasury'

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

const mockAccount: BankAccount = {
  id: 1,
  name: 'Banco Industrial',
  bankName: 'Banco Industrial',
  maskedAccountNumber: '****1234',
  accountType: BankAccountType.CHECKING,
  currency: 'GTQ',
  openingBalance: 10000,
  bookBalance: 8500,
  isPettyCash: false,
  active: true,
  notes: null,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
  createdBy: null,
  updatedBy: null
}

const mockPettyCash: BankAccount = {
  ...mockAccount,
  id: 2,
  name: 'Caja Chica',
  accountType: BankAccountType.PETTY_CASH,
  isPettyCash: true,
  maskedAccountNumber: null
}

const apiSuccess = <T>(data: T) => ({
  data: { success: true, data, message: null }
})

describe('useBankAccountStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchBankAccounts populates bankAccounts', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockAccount, mockPettyCash]))
    const store = useBankAccountStore()

    await store.fetchBankAccounts()

    expect(store.bankAccounts).toHaveLength(2)
    expect(store.bankAccounts[0]!.name).toBe('Banco Industrial')
  })

  it('fetchBankAccounts sets loading to false on success', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockAccount]))
    const store = useBankAccountStore()

    await store.fetchBankAccounts()

    expect(store.loading).toBe(false)
  })

  it('fetchBankAccounts sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useBankAccountStore()

    await expect(store.fetchBankAccounts()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  it('fetchActiveBankAccounts populates activeBankAccounts', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockAccount]))
    const store = useBankAccountStore()

    await store.fetchActiveBankAccounts()

    expect(store.activeBankAccounts).toHaveLength(1)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/bank-accounts/active')
  })

  it('fetchBankAccount sets currentBankAccount and returns it', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockAccount))
    const store = useBankAccountStore()

    const result = await store.fetchBankAccount(1)

    expect(result).toEqual(mockAccount)
    expect(store.currentBankAccount).toEqual(mockAccount)
  })

  it('fetchBankAccount throws when account not found', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Not found' }
    })
    const store = useBankAccountStore()

    await expect(store.fetchBankAccount(99)).rejects.toThrow('Not found')
  })

  it('createBankAccount posts to correct endpoint and returns created account', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess(mockAccount))
    const store = useBankAccountStore()

    const result = await store.createBankAccount({
      name: 'Banco Industrial',
      accountType: BankAccountType.CHECKING,
      currency: 'GTQ',
      openingBalance: 10000
    })

    expect(result).toEqual(mockAccount)
    expect(mockedApi.post).toHaveBeenCalledWith('/v1/treasury/bank-accounts', expect.any(Object))
  })

  it('updateBankAccount sends PUT request and updates currentBankAccount', async () => {
    const updated = { ...mockAccount, name: 'Updated Name' }
    mockedApi.put.mockResolvedValueOnce(apiSuccess(updated))
    const store = useBankAccountStore()

    const result = await store.updateBankAccount(1, {
      name: 'Updated Name',
      accountType: BankAccountType.CHECKING,
      currency: 'GTQ'
    })

    expect(result.name).toBe('Updated Name')
    expect(store.currentBankAccount?.name).toBe('Updated Name')
  })

  it('deleteBankAccount sends DELETE request', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: true, data: null } })
    const store = useBankAccountStore()

    await store.deleteBankAccount(1)

    expect(mockedApi.delete).toHaveBeenCalledWith('/v1/treasury/bank-accounts/1')
  })

  it('deleteBankAccount throws on failure', async () => {
    mockedApi.delete.mockResolvedValueOnce({
      data: { success: false, message: 'Cannot delete petty cash' }
    })
    const store = useBankAccountStore()

    await expect(store.deleteBankAccount(2)).rejects.toThrow('Cannot delete petty cash')
  })

  it('clearCurrentBankAccount sets currentBankAccount to null', () => {
    const store = useBankAccountStore()
    store.currentBankAccount = mockAccount

    store.clearCurrentBankAccount()

    expect(store.currentBankAccount).toBeNull()
  })
})
