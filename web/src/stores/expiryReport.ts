import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { ExpiryReport, MedicationSection } from '@/types/pharmacy'
import type { ApiResponse } from '@/types'

export const useExpiryReportStore = defineStore('expiryReport', () => {
  const report = ref<ExpiryReport | null>(null)
  const loading = ref(false)

  async function fetch(
    opts: {
      window?: number
      urgentWindow?: number
      section?: MedicationSection
      controlled?: boolean
    } = {}
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = {}
      if (opts.window !== undefined) params.window = opts.window
      if (opts.urgentWindow !== undefined) params.urgentWindow = opts.urgentWindow
      if (opts.section) params.section = opts.section
      if (opts.controlled !== undefined) params.controlled = opts.controlled
      const res = await api.get<ApiResponse<ExpiryReport>>('/v1/medications/expiry-report', {
        params
      })
      if (res.data.success && res.data.data) report.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  return { report, loading, fetch }
})
