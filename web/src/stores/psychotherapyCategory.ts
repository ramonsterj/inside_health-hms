import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  PsychotherapyCategory,
  CreatePsychotherapyCategoryRequest,
  UpdatePsychotherapyCategoryRequest
} from '@/types/psychotherapy'
import type { ApiResponse } from '@/types'

export const usePsychotherapyCategoryStore = defineStore('psychotherapyCategory', () => {
  const categories = ref<PsychotherapyCategory[]>([])
  const activeCategories = ref<PsychotherapyCategory[]>([])
  const currentCategory = ref<PsychotherapyCategory | null>(null)
  const loading = ref(false)

  // Fetch all categories (admin view)
  async function fetchCategories(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PsychotherapyCategory[]>>(
        '/v1/admin/psychotherapy-categories'
      )
      if (response.data.success && response.data.data) {
        categories.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  // Fetch active categories only (for dropdown in activity form)
  async function fetchActiveCategories(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PsychotherapyCategory[]>>(
        '/v1/psychotherapy-categories'
      )
      if (response.data.success && response.data.data) {
        activeCategories.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  // Fetch single category by ID
  async function fetchCategory(id: number): Promise<PsychotherapyCategory> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PsychotherapyCategory>>(
        `/v1/admin/psychotherapy-categories/${id}`
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

  // Create category
  async function createCategory(
    data: CreatePsychotherapyCategoryRequest
  ): Promise<PsychotherapyCategory> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<PsychotherapyCategory>>(
        '/v1/admin/psychotherapy-categories',
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

  // Update category
  async function updateCategory(
    id: number,
    data: UpdatePsychotherapyCategoryRequest
  ): Promise<PsychotherapyCategory> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<PsychotherapyCategory>>(
        `/v1/admin/psychotherapy-categories/${id}`,
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

  // Delete category
  async function deleteCategory(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `/v1/admin/psychotherapy-categories/${id}`
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
