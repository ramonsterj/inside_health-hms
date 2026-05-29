import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { Transfer, CreateTransferRequest } from '@/types/warehouse'
import type { ApiResponse, PageResponse } from '@/types'

export const useWarehouseTransferStore = defineStore('warehouseTransfer', () => {
  const transfers = ref<Transfer[]>([])
  const totalTransfers = ref(0)
  const loading = ref(false)

  async function fetchTransfers(
    page = 0,
    size = 20,
    warehouseId?: number,
    itemId?: number
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (warehouseId) params.warehouseId = warehouseId
      if (itemId) params.itemId = itemId
      const response = await api.get<ApiResponse<PageResponse<Transfer>>>('/v1/warehouse-transfers', {
        params
      })
      if (response.data.success && response.data.data) {
        transfers.value = response.data.data.content
        totalTransfers.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function createTransfer(data: CreateTransferRequest): Promise<Transfer> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Transfer>>('/v1/warehouse-transfers', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create transfer failed')
    } finally {
      loading.value = false
    }
  }

  return {
    transfers,
    totalTransfers,
    loading,
    fetchTransfers,
    createTransfer
  }
})
