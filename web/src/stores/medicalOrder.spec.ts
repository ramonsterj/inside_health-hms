import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMedicalOrderStore } from './medicalOrder'
import api from '@/services/api'
import {
  EmergencyAuthorizationReason,
  MedicalOrderCategory,
  MedicalOrderStatus
} from '@/types/medicalRecord'
import type { MedicalOrderResponse, MedicalOrderListItemResponse } from '@/types/medicalRecord'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  put: Mock
}

function makeOrder(
  overrides: Partial<MedicalOrderResponse> = {}
): MedicalOrderResponse {
  return {
    id: 1,
    admissionId: 100,
    category: MedicalOrderCategory.LABORATORIOS,
    startDate: '2026-04-27',
    endDate: null,
    medication: null,
    dosage: null,
    route: null,
    frequency: null,
    schedule: null,
    observations: 'Blood work',
    status: MedicalOrderStatus.SOLICITADO,
    authorizedAt: null,
    authorizedBy: null,
    inProgressAt: null,
    inProgressBy: null,
    resultsReceivedAt: null,
    resultsReceivedBy: null,
    rejectionReason: null,
    emergencyAuthorized: false,
    emergencyReason: null,
    emergencyReasonNote: null,
    emergencyAt: null,
    emergencyBy: null,
    discontinuedAt: null,
    discontinuedBy: null,
    inventoryItemId: null,
    inventoryItemName: null,
    documentCount: 0,
    createdAt: '2026-04-27T10:00:00Z',
    updatedAt: '2026-04-27T10:00:00Z',
    createdBy: null,
    updatedBy: null,
    ...overrides
  }
}

function makeListItem(
  overrides: Partial<MedicalOrderListItemResponse> = {}
): MedicalOrderListItemResponse {
  return {
    id: 1,
    admissionId: 100,
    patientId: 5,
    patientFirstName: 'Juan',
    patientLastName: 'Pérez',
    category: MedicalOrderCategory.LABORATORIOS,
    status: MedicalOrderStatus.SOLICITADO,
    startDate: '2026-04-27',
    summary: 'Hemograma',
    medication: null,
    dosage: null,
    createdAt: '2026-04-27T10:00:00Z',
    createdBy: null,
    authorizedAt: null,
    inProgressAt: null,
    resultsReceivedAt: null,
    discontinuedAt: null,
    emergencyAuthorized: false,
    documentCount: 0,
    ...overrides
  }
}

function successResponse<T>(data: T) {
  return {
    data: { success: true, data, message: null, timestamp: '2026-04-27T10:00:00Z' }
  }
}

describe('useMedicalOrderStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // Default refresh after a transition fetches the per-admission grouped orders
    mockedApi.get.mockResolvedValue(
      successResponse({ orders: { LABORATORIOS: [makeOrder({ status: MedicalOrderStatus.AUTORIZADO })] } })
    )
  })

  describe('authorizeMedicalOrder', () => {
    it('POSTs to the authorize endpoint and refreshes the cache', async () => {
      const store = useMedicalOrderStore()
      const authorized = makeOrder({
        status: MedicalOrderStatus.AUTORIZADO,
        authorizedAt: '2026-04-27T11:00:00Z'
      })
      mockedApi.post.mockResolvedValueOnce(successResponse(authorized))

      const result = await store.authorizeMedicalOrder(100, 1)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/1/authorize',
        undefined
      )
      expect(result.status).toBe(MedicalOrderStatus.AUTORIZADO)
      // Cache refresh
      expect(mockedApi.get).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders',
        { params: {} }
      )
    })
  })

  describe('rejectMedicalOrder', () => {
    it('POSTs the optional reason payload', async () => {
      const store = useMedicalOrderStore()
      const rejected = makeOrder({
        status: MedicalOrderStatus.NO_AUTORIZADO,
        rejectionReason: 'Not covered'
      })
      mockedApi.post.mockResolvedValueOnce(successResponse(rejected))

      const result = await store.rejectMedicalOrder(100, 1, { reason: 'Not covered' })

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/1/reject',
        { reason: 'Not covered' }
      )
      expect(result.rejectionReason).toBe('Not covered')
    })

    it('sends an empty body when no reason provided', async () => {
      const store = useMedicalOrderStore()
      mockedApi.post.mockResolvedValueOnce(
        successResponse(makeOrder({ status: MedicalOrderStatus.NO_AUTORIZADO }))
      )

      await store.rejectMedicalOrder(100, 1)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/1/reject',
        {}
      )
    })
  })

  describe('markInProgress', () => {
    it('POSTs to the mark-in-progress endpoint', async () => {
      const store = useMedicalOrderStore()
      mockedApi.post.mockResolvedValueOnce(
        successResponse(makeOrder({ status: MedicalOrderStatus.EN_PROCESO }))
      )

      const result = await store.markInProgress(100, 1)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/1/mark-in-progress',
        undefined
      )
      expect(result.status).toBe(MedicalOrderStatus.EN_PROCESO)
    })
  })

  describe('emergencyAuthorize', () => {
    it('POSTs to the emergency-authorize endpoint with reason payload', async () => {
      const store = useMedicalOrderStore()
      mockedApi.post.mockResolvedValueOnce(
        successResponse(makeOrder({ status: MedicalOrderStatus.AUTORIZADO }))
      )

      await store.emergencyAuthorize(100, 1, {
        reason: EmergencyAuthorizationReason.PATIENT_IN_CRISIS,
        reasonNote: null
      })

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/1/emergency-authorize',
        { reason: EmergencyAuthorizationReason.PATIENT_IN_CRISIS, reasonNote: null }
      )
    })
  })

  describe('discontinueMedicalOrder', () => {
    it('POSTs to the discontinue endpoint', async () => {
      const store = useMedicalOrderStore()
      mockedApi.post.mockResolvedValueOnce(
        successResponse(makeOrder({ status: MedicalOrderStatus.DESCONTINUADO }))
      )

      const result = await store.discontinueMedicalOrder(100, 1)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/medical-orders/1/discontinue',
        undefined
      )
      expect(result.status).toBe(MedicalOrderStatus.DESCONTINUADO)
    })
  })

  describe('fetchOrdersByStatus', () => {
    it('GETs the cross-admission listing with multi-value status filter', async () => {
      const store = useMedicalOrderStore()
      const pageResponse = {
        content: [makeListItem(), makeListItem({ id: 2 })],
        page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
      }
      mockedApi.get.mockResolvedValueOnce(successResponse(pageResponse))

      const result = await store.fetchOrdersByStatus({
        status: [MedicalOrderStatus.SOLICITADO, MedicalOrderStatus.AUTORIZADO],
        category: [MedicalOrderCategory.LABORATORIOS],
        page: 0,
        size: 20
      })

      const calledUrl = mockedApi.get.mock.calls[0]![0] as string
      expect(calledUrl).toContain('/v1/medical-orders?')
      expect(calledUrl).toContain('status=SOLICITADO')
      expect(calledUrl).toContain('status=AUTORIZADO')
      expect(calledUrl).toContain('category=LABORATORIOS')
      expect(calledUrl).toContain('page=0')
      expect(calledUrl).toContain('size=20')
      expect(result.content).toHaveLength(2)
    })

    it('omits the status param when no filter is set', async () => {
      const store = useMedicalOrderStore()
      mockedApi.get.mockResolvedValueOnce(
        successResponse({
          content: [],
          page: { totalElements: 0, totalPages: 0, size: 20, number: 0 }
        })
      )

      await store.fetchOrdersByStatus({ page: 0, size: 20 })

      const calledUrl = mockedApi.get.mock.calls[0]![0] as string
      expect(calledUrl).not.toContain('status=')
      expect(calledUrl).not.toContain('category=')
    })
  })
})
