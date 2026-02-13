import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  MedicationAdministrationResponse,
  CreateMedicationAdministrationRequest
} from '@/types/medicationAdministration'
import type { ApiResponse, PageResponse } from '@/types'

export const useMedicationAdministrationStore = defineStore('medicationAdministration', () => {
  const administrations = ref<MedicationAdministrationResponse[]>([])
  const loading = ref(false)
  const totalElements = ref(0)
  const totalPages = ref(0)

  async function fetchAdministrations(
    admissionId: number,
    orderId: number,
    page = 0,
    size = 20
  ): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<
        ApiResponse<PageResponse<MedicationAdministrationResponse>>
      >(
        `/v1/admissions/${admissionId}/medical-orders/${orderId}/administrations`,
        { params: { page, size } }
      )
      if (response.data.success && response.data.data) {
        administrations.value = response.data.data.content
        totalElements.value = response.data.data.page.totalElements
        totalPages.value = response.data.data.page.totalPages
      }
    } finally {
      loading.value = false
    }
  }

  async function createAdministration(
    admissionId: number,
    orderId: number,
    data: CreateMedicationAdministrationRequest
  ): Promise<MedicationAdministrationResponse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<MedicationAdministrationResponse>>(
        `/v1/admissions/${admissionId}/medical-orders/${orderId}/administrations`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create administration failed')
    } finally {
      loading.value = false
    }
  }

  function clearState(): void {
    administrations.value = []
    totalElements.value = 0
    totalPages.value = 0
  }

  return {
    administrations,
    loading,
    totalElements,
    totalPages,
    fetchAdministrations,
    createAdministration,
    clearState
  }
})
