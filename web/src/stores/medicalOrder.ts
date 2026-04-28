import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  MedicalOrderResponse,
  MedicalOrderListItemResponse,
  CreateMedicalOrderRequest,
  UpdateMedicalOrderRequest,
  RejectMedicalOrderRequest,
  EmergencyAuthorizeMedicalOrderRequest,
  GroupedMedicalOrdersResponse,
  MedicalOrderCategory,
  MedicalOrderStatus
} from '@/types/medicalRecord'
import type { ApiResponse, PageResponse } from '@/types'

export const useMedicalOrderStore = defineStore('medicalOrder', () => {
  // State - Map by admissionId for caching grouped orders
  const medicalOrders = ref<Map<number, Record<MedicalOrderCategory, MedicalOrderResponse[]>>>(
    new Map()
  )
  const loading = ref(false)

  async function fetchMedicalOrders(
    admissionId: number,
    status?: MedicalOrderStatus
  ): Promise<Record<MedicalOrderCategory, MedicalOrderResponse[]>> {
    loading.value = true
    try {
      const params: Record<string, unknown> = {}
      if (status) {
        params.status = status
      }
      const response = await api.get<ApiResponse<GroupedMedicalOrdersResponse>>(
        `/v1/admissions/${admissionId}/medical-orders`,
        { params }
      )
      if (response.data.success && response.data.data) {
        const orders = response.data.data.orders
        medicalOrders.value.set(admissionId, orders)
        return orders
      }
      return {} as Record<MedicalOrderCategory, MedicalOrderResponse[]>
    } finally {
      loading.value = false
    }
  }

  async function fetchMedicalOrder(
    admissionId: number,
    orderId: number
  ): Promise<MedicalOrderResponse> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<MedicalOrderResponse>>(
        `/v1/admissions/${admissionId}/medical-orders/${orderId}`
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Medical order not found')
    } finally {
      loading.value = false
    }
  }

  async function createMedicalOrder(
    admissionId: number,
    data: CreateMedicalOrderRequest
  ): Promise<MedicalOrderResponse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<MedicalOrderResponse>>(
        `/v1/admissions/${admissionId}/medical-orders`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh the list to include the new order
        await fetchMedicalOrders(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || 'Create medical order failed')
    } finally {
      loading.value = false
    }
  }

  async function updateMedicalOrder(
    admissionId: number,
    orderId: number,
    data: UpdateMedicalOrderRequest
  ): Promise<MedicalOrderResponse> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<MedicalOrderResponse>>(
        `/v1/admissions/${admissionId}/medical-orders/${orderId}`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh the list to update the order
        await fetchMedicalOrders(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || 'Update medical order failed')
    } finally {
      loading.value = false
    }
  }

  async function discontinueMedicalOrder(
    admissionId: number,
    orderId: number
  ): Promise<MedicalOrderResponse> {
    return runStateTransition(admissionId, orderId, 'discontinue')
  }

  async function authorizeMedicalOrder(
    admissionId: number,
    orderId: number
  ): Promise<MedicalOrderResponse> {
    return runStateTransition(admissionId, orderId, 'authorize')
  }

  async function rejectMedicalOrder(
    admissionId: number,
    orderId: number,
    payload?: RejectMedicalOrderRequest
  ): Promise<MedicalOrderResponse> {
    return runStateTransition(admissionId, orderId, 'reject', payload ?? {})
  }

  async function emergencyAuthorize(
    admissionId: number,
    orderId: number,
    payload: EmergencyAuthorizeMedicalOrderRequest
  ): Promise<MedicalOrderResponse> {
    return runStateTransition(admissionId, orderId, 'emergency-authorize', payload)
  }

  async function markInProgress(
    admissionId: number,
    orderId: number
  ): Promise<MedicalOrderResponse> {
    return runStateTransition(admissionId, orderId, 'mark-in-progress')
  }

  async function fetchOrdersByStatus(params: {
    status?: MedicalOrderStatus[]
    category?: MedicalOrderCategory[]
    page?: number
    size?: number
  }): Promise<PageResponse<MedicalOrderListItemResponse>> {
    loading.value = true
    try {
      const queryParams = new URLSearchParams()
      params.status?.forEach((s) => queryParams.append('status', s))
      params.category?.forEach((c) => queryParams.append('category', c))
      if (params.page !== undefined) queryParams.set('page', String(params.page))
      if (params.size !== undefined) queryParams.set('size', String(params.size))

      const response = await api.get<ApiResponse<PageResponse<MedicalOrderListItemResponse>>>(
        `/v1/medical-orders?${queryParams.toString()}`
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to fetch medical orders')
    } finally {
      loading.value = false
    }
  }

  async function runStateTransition(
    admissionId: number,
    orderId: number,
    action: 'discontinue' | 'authorize' | 'reject' | 'emergency-authorize' | 'mark-in-progress',
    body?: object
  ): Promise<MedicalOrderResponse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<MedicalOrderResponse>>(
        `/v1/admissions/${admissionId}/medical-orders/${orderId}/${action}`,
        body
      )
      if (response.data.success && response.data.data) {
        // Refresh the per-admission cache so the card updates everywhere it is rendered
        await fetchMedicalOrders(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || `Failed to ${action} medical order`)
    } finally {
      loading.value = false
    }
  }

  function getMedicalOrders(
    admissionId: number
  ): Record<MedicalOrderCategory, MedicalOrderResponse[]> | undefined {
    return medicalOrders.value.get(admissionId)
  }

  function clearMedicalOrders(admissionId: number): void {
    medicalOrders.value.delete(admissionId)
  }

  function clearAll(): void {
    medicalOrders.value.clear()
  }

  return {
    medicalOrders,
    loading,
    fetchMedicalOrders,
    fetchMedicalOrder,
    createMedicalOrder,
    updateMedicalOrder,
    discontinueMedicalOrder,
    authorizeMedicalOrder,
    rejectMedicalOrder,
    emergencyAuthorize,
    markInProgress,
    fetchOrdersByStatus,
    getMedicalOrders,
    clearMedicalOrders,
    clearAll
  }
})
