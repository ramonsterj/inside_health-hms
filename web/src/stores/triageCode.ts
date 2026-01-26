import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  TriageCode,
  CreateTriageCodeRequest,
  UpdateTriageCodeRequest
} from '@/types/triageCode'
import type { ApiResponse } from '@/types'

export const useTriageCodeStore = defineStore('triageCode', () => {
  const triageCodes = ref<TriageCode[]>([])
  const currentTriageCode = ref<TriageCode | null>(null)
  const loading = ref(false)

  async function fetchTriageCodes(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<TriageCode[]>>('/v1/triage-codes')
      if (response.data.success && response.data.data) {
        triageCodes.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchTriageCode(id: number): Promise<TriageCode> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<TriageCode>>(`/v1/triage-codes/${id}`)
      if (response.data.success && response.data.data) {
        currentTriageCode.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Triage code not found')
    } finally {
      loading.value = false
    }
  }

  async function createTriageCode(data: CreateTriageCodeRequest): Promise<TriageCode> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<TriageCode>>('/v1/triage-codes', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create triage code failed')
    } finally {
      loading.value = false
    }
  }

  async function updateTriageCode(id: number, data: UpdateTriageCodeRequest): Promise<TriageCode> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<TriageCode>>(`/v1/triage-codes/${id}`, data)
      if (response.data.success && response.data.data) {
        currentTriageCode.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Update triage code failed')
    } finally {
      loading.value = false
    }
  }

  async function deleteTriageCode(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/triage-codes/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete triage code failed')
      }
    } finally {
      loading.value = false
    }
  }

  function clearCurrentTriageCode(): void {
    currentTriageCode.value = null
  }

  return {
    triageCodes,
    currentTriageCode,
    loading,
    fetchTriageCodes,
    fetchTriageCode,
    createTriageCode,
    updateTriageCode,
    deleteTriageCode,
    clearCurrentTriageCode
  }
})
