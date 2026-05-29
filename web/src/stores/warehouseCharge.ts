import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { WarehouseCharge, CreateWarehouseChargeRequest } from '@/types/warehouse'
import type { ApiResponse, PageResponse } from '@/types'

export const useWarehouseChargeStore = defineStore('warehouseCharge', () => {
  const charges = ref<WarehouseCharge[]>([])
  const totalCharges = ref(0)
  const loading = ref(false)

  async function fetchCharges(
    page = 0,
    size = 20,
    warehouseId?: number,
    admissionId?: number
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (warehouseId) params.warehouseId = warehouseId
      if (admissionId) params.admissionId = admissionId
      const response = await api.get<ApiResponse<PageResponse<WarehouseCharge>>>(
        '/v1/warehouse-charges',
        { params }
      )
      if (response.data.success && response.data.data) {
        charges.value = response.data.data.content
        totalCharges.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function createCharge(data: CreateWarehouseChargeRequest): Promise<WarehouseCharge> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<WarehouseCharge>>('/v1/warehouse-charges', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create warehouse charge failed')
    } finally {
      loading.value = false
    }
  }

  return {
    charges,
    totalCharges,
    loading,
    fetchCharges,
    createCharge
  }
})
