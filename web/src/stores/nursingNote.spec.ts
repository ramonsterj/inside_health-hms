import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNursingNoteStore } from './nursingNote'
import api from '@/services/api'
import type { NursingNoteResponse } from '@/types/nursing'

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

const mockNursingNote: NursingNoteResponse = {
  id: 1,
  admissionId: 100,
  description: 'Patient resting comfortably. Vital signs stable.',
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

const mockNursingNotes: NursingNoteResponse[] = [
  {
    id: 3,
    admissionId: 100,
    description: 'Evening assessment: Patient sleeping well.',
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
    description: 'Afternoon check: Medication administered as prescribed.',
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
    description: 'Patient resting comfortably. Vital signs stable.',
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

describe('useNursingNoteStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('initial state', () => {
    it('should start with empty state', () => {
      const store = useNursingNoteStore()

      expect(store.nursingNotes.size).toBe(0)
      expect(store.totalNotes.size).toBe(0)
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchNursingNotes', () => {
    it('should fetch notes with pagination params', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            content: mockNursingNotes,
            page: { totalElements: 3 }
          }
        }
      })

      const store = useNursingNoteStore()
      const result = await store.fetchNursingNotes(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/nursing-notes', {
        params: { page: 0, size: 20, sort: 'createdAt,DESC' }
      })
      expect(result).toHaveLength(3)
      expect(store.getNursingNotes(100)).toHaveLength(3)
      expect(store.getTotalNotes(100)).toBe(3)
    })

    it('should support custom sort direction', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            content: [...mockNursingNotes].reverse(),
            page: { totalElements: 3 }
          }
        }
      })

      const store = useNursingNoteStore()
      await store.fetchNursingNotes(100, 0, 20, 'ASC')

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/nursing-notes', {
        params: { page: 0, size: 20, sort: 'createdAt,ASC' }
      })
    })

    it('should return empty array on API failure', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: false,
          data: null
        }
      })

      const store = useNursingNoteStore()
      const result = await store.fetchNursingNotes(100)

      expect(result).toEqual([])
    })

    it('should handle loading state during fetch', async () => {
      let resolvePromise: (value: unknown) => void = () => {}
      const promise = new Promise(resolve => {
        resolvePromise = resolve
      })

      mockedApi.get.mockReturnValueOnce(promise)

      const store = useNursingNoteStore()
      const fetchPromise = store.fetchNursingNotes(100)

      expect(store.loading).toBe(true)

      resolvePromise({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      await fetchPromise
      expect(store.loading).toBe(false)
    })

    it('should cache results in store by admissionId', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      const store = useNursingNoteStore()
      await store.fetchNursingNotes(100)

      expect(store.nursingNotes.size).toBe(1)
      expect(store.getNursingNotes(100)).toHaveLength(3)
      expect(store.getNursingNotes(999)).toEqual([])
    })
  })

  describe('fetchNursingNote', () => {
    it('should fetch single nursing note', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockNursingNote
        }
      })

      const store = useNursingNoteStore()
      const result = await store.fetchNursingNote(100, 1)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/nursing-notes/1')
      expect(result.id).toBe(1)
      expect(result.description).toBe('Patient resting comfortably. Vital signs stable.')
    })

    it('should throw error when not found', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Nursing note not found'
        }
      })

      const store = useNursingNoteStore()

      await expect(store.fetchNursingNote(100, 999)).rejects.toThrow('Nursing note not found')
    })
  })

  describe('createNursingNote', () => {
    it('should create note and refresh list', async () => {
      const createRequest = { description: 'New nursing note' }

      mockedApi.post.mockResolvedValueOnce({
        data: {
          success: true,
          data: mockNursingNote
        }
      })

      // Mock the subsequent fetch after creation
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      const store = useNursingNoteStore()
      const result = await store.createNursingNote(100, createRequest)

      expect(mockedApi.post).toHaveBeenCalledWith('/v1/admissions/100/nursing-notes', createRequest)
      expect(result.description).toBe('Patient resting comfortably. Vital signs stable.')
    })

    it('should throw error on creation failure', async () => {
      mockedApi.post.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'Cannot modify records for discharged admissions'
        }
      })

      const store = useNursingNoteStore()

      await expect(store.createNursingNote(100, { description: 'Test' })).rejects.toThrow(
        'Cannot modify records for discharged admissions'
      )
    })
  })

  describe('updateNursingNote', () => {
    it('should update note and refresh list', async () => {
      const updateRequest = { description: 'Updated nursing note' }

      mockedApi.put.mockResolvedValueOnce({
        data: {
          success: true,
          data: { ...mockNursingNote, description: 'Updated nursing note' }
        }
      })

      // Mock the subsequent fetch after update
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      const store = useNursingNoteStore()
      const result = await store.updateNursingNote(100, 1, updateRequest)

      expect(mockedApi.put).toHaveBeenCalledWith(
        '/v1/admissions/100/nursing-notes/1',
        updateRequest
      )
      expect(result.description).toBe('Updated nursing note')
    })

    it('should throw error on update failure', async () => {
      mockedApi.put.mockResolvedValueOnce({
        data: {
          success: false,
          message: 'This record can no longer be edited (24-hour limit exceeded)'
        }
      })

      const store = useNursingNoteStore()

      await expect(store.updateNursingNote(100, 1, { description: 'Test' })).rejects.toThrow(
        'This record can no longer be edited (24-hour limit exceeded)'
      )
    })
  })

  describe('getNursingNotes', () => {
    it('should return cached data for admission', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      const store = useNursingNoteStore()
      await store.fetchNursingNotes(100)

      const cached = store.getNursingNotes(100)
      expect(cached).toHaveLength(3)
    })

    it('should return empty array for unfetched admission', () => {
      const store = useNursingNoteStore()
      const result = store.getNursingNotes(999)
      expect(result).toEqual([])
    })
  })

  describe('getTotalNotes', () => {
    it('should return cached total or 0', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      const store = useNursingNoteStore()
      expect(store.getTotalNotes(100)).toBe(0)

      await store.fetchNursingNotes(100)
      expect(store.getTotalNotes(100)).toBe(3)
    })
  })

  describe('clearNursingNotes', () => {
    it('should remove data for specific admission', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      const store = useNursingNoteStore()
      await store.fetchNursingNotes(100)
      expect(store.getNursingNotes(100)).toHaveLength(3)

      store.clearNursingNotes(100)
      expect(store.getNursingNotes(100)).toEqual([])
      expect(store.getTotalNotes(100)).toBe(0)
    })
  })

  describe('clearAll', () => {
    it('should clear all cached data', async () => {
      mockedApi.get.mockResolvedValueOnce({
        data: {
          success: true,
          data: { content: mockNursingNotes, page: { totalElements: 3 } }
        }
      })

      const store = useNursingNoteStore()
      await store.fetchNursingNotes(100)
      expect(store.nursingNotes.size).toBe(1)

      store.clearAll()
      expect(store.nursingNotes.size).toBe(0)
      expect(store.totalNotes.size).toBe(0)
    })
  })
})
