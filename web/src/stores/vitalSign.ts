import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  VitalSignResponse,
  CreateVitalSignRequest,
  UpdateVitalSignRequest,
  VitalSignDateRange
} from '@/types/nursing'
import type { ApiResponse, PageResponse } from '@/types'

export const useVitalSignStore = defineStore('vitalSign', () => {
  // State - Map by admissionId for caching lists
  const vitalSigns = ref<Map<number, VitalSignResponse[]>>(new Map())
  const totalVitalSigns = ref<Map<number, number>>(new Map())
  // Summary cache (hub metric only) — kept separate from the list map above so a lightweight
  // size=1 prefetch can never overwrite the full list a drilled-in view has loaded.
  const latestVitalSign = ref<Map<number, VitalSignResponse | null>>(new Map())
  const chartData = ref<Map<number, VitalSignResponse[]>>(new Map())
  const loading = ref(false)
  const chartLoading = ref(false)
  const dateRange = ref<VitalSignDateRange>({ fromDate: null, toDate: null })

  async function fetchVitalSigns(
    admissionId: number,
    page = 0,
    size = 20,
    sortDirection: 'ASC' | 'DESC' = 'DESC'
  ): Promise<VitalSignResponse[]> {
    loading.value = true
    try {
      const params: Record<string, unknown> = {
        page,
        size,
        sort: `recordedAt,${sortDirection}`
      }

      // Add date range filters if set
      if (dateRange.value.fromDate) {
        params.fromDate = dateRange.value.fromDate
      }
      if (dateRange.value.toDate) {
        params.toDate = dateRange.value.toDate
      }

      const response = await api.get<ApiResponse<PageResponse<VitalSignResponse>>>(
        `/v1/admissions/${admissionId}/vital-signs`,
        { params }
      )
      if (response.data.success && response.data.data) {
        const signs = response.data.data.content
        vitalSigns.value.set(admissionId, signs)
        totalVitalSigns.value.set(admissionId, response.data.data.page.totalElements)
        return signs
      }
      return []
    } finally {
      loading.value = false
    }
  }

  /**
   * Lightweight prefetch for the hub card metric: fetches only the single most-recent reading
   * (ignoring any active date-range filter so the hub always shows the true latest). Writes to the
   * dedicated summary cache and total map — never the list map — so it cannot truncate a full list
   * loaded by the drilled-in view (regardless of resolution order).
   */
  async function fetchVitalSignsSummary(admissionId: number): Promise<void> {
    const response = await api.get<ApiResponse<PageResponse<VitalSignResponse>>>(
      `/v1/admissions/${admissionId}/vital-signs`,
      { params: { page: 0, size: 1, sort: 'recordedAt,DESC' } }
    )
    if (response.data.success && response.data.data) {
      latestVitalSign.value.set(admissionId, response.data.data.content[0] ?? null)
      totalVitalSigns.value.set(admissionId, response.data.data.page.totalElements)
    }
  }

  async function fetchChartData(admissionId: number): Promise<VitalSignResponse[]> {
    chartLoading.value = true
    try {
      const params: Record<string, unknown> = {}

      // Add date range filters if set
      if (dateRange.value.fromDate) {
        params.fromDate = dateRange.value.fromDate
      }
      if (dateRange.value.toDate) {
        params.toDate = dateRange.value.toDate
      }

      const response = await api.get<ApiResponse<VitalSignResponse[]>>(
        `/v1/admissions/${admissionId}/vital-signs/chart`,
        { params }
      )
      if (response.data.success && response.data.data) {
        chartData.value.set(admissionId, response.data.data)
        return response.data.data
      }
      return []
    } finally {
      chartLoading.value = false
    }
  }

  async function fetchVitalSign(
    admissionId: number,
    vitalSignId: number
  ): Promise<VitalSignResponse> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<VitalSignResponse>>(
        `/v1/admissions/${admissionId}/vital-signs/${vitalSignId}`
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Vital sign not found')
    } finally {
      loading.value = false
    }
  }

  async function createVitalSign(
    admissionId: number,
    data: CreateVitalSignRequest
  ): Promise<VitalSignResponse> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<VitalSignResponse>>(
        `/v1/admissions/${admissionId}/vital-signs`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh both table and chart data in parallel
        await Promise.all([fetchVitalSigns(admissionId), fetchChartData(admissionId)])
        return response.data.data
      }
      throw new Error(response.data.message || 'Create vital sign failed')
    } finally {
      loading.value = false
    }
  }

  async function updateVitalSign(
    admissionId: number,
    vitalSignId: number,
    data: UpdateVitalSignRequest
  ): Promise<VitalSignResponse> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<VitalSignResponse>>(
        `/v1/admissions/${admissionId}/vital-signs/${vitalSignId}`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh both table and chart data in parallel
        await Promise.all([fetchVitalSigns(admissionId), fetchChartData(admissionId)])
        return response.data.data
      }
      throw new Error(response.data.message || 'Update vital sign failed')
    } finally {
      loading.value = false
    }
  }

  function getVitalSigns(admissionId: number): VitalSignResponse[] {
    return vitalSigns.value.get(admissionId) || []
  }

  function getTotalVitalSigns(admissionId: number): number {
    return totalVitalSigns.value.get(admissionId) || 0
  }

  // Most-recent reading for the hub metric: prefer the live list when a drilled-in view has loaded
  // it (stays reactive to creates/edits), otherwise fall back to the summary cache. When a date
  // range filter is active the live list is date-scoped, so its first row is not the true latest —
  // fall through to the unfiltered summary cache in that case.
  function getLatestVitalSign(admissionId: number): VitalSignResponse | null {
    const isFiltered = !!(dateRange.value.fromDate || dateRange.value.toDate)
    const list = vitalSigns.value.get(admissionId)
    if (!isFiltered && list !== undefined) return list[0] ?? null
    return latestVitalSign.value.get(admissionId) ?? null
  }

  function getChartData(admissionId: number): VitalSignResponse[] {
    return chartData.value.get(admissionId) || []
  }

  function setDateRange(range: VitalSignDateRange): void {
    dateRange.value = range
  }

  function clearDateRange(): void {
    dateRange.value = { fromDate: null, toDate: null }
  }

  function clearVitalSigns(admissionId: number): void {
    vitalSigns.value.delete(admissionId)
    totalVitalSigns.value.delete(admissionId)
    latestVitalSign.value.delete(admissionId)
    chartData.value.delete(admissionId)
  }

  function clearAll(): void {
    vitalSigns.value.clear()
    totalVitalSigns.value.clear()
    latestVitalSign.value.clear()
    chartData.value.clear()
    dateRange.value = { fromDate: null, toDate: null }
  }

  return {
    vitalSigns,
    totalVitalSigns,
    latestVitalSign,
    chartData,
    loading,
    chartLoading,
    dateRange,
    fetchVitalSigns,
    fetchVitalSignsSummary,
    fetchChartData,
    fetchVitalSign,
    createVitalSign,
    updateVitalSign,
    getVitalSigns,
    getTotalVitalSigns,
    getLatestVitalSign,
    getChartData,
    setDateRange,
    clearDateRange,
    clearVitalSigns,
    clearAll
  }
})
