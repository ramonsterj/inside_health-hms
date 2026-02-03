import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  AdmissionListItem,
  AdmissionDetail,
  CreateAdmissionRequest,
  UpdateAdmissionRequest,
  AdmissionStatus,
  AdmissionType,
  Doctor,
  ConsultingPhysician,
  AddConsultingPhysicianRequest
} from '@/types/admission'
import type { PatientSummary, ApiResponse, PageResponse } from '@/types'

export const useAdmissionStore = defineStore('admission', () => {
  const admissions = ref<AdmissionListItem[]>([])
  const totalAdmissions = ref(0)
  const currentAdmission = ref<AdmissionDetail | null>(null)
  const doctors = ref<Doctor[]>([])
  const loading = ref(false)

  async function fetchAdmissions(
    page = 0,
    size = 20,
    status: AdmissionStatus | null = null,
    type: AdmissionType | null = null
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (status) {
        params.status = status
      }
      if (type) {
        params.type = type
      }
      const response = await api.get<ApiResponse<PageResponse<AdmissionListItem>>>(
        '/v1/admissions',
        {
          params
        }
      )
      if (response.data.success && response.data.data) {
        admissions.value = response.data.data.content
        totalAdmissions.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchAdmission(id: number): Promise<AdmissionDetail> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<AdmissionDetail>>(`/v1/admissions/${id}`)
      if (response.data.success && response.data.data) {
        currentAdmission.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Admission not found')
    } finally {
      loading.value = false
    }
  }

  async function createAdmission(data: CreateAdmissionRequest): Promise<AdmissionDetail> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<AdmissionDetail>>('/v1/admissions', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create admission failed')
    } finally {
      loading.value = false
    }
  }

  async function updateAdmission(
    id: number,
    data: UpdateAdmissionRequest
  ): Promise<AdmissionDetail> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<AdmissionDetail>>(`/v1/admissions/${id}`, data)
      if (response.data.success && response.data.data) {
        currentAdmission.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Update admission failed')
    } finally {
      loading.value = false
    }
  }

  async function dischargePatient(id: number): Promise<AdmissionDetail> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<AdmissionDetail>>(
        `/v1/admissions/${id}/discharge`
      )
      if (response.data.success && response.data.data) {
        currentAdmission.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Discharge failed')
    } finally {
      loading.value = false
    }
  }

  async function deleteAdmission(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/admissions/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete admission failed')
      }
    } finally {
      loading.value = false
    }
  }

  async function uploadConsentDocument(admissionId: number, file: File): Promise<AdmissionDetail> {
    loading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await api.post<ApiResponse<AdmissionDetail>>(
        `/v1/admissions/${admissionId}/consent`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )
      if (response.data.success && response.data.data) {
        currentAdmission.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Upload consent document failed')
    } finally {
      loading.value = false
    }
  }

  async function downloadConsentDocument(admissionId: number): Promise<Blob> {
    loading.value = true
    try {
      const response = await api.get(`/v1/admissions/${admissionId}/consent`, {
        responseType: 'blob'
      })
      return response.data
    } finally {
      loading.value = false
    }
  }

  async function searchPatients(query: string): Promise<PatientSummary[]> {
    if (!query.trim()) return []
    try {
      const response = await api.get<ApiResponse<PatientSummary[]>>(
        '/v1/admissions/patients/search',
        {
          params: { q: query }
        }
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      return []
    } catch {
      return []
    }
  }

  async function fetchPatientSummary(patientId: number): Promise<PatientSummary | null> {
    try {
      const response = await api.get<ApiResponse<PatientSummary>>(
        `/v1/admissions/patients/${patientId}`
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      return null
    } catch {
      return null
    }
  }

  async function fetchDoctors(): Promise<void> {
    try {
      const response = await api.get<ApiResponse<Doctor[]>>('/v1/admissions/doctors')
      if (response.data.success && response.data.data) {
        doctors.value = response.data.data
      }
    } catch {
      doctors.value = []
    }
  }

  async function addConsultingPhysician(
    admissionId: number,
    request: AddConsultingPhysicianRequest
  ): Promise<ConsultingPhysician> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<ConsultingPhysician>>(
        `/v1/admissions/${admissionId}/consulting-physicians`,
        request
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Add consulting physician failed')
    } finally {
      loading.value = false
    }
  }

  async function removeConsultingPhysician(
    admissionId: number,
    consultingPhysicianId: number
  ): Promise<void> {
    loading.value = true
    try {
      await api.delete(
        `/v1/admissions/${admissionId}/consulting-physicians/${consultingPhysicianId}`
      )
      // Refresh current admission to get updated list
      await fetchAdmission(admissionId)
    } finally {
      loading.value = false
    }
  }

  function clearCurrentAdmission(): void {
    currentAdmission.value = null
  }

  return {
    admissions,
    totalAdmissions,
    currentAdmission,
    doctors,
    loading,
    fetchAdmissions,
    fetchAdmission,
    createAdmission,
    updateAdmission,
    dischargePatient,
    deleteAdmission,
    uploadConsentDocument,
    downloadConsentDocument,
    searchPatients,
    fetchPatientSummary,
    fetchDoctors,
    addConsultingPhysician,
    removeConsultingPhysician,
    clearCurrentAdmission
  }
})
