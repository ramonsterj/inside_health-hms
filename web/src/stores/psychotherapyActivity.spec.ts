import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePsychotherapyActivityStore } from './psychotherapyActivity'
import api from '@/services/api'
import type { PsychotherapyActivity } from '@/types/psychotherapy'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  delete: Mock
}

const mockActivity: PsychotherapyActivity = {
  id: 1,
  admissionId: 100,
  category: { id: 1, name: 'Taller' },
  description: 'Patient participated in art therapy workshop',
  createdAt: '2026-02-05T10:30:00Z',
  createdBy: {
    id: 10,
    salutation: 'LIC',
    firstName: 'Sofia',
    lastName: 'Martinez',
    roles: ['PSYCHOLOGIST']
  }
}

const mockActivities: PsychotherapyActivity[] = [
  {
    id: 3,
    admissionId: 100,
    category: { id: 2, name: 'Meditación guiada' },
    description: 'Evening guided meditation session',
    createdAt: '2026-02-05T18:00:00Z',
    createdBy: {
      id: 10,
      salutation: 'LIC',
      firstName: 'Sofia',
      lastName: 'Martinez',
      roles: ['PSYCHOLOGIST']
    }
  },
  {
    id: 2,
    admissionId: 100,
    category: { id: 1, name: 'Sesión individual' },
    description: 'Private session focusing on anxiety management',
    createdAt: '2026-02-05T14:00:00Z',
    createdBy: {
      id: 10,
      salutation: 'LIC',
      firstName: 'Sofia',
      lastName: 'Martinez',
      roles: ['PSYCHOLOGIST']
    }
  },
  {
    id: 1,
    admissionId: 100,
    category: { id: 1, name: 'Taller' },
    description: 'Patient participated in art therapy workshop',
    createdAt: '2026-02-05T10:30:00Z',
    createdBy: {
      id: 10,
      salutation: 'LIC',
      firstName: 'Sofia',
      lastName: 'Martinez',
      roles: ['PSYCHOLOGIST']
    }
  }
]

describe('usePsychotherapyActivityStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = usePsychotherapyActivityStore()

      expect(store.activities.size).toBe(0)
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchActivities', () => {
    it('should fetch activities for an admission with default desc sort', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockActivities
        }
      })

      const store = usePsychotherapyActivityStore()
      const result = await store.fetchActivities(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/psychotherapy-activities', {
        params: { sort: 'desc' }
      })
      expect(result).toHaveLength(3)
      expect(store.getActivities(100)).toHaveLength(3)
    })

    it('should fetch activities with asc sort', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: [...mockActivities].reverse()
        }
      })

      const store = usePsychotherapyActivityStore()
      await store.fetchActivities(100, 'asc')

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/psychotherapy-activities', {
        params: { sort: 'asc' }
      })
    })

    it('should handle loading state during fetch', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })

      mockedApi.get.mockReturnValueOnce(promise)

      const store = usePsychotherapyActivityStore()
      const fetchPromise = store.fetchActivities(100)

      expect(store.loading).toBe(true)

      resolvePromise({
        data: { success: true, data: mockActivities }
      })

      await fetchPromise
      expect(store.loading).toBe(false)
    })

    it('should return empty array on failure', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: false,
          data: null
        }
      })

      const store = usePsychotherapyActivityStore()
      const result = await store.fetchActivities(100)

      expect(result).toEqual([])
    })
  })

  describe('createActivity', () => {
    it('should create an activity and refresh the list', async () => {
      const createRequest = {
        categoryId: 1,
        description: 'New activity description'
      }

      mockedApi.post.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockActivity
        }
      })

      // Mock the subsequent fetch after creation
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockActivities
        }
      })

      const store = usePsychotherapyActivityStore()
      const result = await store.createActivity(100, createRequest)

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/v1/admissions/100/psychotherapy-activities',
        createRequest
      )
      expect(result.description).toBe('Patient participated in art therapy workshop')
    })

    it('should throw error on creation failure', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Only psychologists can register activities'
        }
      })

      const store = usePsychotherapyActivityStore()

      await expect(
        store.createActivity(100, { categoryId: 1, description: 'Test' })
      ).rejects.toThrow('Only psychologists can register activities')
    })
  })

  describe('deleteActivity', () => {
    it('should delete an activity and refresh the list', async () => {
      // First populate the store
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockActivities }
      })
      const store = usePsychotherapyActivityStore()
      await store.fetchActivities(100)

      // Now delete
      mockedApi.delete.mockResolvedValueOnce({
        data: { success: true }
      })

      // Mock the subsequent fetch after deletion
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockActivities.slice(1) // One less activity
        }
      })

      await store.deleteActivity(100, 3)

      expect(mockedApi.delete).toHaveBeenCalledWith('/v1/admissions/100/psychotherapy-activities/3')
    })

    it('should throw error on deletion failure', async () => {
      mockedApi.delete.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Only admins can delete activities'
        }
      })

      const store = usePsychotherapyActivityStore()

      await expect(store.deleteActivity(100, 1)).rejects.toThrow(
        'Only admins can delete activities'
      )
    })
  })

  describe('getActivities', () => {
    it('should return activities from cache for admission', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockActivities }
      })

      const store = usePsychotherapyActivityStore()
      await store.fetchActivities(100)

      const cached = store.getActivities(100)
      expect(cached).toHaveLength(3)
    })

    it('should return empty array for unfetched admission', () => {
      const store = usePsychotherapyActivityStore()
      const result = store.getActivities(999)
      expect(result).toEqual([])
    })
  })

  describe('clearActivities', () => {
    it('should clear activities for a specific admission', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockActivities }
      })

      const store = usePsychotherapyActivityStore()
      await store.fetchActivities(100)
      expect(store.getActivities(100)).toHaveLength(3)

      store.clearActivities(100)
      expect(store.getActivities(100)).toEqual([])
    })
  })

  describe('clearAll', () => {
    it('should clear all cached activities', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: { success: true, data: mockActivities }
      })

      const store = usePsychotherapyActivityStore()
      await store.fetchActivities(100)
      expect(store.activities.size).toBe(1)

      store.clearAll()
      expect(store.activities.size).toBe(0)
    })
  })
})
