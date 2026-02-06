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

  function clearNursingNotes(admissionId: number): void {
    nursingNotes.value.delete(admissionId)
    totalNotes.value.delete(admissionId)
  }

  function clearAll(): void {
    nursingNotes.value.clear()
    totalNotes.value.clear()
  }

  return {
    nursingNotes,
    totalNotes,
    loading,
    fetchNursingNotes,
    fetchNursingNote,
    createNursingNote,
    updateNursingNote,
    getNursingNotes,
    getTotalNotes,
    clearNursingNotes,
    clearAll
  }
})
