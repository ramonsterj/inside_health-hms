import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  TreasuryEmployee,
  SalaryHistory,
  PayrollEntry,
  Expense,
  IndemnizacionResult,
  EmployeePaymentHistory,
  CreateTreasuryEmployeeRequest,
  UpdateTreasuryEmployeeRequest,
  UpdateSalaryRequest,
  RecordContractorPaymentRequest,
  TerminateEmployeeRequest,
  GeneratePayrollScheduleRequest,
  RecordPayrollPaymentRequest
} from '@/types/treasury'
import type { ApiResponse } from '@/types'

export const useTreasuryEmployeeStore = defineStore('treasuryEmployee', () => {
  const employees = ref<TreasuryEmployee[]>([])
  const currentEmployee = ref<TreasuryEmployee | null>(null)
  const payrollEntries = ref<PayrollEntry[]>([])
  const salaryHistory = ref<SalaryHistory[]>([])
  const paymentHistory = ref<EmployeePaymentHistory[]>([])
  const loading = ref(false)

  async function fetchEmployees(filters?: {
    type?: string
    activeOnly?: boolean
    search?: string
  }): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = {}
      if (filters?.type) params.type = filters.type
      if (filters?.activeOnly) params.activeOnly = filters.activeOnly
      if (filters?.search) params.search = filters.search

      const response = await api.get<ApiResponse<TreasuryEmployee[]>>('/v1/treasury/employees', {
        params
      })
      if (response.data.success && response.data.data) {
        employees.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchEmployee(id: number): Promise<TreasuryEmployee> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<TreasuryEmployee>>(`/v1/treasury/employees/${id}`)
      if (response.data.success && response.data.data) {
        currentEmployee.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Employee not found')
    } finally {
      loading.value = false
    }
  }

  async function createEmployee(data: CreateTreasuryEmployeeRequest): Promise<TreasuryEmployee> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<TreasuryEmployee>>('/v1/treasury/employees', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to create employee')
    } finally {
      loading.value = false
    }
  }

  async function updateEmployee(
    id: number,
    data: UpdateTreasuryEmployeeRequest
  ): Promise<TreasuryEmployee> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<TreasuryEmployee>>(
        `/v1/treasury/employees/${id}`,
        data
      )
      if (response.data.success && response.data.data) {
        currentEmployee.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to update employee')
    } finally {
      loading.value = false
    }
  }

  async function updateSalary(id: number, data: UpdateSalaryRequest): Promise<TreasuryEmployee> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<TreasuryEmployee>>(
        `/v1/treasury/employees/${id}/salary`,
        data
      )
      if (response.data.success && response.data.data) {
        currentEmployee.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to update salary')
    } finally {
      loading.value = false
    }
  }

  async function fetchSalaryHistory(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<SalaryHistory[]>>(
        `/v1/treasury/employees/${id}/salary-history`
      )
      if (response.data.success && response.data.data) {
        salaryHistory.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function generatePayroll(
    id: number,
    data: GeneratePayrollScheduleRequest
  ): Promise<PayrollEntry[]> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<PayrollEntry[]>>(
        `/v1/treasury/employees/${id}/payroll/generate`,
        data
      )
      if (response.data.success && response.data.data) {
        payrollEntries.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to generate payroll')
    } finally {
      loading.value = false
    }
  }

  async function fetchPayroll(id: number, year?: number): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = {}
      if (year !== undefined) params.year = year
      const response = await api.get<ApiResponse<PayrollEntry[]>>(
        `/v1/treasury/employees/${id}/payroll`,
        { params }
      )
      if (response.data.success && response.data.data) {
        payrollEntries.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function recordContractorPayment(
    id: number,
    data: RecordContractorPaymentRequest
  ): Promise<Expense> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Expense>>(
        `/v1/treasury/employees/${id}/payments`,
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

  async function payPayrollEntry(
    entryId: number,
    data: RecordPayrollPaymentRequest
  ): Promise<PayrollEntry> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<PayrollEntry>>(
        `/v1/treasury/employees/payroll/${entryId}/pay`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to pay payroll entry')
    } finally {
      loading.value = false
    }
  }

  async function terminateEmployee(
    id: number,
    data: TerminateEmployeeRequest
  ): Promise<TreasuryEmployee> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<TreasuryEmployee>>(
        `/v1/treasury/employees/${id}/terminate`,
        data
      )
      if (response.data.success && response.data.data) {
        currentEmployee.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to terminate employee')
    } finally {
      loading.value = false
    }
  }

  async function calculateIndemnizacion(id: number): Promise<IndemnizacionResult> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<IndemnizacionResult>>(
        `/v1/treasury/employees/${id}/indemnizacion`
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to calculate indemnización')
    } finally {
      loading.value = false
    }
  }

  async function fetchPaymentHistory(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<EmployeePaymentHistory[]>>(
        `/v1/treasury/employees/${id}/payment-history`
      )
      if (response.data.success && response.data.data) {
        paymentHistory.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  function clearCurrentEmployee(): void {
    currentEmployee.value = null
    payrollEntries.value = []
    salaryHistory.value = []
    paymentHistory.value = []
  }

  return {
    employees,
    currentEmployee,
    payrollEntries,
    salaryHistory,
    paymentHistory,
    loading,
    fetchEmployees,
    fetchEmployee,
    createEmployee,
    updateEmployee,
    updateSalary,
    fetchSalaryHistory,
    generatePayroll,
    fetchPayroll,
    recordContractorPayment,
    payPayrollEntry,
    terminateEmployee,
    calculateIndemnizacion,
    fetchPaymentHistory,
    clearCurrentEmployee
  }
})
