import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useKardexStore } from './kardex'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  put: Mock
  delete: Mock
}

const mockSummary = {
  admissionId: 1,
  patientId: 10,
  patientName: 'Juan Perez',
  roomNumber: '101',
  triageCode: 'GREEN',
  triageColorCode: '#00FF00',
  admissionType: 'HOSPITALIZATION',
  admissionDate: '2026-03-14T10:00:00',
  daysAdmitted: 3,
  treatingPhysicianName: 'Dr. Maria Garcia',
  activeMedicationCount: 1,
  medications: [
    {
      orderId: 100,
      medication: 'Acetaminophen',
      dosage: '500mg',
      route: 'ORAL',
      frequency: 'Every 6 hours',
      schedule: '06:00, 12:00, 18:00, 00:00',
      inventoryItemId: null,
      inventoryItemName: null,
      observations: null,
      lastAdministration: null,
    },
  ],
  activeCareInstructionCount: 0,
  careInstructions: [],
  latestVitalSigns: {
    recordedAt: '2026-03-17T08:00:00',
    systolicBp: 120,
    diastolicBp: 80,
    heartRate: 72,
    respiratoryRate: 16,
    temperature: 36.5,
    oxygenSaturation: 98,
    recordedByName: 'Nurse Johnson',
  },
  hoursSinceLastVitals: 2.5,
  lastNursingNotePreview: 'Patient is stable',
  lastNursingNoteAt: '2026-03-17T07:00:00',
}

const mockSummary2 = {
  ...mockSummary,
  admissionId: 2,
  patientId: 20,
  patientName: 'Maria Lopez',
  roomNumber: '102',
}

function mockPageResponse(content: unknown[], totalElements: number) {
  return {
    data: {
      success: true,
      data: {
        content,
        page: {
          totalElements,
          totalPages: Math.ceil(totalElements / 20),
          size: 20,
          number: 0,
        },
      },
    },
  }
}

describe('useKardexStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useKardexStore()
      expect(store.summaries).toEqual([])
      expect(store.totalElements).toBe(0)
      expect(store.totalPages).toBe(0)
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchSummaries', () => {
    it('should fetch summaries and update state', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse([mockSummary, mockSummary2], 2))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/nursing-kardex', {
        params: { page: 0, size: 20, sort: 'room_id,asc' },
      })
      expect(store.summaries).toHaveLength(2)
      expect(store.totalElements).toBe(2)
      expect(store.loading).toBe(false)
    })

    it('should pass type and search filters', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse([mockSummary], 1))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20, 'HOSPITALIZATION', 'Juan')

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/nursing-kardex', {
        params: { page: 0, size: 20, sort: 'room_id,asc', type: 'HOSPITALIZATION', search: 'Juan' },
      })
      expect(store.summaries).toHaveLength(1)
    })

    it('should not pass null type or search', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse([], 0))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20, null, null)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/nursing-kardex', {
        params: { page: 0, size: 20, sort: 'room_id,asc' },
      })
    })

    it('should handle loading state', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })
      mockedApi.get.mockReturnValueOnce(promise)

      const store = useKardexStore()
      const fetchPromise = store.fetchSummaries(0, 20)

      expect(store.loading).toBe(true)

      resolvePromise(mockPageResponse([], 0))
      await fetchPromise

      expect(store.loading).toBe(false)
    })

    it('should set loading to false on error', async () => {
      mockedApi.get.mockRejectedValueOnce(new Error('Network error'))

      const store = useKardexStore()
      await expect(store.fetchSummaries(0, 20)).rejects.toThrow('Network error')
      expect(store.loading).toBe(false)
    })
  })

  describe('refreshSingleAdmission', () => {
    it('should update existing summary in place', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse([mockSummary, mockSummary2], 2))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20)

      const updatedSummary = { ...mockSummary, activeMedicationCount: 3 }
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: updatedSummary },
      })

      await store.refreshSingleAdmission(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/nursing-kardex/1')
      expect(store.summaries[0]!.activeMedicationCount).toBe(3)
      expect(store.summaries).toHaveLength(2)
    })

    it('should fall back to full refresh on error', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse([mockSummary], 1))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20)

      // Single refresh fails
      mockedApi.get.mockRejectedValueOnce(new Error('Not found'))
      // Full refresh succeeds
      mockedApi.get.mockResolvedValueOnce(mockPageResponse([mockSummary2], 1))

      await store.refreshSingleAdmission(1)

      // Should have called full refresh
      expect(mockedApi.get).toHaveBeenCalledTimes(3)
      expect(store.summaries[0]!.patientName).toBe('Maria Lopez')
    })
  })

  describe('auto-refresh', () => {
    it('should start auto-refresh timer', async () => {
      mockedApi.get.mockResolvedValue(mockPageResponse([], 0))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20)
      store.startAutoRefresh()

      // Advance 5 minutes
      await vi.advanceTimersByTimeAsync(5 * 60 * 1000)

      // Initial fetch + auto-refresh fetch
      expect(mockedApi.get).toHaveBeenCalledTimes(2)

      store.stopAutoRefresh()
    })

    it('should stop auto-refresh', async () => {
      mockedApi.get.mockResolvedValue(mockPageResponse([], 0))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20)
      store.startAutoRefresh()
      store.stopAutoRefresh()

      await vi.advanceTimersByTimeAsync(5 * 60 * 1000)

      // Only the initial fetch
      expect(mockedApi.get).toHaveBeenCalledTimes(1)
    })

    it('should clear previous timer when starting again', async () => {
      mockedApi.get.mockResolvedValue(mockPageResponse([], 0))

      const store = useKardexStore()
      await store.fetchSummaries(0, 20)
      store.startAutoRefresh()
      store.startAutoRefresh()

      await vi.advanceTimersByTimeAsync(5 * 60 * 1000)

      // Initial fetch + only one auto-refresh (not two)
      expect(mockedApi.get).toHaveBeenCalledTimes(2)

      store.stopAutoRefresh()
    })
  })
})
