import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  ProgressNoteResponse,
  CreateProgressNoteRequest,
  UpdateProgressNoteRequest
} from '@/types/medicalRecord'
import type { ApiResponse, PageResponse } from '@/types'

export const useProgressNoteStore = defineStore('progressNote', () => {
  // State - Map by admissionId for caching lists
  const progressNotes = ref<Map<number, ProgressNoteResponse[]>>(new Map())
  const totalNotes = ref<Map<number, number>>(new Map())
  const loading = ref(false)

  async function fetchProgressNotes(
    admissionId: number,
    page = 0,
    size = 10,
    sortDirection: 'ASC' | 'DESC' = 'DESC'
  ): Promise<ProgressNoteResponse[]> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PageResponse<ProgressNoteResponse>>>(
        `/v1/admissions/${admissionId}/progress-notes`,
        {
          params: { page, size, sort: `createdAt,${sortDirection}` }
        }
      )
      if (response.data.success && response.data.data) {
        const notes = response.data.data.content
        progressNotes.value.set(admissionId, notes)
        totalNotes.value.set(admissionId, response.data.data.page.totalElements)
        return notes
      }
      return []
    } finally {
      loading.value = false
    }
  }

  async function fetchProgressNote(
    admissionId: number,
    noteId: number
  ): Promise<ProgressNoteResponse> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<ProgressNoteResponse>>(
        `/v1/admissions/${admissionId}/progress-notes/${noteId}`
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Progress note not found')
    } finally {
      loading.value = false
    }
  }

  async function createProgressNote(
    admissionId: number,
    data: CreateProgressNoteRequest
  ): Promise<ProgressNoteResponse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<ProgressNoteResponse>>(
        `/v1/admissions/${admissionId}/progress-notes`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh the list to include the new note
        await fetchProgressNotes(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || 'Create progress note failed')
    } finally {
      loading.value = false
    }
  }

  async function updateProgressNote(
    admissionId: number,
    noteId: number,
    data: UpdateProgressNoteRequest
  ): Promise<ProgressNoteResponse> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<ProgressNoteResponse>>(
        `/v1/admissions/${admissionId}/progress-notes/${noteId}`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh the list to update the note
        await fetchProgressNotes(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || 'Update progress note failed')
    } finally {
      loading.value = false
    }
  }

  function getProgressNotes(admissionId: number): ProgressNoteResponse[] {
    return progressNotes.value.get(admissionId) || []
  }

  function getTotalNotes(admissionId: number): number {
    return totalNotes.value.get(admissionId) || 0
  }

  function clearProgressNotes(admissionId: number): void {
    progressNotes.value.delete(admissionId)
    totalNotes.value.delete(admissionId)
  }

  function clearAll(): void {
    progressNotes.value.clear()
    totalNotes.value.clear()
  }

  return {
    progressNotes,
    totalNotes,
    loading,
    fetchProgressNotes,
    fetchProgressNote,
    createProgressNote,
    updateProgressNote,
    getProgressNotes,
    getTotalNotes,
    clearProgressNotes,
    clearAll
  }
})
