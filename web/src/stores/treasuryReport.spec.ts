import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTreasuryReportStore } from './treasuryReport'
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

const mockDashboard: TreasuryDashboardData = {
  bankBalances: [
    { bankAccountId: 1, name: 'Cuenta Principal', bookBalance: 150000, currency: 'GTQ', active: true }
  ],
  pendingPayablesCount: 3,
  pendingPayablesTotal: 25000,
  next7DayObligations: [
    {
      type: 'EXPENSE',
      id: 10,
      description: 'Rent payment',
      amount: 8000,
      dueDate: '2026-03-25',
      employeeName: null,
      supplierName: 'Inmobiliaria GT',
      category: 'RENT'
    }
  ],
  next7DayTotal: 8000
}

const mockMonthlyReport: MonthlyPaymentReport = {
  from: '2026-03-01',
  to: '2026-03-31',
  expensesByCategory: [
    { category: 'PAYROLL', label: 'Payroll', total: 40000, count: 8 },
    { category: 'SUPPLIES', label: 'Supplies', total: 5000, count: 3 }
  ],
  totalExpenses: 45000,
  incomeByCategory: [
    { category: 'PATIENT_BILLING', label: 'Patient Billing', total: 80000, count: 15 }
  ],
  totalIncome: 80000,
  netBalance: 35000
}

const mockUpcomingPayments: UpcomingPaymentsData = {
  windowDays: 30,
  from: '2026-03-19',
  to: '2026-04-18',
  items: [
    {
      type: 'PAYROLL',
      id: 20,
      description: 'March payroll - Ana Lopez',
      amount: 5000,
      dueDate: '2026-03-31',
      employeeName: 'Ana Lopez',
      supplierName: null,
      category: null
    }
  ],
  totalAmount: 5000,
  expenseCount: 0,
  payrollCount: 1
}

const mockBankSummary: BankAccountSummaryData = {
  accounts: [
    {
      bankAccountId: 1,
      name: 'Cuenta Principal',
      bankName: 'Banrural',
      accountType: 'CHECKING',
      currency: 'GTQ',
      openingBalance: 100000,
      bookBalance: 150000,
      lastStatementDate: '2026-03-15',
      lastStatementBalance: 148000,
      active: true,
      recentTransactions: [
        {
          type: 'EXPENSE_PAYMENT',
          date: '2026-03-18',
          description: 'Office supplies',
          amount: 1200,
          reference: 'PAY-45'
        }
      ]
    }
  ],
  totalBookBalance: 150000
}

const mockCompensation: EmployeeCompensationData = {
  year: 2026,
  employees: [
    {
      employeeId: 1,
      fullName: 'Ana Lopez',
      employeeType: 'PAYROLL',
      position: 'Enfermera',
      compensation: 5000,
      ytdPayments: 10000,
      pendingAmount: 5000
    }
  ],
  totalYtdPayments: 10000,
  totalPending: 5000
}

const mockIndemnizacion: IndemnizacionLiabilityData = {
  asOfDate: '2026-03-19',
  employees: [
    {
      employeeId: 1,
      fullName: 'Ana Lopez',
      position: 'Enfermera',
      hireDate: '2024-01-01',
      tenureDays: 808,
      currentSalary: 5000,
      liability: 11068.49
    }
  ],
  grandTotal: 11068.49
}

const mockReconciliation: ReconciliationSummaryData = {
  accounts: [
    {
      bankAccountId: 1,
      bankAccountName: 'Cuenta Principal',
      lastReconciliationDate: '2026-03-15',
      lastStatementStatus: 'COMPLETED',
      totalStatements: 3,
      totalRows: 120,
      matchedCount: 110,
      unmatchedCount: 5,
      acknowledgedCount: 5,
      coveragePct: 95.83
    }
  ]
}

const apiSuccess = <T>(data: T) => ({
  data: { success: true, data, message: null }
})

describe('useTreasuryReportStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // --- fetchDashboard ---------------------------------------------------------

  it('fetchDashboard populates dashboard state', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockDashboard))
    const store = useTreasuryReportStore()

    await store.fetchDashboard()

    expect(store.dashboard).toEqual(mockDashboard)
    expect(store.dashboard!.bankBalances).toHaveLength(1)
    expect(store.dashboard!.pendingPayablesCount).toBe(3)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/reports/dashboard')
  })

  it('fetchDashboard sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryReportStore()

    await expect(store.fetchDashboard()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // --- fetchMonthlyReport ----------------------------------------------------

  it('fetchMonthlyReport passes from/to params and populates monthlyReport', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockMonthlyReport))
    const store = useTreasuryReportStore()

    await store.fetchMonthlyReport('2026-03-01', '2026-03-31')

    expect(store.monthlyReport).toEqual(mockMonthlyReport)
    expect(store.monthlyReport!.totalExpenses).toBe(45000)
    expect(store.monthlyReport!.netBalance).toBe(35000)
    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/reports/monthly',
      { params: { from: '2026-03-01', to: '2026-03-31' } }
    )
  })

  it('fetchMonthlyReport sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryReportStore()

    await expect(store.fetchMonthlyReport('2026-03-01', '2026-03-31')).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // --- fetchUpcomingPayments -------------------------------------------------

  it('fetchUpcomingPayments passes windowDays param and populates upcomingPayments', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockUpcomingPayments))
    const store = useTreasuryReportStore()

    await store.fetchUpcomingPayments(30)

    expect(store.upcomingPayments).toEqual(mockUpcomingPayments)
    expect(store.upcomingPayments!.items).toHaveLength(1)
    expect(store.upcomingPayments!.payrollCount).toBe(1)
    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/reports/upcoming-payments',
      { params: { windowDays: 30 } }
    )
  })

  it('fetchUpcomingPayments uses default windowDays of 30', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockUpcomingPayments))
    const store = useTreasuryReportStore()

    await store.fetchUpcomingPayments()

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/reports/upcoming-payments',
      { params: { windowDays: 30 } }
    )
  })

  it('fetchUpcomingPayments sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryReportStore()

    await expect(store.fetchUpcomingPayments()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // --- fetchBankSummary ------------------------------------------------------

  it('fetchBankSummary populates bankSummary state', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockBankSummary))
    const store = useTreasuryReportStore()

    await store.fetchBankSummary()

    expect(store.bankSummary).toEqual(mockBankSummary)
    expect(store.bankSummary!.accounts).toHaveLength(1)
    expect(store.bankSummary!.totalBookBalance).toBe(150000)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/reports/bank-summary')
  })

  it('fetchBankSummary sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryReportStore()

    await expect(store.fetchBankSummary()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // --- fetchCompensation -----------------------------------------------------

  it('fetchCompensation passes year param and populates compensation', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockCompensation))
    const store = useTreasuryReportStore()

    await store.fetchCompensation(2026)

    expect(store.compensation).toEqual(mockCompensation)
    expect(store.compensation!.year).toBe(2026)
    expect(store.compensation!.employees).toHaveLength(1)
    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/reports/compensation',
      { params: { year: 2026 } }
    )
  })

  it('fetchCompensation sends empty params when year is not provided', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockCompensation))
    const store = useTreasuryReportStore()

    await store.fetchCompensation()

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/reports/compensation',
      { params: {} }
    )
  })

  it('fetchCompensation sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryReportStore()

    await expect(store.fetchCompensation(2026)).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // --- fetchIndemnizacion ----------------------------------------------------

  it('fetchIndemnizacion populates indemnizacion state', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockIndemnizacion))
    const store = useTreasuryReportStore()

    await store.fetchIndemnizacion()

    expect(store.indemnizacion).toEqual(mockIndemnizacion)
    expect(store.indemnizacion!.grandTotal).toBe(11068.49)
    expect(store.indemnizacion!.employees).toHaveLength(1)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/reports/indemnizacion')
  })

  it('fetchIndemnizacion sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryReportStore()

    await expect(store.fetchIndemnizacion()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // --- fetchReconciliation ---------------------------------------------------

  it('fetchReconciliation populates reconciliation state', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockReconciliation))
    const store = useTreasuryReportStore()

    await store.fetchReconciliation()

    expect(store.reconciliation).toEqual(mockReconciliation)
    expect(store.reconciliation!.accounts).toHaveLength(1)
    expect(store.reconciliation!.accounts[0]!.coveragePct).toBe(95.83)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/reports/reconciliation')
  })

  it('fetchReconciliation sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryReportStore()

    await expect(store.fetchReconciliation()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })
})
