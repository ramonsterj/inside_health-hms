import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  DoctorFee,
  DoctorFeeSummary,
  CreateDoctorFeeRequest,
  UpdateDoctorFeeStatusRequest,
  SettleDoctorFeeRequest
} from '@/types/treasury'
import type { ApiResponse } from '@/types'

export const useDoctorFeeStore = defineStore('doctorFee', () => {
  const doctorFees = ref<DoctorFee[]>([])
  const currentDoctorFee = ref<DoctorFee | null>(null)
  const summary = ref<DoctorFeeSummary | null>(null)
  const loading = ref(false)

  async function fetchDoctorFees(
    employeeId: number,
    filters?: { status?: string; from?: string; to?: string }
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = {}
      if (filters?.status) params.status = filters.status
      if (filters?.from) params.from = filters.from
      if (filters?.to) params.to = filters.to

      const response = await api.get<ApiResponse<DoctorFee[]>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees`,
        { params }
      )
      if (response.data.success && response.data.data) {
        doctorFees.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchDoctorFee(employeeId: number, id: number): Promise<DoctorFee> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<DoctorFee>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees/${id}`
      )
      if (response.data.success && response.data.data) {
        currentDoctorFee.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Doctor fee not found')
    } finally {
      loading.value = false
    }
  }

  async function createDoctorFee(
    employeeId: number,
    data: CreateDoctorFeeRequest
  ): Promise<DoctorFee> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<DoctorFee>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to create doctor fee')
    } finally {
      loading.value = false
    }
  }

  async function updateDoctorFeeStatus(
    employeeId: number,
    id: number,
    data: UpdateDoctorFeeStatusRequest
  ): Promise<DoctorFee> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<DoctorFee>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees/${id}/status`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to update status')
    } finally {
      loading.value = false
    }
  }

  async function uploadInvoiceDocument(
    employeeId: number,
    id: number,
    file: File
  ): Promise<DoctorFee> {
    loading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      const response = await api.post<ApiResponse<DoctorFee>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees/${id}/invoice-document`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to upload document')
    } finally {
      loading.value = false
    }
  }

  async function settleDoctorFee(
    employeeId: number,
    id: number,
    data: SettleDoctorFeeRequest
  ): Promise<DoctorFee> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<DoctorFee>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees/${id}/settle`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to settle doctor fee')
    } finally {
      loading.value = false
    }
  }

  async function deleteDoctorFee(employeeId: number, id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees/${id}`
      )
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to delete doctor fee')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchSummary(employeeId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<DoctorFeeSummary>>(
        `/v1/treasury/employees/${employeeId}/doctor-fees/summary`
      )
      if (response.data.success && response.data.data) {
        summary.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  function clearState(): void {
    doctorFees.value = []
    currentDoctorFee.value = null
    summary.value = null
  }

  return {
    doctorFees,
    currentDoctorFee,
    summary,
    loading,
    fetchDoctorFees,
    fetchDoctorFee,
    createDoctorFee,
    updateDoctorFeeStatus,
    uploadInvoiceDocument,
    settleDoctorFee,
    deleteDoctorFee,
    fetchSummary,
    clearState
  }
})
