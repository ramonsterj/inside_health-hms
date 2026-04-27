import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { z } from 'zod'
import { useAuthStore } from '@/stores/auth'

export type AdmissionsListViewMode = 'cards' | 'table'
export type AdmissionsListGroupBy = 'none' | 'gender' | 'type' | 'triage'

export interface AdmissionsListPreferences {
  viewMode: AdmissionsListViewMode
  groupBy: AdmissionsListGroupBy
}

// Versioned suffix lets us evolve the schema later without surprising parses.
const STORAGE_KEY_PREFIX = 'hms.admissionsListView.v1'

const DEFAULTS: AdmissionsListPreferences = {
  viewMode: 'cards',
  groupBy: 'gender'
}

const preferencesSchema = z.object({
  viewMode: z.enum(['cards', 'table']),
  groupBy: z.enum(['none', 'gender', 'type', 'triage'])
})

function storageKeyForUser(userId: number | null | undefined): string | null {
  if (userId === null || userId === undefined) return null
  return `${STORAGE_KEY_PREFIX}.user.${userId}`
}

function readFromStorage(userId: number): AdmissionsListPreferences {
  const key = storageKeyForUser(userId)
  if (!key) return { ...DEFAULTS }
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return { ...DEFAULTS }
    const parsed = preferencesSchema.safeParse(JSON.parse(raw))
    if (!parsed.success) return { ...DEFAULTS }
    return parsed.data
  } catch {
    return { ...DEFAULTS }
  }
}

function writeToStorage(userId: number, prefs: AdmissionsListPreferences): void {
  const key = storageKeyForUser(userId)
  if (!key) return
  try {
    localStorage.setItem(key, JSON.stringify(prefs))
  } catch {
    // Ignore quota / serialization errors — preference is non-critical.
  }
}

export const useAdmissionsListPreferencesStore = defineStore('admissionsListPreferences', () => {
  const viewMode = ref<AdmissionsListViewMode>(DEFAULTS.viewMode)
  const groupBy = ref<AdmissionsListGroupBy>(DEFAULTS.groupBy)
  const hydratedUserId = ref<number | null>(null)

  function hydrate(): void {
    const auth = useAuthStore()
    const userId = auth.user?.id
    if (userId === null || userId === undefined) {
      // Pre-login: keep defaults but don't write to storage.
      viewMode.value = DEFAULTS.viewMode
      groupBy.value = DEFAULTS.groupBy
      hydratedUserId.value = null
      return
    }
    if (hydratedUserId.value === userId) return
    const stored = readFromStorage(userId)
    viewMode.value = stored.viewMode
    groupBy.value = stored.groupBy
    hydratedUserId.value = userId
  }

  function persist(): void {
    if (hydratedUserId.value === null) return
    writeToStorage(hydratedUserId.value, {
      viewMode: viewMode.value,
      groupBy: groupBy.value
    })
  }

  function setViewMode(mode: AdmissionsListViewMode): void {
    viewMode.value = mode
    persist()
  }

  function setGroupBy(group: AdmissionsListGroupBy): void {
    groupBy.value = group
    persist()
  }

  // Re-hydrate when the authenticated user changes (login/logout/switch user).
  // `flush: 'sync'` ensures hydration happens before any subsequent read of the
  // store's preferences in the same tick.
  const auth = useAuthStore()
  watch(
    () => auth.user?.id ?? null,
    () => hydrate(),
    { immediate: true, flush: 'sync' }
  )

  return {
    viewMode,
    groupBy,
    hydrate,
    setViewMode,
    setGroupBy
  }
})
