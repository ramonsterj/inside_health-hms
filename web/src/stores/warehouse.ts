import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  Warehouse,
  WarehouseStock,
  WarehouseStockQuery,
  CreateWarehouseRequest,
  UpdateWarehouseRequest
} from '@/types/warehouse'
import type { ApiResponse, PageResponse } from '@/types'

export const useWarehouseStore = defineStore('warehouse', () => {
  const warehouses = ref<Warehouse[]>([])
  const stock = ref<WarehouseStock[]>([])
  const totalStock = ref(0)
  const loading = ref(false)

  async function fetchWarehouses(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Warehouse[]>>('/v1/warehouses')
      if (response.data.success && response.data.data) {
        warehouses.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createWarehouse(data: CreateWarehouseRequest): Promise<Warehouse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Warehouse>>('/v1/warehouses', data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create warehouse failed')
    } finally {
      loading.value = false
    }
  }

  async function updateWarehouse(id: number, data: UpdateWarehouseRequest): Promise<Warehouse> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<Warehouse>>(`/v1/warehouses/${id}`, data)
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Update warehouse failed')
    } finally {
      loading.value = false
    }
  }

  async function deleteWarehouse(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/v1/warehouses/${id}`)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete warehouse failed')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchStock(warehouseId: number, query: WarehouseStockQuery = {}): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = {
        page: query.page ?? 0,
        size: query.size ?? 20
      }
      if (query.search) params.search = query.search
      if (query.lowStockOnly) params.lowStockOnly = query.lowStockOnly
      const response = await api.get<ApiResponse<PageResponse<WarehouseStock>>>(
        `/v1/warehouses/${warehouseId}/stock`,
        { params }
      )
      if (response.data.success && response.data.data) {
        stock.value = response.data.data.content
        totalStock.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  return {
    warehouses,
    stock,
    totalStock,
    loading,
    fetchWarehouses,
    createWarehouse,
    updateWarehouse,
    deleteWarehouse,
    fetchStock
  }
})
