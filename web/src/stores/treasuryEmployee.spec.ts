import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTreasuryEmployeeStore } from './treasuryEmployee'
import api from '@/services/api'
import type {
  TreasuryEmployee,
  PayrollEntry,
  SalaryHistory,
  EmployeePaymentHistory
} from '@/types/treasury'
import { EmployeeType, PayrollStatus, PayrollPeriod, EmployeePaymentType } from '@/types/treasury'

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

const mockEmployee: TreasuryEmployee = {
  id: 1,
  fullName: 'Ana Lopez',
  employeeType: EmployeeType.PAYROLL,
  taxId: '1234567890101',
  position: 'Enfermera',
  baseSalary: 5000,
  contractedRate: null,
  doctorFeeArrangement: null,
  hospitalCommissionPct: 0,
  hireDate: '2024-01-01',
  terminationDate: null,
  terminationReason: null,
  active: true,
  userId: null,
  notes: null,
  indemnizacionLiability: 13698.63,
  createdAt: '2024-01-01T08:00:00',
  updatedAt: '2024-01-01T08:00:00',
  createdBy: null,
  updatedBy: null
}

const mockPayrollEntry: PayrollEntry = {
  id: 10,
  employeeId: 1,
  employeeName: 'Ana Lopez',
  year: 2026,
  period: PayrollPeriod.MARCH,
  periodLabel: 'March 2026',
  baseSalary: 5000,
  grossAmount: 5000,
  dueDate: '2026-03-31',
  status: PayrollStatus.PENDING,
  paidDate: null,
  expenseId: null,
  notes: null,
  createdAt: '2026-01-01T08:00:00'
}

const mockSalaryHistory: SalaryHistory = {
  id: 1,
  employeeId: 1,
  baseSalary: 5000,
  effectiveFrom: '2024-01-01',
  effectiveTo: null,
  notes: null,
  createdAt: '2024-01-01T08:00:00'
}

const mockPaymentHistory: EmployeePaymentHistory = {
  type: EmployeePaymentType.PAYROLL_ENTRY,
  amount: 5000,
  date: '2026-02-28',
  reference: 'PAYROLL-1-2026-FEBRUARY',
  status: 'PAID',
  relatedEntityId: 9,
  createdAt: '2026-02-28T10:00:00'
}

const apiSuccess = <T>(data: T) => ({
  data: { success: true, data, message: null }
})

describe('useTreasuryEmployeeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ─── fetchEmployees ─────────────────────────────────────────────────────────

  it('fetchEmployees populates employees', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockEmployee]))
    const store = useTreasuryEmployeeStore()

    await store.fetchEmployees()

    expect(store.employees).toHaveLength(1)
    expect(store.employees[0]!.fullName).toBe('Ana Lopez')
  })

  it('fetchEmployees passes filters as query params', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([]))
    const store = useTreasuryEmployeeStore()

    await store.fetchEmployees({ type: 'PAYROLL', activeOnly: true, search: 'ana' })

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/employees',
      expect.objectContaining({
        params: expect.objectContaining({ type: 'PAYROLL', activeOnly: true, search: 'ana' })
      })
    )
  })

  it('fetchEmployees sets loading to false on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useTreasuryEmployeeStore()

    await expect(store.fetchEmployees()).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // ─── fetchEmployee ──────────────────────────────────────────────────────────

  it('fetchEmployee sets currentEmployee and returns it', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockEmployee))
    const store = useTreasuryEmployeeStore()

    const result = await store.fetchEmployee(1)

    expect(result).toEqual(mockEmployee)
    expect(store.currentEmployee).toEqual(mockEmployee)
  })

  it('fetchEmployee throws when employee not found', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Not found' }
    })
    const store = useTreasuryEmployeeStore()

    await expect(store.fetchEmployee(99)).rejects.toThrow('Not found')
  })

  // ─── createEmployee ─────────────────────────────────────────────────────────

  it('createEmployee posts data and returns created employee', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess(mockEmployee))
    const store = useTreasuryEmployeeStore()

    const result = await store.createEmployee({
      fullName: 'Ana Lopez',
      employeeType: EmployeeType.PAYROLL,
      baseSalary: 5000,
      hospitalCommissionPct: 0
    })

    expect(result).toEqual(mockEmployee)
    expect(mockedApi.post).toHaveBeenCalledWith('/v1/treasury/employees', expect.any(Object))
  })

  it('createEmployee throws on failure', async () => {
    mockedApi.post.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Bad request' }
    })
    const store = useTreasuryEmployeeStore()

    await expect(
      store.createEmployee({
        fullName: 'X',
        employeeType: EmployeeType.PAYROLL,
        hospitalCommissionPct: 0
      })
    ).rejects.toThrow('Bad request')
  })

  // ─── updateEmployee ─────────────────────────────────────────────────────────

  it('updateEmployee sends PUT and updates currentEmployee', async () => {
    const updated = { ...mockEmployee, fullName: 'Ana Garcia' }
    mockedApi.put.mockResolvedValueOnce(apiSuccess(updated))
    const store = useTreasuryEmployeeStore()

    const result = await store.updateEmployee(1, {
      fullName: 'Ana Garcia',
      hospitalCommissionPct: 0
    })

    expect(result.fullName).toBe('Ana Garcia')
    expect(store.currentEmployee?.fullName).toBe('Ana Garcia')
    expect(mockedApi.put).toHaveBeenCalledWith('/v1/treasury/employees/1', expect.any(Object))
  })

  // ─── updateSalary ───────────────────────────────────────────────────────────

  it('updateSalary sends PUT to salary endpoint', async () => {
    const updated = { ...mockEmployee, baseSalary: 6000 }
    mockedApi.put.mockResolvedValueOnce(apiSuccess(updated))
    const store = useTreasuryEmployeeStore()

    const result = await store.updateSalary(1, { newSalary: 6000, effectiveFrom: '2026-04-01' })

    expect(result.baseSalary).toBe(6000)
    expect(mockedApi.put).toHaveBeenCalledWith(
      '/v1/treasury/employees/1/salary',
      expect.any(Object)
    )
  })

  // ─── fetchSalaryHistory ─────────────────────────────────────────────────────

  it('fetchSalaryHistory populates salaryHistory', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockSalaryHistory]))
    const store = useTreasuryEmployeeStore()

    await store.fetchSalaryHistory(1)

    expect(store.salaryHistory).toHaveLength(1)
    expect(store.salaryHistory[0]!.baseSalary).toBe(5000)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/employees/1/salary-history')
  })

  // ─── generatePayroll ────────────────────────────────────────────────────────

  it('generatePayroll posts to generate endpoint and populates payrollEntries', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess([mockPayrollEntry]))
    const store = useTreasuryEmployeeStore()

    const result = await store.generatePayroll(1, { year: 2026 })

    expect(result).toHaveLength(1)
    expect(store.payrollEntries).toHaveLength(1)
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/employees/1/payroll/generate',
      expect.any(Object)
    )
  })

  // ─── fetchPayroll ───────────────────────────────────────────────────────────

  it('fetchPayroll populates payrollEntries', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockPayrollEntry]))
    const store = useTreasuryEmployeeStore()

    await store.fetchPayroll(1)

    expect(store.payrollEntries).toHaveLength(1)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/employees/1/payroll', { params: {} })
  })

  it('fetchPayroll passes year filter when provided', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockPayrollEntry]))
    const store = useTreasuryEmployeeStore()

    await store.fetchPayroll(1, 2026)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/employees/1/payroll', {
      params: { year: 2026 }
    })
  })

  // ─── payPayrollEntry ────────────────────────────────────────────────────────

  it('payPayrollEntry posts to pay endpoint and returns updated entry', async () => {
    const paid = { ...mockPayrollEntry, status: PayrollStatus.PAID, paidDate: '2026-03-31' }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(paid))
    const store = useTreasuryEmployeeStore()

    const result = await store.payPayrollEntry(10, {
      paymentDate: '2026-03-31',
      bankAccountId: 5
    })

    expect(result.status).toBe(PayrollStatus.PAID)
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/employees/payroll/10/pay',
      expect.any(Object)
    )
  })

  // ─── terminateEmployee ──────────────────────────────────────────────────────

  it('terminateEmployee posts to terminate endpoint and updates currentEmployee', async () => {
    const terminated = { ...mockEmployee, active: false, terminationDate: '2026-03-31' }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(terminated))
    const store = useTreasuryEmployeeStore()

    const result = await store.terminateEmployee(1, {
      terminationDate: '2026-03-31',
      cancelPendingPayroll: true
    })

    expect(result.active).toBe(false)
    expect(store.currentEmployee?.active).toBe(false)
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/employees/1/terminate',
      expect.any(Object)
    )
  })

  // ─── calculateIndemnizacion ─────────────────────────────────────────────────

  it('calculateIndemnizacion fetches and returns indemnización result', async () => {
    const indemnizacion = {
      employeeId: 1,
      employeeName: 'Ana Lopez',
      baseSalary: 5000,
      hireDate: '2024-01-01',
      daysWorked: 808,
      liability: 11068.49,
      asOfDate: '2026-03-18'
    }
    mockedApi.get.mockResolvedValueOnce(apiSuccess(indemnizacion))
    const store = useTreasuryEmployeeStore()

    const result = await store.calculateIndemnizacion(1)

    expect(result.employeeId).toBe(1)
    expect(result.daysWorked).toBe(808)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/employees/1/indemnizacion')
  })

  // ─── fetchPaymentHistory ────────────────────────────────────────────────────

  it('fetchPaymentHistory populates paymentHistory', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockPaymentHistory]))
    const store = useTreasuryEmployeeStore()

    await store.fetchPaymentHistory(1)

    expect(store.paymentHistory).toHaveLength(1)
    expect(store.paymentHistory[0]!.type).toBe(EmployeePaymentType.PAYROLL_ENTRY)
    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/employees/1/payment-history')
  })

  // ─── recordContractorPayment ────────────────────────────────────────────────

  it('recordContractorPayment posts to payments endpoint', async () => {
    const expense = {
      id: 50,
      supplierName: 'Pedro Contractor',
      category: 'PAYROLL',
      amount: 2000,
      expenseDate: '2026-03-15',
      invoiceNumber: 'INV-001'
    }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(expense))
    const store = useTreasuryEmployeeStore()

    const result = await store.recordContractorPayment(2, {
      amount: 2000,
      paymentDate: '2026-03-15',
      invoiceNumber: 'INV-001',
      bankAccountId: 5
    })

    expect(result).toEqual(expense)
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/employees/2/payments',
      expect.any(Object)
    )
  })

  // ─── clearCurrentEmployee ───────────────────────────────────────────────────

  it('clearCurrentEmployee resets all employee state', () => {
    const store = useTreasuryEmployeeStore()
    store.currentEmployee = mockEmployee
    store.payrollEntries = [mockPayrollEntry]
    store.salaryHistory = [mockSalaryHistory]
    store.paymentHistory = [mockPaymentHistory]

    store.clearCurrentEmployee()

    expect(store.currentEmployee).toBeNull()
    expect(store.payrollEntries).toHaveLength(0)
    expect(store.salaryHistory).toHaveLength(0)
    expect(store.paymentHistory).toHaveLength(0)
  })
})
