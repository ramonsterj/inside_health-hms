import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  InventoryItem,
  InventoryMovement,
  CreateInventoryItemRequest,
  UpdateInventoryItemRequest,
  CreateInventoryMovementRequest
} from '@/types/inventoryItem'
import type { ApiResponse, PageResponse } from '@/types'

export const useInventoryItemStore = defineStore('inventoryItem', () => {
  const items = ref<InventoryItem[]>([])
  const totalItems = ref(0)
  const currentItem = ref<InventoryItem | null>(null)
  const movements = ref<InventoryMovement[]>([])
  const lowStockItems = ref<InventoryItem[]>([])
  const loading = ref(false)

  async function fetchItems(
    page = 0,
    size = 20,
    categoryId?: number,
    search?: string
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (categoryId) params.categoryId = categoryId
      if (search) params.search = search

      const response = await api.get<ApiResponse<PageResponse<InventoryItem>>>(
        '/v1/admin/inventory-items',
        { params }
      )
      if (response.data.success && response.data.data) {
        items.value = response.data.data.content
        totalItems.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchItem(id: number): Promise<InventoryItem> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<InventoryItem>>(
        `/v1/admin/inventory-items/${id}`
      )
      if (response.data.success && response.data.data) {
        currentItem.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Item not found')
    } finally {
      loading.value = false
    }
  }

  async function createItem(data: CreateInventoryItemRequest): Promise<InventoryItem> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<InventoryItem>>(
        '/v1/admin/inventory-items',
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create item failed')
    } finally {
      loading.value = false
    }
  }

  async function updateItem(
    id: number,
    data: UpdateInventoryItemRequest
  ): Promise<InventoryItem> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<InventoryItem>>(
        `/v1/admin/inventory-items/${id}`,
        data
      )
      if (response.data.success && response.data.data) {
        currentItem.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Update item failed')
    } finally {
      loading.value = false
    }
  }

  async function deleteItem(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `/v1/admin/inventory-items/${id}`
      )
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete item failed')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchLowStock(categoryId?: number): Promise<void> {
    loading.value = true
    try {
      const query = categoryId ? `?categoryId=${categoryId}` : ''
      const response = await api.get<ApiResponse<InventoryItem[]>>(
        `/v1/admin/inventory-items/low-stock${query}`
      )
      if (response.data.success && response.data.data) {
        lowStockItems.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchMovements(itemId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<InventoryMovement[]>>(
        `/v1/admin/inventory-items/${itemId}/movements`
      )
      if (response.data.success && response.data.data) {
        movements.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createMovement(
    itemId: number,
    data: CreateInventoryMovementRequest
  ): Promise<InventoryMovement> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<InventoryMovement>>(
        `/v1/admin/inventory-items/${itemId}/movements`,
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create movement failed')
    } finally {
      loading.value = false
    }
  }

  function clearCurrentItem(): void {
    currentItem.value = null
    movements.value = []
  }

  return {
    items,
    totalItems,
    currentItem,
    movements,
    lowStockItems,
    loading,
    fetchItems,
    fetchItem,
    createItem,
    updateItem,
    deleteItem,
    fetchLowStock,
    fetchMovements,
    createMovement,
    clearCurrentItem
  }
})
