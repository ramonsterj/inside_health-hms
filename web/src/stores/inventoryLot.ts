import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  InventoryLot,
  CreateInventoryLotRequest,
  UpdateInventoryLotRequest
} from '@/types/pharmacy'
import type { ApiResponse } from '@/types'

export const useInventoryLotStore = defineStore('inventoryLot', () => {
  const lots = ref<InventoryLot[]>([])
  const loading = ref(false)

  async function fetchByItem(itemId: number): Promise<void> {
    loading.value = true
    lots.value = []
    try {
      const res = await api.get<ApiResponse<InventoryLot[]>>(`/v1/inventory/items/${itemId}/lots`)
      if (res.data.success && res.data.data) lots.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function createLot(itemId: number, data: CreateInventoryLotRequest): Promise<InventoryLot> {
    const res = await api.post<ApiResponse<InventoryLot>>(
      `/v1/inventory/items/${itemId}/lots`,
      data
    )
    if (!res.data.success || !res.data.data)
      throw new Error(res.data.message || 'Create lot failed')
    return res.data.data
  }

  async function updateLot(lotId: number, data: UpdateInventoryLotRequest): Promise<InventoryLot> {
    const res = await api.put<ApiResponse<InventoryLot>>(`/v1/inventory/lots/${lotId}`, data)
    if (!res.data.success || !res.data.data)
      throw new Error(res.data.message || 'Update lot failed')
    return res.data.data
  }

  async function deleteLot(lotId: number): Promise<void> {
    const res = await api.delete<ApiResponse<void>>(`/v1/inventory/lots/${lotId}`)
    if (!res.data.success && res.status !== 204) {
      throw new Error(res.data?.message || 'Delete lot failed')
    }
  }

  return { lots, loading, fetchByItem, createLot, updateLot, deleteLot }
})
