import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  PatientCharge,
  AdmissionBalance,
  Invoice,
  CreateChargeRequest,
  CreateAdjustmentRequest
} from '@/types/billing'
import type { ApiResponse } from '@/types'

export const useBillingStore = defineStore('billing', () => {
  const charges = ref<PatientCharge[]>([])
  const balance = ref<AdmissionBalance | null>(null)
  const invoice = ref<Invoice | null>(null)
  const loading = ref(false)

  async function fetchCharges(admissionId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PatientCharge[]>>(
        `/v1/admissions/${admissionId}/charges`
      )
      if (response.data.success && response.data.data) {
        charges.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchBalance(admissionId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<AdmissionBalance>>(
        `/v1/admissions/${admissionId}/balance`
      )
      if (response.data.success && response.data.data) {
        balance.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createCharge(
    admissionId: number,
    data: CreateChargeRequest
  ): Promise<PatientCharge> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<PatientCharge>>(
        `/v1/admissions/${admissionId}/charges`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create charge failed')
    } finally {
      loading.value = false
    }
  }

  async function createAdjustment(
    admissionId: number,
    data: CreateAdjustmentRequest
  ): Promise<PatientCharge> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<PatientCharge>>(
        `/v1/admissions/${admissionId}/adjustments`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create adjustment failed')
    } finally {
      loading.value = false
    }
  }

  async function fetchInvoice(admissionId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Invoice>>(
        `/v1/admissions/${admissionId}/invoice`
      )
      if (response.data.success && response.data.data) {
        invoice.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function generateInvoice(admissionId: number): Promise<Invoice> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Invoice>>(
        `/v1/admissions/${admissionId}/invoice`
      )
      if (response.data.success && response.data.data) {
        invoice.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Generate invoice failed')
    } finally {
      loading.value = false
    }
  }

  function clearState(): void {
    charges.value = []
    balance.value = null
    invoice.value = null
  }

  return {
    charges,
    balance,
    invoice,
    loading,
    fetchCharges,
    fetchBalance,
    createCharge,
    createAdjustment,
    fetchInvoice,
    generateInvoice,
    clearState
  }
})
