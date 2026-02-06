import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useVitalSignStore } from './vitalSign'
import api from '@/services/api'
import type { VitalSignResponse } from '@/types/nursing'

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

const mockVitalSign: VitalSignResponse = {
  id: 1,
  admissionId: 100,
  recordedAt: '2026-02-05T10:30:00Z',
  systolicBp: 120,
  diastolicBp: 80,
  heartRate: 72,
  respiratoryRate: 16,
  temperature: 36.5,
  oxygenSaturation: 98,
  other: null,
  createdAt: '2026-02-05T10:30:00Z',
  updatedAt: '2026-02-05T10:30:00Z',
  createdBy: {
    id: 10,
    salutation: 'LICDA',
    firstName: 'Ana',
    lastName: 'Lopez',
    roles: ['NURSE']
  },
  updatedBy: null,
  canEdit: true
}

const mockVitalSigns: VitalSignResponse[] = [
  {
    id: 3,
    admissionId: 100,
    recordedAt: '2026-02-05T22:00:00Z',
    systolicBp: 118,
    diastolicBp: 76,
    heartRate: 68,
    respiratoryRate: 15,
    temperature: 36.3,
    oxygenSaturation: 99,
    other: 'Evening reading',
    createdAt: '2026-02-05T22:00:00Z',
    updatedAt: '2026-02-05T22:00:00Z',
    createdBy: {
      id: 10,
      salutation: 'LICDA',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    },
    updatedBy: null,
    canEdit: true
  },
  {
    id: 2,
    admissionId: 100,
    recordedAt: '2026-02-05T14:00:00Z',
    systolicBp: 125,
    diastolicBp: 82,
    heartRate: 75,
    respiratoryRate: 18,
    temperature: 36.8,
    oxygenSaturation: 97,
    other: null,
    createdAt: '2026-02-05T14:00:00Z',
    updatedAt: '2026-02-05T14:00:00Z',
    createdBy: {
      id: 10,
      salutation: 'LICDA',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    },
    updatedBy: null,
    canEdit: true
  },
  {
    id: 1,
    admissionId: 100,
    recordedAt: '2026-02-05T10:30:00Z',
    systolicBp: 120,
    diastolicBp: 80,
    heartRate: 72,
    respiratoryRate: 16,
    temperature: 36.5,
    oxygenSaturation: 98,
    other: null,
    createdAt: '2026-02-05T10:30:00Z',
    updatedAt: '2026-02-05T10:30:00Z',
    createdBy: {
      id: 10,
      salutation: 'LICDA',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    },
    updatedBy: null,
    canEdit: true
  }
]

function mockPageResponse(content: VitalSignResponse[], totalElements: number) {
  return {
    data: {
      success: true,
      data: { content, page: { totalElements } }
    }
  }
}

function mockListResponse(data: VitalSignResponse[]) {
  return {
    data: {
      success: true,
      data
    }
  }
}

describe('useVitalSignStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useVitalSignStore()

      expect(store.vitalSigns.size).toBe(0)
      expect(store.totalVitalSigns.size).toBe(0)
      expect(store.chartData.size).toBe(0)
      expect(store.loading).toBe(false)
      expect(store.chartLoading).toBe(false)
      expect(store.dateRange).toEqual({ fromDate: null, toDate: null })
    })
  })

  describe('fetchVitalSigns', () => {
    it('should fetch vital signs with pagination params', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))

      const store = useVitalSignStore()
      const result = await store.fetchVitalSigns(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/vital-signs', {
        params: { page: 0, size: 20, sort: 'recordedAt,DESC' }
      })
      expect(result).toHaveLength(3)
      expect(store.getVitalSigns(100)).toHaveLength(3)
      expect(store.getTotalVitalSigns(100)).toBe(3)
    })

    it('should include date range filters when set', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))

      const store = useVitalSignStore()
      store.setDateRange({ fromDate: '2026-02-01', toDate: '2026-02-05' })
      await store.fetchVitalSigns(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/vital-signs', {
        params: {
          page: 0,
          size: 20,
          sort: 'recordedAt,DESC',
          fromDate: '2026-02-01',
          toDate: '2026-02-05'
        }
      })
    })

    it('should return empty array on API failure', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, data: null }
      })

      const store = useVitalSignStore()
      const result = await store.fetchVitalSigns(100)

      expect(result).toEqual([])
    })

    it('should handle loading state', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })

      mockedApi.get.mockReturnValueOnce(promise)

      const store = useVitalSignStore()
      const fetchPromise = store.fetchVitalSigns(100)

      expect(store.loading).toBe(true)

      resolvePromise(mockPageResponse(mockVitalSigns, 3))

      await fetchPromise
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchChartData', () => {
    it('should fetch chart data (non-paginated)', async () => {
      mockedApi.get.mockResolvedValueOnce(mockListResponse(mockVitalSigns))

      const store = useVitalSignStore()
      const result = await store.fetchChartData(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/vital-signs/chart', {
        params: {}
      })
      expect(result).toHaveLength(3)
      expect(store.getChartData(100)).toHaveLength(3)
    })

    it('should include date range params when set', async () => {
      mockedApi.get.mockResolvedValueOnce(mockListResponse(mockVitalSigns))

      const store = useVitalSignStore()
      store.setDateRange({ fromDate: '2026-02-01', toDate: '2026-02-05' })
      await store.fetchChartData(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/vital-signs/chart', {
        params: { fromDate: '2026-02-01', toDate: '2026-02-05' }
      })
    })

    it('should handle chartLoading state separately from loading', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })

      mockedApi.get.mockReturnValueOnce(promise)

      const store = useVitalSignStore()
      const fetchPromise = store.fetchChartData(100)

      expect(store.chartLoading).toBe(true)
      expect(store.loading).toBe(false)

      resolvePromise(mockListResponse(mockVitalSigns))

      await fetchPromise
      expect(store.chartLoading).toBe(false)
    })

    it('should return empty array on failure', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, data: null }
      })

      const store = useVitalSignStore()
      const result = await store.fetchChartData(100)

      expect(result).toEqual([])
    })
  })

  describe('fetchVitalSign', () => {
    it('should fetch single vital sign', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockVitalSign }
      })

      const store = useVitalSignStore()
      const result = await store.fetchVitalSign(100, 1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/vital-signs/1')
      expect(result.id).toBe(1)
      expect(result.systolicBp).toBe(120)
    })

    it('should throw error when not found', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, message: 'Vital sign not found' }
      })

      const store = useVitalSignStore()

      await expect(store.fetchVitalSign(100, 999)).rejects.toThrow('Vital sign not found')
    })
  })

  describe('createVitalSign', () => {
    it('should create vital sign and refresh both table and chart', async () => {
      const createRequest = {
        systolicBp: 120,
        diastolicBp: 80,
        heartRate: 72,
        respiratoryRate: 16,
        temperature: 36.5,
        oxygenSaturation: 98
      }

      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: mockVitalSign }
      })

      // Mock the subsequent fetches (table + chart) after creation
      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))
      mockedApi.get.mockResolvedValueOnce(mockListResponse(mockVitalSigns))

      const store = useVitalSignStore()
      const result = await store.createVitalSign(100, createRequest)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/admissions/100/vital-signs', createRequest)
      expect(result.systolicBp).toBe(120)
      // Verify both table and chart were refreshed
      expect(mockedApi.get).toHaveBeenCalledTimes(2)
    })

    it('should throw error on failure', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Cannot modify records for discharged admissions'
        }
      })

      const store = useVitalSignStore()

      await expect(
        store.createVitalSign(100, {
          systolicBp: 120,
          diastolicBp: 80,
          heartRate: 72,
          respiratoryRate: 16,
          temperature: 36.5,
          oxygenSaturation: 98
        })
      ).rejects.toThrow('Cannot modify records for discharged admissions')
    })
  })

  describe('updateVitalSign', () => {
    it('should update vital sign and refresh both table and chart', async () => {
      const updateRequest = {
        systolicBp: 130,
        diastolicBp: 85,
        heartRate: 78,
        respiratoryRate: 18,
        temperature: 36.7,
        oxygenSaturation: 97
      }

      mockedApi.put.mockResolvedValueOnce({
        data: { success: true, data: { ...mockVitalSign, ...updateRequest } }
      })

      // Mock the subsequent fetches (table + chart) after update
      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))
      mockedApi.get.mockResolvedValueOnce(mockListResponse(mockVitalSigns))

      const store = useVitalSignStore()
      const result = await store.updateVitalSign(100, 1, updateRequest)

      expect(mockedApi.put).toHaveBeenCalledWith('/v1/admissions/100/vital-signs/1', updateRequest)
      expect(result.systolicBp).toBe(130)
      // Verify both table and chart were refreshed
      expect(mockedApi.get).toHaveBeenCalledTimes(2)
    })

    it('should throw error on failure', async () => {
      mockedApi.put.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'This record can no longer be edited (24-hour limit exceeded)'
        }
      })

      const store = useVitalSignStore()

      await expect(
        store.updateVitalSign(100, 1, {
          systolicBp: 120,
          diastolicBp: 80,
          heartRate: 72,
          respiratoryRate: 16,
          temperature: 36.5,
          oxygenSaturation: 98
        })
      ).rejects.toThrow('This record can no longer be edited (24-hour limit exceeded)')
    })
  })

  describe('date range', () => {
    it('setDateRange updates state', () => {
      const store = useVitalSignStore()
      store.setDateRange({ fromDate: '2026-02-01', toDate: '2026-02-05' })

      expect(store.dateRange).toEqual({ fromDate: '2026-02-01', toDate: '2026-02-05' })
    })

    it('clearDateRange resets to null values', () => {
      const store = useVitalSignStore()
      store.setDateRange({ fromDate: '2026-02-01', toDate: '2026-02-05' })
      store.clearDateRange()

      expect(store.dateRange).toEqual({ fromDate: null, toDate: null })
    })

    it('date range filters are included in subsequent fetches', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse([], 0))

      const store = useVitalSignStore()
      store.setDateRange({ fromDate: '2026-02-01', toDate: null })
      await store.fetchVitalSigns(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/vital-signs', {
        params: {
          page: 0,
          size: 20,
          sort: 'recordedAt,DESC',
          fromDate: '2026-02-01'
        }
      })
    })
  })

  describe('getters and state', () => {
    it('getVitalSigns returns cached data or empty array', async () => {
      const store = useVitalSignStore()
      expect(store.getVitalSigns(100)).toEqual([])

      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))
      await store.fetchVitalSigns(100)

      expect(store.getVitalSigns(100)).toHaveLength(3)
    })

    it('getTotalVitalSigns returns cached total or 0', async () => {
      const store = useVitalSignStore()
      expect(store.getTotalVitalSigns(100)).toBe(0)

      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))
      await store.fetchVitalSigns(100)

      expect(store.getTotalVitalSigns(100)).toBe(3)
    })

    it('getChartData returns cached chart data or empty array', async () => {
      const store = useVitalSignStore()
      expect(store.getChartData(100)).toEqual([])

      mockedApi.get.mockResolvedValueOnce(mockListResponse(mockVitalSigns))
      await store.fetchChartData(100)

      expect(store.getChartData(100)).toHaveLength(3)
    })
  })

  describe('clearVitalSigns', () => {
    it('should remove all data for specific admission', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))
      mockedApi.get.mockResolvedValueOnce(mockListResponse(mockVitalSigns))

      const store = useVitalSignStore()
      await store.fetchVitalSigns(100)
      await store.fetchChartData(100)

      expect(store.getVitalSigns(100)).toHaveLength(3)
      expect(store.getChartData(100)).toHaveLength(3)

      store.clearVitalSigns(100)
      expect(store.getVitalSigns(100)).toEqual([])
      expect(store.getTotalVitalSigns(100)).toBe(0)
      expect(store.getChartData(100)).toEqual([])
    })
  })

  describe('clearAll', () => {
    it('should clear everything including date range', async () => {
      mockedApi.get.mockResolvedValueOnce(mockPageResponse(mockVitalSigns, 3))
      mockedApi.get.mockResolvedValueOnce(mockListResponse(mockVitalSigns))

      const store = useVitalSignStore()
      await store.fetchVitalSigns(100)
      await store.fetchChartData(100)
      store.setDateRange({ fromDate: '2026-02-01', toDate: '2026-02-05' })

      store.clearAll()
      expect(store.vitalSigns.size).toBe(0)
      expect(store.totalVitalSigns.size).toBe(0)
      expect(store.chartData.size).toBe(0)
      expect(store.dateRange).toEqual({ fromDate: null, toDate: null })
    })
  })
})
