import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  PsychotherapyActivity,
  CreatePsychotherapyActivityRequest
} from '@/types/psychotherapy'
import type { ApiResponse } from '@/types'

export const usePsychotherapyActivityStore = defineStore('psychotherapyActivity', () => {
  // State - Map by admissionId for caching lists
  const activities = ref<Map<number, PsychotherapyActivity[]>>(new Map())
  const loading = ref(false)

  // Fetch activities for an admission
  async function fetchActivities(
    admissionId: number,
    sortDirection: 'asc' | 'desc' = 'desc'
  ): Promise<PsychotherapyActivity[]> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<PsychotherapyActivity[]>>(
        `/v1/admissions/${admissionId}/psychotherapy-activities`,
        {
          params: { sort: sortDirection }
        }
      )
      if (response.data.success && response.data.data) {
        const activityList = response.data.data
        activities.value.set(admissionId, activityList)
        return activityList
      }
      return []
    } finally {
      loading.value = false
    }
  }

  // Create activity
  async function createActivity(
    admissionId: number,
    data: CreatePsychotherapyActivityRequest
  ): Promise<PsychotherapyActivity> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<PsychotherapyActivity>>(
        `/v1/admissions/${admissionId}/psychotherapy-activities`,
        data
      )
      if (response.data.success && response.data.data) {
        // Refresh the list to include the new activity
        await fetchActivities(admissionId)
        return response.data.data
      }
      throw new Error(response.data.message || 'Create activity failed')
    } finally {
      loading.value = false
    }
  }

  // Delete activity (admin only)
  async function deleteActivity(admissionId: number, activityId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `/v1/admissions/${admissionId}/psychotherapy-activities/${activityId}`
      )
      if (!response.data.success) {
        throw new Error(response.data.message || 'Delete activity failed')
      }
      // Refresh the list after deletion
      await fetchActivities(admissionId)
    } finally {
      loading.value = false
    }
  }

  // Get activities from cache
  function getActivities(admissionId: number): PsychotherapyActivity[] {
    return activities.value.get(admissionId) || []
  }

  // Clear activities for an admission
  function clearActivities(admissionId: number): void {
    activities.value.delete(admissionId)
  }

  // Clear all cached activities
  function clearAll(): void {
    activities.value.clear()
  }

  return {
    activities,
    loading,
    fetchActivities,
    createActivity,
    deleteActivity,
    getActivities,
    clearActivities,
    clearAll
  }
})
