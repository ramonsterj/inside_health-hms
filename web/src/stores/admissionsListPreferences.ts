import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { z } from 'zod'
import { useAuthStore } from '@/stores/auth'

export type AdmissionsListGroupBy = 'none' | 'gender' | 'type' | 'triage'

export interface AdmissionsListPreferences {
  primaryGroupBy: AdmissionsListGroupBy
  secondaryGroupBy: AdmissionsListGroupBy
}

// Versioned suffix lets us evolve the schema later without surprising parses.
// Bumped v1 → v2 when the single `groupBy` (plus a removed `viewMode`) was
// replaced by two-level `primaryGroupBy` / `secondaryGroupBy` grouping; old v1
// payloads simply fail the v2 schema and fall back to defaults.
const STORAGE_KEY_PREFIX = 'hms.admissionsListView.v2'

const DEFAULTS: AdmissionsListPreferences = {
  primaryGroupBy: 'type',
  secondaryGroupBy: 'none'
}

const preferencesSchema = z.object({
  primaryGroupBy: z.enum(['none', 'gender', 'type', 'triage']),
  secondaryGroupBy: z.enum(['none', 'gender', 'type', 'triage'])
})

/**
 * Enforces the two-level grouping invariants so the render layer never needs
 * defensive checks:
 * - if the primary level is `none`, there is nothing to subdivide → secondary
 *   collapses to `none`;
 * - the secondary level may never repeat the primary dimension → collapses to
 *   `none`.
 */
function normalize(prefs: AdmissionsListPreferences): AdmissionsListPreferences {
  if (prefs.primaryGroupBy === 'none') {
    return { primaryGroupBy: 'none', secondaryGroupBy: 'none' }
  }
  if (prefs.secondaryGroupBy === prefs.primaryGroupBy) {
    return { primaryGroupBy: prefs.primaryGroupBy, secondaryGroupBy: 'none' }
  }
  return prefs
}

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
    return normalize(parsed.data)
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
  const primaryGroupBy = ref<AdmissionsListGroupBy>(DEFAULTS.primaryGroupBy)
  const secondaryGroupBy = ref<AdmissionsListGroupBy>(DEFAULTS.secondaryGroupBy)
  const hydratedUserId = ref<number | null>(null)

  function hydrate(): void {
    const auth = useAuthStore()
    const userId = auth.user?.id
    if (userId === null || userId === undefined) {
      // Pre-login: keep defaults but don't write to storage.
      primaryGroupBy.value = DEFAULTS.primaryGroupBy
      secondaryGroupBy.value = DEFAULTS.secondaryGroupBy
      hydratedUserId.value = null
      return
    }
    if (hydratedUserId.value === userId) return
    const stored = readFromStorage(userId)
    primaryGroupBy.value = stored.primaryGroupBy
    secondaryGroupBy.value = stored.secondaryGroupBy
    hydratedUserId.value = userId
  }

  function persist(): void {
    if (hydratedUserId.value === null) return
    writeToStorage(hydratedUserId.value, {
      primaryGroupBy: primaryGroupBy.value,
      secondaryGroupBy: secondaryGroupBy.value
    })
  }

  function setPrimaryGroupBy(value: AdmissionsListGroupBy): void {
    const next = normalize({ primaryGroupBy: value, secondaryGroupBy: secondaryGroupBy.value })
    primaryGroupBy.value = next.primaryGroupBy
    secondaryGroupBy.value = next.secondaryGroupBy
    persist()
  }

  function setSecondaryGroupBy(value: AdmissionsListGroupBy): void {
    const next = normalize({ primaryGroupBy: primaryGroupBy.value, secondaryGroupBy: value })
    // `normalize` is a no-op for the primary; only the secondary can change here.
    secondaryGroupBy.value = next.secondaryGroupBy
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
    primaryGroupBy,
    secondaryGroupBy,
    hydrate,
    setPrimaryGroupBy,
    setSecondaryGroupBy
  }
})
