import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import type {
  Role,
  Permission,
  CreateRoleRequest,
  UpdateRoleRequest,
  AssignPermissionsRequest,
  ApiResponse
} from '@/types'

export const useRoleStore = defineStore('role', () => {
  const roles = ref<Role[]>([])
  const permissions = ref<Permission[]>([])
  const loading = ref(false)

  const permissionsByResource = computed(() => {
    const grouped = new Map<string, Permission[]>()
    for (const permission of permissions.value) {
      const resource = permission.resource
      const existing = grouped.get(resource)
      if (!existing) {
        grouped.set(resource, [permission])
      } else {
        existing.push(permission)
      }
    }
    return Object.fromEntries(grouped)
  })

  async function fetchRoles(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Role[]>>('/roles')
      if (response.data.success && response.data.data) {
        roles.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchPermissions(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<Permission[]>>('/roles/permissions')
      if (response.data.success && response.data.data) {
        permissions.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function getRoleById(id: number): Promise<Role> {
    const response = await api.get<ApiResponse<Role>>(`/roles/${id}`)
    if (response.data.success && response.data.data) {
      return response.data.data
    }
    throw new Error(response.data.message || 'Role not found')
  }

  async function createRole(request: CreateRoleRequest): Promise<Role> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<Role>>('/roles', request)
      if (response.data.success && response.data.data) {
        roles.value.push(response.data.data)
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to create role')
    } finally {
      loading.value = false
    }
  }

  async function updateRole(id: number, request: UpdateRoleRequest): Promise<Role> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<Role>>(`/roles/${id}`, request)
      if (response.data.success && response.data.data) {
        const index = roles.value.findIndex(r => r.id === id)
        if (index !== -1) {
          roles.value.splice(index, 1, response.data.data)
        }
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to update role')
    } finally {
      loading.value = false
    }
  }

  async function deleteRole(id: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(`/roles/${id}`)
      if (response.data.success) {
        roles.value = roles.value.filter(r => r.id !== id)
      } else {
        throw new Error(response.data.message || 'Failed to delete role')
      }
    } finally {
      loading.value = false
    }
  }

  async function assignPermissions(id: number, permissionCodes: string[]): Promise<Role> {
    loading.value = true
    try {
      const request: AssignPermissionsRequest = { permissionCodes }
      const response = await api.put<ApiResponse<Role>>(`/roles/${id}/permissions`, request)
      if (response.data.success && response.data.data) {
        const index = roles.value.findIndex(r => r.id === id)
        if (index !== -1) {
          roles.value.splice(index, 1, response.data.data)
        }
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to assign permissions')
    } finally {
      loading.value = false
    }
  }

  return {
    roles,
    permissions,
    loading,
    permissionsByResource,
    fetchRoles,
    fetchPermissions,
    getRoleById,
    createRole,
    updateRole,
    deleteRole,
    assignPermissions
  }
})
