import { describe, it, expect, beforeEach, vi, afterEach, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProgressNoteStore } from './progressNote'
import api from '@/services/api'
import type { ProgressNoteResponse } from '@/types/medicalRecord'

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

const mockProgressNotes: ProgressNoteResponse[] = [
  { id: 3, admissionId: 100, createdAt: '2026-02-05T22:00:00Z' } as ProgressNoteResponse,
  { id: 2, admissionId: 100, createdAt: '2026-02-05T14:00:00Z' } as ProgressNoteResponse,
  { id: 1, admissionId: 100, createdAt: '2026-02-05T10:30:00Z' } as ProgressNoteResponse
]

function pageResponse(content: ProgressNoteResponse[], totalElements: number) {
  return { data: { success: true, data: { content, page: { totalElements } } } }
}

describe('useProgressNoteStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.resetAllMocks()
  })

  describe('fetchProgressNotes', () => {
    it('should cache the full list and total by admissionId', async () => {
      mockedApi.get.mockResolvedValueOnce(pageResponse(mockProgressNotes, 3))

      const store = useProgressNoteStore()
      const result = await store.fetchProgressNotes(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/progress-notes', {
        params: { page: 0, size: 10, sort: 'createdAt,DESC' }
      })
      expect(result).toHaveLength(3)
      expect(store.getProgressNotes(100)).toHaveLength(3)
      expect(store.getTotalNotes(100)).toBe(3)
    })
  })

  describe('fetchProgressNotesSummary', () => {
    it('should fetch only the latest note (size=1) and set total + summary cache', async () => {
      mockedApi.get.mockResolvedValueOnce(pageResponse([mockProgressNotes[0]!], 3))

      const store = useProgressNoteStore()
      await store.fetchProgressNotesSummary(100)

      expect(mockedApi.get).toHaveBeenCalledWith('/v1/admissions/100/progress-notes', {
        params: { page: 0, size: 1, sort: 'createdAt,DESC' }
      })
      expect(store.getTotalNotes(100)).toBe(3)
      expect(store.getLatestNote(100)?.id).toBe(3)
      // List map is left untouched so a drilled-in view's full list is never clobbered.
      expect(store.progressNotes.has(100)).toBe(false)
    })

    it('should not truncate a full list when it resolves after the list fetch', async () => {
      const store = useProgressNoteStore()

      // Full list loaded first by the drilled-in view.
      mockedApi.get.mockResolvedValueOnce(pageResponse(mockProgressNotes, 3))
      await store.fetchProgressNotes(100)
      expect(store.getProgressNotes(100)).toHaveLength(3)

      // Late-resolving size=1 summary prefetch must not shrink the list.
      mockedApi.get.mockResolvedValueOnce(pageResponse([mockProgressNotes[0]!], 3))
      await store.fetchProgressNotesSummary(100)

      expect(store.getProgressNotes(100)).toHaveLength(3)
      // getLatestNote prefers the live list when present.
      expect(store.getLatestNote(100)?.id).toBe(3)
    })

    it('should store null latest when there are no notes', async () => {
      mockedApi.get.mockResolvedValueOnce(pageResponse([], 0))

      const store = useProgressNoteStore()
      await store.fetchProgressNotesSummary(100)

      expect(store.getTotalNotes(100)).toBe(0)
      expect(store.getLatestNote(100)).toBeNull()
    })
  })

  describe('getLatestNote', () => {
    it('should return null for an unfetched admission', () => {
      const store = useProgressNoteStore()
      expect(store.getLatestNote(999)).toBeNull()
    })
  })

  describe('clearAll', () => {
    it('should clear the summary cache too', async () => {
      mockedApi.get.mockResolvedValueOnce(pageResponse([mockProgressNotes[0]!], 3))

      const store = useProgressNoteStore()
      await store.fetchProgressNotesSummary(100)
      expect(store.latestNote.size).toBe(1)

      store.clearAll()
      expect(store.latestNote.size).toBe(0)
      expect(store.totalNotes.size).toBe(0)
    })
  })
})
