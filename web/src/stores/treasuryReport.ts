import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  TreasuryDashboardData,
  MonthlyPaymentReport,
  UpcomingPaymentsData,
  BankAccountSummaryData,
  EmployeeCompensationData,
  IndemnizacionLiabilityData,
  ReconciliationSummaryData
} from '@/types/treasury'
import type { ApiResponse } from '@/types'

export const useTreasuryReportStore = defineStore('treasuryReport', () => {
  const dashboard = ref<TreasuryDashboardData | null>(null)
  const monthlyReport = ref<MonthlyPaymentReport | null>(null)
  const upcomingPayments = ref<UpcomingPaymentsData | null>(null)
  const bankSummary = ref<BankAccountSummaryData | null>(null)
  const compensation = ref<EmployeeCompensationData | null>(null)
  const indemnizacion = ref<IndemnizacionLiabilityData | null>(null)
  const reconciliation = ref<ReconciliationSummaryData | null>(null)
  const loading = ref(false)

  async function fetchDashboard(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<TreasuryDashboardData>>(
        '/v1/treasury/reports/dashboard'
      )
      if (response.data.success && response.data.data) {
        dashboard.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchMonthlyReport(from: string, to: string): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<MonthlyPaymentReport>>(
        '/v1/treasury/reports/monthly',
        { params: { from, to } }
      )
      if (response.data.success && response.data.data) {
        monthlyReport.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUpcomingPayments(windowDays: number = 30): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<UpcomingPaymentsData>>(
        '/v1/treasury/reports/upcoming-payments',
        { params: { windowDays } }
      )
      if (response.data.success && response.data.data) {
        upcomingPayments.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchBankSummary(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<BankAccountSummaryData>>(
        '/v1/treasury/reports/bank-summary'
      )
      if (response.data.success && response.data.data) {
        bankSummary.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchCompensation(year?: number): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, number> = {}
      if (year) params.year = year
      const response = await api.get<ApiResponse<EmployeeCompensationData>>(
        '/v1/treasury/reports/compensation',
        { params }
      )
      if (response.data.success && response.data.data) {
        compensation.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchIndemnizacion(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<IndemnizacionLiabilityData>>(
        '/v1/treasury/reports/indemnizacion'
      )
      if (response.data.success && response.data.data) {
        indemnizacion.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchReconciliation(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<ReconciliationSummaryData>>(
        '/v1/treasury/reports/reconciliation'
      )
      if (response.data.success && response.data.data) {
        reconciliation.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  return {
    dashboard,
    monthlyReport,
    upcomingPayments,
    bankSummary,
    compensation,
    indemnizacion,
    reconciliation,
    loading,
    fetchDashboard,
    fetchMonthlyReport,
    fetchUpcomingPayments,
    fetchBankSummary,
    fetchCompensation,
    fetchIndemnizacion,
    fetchReconciliation
  }
})
