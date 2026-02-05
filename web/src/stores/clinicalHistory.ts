import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  ClinicalHistoryResponse,
  CreateClinicalHistoryRequest,
  UpdateClinicalHistoryRequest
} from '@/types/medicalRecord'
import type { ApiResponse } from '@/types'

export const useClinicalHistoryStore = defineStore('clinicalHistory', () => {
  // State - Map by admissionId for caching
  const clinicalHistories = ref<Map<number, ClinicalHistoryResponse>>(new Map())
  const loading = ref(false)

  async function fetchClinicalHistory(
    admissionId: number
  ): Promise<ClinicalHistoryResponse | null> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<ClinicalHistoryResponse>>(
        `/v1/admissions/${admissionId}/clinical-history`
      )
      if (response.data.success && response.data.data) {
        clinicalHistories.value.set(admissionId, response.data.data)
        return response.data.data
      }
      return null
    } finally {
      loading.value = false
    }
  }

  async function createClinicalHistory(
    admissionId: number,
    data: CreateClinicalHistoryRequest
  ): Promise<ClinicalHistoryResponse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<ClinicalHistoryResponse>>(
        `/v1/admissions/${admissionId}/clinical-history`,
        data
      )
      if (response.data.success && response.data.data) {
        clinicalHistories.value.set(admissionId, response.data.data)
        return response.data.data
      }
      throw new Error(response.data.message || 'Create clinical history failed')
    } finally {
      loading.value = false
    }
  }

  async function updateClinicalHistory(
    admissionId: number,
    data: UpdateClinicalHistoryRequest
  ): Promise<ClinicalHistoryResponse> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<ClinicalHistoryResponse>>(
        `/v1/admissions/${admissionId}/clinical-history`,
        data
      )
      if (response.data.success && response.data.data) {
        clinicalHistories.value.set(admissionId, response.data.data)
        return response.data.data
      }
      throw new Error(response.data.message || 'Update clinical history failed')
    } finally {
      loading.value = false
    }
  }

  function getClinicalHistory(admissionId: number): ClinicalHistoryResponse | undefined {
    return clinicalHistories.value.get(admissionId)
  }

  function clearClinicalHistory(admissionId: number): void {
    clinicalHistories.value.delete(admissionId)
  }

  function clearAll(): void {
    clinicalHistories.value.clear()
  }

  return {
    clinicalHistories,
    loading,
    fetchClinicalHistory,
    createClinicalHistory,
    updateClinicalHistory,
    getClinicalHistory,
    clearClinicalHistory,
    clearAll
  }
})
