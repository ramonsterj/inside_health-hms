import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import { useAuthStore } from './auth'
import type {
  User,
  UpdateUserRequest,
  ChangePasswordRequest,
  CreateUserRequest,
  ResetPasswordResponse,
  AdminUpdateUserRequest,
  ApiResponse,
  PageResponse
} from '@/types'
import { UserStatus } from '@/types'

export const useUserStore = defineStore('user', () => {
  const users = ref<User[]>([])
  const totalUsers = ref(0)
  const deletedUsers = ref<User[]>([])
  const totalDeletedUsers = ref(0)
  const loading = ref(false)

  async function updateProfile(data: UpdateUserRequest): Promise<User> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<User>>('/users/me', data)
      if (response.data.success && response.data.data) {
        const authStore = useAuthStore()
        authStore.setUser(response.data.data)
        return response.data.data
      }
      throw new Error(response.data.message || 'Update failed')
    } finally {
      loading.value = false
    }
  }

  async function changePassword(data: ChangePasswordRequest): Promise<void> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<void>>('/users/me/password', data)
      if (!response.data.success) {
        throw new Error(response.data.message || 'Password change failed')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUsers(page = 0, size = 10, status: UserStatus | null = null): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (status !== null) {
        params.status = status
      }
      const response = await api.get<ApiResponse<PageResponse<User>>>('/users', { params })
      if (response.data.success && response.data.data) {
        users.value = response.data.data.content
        totalUsers.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchDeletedUsers(page = 0, size = 10): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PageResponse<User>>>('/users/deleted', {
        params: { page, size }
      })
      if (response.data.success && response.data.data) {
        deletedUsers.value = response.data.data.content
        totalDeletedUsers.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function restoreUser(id: number): Promise<User> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<User>>(`/users/${id}/restore`)
      if (response.data.success && response.data.data) {
        deletedUsers.value = deletedUsers.value.filter(u => u.id !== id)
        totalDeletedUsers.value--
        return response.data.data
      }
      throw new Error(response.data.message || 'Restore failed')
    } finally {
      loading.value = false
    }
  }

  async function getUserById(id: number): Promise<User> {
    const response = await api.get<ApiResponse<User>>(`/users/${id}`)
    if (response.data.success && response.data.data) {
      return response.data.data
    }
    throw new Error(response.data.message || 'User not found')
  }

  async function deleteUser(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/users/${id}`)
      if (response.data.success) {
        users.value = users.value.filter(u => u.id !== id)
        totalUsers.value--
      } else {
        throw new Error(response.data.message || 'Delete failed')
      }
    } finally {
      loading.value = false
    }
  }

  async function createUser(data: CreateUserRequest): Promise<User> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<User>>('/users', data)
      if (response.data.success && response.data.data) {
        users.value.unshift(response.data.data)
        totalUsers.value++
        return response.data.data
      }
      throw new Error(response.data.message || 'Create user failed')
    } finally {
      loading.value = false
    }
  }

  async function resetUserPassword(id: number): Promise<string> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<ResetPasswordResponse>>(
        `/users/${id}/reset-password`
      )
      if (response.data.success && response.data.data) {
        return response.data.data.temporaryPassword
      }
      throw new Error(response.data.message || 'Reset password failed')
    } finally {
      loading.value = false
    }
  }

  async function updateUser(id: number, data: AdminUpdateUserRequest): Promise<User> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<User>>(`/users/${id}`, data)
      if (response.data.success && response.data.data) {
        const index = users.value.findIndex(u => u.id === id)
        if (index !== -1) {
          users.value.splice(index, 1, response.data.data)
        }
        return response.data.data
      }
      throw new Error(response.data.message || 'Update user failed')
    } finally {
      loading.value = false
    }
  }

  return {
    users,
    totalUsers,
    deletedUsers,
    totalDeletedUsers,
    loading,
    updateProfile,
    changePassword,
    fetchUsers,
    fetchDeletedUsers,
    restoreUser,
    getUserById,
    deleteUser,
    createUser,
    resetUserPassword,
    updateUser
  }
})
