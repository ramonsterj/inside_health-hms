import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBillingStore } from './billing'
import type { PatientCharge, AdmissionBalance, Invoice } from '@/types/billing'
import { ChargeType } from '@/types/billing'

vi.mock('@/services/api')

const mockCharge: PatientCharge = {
  id: 1,
  admissionId: 10,
  chargeType: ChargeType.SERVICE,
  description: 'Physical therapy session',
  quantity: 1,
  unitPrice: 150.0,
  totalAmount: 150.0,
  inventoryItemName: null,
  roomNumber: null,
  invoiced: false,
  reason: null,
  chargeDate: '2026-02-07',
  createdAt: '2026-02-07T14:30:00',
  createdByName: 'Admin User'
}

const mockBalance: AdmissionBalance = {
  admissionId: 10,
  patientName: 'Juan Perez',
  admissionDate: '2026-02-01',
  totalBalance: 575.0,
  dailyBreakdown: [
    {
      date: '2026-02-01',
      charges: [
        {
          id: 1,
          chargeType: ChargeType.ROOM,
          description: 'Room 101 - Daily Rate',
          quantity: 1,
          unitPrice: 500.0,
          totalAmount: 500.0
        },
        {
          id: 2,
          chargeType: ChargeType.MEDICATION,
          description: 'Amoxicillin',
          quantity: 3,
          unitPrice: 25.0,
          totalAmount: 75.0
        }
      ],
      dailyTotal: 575.0,
      cumulativeTotal: 575.0
    }
  ]
}

const mockInvoice: Invoice = {
  id: 1,
  invoiceNumber: 'INV-2026-0001',
  admissionId: 10,
  patientName: 'Juan Perez',
  admissionDate: '2026-02-01',
  dischargeDate: '2026-02-07',
  totalAmount: 3250.0,
  chargeCount: 18,
  chargeSummary: [
    { chargeType: ChargeType.ROOM, count: 6, subtotal: 3000.0 },
    { chargeType: ChargeType.MEDICATION, count: 10, subtotal: 350.0 },
    { chargeType: ChargeType.ADJUSTMENT, count: 2, subtotal: -100.0 }
  ],
  generatedAt: '2026-02-07T15:00:00',
  generatedByName: 'Admin User'
}

describe('Billing Store', () => {
  let store: ReturnType<typeof useBillingStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useBillingStore()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should have correct initial state', () => {
    expect(store.charges).toEqual([])
    expect(store.balance).toBeNull()
    expect(store.invoice).toBeNull()
    expect(store.loading).toBe(false)
  })

  describe('fetchCharges', () => {
    it('should fetch and store charges', async () => {
      const api = await import('@/services/api')
      vi.mocked(api.default.get).mockResolvedValueOnce({
        data: { success: true, data: [mockCharge] }
      })

      await store.fetchCharges(10)

      expect(store.charges).toHaveLength(1)
      expect(store.charges[0]!.chargeType).toBe(ChargeType.SERVICE)
      expect(api.default.get).toHaveBeenCalledWith('/v1/admissions/10/charges')
    })
  })

  describe('fetchBalance', () => {
    it('should fetch and store balance', async () => {
      const api = await import('@/services/api')
      vi.mocked(api.default.get).mockResolvedValueOnce({
        data: { success: true, data: mockBalance }
      })

      await store.fetchBalance(10)

      expect(store.balance).not.toBeNull()
      expect(store.balance?.totalBalance).toBe(575.0)
      expect(store.balance?.dailyBreakdown).toHaveLength(1)
    })
  })

  describe('createCharge', () => {
    it('should create charge and return result', async () => {
      const api = await import('@/services/api')
      vi.mocked(api.default.post).mockResolvedValueOnce({
        data: { success: true, data: mockCharge }
      })

      const result = await store.createCharge(10, {
        chargeType: ChargeType.SERVICE,
        description: 'Physical therapy session',
        quantity: 1,
        unitPrice: 150.0
      })

      expect(result.chargeType).toBe(ChargeType.SERVICE)
      expect(result.totalAmount).toBe(150.0)
    })
  })

  describe('createAdjustment', () => {
    it('should create adjustment and return result', async () => {
      const adjustmentCharge = {
        ...mockCharge,
        id: 2,
        chargeType: ChargeType.ADJUSTMENT,
        totalAmount: -75.0,
        reason: 'Duplicate charge'
      }

      const api = await import('@/services/api')
      vi.mocked(api.default.post).mockResolvedValueOnce({
        data: { success: true, data: adjustmentCharge }
      })

      const result = await store.createAdjustment(10, {
        description: 'Correction',
        amount: -75.0,
        reason: 'Duplicate charge'
      })

      expect(result.chargeType).toBe(ChargeType.ADJUSTMENT)
      expect(result.totalAmount).toBe(-75.0)
    })
  })

  describe('fetchInvoice', () => {
    it('should fetch and store invoice', async () => {
      const api = await import('@/services/api')
      vi.mocked(api.default.get).mockResolvedValueOnce({
        data: { success: true, data: mockInvoice }
      })

      await store.fetchInvoice(10)

      expect(store.invoice).not.toBeNull()
      expect(store.invoice?.invoiceNumber).toBe('INV-2026-0001')
      expect(store.invoice?.chargeSummary).toHaveLength(3)
    })
  })

  describe('generateInvoice', () => {
    it('should generate invoice and store result', async () => {
      const api = await import('@/services/api')
      vi.mocked(api.default.post).mockResolvedValueOnce({
        data: { success: true, data: mockInvoice }
      })

      const result = await store.generateInvoice(10)

      expect(result.invoiceNumber).toBe('INV-2026-0001')
      expect(store.invoice).not.toBeNull()
    })
  })

  describe('clearState', () => {
    it('should reset all state', async () => {
      store.charges = [mockCharge]
      store.balance = mockBalance
      store.invoice = mockInvoice

      store.clearState()

      expect(store.charges).toEqual([])
      expect(store.balance).toBeNull()
      expect(store.invoice).toBeNull()
    })
  })
})
