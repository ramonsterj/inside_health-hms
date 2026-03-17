import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { ApiResponse, PageResponse, KardexAdmissionSummary } from '@/types'

const AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1000

export const useKardexStore = defineStore('kardex', () => {
  const summaries = ref<KardexAdmissionSummary[]>([])
  const totalElements = ref(0)
  const totalPages = ref(0)
  const loading = ref(false)
  let autoRefreshTimer: ReturnType<typeof setInterval> | null = null
  let lastFetchParams: {
    page: number
    size: number
    type?: string | null
    search?: string | null
  } = {
    page: 0,
    size: 20
  }

  async function fetchSummaries(
    page = 0,
    size = 20,
    type?: string | null,
    search?: string | null
  ): Promise<void> {
    loading.value = true
    lastFetchParams = { page, size, type, search }
    try {
      const params: Record<string, unknown> = { page, size, sort: 'room_id,asc' }
      if (type) params.type = type
      if (search) params.search = search

      const response = await api.get<ApiResponse<PageResponse<KardexAdmissionSummary>>>(
        '/v1/nursing-kardex',
        { params }
      )
      if (response.data.success && response.data.data) {
        summaries.value = response.data.data.content
        totalElements.value = response.data.data.page.totalElements
        totalPages.value = response.data.data.page.totalPages
      }
    } finally {
      loading.value = false
    }
  }

  async function refreshSingleAdmission(admissionId: number): Promise<void> {
    try {
      const response = await api.get<ApiResponse<KardexAdmissionSummary>>(
        `/v1/nursing-kardex/${admissionId}`
      )
      if (response.data.success && response.data.data) {
        const index = summaries.value.findIndex(s => s.admissionId === admissionId)
        if (index !== -1) {
          summaries.value.splice(index, 1, response.data.data)
        }
      }
    } catch {
      // If single refresh fails, do a full refresh
      await fetchSummaries(
        lastFetchParams.page,
        lastFetchParams.size,
        lastFetchParams.type,
        lastFetchParams.search
      )
    }
  }

  function startAutoRefresh(): void {
    stopAutoRefresh()
    autoRefreshTimer = setInterval(() => {
      fetchSummaries(
        lastFetchParams.page,
        lastFetchParams.size,
        lastFetchParams.type,
        lastFetchParams.search
      )
    }, AUTO_REFRESH_INTERVAL_MS)
  }

  function stopAutoRefresh(): void {
    if (autoRefreshTimer) {
      clearInterval(autoRefreshTimer)
      autoRefreshTimer = null
    }
  }

  return {
    summaries,
    totalElements,
    totalPages,
    loading,
    fetchSummaries,
    refreshSingleAdmission,
    startAutoRefresh,
    stopAutoRefresh
  }
})
