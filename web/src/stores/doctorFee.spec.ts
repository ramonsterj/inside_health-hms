import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDoctorFeeStore } from './doctorFee'
import api from '@/services/api'
import {
  DoctorFeeStatus,
  DoctorFeeBillingType
} from '@/types/treasury'
import type {
  DoctorFee,
  DoctorFeeSummary
} from '@/types/treasury'

vi.mock('@/services/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() }
}))

const mockedApi = api as unknown as { get: Mock; post: Mock; put: Mock; delete: Mock }

const mockDoctorFee: DoctorFee = {
  id: 1,
  treasuryEmployeeId: 10,
  employeeName: 'Dr. Test',
  patientChargeId: null,
  billingType: DoctorFeeBillingType.HOSPITAL_BILLED,
  grossAmount: 1000,
  commissionPct: 15,
  netAmount: 850,
  status: DoctorFeeStatus.PENDING,
  doctorInvoiceNumber: null,
  invoiceDocumentPath: null,
  expenseId: null,
  feeDate: '2026-03-15',
  description: null,
  notes: null,
  createdAt: '2026-03-15T10:00:00',
  updatedAt: '2026-03-15T10:00:00'
}

const mockSummary: DoctorFeeSummary = {
  employeeId: 10,
  employeeName: 'Dr. Test',
  totalFees: 3,
  totalGross: 3000,
  totalNet: 2550,
  totalCommission: 450,
  pendingCount: 1,
  invoicedCount: 1,
  paidCount: 1
}

function apiSuccess<T>(data: T) {
  return { data: { success: true, data, message: null } }
}

describe('useDoctorFeeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ─── fetchDoctorFees ────────────────────────

  it('fetchDoctorFees populates doctorFees', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockDoctorFee]))
    const store = useDoctorFeeStore()

    await store.fetchDoctorFees(10)

    expect(store.doctorFees).toHaveLength(1)
    expect(store.doctorFees[0]!.grossAmount).toBe(1000)
  })

  it('fetchDoctorFees passes filter params', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([]))
    const store = useDoctorFeeStore()

    await store.fetchDoctorFees(10, { status: 'PENDING', from: '2026-03-01', to: '2026-03-31' })

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/employees/10/doctor-fees',
      expect.objectContaining({
        params: expect.objectContaining({ status: 'PENDING', from: '2026-03-01', to: '2026-03-31' })
      })
    )
  })

  it('fetchDoctorFees resets loading on error', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useDoctorFeeStore()

    await expect(store.fetchDoctorFees(10)).rejects.toThrow()
    expect(store.loading).toBe(false)
  })

  // ─── fetchDoctorFee ─────────────────────────

  it('fetchDoctorFee returns fee and sets currentDoctorFee', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockDoctorFee))
    const store = useDoctorFeeStore()

    const result = await store.fetchDoctorFee(10, 1)

    expect(result).toEqual(mockDoctorFee)
    expect(store.currentDoctorFee).toEqual(mockDoctorFee)
  })

  it('fetchDoctorFee throws when not found', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Not found' }
    })
    const store = useDoctorFeeStore()

    await expect(store.fetchDoctorFee(10, 99)).rejects.toThrow('Not found')
  })

  // ─── createDoctorFee ────────────────────────

  it('createDoctorFee returns created fee', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess(mockDoctorFee))
    const store = useDoctorFeeStore()

    const result = await store.createDoctorFee(10, {
      billingType: DoctorFeeBillingType.HOSPITAL_BILLED,
      grossAmount: 1000,
      feeDate: '2026-03-15'
    })

    expect(result).toEqual(mockDoctorFee)
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/employees/10/doctor-fees',
      expect.any(Object)
    )
  })

  it('createDoctorFee throws on failure', async () => {
    mockedApi.post.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Validation error' }
    })
    const store = useDoctorFeeStore()

    await expect(
      store.createDoctorFee(10, {
        billingType: DoctorFeeBillingType.HOSPITAL_BILLED,
        grossAmount: 1000,
        feeDate: '2026-03-15'
      })
    ).rejects.toThrow('Validation error')
  })

  // ─── updateDoctorFeeStatus ──────────────────

  it('updateDoctorFeeStatus returns updated fee', async () => {
    const invoiced = { ...mockDoctorFee, status: DoctorFeeStatus.INVOICED, doctorInvoiceNumber: 'INV-001' }
    mockedApi.put.mockResolvedValueOnce(apiSuccess(invoiced))
    const store = useDoctorFeeStore()

    const result = await store.updateDoctorFeeStatus(10, 1, {
      status: DoctorFeeStatus.INVOICED,
      doctorInvoiceNumber: 'INV-001'
    })

    expect(result.status).toBe(DoctorFeeStatus.INVOICED)
    expect(mockedApi.put).toHaveBeenCalledWith(
      '/v1/treasury/employees/10/doctor-fees/1/status',
      expect.any(Object)
    )
  })

  // ─── uploadInvoiceDocument ──────────────────

  it('uploadInvoiceDocument sends FormData and returns fee', async () => {
    const withDoc = { ...mockDoctorFee, invoiceDocumentPath: 'treasury/doctor-fees/1/uuid_invoice.pdf' }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(withDoc))
    const store = useDoctorFeeStore()
    const file = new File(['content'], 'invoice.pdf', { type: 'application/pdf' })

    const result = await store.uploadInvoiceDocument(10, 1, file)

    expect(result.invoiceDocumentPath).toBe('treasury/doctor-fees/1/uuid_invoice.pdf')
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/employees/10/doctor-fees/1/invoice-document',
      expect.any(FormData),
      expect.objectContaining({ headers: { 'Content-Type': 'multipart/form-data' } })
    )
  })

  // ─── settleDoctorFee ────────────────────────

  it('settleDoctorFee returns settled fee', async () => {
    const paid = { ...mockDoctorFee, status: DoctorFeeStatus.PAID, expenseId: 42 }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(paid))
    const store = useDoctorFeeStore()

    const result = await store.settleDoctorFee(10, 1, {
      bankAccountId: 5,
      paymentDate: '2026-03-20'
    })

    expect(result.status).toBe(DoctorFeeStatus.PAID)
    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/employees/10/doctor-fees/1/settle',
      expect.any(Object)
    )
  })

  // ─── deleteDoctorFee ────────────────────────

  it('deleteDoctorFee calls delete endpoint', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: { success: true, data: null } })
    const store = useDoctorFeeStore()

    await store.deleteDoctorFee(10, 1)

    expect(mockedApi.delete).toHaveBeenCalledWith('/v1/treasury/employees/10/doctor-fees/1')
  })

  it('deleteDoctorFee throws on failure', async () => {
    mockedApi.delete.mockResolvedValueOnce({
      data: { success: false, data: null, message: 'Cannot delete' }
    })
    const store = useDoctorFeeStore()

    await expect(store.deleteDoctorFee(10, 1)).rejects.toThrow('Cannot delete')
  })

  // ─── fetchSummary ───────────────────────────

  it('fetchSummary populates summary', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockSummary))
    const store = useDoctorFeeStore()

    await store.fetchSummary(10)

    expect(store.summary).toEqual(mockSummary)
    expect(store.summary!.totalFees).toBe(3)
    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/employees/10/doctor-fees/summary'
    )
  })

  // ─── clearState ─────────────────────────────

  it('clearState resets all state', async () => {
    mockedApi.get
      .mockResolvedValueOnce(apiSuccess([mockDoctorFee]))
      .mockResolvedValueOnce(apiSuccess(mockDoctorFee))
      .mockResolvedValueOnce(apiSuccess(mockSummary))
    const store = useDoctorFeeStore()
    await store.fetchDoctorFees(10)
    await store.fetchDoctorFee(10, 1)
    await store.fetchSummary(10)

    store.clearState()

    expect(store.doctorFees).toHaveLength(0)
    expect(store.currentDoctorFee).toBeNull()
    expect(store.summary).toBeNull()
  })
})
