import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { Income, InvoiceSummary, CreateIncomeRequest, UpdateIncomeRequest } from '@/types/treasury'
import type { ApiResponse, PageResponse } from '@/types'

export interface IncomeFilters {
  category?: string
  bankAccountId?: number
  from?: string
  to?: string
  search?: string
}

export const useIncomeStore = defineStore('income', () => {
  const incomes = ref<Income[]>([])
  const totalIncomes = ref(0)
  const currentIncome = ref<Income | null>(null)
  const invoices = ref<InvoiceSummary[]>([])
  const loading = ref(false)

  async function fetchIncomes(page = 0, size = 20, filters?: IncomeFilters): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (filters?.category) params.category = filters.category
      if (filters?.bankAccountId) params.bankAccountId = filters.bankAccountId
      if (filters?.from) params.from = filters.from
      if (filters?.to) params.to = filters.to
      if (filters?.search) params.search = filters.search

      const response = await api.get<ApiResponse<PageResponse<Income>>>('/v1/treasury/income', {
        params
      })
      if (response.data.success && response.data.data) {
        incomes.value = response.data.data.content
        totalIncomes.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchIncome(id: number): Promise<Income> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Income>>(`/v1/treasury/income/${id}`)
      if (response.data.success && response.data.data) {
        currentIncome.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Income record not found')
    } finally {
      loading.value = false
    }
  }

  async function createIncome(data: CreateIncomeRequest): Promise<Income> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Income>>('/v1/treasury/income', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to create income record')
    } finally {
      loading.value = false
    }
  }

  async function updateIncome(id: number, data: UpdateIncomeRequest): Promise<Income> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<Income>>(`/v1/treasury/income/${id}`, data)
      if (response.data.success && response.data.data) {
        currentIncome.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to update income record')
    } finally {
      loading.value = false
    }
  }

  async function deleteIncome(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/treasury/income/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to delete income record')
      }
    } finally {
      loading.value = false
    }
  }

  async function searchInvoices(search?: string): Promise<void> {
    try {
      const params: Record<string, unknown> = {}
      if (search) params.search = search
      const response = await api.get<ApiResponse<InvoiceSummary[]>>(
        '/v1/treasury/income/invoices',
        { params }
      )
      if (response.data.success && response.data.data) {
        invoices.value = response.data.data
      }
    } catch {
      invoices.value = []
    }
  }

  function clearCurrentIncome(): void {
    currentIncome.value = null
  }

  return {
    incomes,
    totalIncomes,
    currentIncome,
    invoices,
    loading,
    fetchIncomes,
    fetchIncome,
    createIncome,
    updateIncome,
    deleteIncome,
    searchInvoices,
    clearCurrentIncome
  }
})
