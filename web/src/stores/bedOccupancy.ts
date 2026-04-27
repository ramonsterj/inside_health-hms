import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { BedOccupancyResponse, OccupancySummary, RoomOccupancyItem } from '@/types/room'
import type { ApiResponse } from '@/types'

export const useBedOccupancyStore = defineStore('bedOccupancy', () => {
  const rooms = ref<RoomOccupancyItem[]>([])
  const summary = ref<OccupancySummary>({
    totalBeds: 0,
    occupiedBeds: 0,
    freeBeds: 0,
    occupancyPercent: 0
  })
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastFetchedAt = ref<Date | null>(null)
  const hasLoadedOnce = ref(false)

  async function fetchOccupancy(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const response = await api.get<ApiResponse<BedOccupancyResponse>>('/v1/rooms/occupancy')
      if (response.data.success && response.data.data) {
        rooms.value = response.data.data.rooms
        summary.value = response.data.data.summary
        lastFetchedAt.value = new Date()
        hasLoadedOnce.value = true
      } else {
        throw new Error(response.data.message || 'Failed to load bed occupancy')
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load bed occupancy'
      throw e
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    rooms.value = []
    summary.value = { totalBeds: 0, occupiedBeds: 0, freeBeds: 0, occupancyPercent: 0 }
    loading.value = false
    error.value = null
    lastFetchedAt.value = null
    hasLoadedOnce.value = false
  }

  return {
    rooms,
    summary,
    loading,
    error,
    lastFetchedAt,
    hasLoadedOnce,
    fetchOccupancy,
    reset
  }
})
