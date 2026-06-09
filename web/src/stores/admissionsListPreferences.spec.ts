import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAdmissionsListPreferencesStore } from './admissionsListPreferences'
import { useAuthStore } from './auth'
import type { User } from '@/types'
import { UserStatus } from '@/types/user'

const STORAGE_KEY_PREFIX = 'hms.admissionsListView.v2'

function makeUser(id: number): User {
  return {
    id,
    username: `user${id}`,
    email: `user${id}@example.com`,
    firstName: 'Test',
    lastName: 'User',
    salutation: null,
    salutationDisplay: null,
    roles: ['ADMINISTRADOR'],
    permissions: [],
    status: UserStatus.ACTIVE,
    emailVerified: true,
    mustChangePassword: false,
    createdAt: null,
    localePreference: null,
    phoneNumbers: [],
    assignedWarehouseIds: []
  }
}

describe('useAdmissionsListPreferencesStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('hydration', () => {
    it('falls back to defaults when no user is logged in', () => {
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('falls back to defaults on first visit (no stored value)', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(42))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('reads stored preferences keyed by user id', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.7`,
        JSON.stringify({ primaryGroupBy: 'type', secondaryGroupBy: 'gender' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(7))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('gender')
    })

    it('normalizes a stored payload that violates invariants (secondary == primary)', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.8`,
        JSON.stringify({ primaryGroupBy: 'type', secondaryGroupBy: 'type' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(8))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('normalizes a stored payload with primary none and a stale secondary', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.10`,
        JSON.stringify({ primaryGroupBy: 'none', secondaryGroupBy: 'gender' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(10))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('none')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('falls back to defaults when stored JSON is corrupt', () => {
      localStorage.setItem(`${STORAGE_KEY_PREFIX}.user.9`, '{not-valid-json')
      const auth = useAuthStore()
      auth.setUser(makeUser(9))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('falls back to defaults when stored payload fails schema validation', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.11`,
        JSON.stringify({ primaryGroupBy: 'wat', secondaryGroupBy: 'nope' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(11))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('falls back to defaults for a legacy v1 payload (different storage key)', () => {
      // Old v1 key/shape is not read by the v2 store.
      localStorage.setItem(
        `hms.admissionsListView.v1.user.12`,
        JSON.stringify({ viewMode: 'table', groupBy: 'gender' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(12))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('uses different keys for different users', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.1`,
        JSON.stringify({ primaryGroupBy: 'type', secondaryGroupBy: 'gender' })
      )
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.2`,
        JSON.stringify({ primaryGroupBy: 'none', secondaryGroupBy: 'none' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(1))
      const store = useAdmissionsListPreferencesStore()
      expect(store.primaryGroupBy).toBe('type')
      expect(store.secondaryGroupBy).toBe('gender')

      auth.setUser(makeUser(2))
      // The watcher rehydrates after the user switches.
      expect(store.primaryGroupBy).toBe('none')
      expect(store.secondaryGroupBy).toBe('none')
    })
  })

  describe('persistence', () => {
    it('persists primary group changes and re-normalizes the secondary', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(5))
      const store = useAdmissionsListPreferencesStore()

      store.setSecondaryGroupBy('gender')
      store.setPrimaryGroupBy('gender')

      // Primary now equals the old secondary → secondary collapses to 'none'.
      expect(store.primaryGroupBy).toBe('gender')
      expect(store.secondaryGroupBy).toBe('none')
      const raw = localStorage.getItem(`${STORAGE_KEY_PREFIX}.user.5`)
      expect(raw).not.toBeNull()
      expect(JSON.parse(raw!)).toEqual({ primaryGroupBy: 'gender', secondaryGroupBy: 'none' })
    })

    it('persists secondary group changes to localStorage', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(5))
      const store = useAdmissionsListPreferencesStore()

      store.setSecondaryGroupBy('gender')

      const raw = localStorage.getItem(`${STORAGE_KEY_PREFIX}.user.5`)
      expect(JSON.parse(raw!)).toEqual({ primaryGroupBy: 'type', secondaryGroupBy: 'gender' })
    })

    it('ignores a secondary value that equals the primary', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(5))
      const store = useAdmissionsListPreferencesStore()

      store.setSecondaryGroupBy('type')

      expect(store.secondaryGroupBy).toBe('none')
    })

    it('forces secondary to none when primary becomes none', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(5))
      const store = useAdmissionsListPreferencesStore()

      store.setSecondaryGroupBy('gender')
      store.setPrimaryGroupBy('none')

      expect(store.primaryGroupBy).toBe('none')
      expect(store.secondaryGroupBy).toBe('none')
    })

    it('does not write to localStorage before a user has been hydrated', () => {
      const store = useAdmissionsListPreferencesStore()
      store.setPrimaryGroupBy('gender')
      // No user → no key was created.
      expect(localStorage.getItem(`${STORAGE_KEY_PREFIX}.user.null`)).toBeNull()
    })
  })
})
