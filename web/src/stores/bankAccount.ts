import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  BankAccount,
  CreateBankAccountRequest,
  UpdateBankAccountRequest
} from '@/types/treasury'
import type { ApiResponse } from '@/types'

export const useBankAccountStore = defineStore('bankAccount', () => {
  const bankAccounts = ref<BankAccount[]>([])
  const activeBankAccounts = ref<BankAccount[]>([])
  const currentBankAccount = ref<BankAccount | null>(null)
  const loading = ref(false)

  async function fetchBankAccounts(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<BankAccount[]>>('/v1/treasury/bank-accounts')
      if (response.data.success && response.data.data) {
        bankAccounts.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchActiveBankAccounts(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<BankAccount[]>>(
        '/v1/treasury/bank-accounts/active'
      )
      if (response.data.success && response.data.data) {
        activeBankAccounts.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchBankAccount(id: number): Promise<BankAccount> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<BankAccount>>(`/v1/treasury/bank-accounts/${id}`)
      if (response.data.success && response.data.data) {
        currentBankAccount.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Bank account not found')
    } finally {
      loading.value = false
    }
  }

  async function createBankAccount(data: CreateBankAccountRequest): Promise<BankAccount> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<BankAccount>>('/v1/treasury/bank-accounts', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to create bank account')
    } finally {
      loading.value = false
    }
  }

  async function updateBankAccount(
    id: number,
    data: UpdateBankAccountRequest
  ): Promise<BankAccount> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<BankAccount>>(
        `/v1/treasury/bank-accounts/${id}`,
        data
      )
      if (response.data.success && response.data.data) {
        currentBankAccount.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to update bank account')
    } finally {
      loading.value = false
    }
  }

  async function deleteBankAccount(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/treasury/bank-accounts/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to delete bank account')
      }
    } finally {
      loading.value = false
    }
  }

  function clearCurrentBankAccount(): void {
    currentBankAccount.value = null
  }

  return {
    bankAccounts,
    activeBankAccounts,
    currentBankAccount,
    loading,
    fetchBankAccounts,
    fetchActiveBankAccounts,
    fetchBankAccount,
    createBankAccount,
    updateBankAccount,
    deleteBankAccount,
    clearCurrentBankAccount
  }
})
