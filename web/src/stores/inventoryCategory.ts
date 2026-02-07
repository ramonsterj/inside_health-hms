import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  InventoryCategory,
  CreateInventoryCategoryRequest,
  UpdateInventoryCategoryRequest
} from '@/types/inventoryCategory'
import type { ApiResponse } from '@/types'

export const useInventoryCategoryStore = defineStore('inventoryCategory', () => {
  const categories = ref<InventoryCategory[]>([])
  const activeCategories = ref<InventoryCategory[]>([])
  const currentCategory = ref<InventoryCategory | null>(null)
  const loading = ref(false)

  async function fetchCategories(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<InventoryCategory[]>>(
        '/v1/admin/inventory-categories'
      )
      if (response.data.success && response.data.data) {
        categories.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchActiveCategories(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<InventoryCategory[]>>(
        '/v1/inventory-categories'
      )
      if (response.data.success && response.data.data) {
        activeCategories.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchCategory(id: number): Promise<InventoryCategory> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<InventoryCategory>>(
        `/v1/admin/inventory-categories/${id}`
      )
      if (response.data.success && response.data.data) {
        currentCategory.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Category not found')
    } finally {
      loading.value = false
    }
  }

  async function createCategory(
    data: CreateInventoryCategoryRequest
  ): Promise<InventoryCategory> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<InventoryCategory>>(
        '/v1/admin/inventory-categories',
        data
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Create category failed')
    } finally {
      loading.value = false
    }
  }

  async function updateCategory(
    id: number,
    data: UpdateInventoryCategoryRequest
  ): Promise<InventoryCategory> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<InventoryCategory>>(
        `/v1/admin/inventory-categories/${id}`,
        data
      )
      if (response.data.success && response.data.data) {
        currentCategory.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Update category failed')
    } finally {
      loading.value = false
    }
  }

  async function deleteCategory(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `/v1/admin/inventory-categories/${id}`
      )
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete category failed')
      }
    } finally {
      loading.value = false
    }
  }

  function clearCurrentCategory(): void {
    currentCategory.value = null
  }

  return {
    categories,
    activeCategories,
    currentCategory,
    loading,
    fetchCategories,
    fetchActiveCategories,
    fetchCategory,
    createCategory,
    updateCategory,
    deleteCategory,
    clearCurrentCategory
  }
})
