import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  Medication,
  CreateMedicationRequest,
  UpdateMedicationRequest,
  MedicationSection,
  InventoryLot
} from '@/types/pharmacy'
import type { ApiResponse, PageResponse } from '@/types'

export const usePharmacyStore = defineStore('pharmacy', () => {
  const items = ref<Medication[]>([])
  const totalItems = ref(0)
  const currentMedication = ref<Medication | null>(null)
  const loading = ref(false)

  async function fetchMedications(
    page = 0,
    size = 20,
    filters: { section?: MedicationSection; controlled?: boolean; search?: string } = {}
  ): Promise<void> {
    loading.value = true
    try {
      const params: Record<string, unknown> = { page, size }
      if (filters.section) params.section = filters.section
      if (filters.controlled !== undefined) params.controlled = filters.controlled
      if (filters.search) params.search = filters.search
      const res = await api.get<ApiResponse<PageResponse<Medication>>>('/v1/medications', {
        params
      })
      if (res.data.success && res.data.data) {
        items.value = res.data.data.content
        totalItems.value = res.data.data.page.totalElements
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchMedication(itemId: number): Promise<Medication> {
    loading.value = true
    currentMedication.value = null
    try {
      const res = await api.get<ApiResponse<Medication>>(`/v1/medications/${itemId}`)
      if (res.data.success && res.data.data) {
        currentMedication.value = res.data.data
        return res.data.data
      }
      throw new Error(res.data.message || 'Medication not found')
    } finally {
      loading.value = false
    }
  }

  async function createMedication(data: CreateMedicationRequest): Promise<Medication> {
    const res = await api.post<ApiResponse<Medication>>('/v1/medications', data)
    if (!res.data.success || !res.data.data) throw new Error(res.data.message || 'Create failed')
    return res.data.data
  }

  async function updateMedication(
    itemId: number,
    data: UpdateMedicationRequest
  ): Promise<Medication> {
    const res = await api.put<ApiResponse<Medication>>(`/v1/medications/${itemId}`, data)
    if (!res.data.success || !res.data.data) throw new Error(res.data.message || 'Update failed')
    currentMedication.value = res.data.data
    return res.data.data
  }

  async function fefoPreview(itemId: number, quantity = 1): Promise<InventoryLot | null> {
    const res = await api.get<ApiResponse<InventoryLot | null>>(
      `/v1/medications/${itemId}/fefo-preview`,
      { params: { quantity } }
    )
    if (res.data.success) return (res.data.data ?? null) as InventoryLot | null
    return null
  }

  return {
    items,
    totalItems,
    currentMedication,
    loading,
    fetchMedications,
    fetchMedication,
    createMedication,
    updateMedication,
    fefoPreview
  }
})
