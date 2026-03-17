import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  Expense,
  ExpensePayment,
  CreateExpenseRequest,
  UpdateExpenseRequest,
  RecordPaymentRequest
} from '@/types/treasury'
import type { ApiResponse, PageResponse } from '@/types'

export interface ExpenseFilters {
  status?: string
  category?: string
  from?: string
  to?: string
  search?: string
}

export const useExpenseStore = defineStore('expense', () => {
  const expenses = ref<Expense[]>([])
  const totalExpenses = ref(0)
  const currentExpense = ref<Expense | null>(null)
  const payments = ref<ExpensePayment[]>([])
  const loading = ref(false)

  async function fetchExpenses(page = 0, size = 20, filters?: ExpenseFilters): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (filters?.status) params.status = filters.status
      if (filters?.category) params.category = filters.category
      if (filters?.from) params.from = filters.from
      if (filters?.to) params.to = filters.to
      if (filters?.search) params.search = filters.search

      const response = await api.get<ApiResponse<PageResponse<Expense>>>('/v1/treasury/expenses', {
        params
      })
      if (response.data.success && response.data.data) {
        expenses.value = response.data.data.content
        totalExpenses.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchExpense(id: number): Promise<Expense> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Expense>>(`/v1/treasury/expenses/${id}`)
      if (response.data.success && response.data.data) {
        currentExpense.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Expense not found')
    } finally {
      loading.value = false
    }
  }

  async function createExpense(data: CreateExpenseRequest, invoiceFile?: File): Promise<Expense> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Expense>>('/v1/treasury/expenses', data)
      if (!response.data.success || !response.data.data) {
        throw new Error(response.data.message || 'Failed to create expense')
      }
      let expense = response.data.data
      if (invoiceFile) {
        expense = await uploadInvoiceDocument(expense.id, invoiceFile)
      }
      return expense
    } finally {
      loading.value = false
    }
  }

  async function updateExpense(id: number, data: UpdateExpenseRequest): Promise<Expense> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<Expense>>(`/v1/treasury/expenses/${id}`, data)
      if (response.data.success && response.data.data) {
        currentExpense.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to update expense')
    } finally {
      loading.value = false
    }
  }

  async function deleteExpense(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/treasury/expenses/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to delete expense')
      }
    } finally {
      loading.value = false
    }
  }

  async function uploadInvoiceDocument(id: number, file: File): Promise<Expense> {
    loading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      const response = await api.post<ApiResponse<Expense>>(
        `/v1/treasury/expenses/${id}/invoice-document`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to upload invoice')
    } finally {
      loading.value = false
    }
  }

  async function recordPayment(id: number, data: RecordPaymentRequest): Promise<ExpensePayment> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<ExpensePayment>>(
        `/v1/treasury/expenses/${id}/payments`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to record payment')
    } finally {
      loading.value = false
    }
  }

  async function fetchPayments(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<ExpensePayment[]>>(
        `/v1/treasury/expenses/${id}/payments`
      )
      if (response.data.success && response.data.data) {
        payments.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  function clearCurrentExpense(): void {
    currentExpense.value = null
  }

  return {
    expenses,
    totalExpenses,
    currentExpense,
    payments,
    loading,
    fetchExpenses,
    fetchExpense,
    createExpense,
    updateExpense,
    deleteExpense,
    uploadInvoiceDocument,
    recordPayment,
    fetchPayments,
    clearCurrentExpense
  }
})
