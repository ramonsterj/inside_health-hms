import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  Patient,
  PatientSummary,
  CreatePatientRequest,
  UpdatePatientRequest,
  DuplicatePatientResponse,
  ApiResponse,
  PageResponse
} from '@/types'
import type { AxiosError } from 'axios'

export class DuplicatePatientError extends Error {
  potentialDuplicates: PatientSummary[]

  constructor(message: string, duplicates: PatientSummary[]) {
    super(message)
    this.name = 'DuplicatePatientError'
    this.potentialDuplicates = duplicates
  }
}

export const usePatientStore = defineStore('patient', () => {
  const patients = ref<PatientSummary[]>([])
  const totalPatients = ref(0)
  const currentPatient = ref<Patient | null>(null)
  const loading = ref(false)

  async function fetchPatients(page = 0, size = 20, search: string | null = null): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (search) {
        params.search = search
      }
      const response = await api.get<ApiResponse<PageResponse<PatientSummary>>>('/v1/patients', {
        params
      })
      if (response.data.success && response.data.data) {
        patients.value = response.data.data.content
        totalPatients.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchPatient(id: number): Promise<Patient> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Patient>>(`/v1/patients/${id}`)
      if (response.data.success && response.data.data) {
        currentPatient.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Patient not found')
    } finally {
      loading.value = false
    }
  }

  async function createPatient(data: CreatePatientRequest): Promise<Patient> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Patient>>('/v1/patients', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create patient failed')
    } catch (error) {
      const axiosError = error as AxiosError<DuplicatePatientResponse>
      if (
        axiosError.response?.status === 409 &&
        axiosError.response.data?.data?.potentialDuplicates
      ) {
        throw new DuplicatePatientError(
          axiosError.response.data.message,
          axiosError.response.data.data.potentialDuplicates
        )
      }
      throw error
    } finally {
      loading.value = false
    }
  }

  async function updatePatient(id: number, data: UpdatePatientRequest): Promise<Patient> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<Patient>>(`/v1/patients/${id}`, data)
      if (response.data.success && response.data.data) {
        currentPatient.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Update patient failed')
    } catch (error) {
      const axiosError = error as AxiosError<DuplicatePatientResponse>
      if (
        axiosError.response?.status === 409 &&
        axiosError.response.data?.data?.potentialDuplicates
      ) {
        throw new DuplicatePatientError(
          axiosError.response.data.message,
          axiosError.response.data.data.potentialDuplicates
        )
      }
      throw error
    } finally {
      loading.value = false
    }
  }

  async function uploadIdDocument(patientId: number, file: File): Promise<Patient> {
    loading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await api.post<ApiResponse<Patient>>(
        `/v1/patients/${patientId}/id-document`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      )
      if (response.data.success && response.data.data) {
        currentPatient.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Upload failed')
    } finally {
      loading.value = false
    }
  }

  async function downloadIdDocument(patientId: number): Promise<Blob> {
    loading.value = true
    try {
      const response = await api.get(`/v1/patients/${patientId}/id-document`, {
        responseType: 'blob'
      })
      return response.data
    } finally {
      loading.value = false
    }
  }

  async function deleteIdDocument(patientId: number): Promise<Patient> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<Patient>>(
        `/v1/patients/${patientId}/id-document`
      )
      if (response.data.success && response.data.data) {
        currentPatient.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Delete failed')
    } finally {
      loading.value = false
    }
  }

  function clearCurrentPatient(): void {
    currentPatient.value = null
  }

  return {
    patients,
    totalPatients,
    currentPatient,
    loading,
    fetchPatients,
    fetchPatient,
    createPatient,
    updatePatient,
    uploadIdDocument,
    downloadIdDocument,
    deleteIdDocument,
    clearCurrentPatient
  }
})
