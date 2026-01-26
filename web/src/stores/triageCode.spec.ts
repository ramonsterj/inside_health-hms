import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTriageCodeStore } from './triageCode'
import api from '@/services/api'
import type { TriageCode } from '@/types/triageCode'

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

const mockTriageCode: TriageCode = {
  id: 1,
  code: 'A',
  color: '#FF0000',
  description: 'Critical - Immediate attention required',
  displayOrder: 1,
  createdAt: null,
  createdBy: null,
  updatedAt: null,
  updatedBy: null
}

const mockTriageCodes: TriageCode[] = [
  {
    id: 1,
    code: 'A',
    color: '#FF0000',
    description: 'Critical',
    displayOrder: 1,
    createdAt: null,
    createdBy: null,
    updatedAt: null,
    updatedBy: null
  },
  {
    id: 2,
    code: 'B',
    color: '#FFA500',
    description: 'Urgent',
    displayOrder: 2,
    createdAt: null,
    createdBy: null,
    updatedAt: null,
    updatedBy: null
  },
  {
    id: 3,
    code: 'C',
    color: '#FFFF00',
    description: 'Semi-urgent',
    displayOrder: 3,
    createdAt: null,
    createdBy: null,
    updatedAt: null,
    updatedBy: null
  },
  {
    id: 4,
    code: 'D',
    color: '#00FF00',
    description: 'Standard',
    displayOrder: 4,
    createdAt: null,
    createdBy: null,
    updatedAt: null,
    updatedBy: null
  },
  {
    id: 5,
    code: 'E',
    color: '#0000FF',
    description: 'Non-urgent',
    displayOrder: 5,
    createdAt: null,
    createdBy: null,
    updatedAt: null,
    updatedBy: null
  }
]

describe('useTriageCodeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useTriageCodeStore()

      expect(store.triageCodes).toEqual([])
      expect(store.currentTriageCode).toBeNull()
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchTriageCodes', () => {
    it('should fetch triage codes and update state', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockTriageCodes
        }
      })

      const store = useTriageCodeStore()
      await store.fetchTriageCodes()

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/triage-codes')
      expect(store.triageCodes).toHaveLength(5)
      expect(store.triageCodes[0]).toEqual(mockTriageCodes[0])
    })

    it('should return triage codes sorted by display order', async () => {
      // API should return sorted by displayOrder
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockTriageCodes
        }
      })

      const store = useTriageCodeStore()
      await store.fetchTriageCodes()

      // First should be A (displayOrder: 1), last should be E (displayOrder: 5)
      expect(store.triageCodes[0]!.code).toBe('A')
      expect(store.triageCodes[4]!.code).toBe('E')
    })

    it('should handle loading state', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })
      mockedApi.get.mockReturnValueOnce(promise as Promise<{ data: unknown }>)

      const store = useTriageCodeStore()
      const fetchPromise = store.fetchTriageCodes()

      expect(store.loading).toBe(true)

      resolvePromise({
        data: {
          success: true,
          data: []
        }
      })

      await fetchPromise
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchTriageCode', () => {
    it('should fetch a single triage code and set as current', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockTriageCode }
      })

      const store = useTriageCodeStore()
      const result = await store.fetchTriageCode(1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/triage-codes/1')
      expect(result).toEqual(mockTriageCode)
      expect(store.currentTriageCode).toEqual(mockTriageCode)
    })

    it('should throw error when triage code not found', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: false, message: 'Triage code not found' }
      })

      const store = useTriageCodeStore()

      await expect(store.fetchTriageCode(999)).rejects.toThrow('Triage code not found')
    })
  })

  describe('createTriageCode', () => {
    it('should create triage code and return data', async () => {
      const newTriageCode = { ...mockTriageCode, id: 6, code: 'X', displayOrder: 99 }
      mockedApi.post.mockResolvedValueOnce({
        data: { success: true, data: newTriageCode }
      })

      const store = useTriageCodeStore()
      const createData = {
        code: 'X',
        color: '#FF00FF',
        description: 'Test triage code',
        displayOrder: 99
      }

      const result = await store.createTriageCode(createData)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/triage-codes', createData)
      expect(result).toEqual(newTriageCode)
    })

    it('should throw error on duplicate code', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: false, message: 'Triage code with code A already exists' }
      })

      const store = useTriageCodeStore()
      const createData = {
        code: 'A',
        color: '#FF0000',
        description: 'Duplicate',
        displayOrder: 1
      }

      await expect(store.createTriageCode(createData)).rejects.toThrow(
        'Triage code with code A already exists'
      )
    })

    it('should validate hex color format', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: { success: false, message: 'Invalid color format' }
      })

      const store = useTriageCodeStore()
      const createData = {
        code: 'X',
        color: 'invalid',
        description: 'Test',
        displayOrder: 99
      }

      await expect(store.createTriageCode(createData)).rejects.toThrow('Invalid color format')
    })
  })

  describe('updateTriageCode', () => {
    it('should update triage code and update current state', async () => {
      const updatedTriageCode = { ...mockTriageCode, description: 'Updated description' }
      mockedApi.put.mockResolvedValueOnce({
        data: { success: true, data: updatedTriageCode }
      })

      const store = useTriageCodeStore()
      const updateData = {
        code: 'A',
        color: '#FF0000',
        description: 'Updated description',
        displayOrder: 1
      }

      const result = await store.updateTriageCode(1, updateData)

      expect(mockedApi.put).toHaveBeenCalledWith('/v1/triage-codes/1', updateData)
      expect(result.description).toBe('Updated description')
      expect(store.currentTriageCode?.description).toBe('Updated description')
    })

    it('should allow updating display order', async () => {
      const updatedTriageCode = { ...mockTriageCode, displayOrder: 10 }
      mockedApi.put.mockResolvedValueOnce({
        data: { success: true, data: updatedTriageCode }
      })

      const store = useTriageCodeStore()
      const updateData = {
        code: 'A',
        color: '#FF0000',
        description: 'Critical',
        displayOrder: 10
      }

      const result = await store.updateTriageCode(1, updateData)

      expect(result.displayOrder).toBe(10)
    })

    it('should throw error when triage code not found', async () => {
      mockedApi.put.mockResolvedValueOnce({
        data: { success: false, message: 'Triage code not found' }
      })

      const store = useTriageCodeStore()
      const updateData = {
        code: 'X',
        color: '#FF0000',
        description: 'Test',
        displayOrder: 1
      }

      await expect(store.updateTriageCode(999, updateData)).rejects.toThrow('Triage code not found')
    })
  })

  describe('deleteTriageCode', () => {
    it('should delete triage code', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true }
      })

      const store = useTriageCodeStore()
      await store.deleteTriageCode(5)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/triage-codes/5')
    })

    it('should throw error when triage code is in use', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Cannot delete triage code that is in use by active admissions'
        }
      })

      const store = useTriageCodeStore()

      await expect(store.deleteTriageCode(1)).rejects.toThrow(
        'Cannot delete triage code that is in use by active admissions'
      )
    })
  })

  describe('clearCurrentTriageCode', () => {
    it('should clear current triage code', () => {
      const store = useTriageCodeStore()
      store.currentTriageCode = mockTriageCode

      expect(store.currentTriageCode).not.toBeNull()

      store.clearCurrentTriageCode()

      expect(store.currentTriageCode).toBeNull()
    })
  })

  describe('color validation', () => {
    it('should accept valid hex colors', async () => {
      const validColors = ['#FF0000', '#00FF00', '#0000FF', '#FFFFFF', '#000000', '#AbCdEf']

      for (const color of validColors) {
        mockedApi.post.mockResolvedValueOnce({
          data: {
            success: true,
            data: { ...mockTriageCode, id: Math.random(), color }
          }
        })

        const store = useTriageCodeStore()
        const result = await store.createTriageCode({
          code: 'X',
          color,
          description: 'Test',
          displayOrder: 99
        })

        expect(result.color).toBe(color)
      }
    })
  })
})
