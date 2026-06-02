import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  NursingNoteResponse,
  CreateNursingNoteRequest,
  UpdateNursingNoteRequest
} from '@/types/nursing'
import type { ApiResponse, PageResponse } from '@/types'

export const useNursingNoteStore = defineStore('nursingNote', () => {
  // State - Map by admissionId for caching lists
  const nursingNotes = ref<Map<number, NursingNoteResponse[]>>(new Map())
  const totalNotes = ref<Map<number, number>>(new Map())
  // Summary cache (hub metric only) — kept separate from the list map above so a lightweight
  // size=1 prefetch can never overwrite the full list a drilled-in view has loaded.
  const latestNote = ref<Map<number, NursingNoteResponse | null>>(new Map())
  const loading = ref(false)

  async function fetchNursingNotes(
    admissionId: number,
    page = 0,
    size = 20,
    sortDirection: 'ASC' | 'DESC' = 'DESC'
  ): Promise<NursingNoteResponse[]> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PageResponse<NursingNoteResponse>>>(
        `/v1/admissions/${admissionId}/nursing-notes`,
        {
          params: { page, size, sort: `createdAt,${sortDirection}` }
        }
      )
      if (response.data.success && response.data.data) {
        const notes = response.data.data.content
        nursingNotes.value.set(admissionId, notes)
        totalNotes.value.set(admissionId, response.data.data.page.totalElements)
        return notes
      }
      return []
    } finally {
      loading.value = false
    }
  }

  /**
   * Lightweight prefetch for the hub card metric: fetches only the total count and the single
   * most-recent note. Writes to the dedicated summary cache and total map — never the list map —
   * so it cannot truncate a full list loaded by the drilled-in view (regardless of resolution order).
   */
  async function fetchNursingNotesSummary(admissionId: number): Promise<void> {
    const response = await api.get<ApiResponse<PageResponse<NursingNoteResponse>>>(
      `/v1/admissions/${admissionId}/nursing-notes`,
      { params: { page: 0, size: 1, sort: 'createdAt,DESC' } }
    )
    if (response.data.success && response.data.data) {
      latestNote.value.set(admissionId, response.data.data.content[0] ?? null)
      totalNotes.value.set(admissionId, response.data.data.page.totalElements)
    }
  }

  async function fetchNursingNote(
    admissionId: number,
    noteId: number
  ): Promise<NursingNoteResponse> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<NursingNoteResponse>>(
        `/v1/admissions/${admissionId}/nursing-notes/${noteId}`
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Nursing note not found')
    } finally {
      loading.value = false
    }
  }

  async function createNursingNote(
    admissionId: number,
    data: CreateNursingNoteRequest
  ): Promise<NursingNoteResponse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<NursingNoteResponse>>(
        `/v1/admissions/${admissionId}/nursing-notes`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh the list to include the new note
        await fetchNursingNotes(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || 'Create nursing note failed')
    } finally {
      loading.value = false
    }
  }

  async function updateNursingNote(
    admissionId: number,
    noteId: number,
    data: UpdateNursingNoteRequest
  ): Promise<NursingNoteResponse> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<NursingNoteResponse>>(
        `/v1/admissions/${admissionId}/nursing-notes/${noteId}`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh the list to update the note
        await fetchNursingNotes(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || 'Update nursing note failed')
    } finally {
      loading.value = false
    }
  }

  function getNursingNotes(admissionId: number): NursingNoteResponse[] {
    return nursingNotes.value.get(admissionId) || []
  }

  function getTotalNotes(admissionId: number): number {
    return totalNotes.value.get(admissionId) || 0
  }

  // Most-recent note for the hub metric: prefer the live list when a drilled-in view has loaded it
  // (stays reactive to creates/edits), otherwise fall back to the summary cache.
  function getLatestNote(admissionId: number): NursingNoteResponse | null {
    const list = nursingNotes.value.get(admissionId)
    if (list !== undefined) return list[0] ?? null
    return latestNote.value.get(admissionId) ?? null
  }

  function clearNursingNotes(admissionId: number): void {
    nursingNotes.value.delete(admissionId)
    totalNotes.value.delete(admissionId)
    latestNote.value.delete(admissionId)
  }

  function clearAll(): void {
    nursingNotes.value.clear()
    totalNotes.value.clear()
    latestNote.value.clear()
  }

  return {
    nursingNotes,
    totalNotes,
    latestNote,
    loading,
    fetchNursingNotes,
    fetchNursingNotesSummary,
    fetchNursingNote,
    createNursingNote,
    updateNursingNote,
    getNursingNotes,
    getTotalNotes,
    getLatestNote,
    clearNursingNotes,
    clearAll
  }
})
