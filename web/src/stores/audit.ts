import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { AuditLog, AuditLogFilters, AuditUser } from '@/types/audit'
import type { ApiResponse, PageResponse } from '@/types'

export const useAuditStore = defineStore('audit', () => {
  const logs = ref<AuditLog[]>([])
  const totalLogs = ref(0)
  const loading = ref(false)
  const filters = ref<AuditLogFilters>({})
  const entityTypes = ref<string[]>([])
  const users = ref<AuditUser[]>([])

  async function fetchLogs(page = 0, size = 50): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }

      if (filters.value.userId) {
        params.userId = filters.value.userId
      }
      if (filters.value.entityType) {
        params.entityType = filters.value.entityType
      }
      if (filters.value.action) {
        params.action = filters.value.action
      }
      if (filters.value.startDate) {
        params.startDate = filters.value.startDate
      }
      if (filters.value.endDate) {
        params.endDate = filters.value.endDate
      }

      const response = await api.get<ApiResponse<PageResponse<AuditLog>>>('/audit-logs', { params })
      if (response.data.success && response.data.data) {
        logs.value = response.data.data.content
        totalLogs.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchLogsForEntity(
    entityType: string,
    entityId: number,
    page = 0,
    size = 50
  ): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PageResponse<AuditLog>>>('/audit-logs/entity', {
        params: { entityType, entityId, page, size }
      })
      if (response.data.success && response.data.data) {
        logs.value = response.data.data.content
        totalLogs.value = response.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchEntityTypes(): Promise<void> {
    try {
      const response = await api.get<ApiResponse<string[]>>('/audit-logs/entity-types')
      if (response.data.success && response.data.data) {
        entityTypes.value = response.data.data
      }
    } catch {
      // Filter data is non-critical; silently fall back to empty list
    }
  }

  async function fetchUsers(): Promise<void> {
    try {
      const response = await api.get<ApiResponse<AuditUser[]>>('/audit-logs/users')
      if (response.data.success && response.data.data) {
        users.value = response.data.data
      }
    } catch {
      // Filter data is non-critical; silently fall back to empty list
    }
  }

  function setFilters(newFilters: AuditLogFilters): void {
    filters.value = { ...newFilters }
  }

  function clearFilters(): void {
    filters.value = {}
  }

  function parseJsonValues(jsonString: string | null): Record<string, unknown> | null {
    if (!jsonString) return null
    try {
      return JSON.parse(jsonString)
    } catch {
      return null
    }
  }

  return {
    logs,
    totalLogs,
    loading,
    filters,
    entityTypes,
    users,
    fetchLogs,
    fetchLogsForEntity,
    fetchEntityTypes,
    fetchUsers,
    setFilters,
    clearFilters,
    parseJsonValues
  }
})
