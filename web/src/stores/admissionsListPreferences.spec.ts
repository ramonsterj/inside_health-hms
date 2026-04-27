import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAdmissionsListPreferencesStore } from './admissionsListPreferences'
import { useAuthStore } from './auth'
import type { User } from '@/types'
import { UserStatus } from '@/types/user'

const STORAGE_KEY_PREFIX = 'hms.admissionsListView.v1'

function makeUser(id: number): User {
  return {
    id,
    username: `user${id}`,
    email: `user${id}@example.com`,
    firstName: 'Test',
    lastName: 'User',
    salutation: null,
    salutationDisplay: null,
    roles: ['ADMIN'],
    permissions: [],
    status: UserStatus.ACTIVE,
    emailVerified: true,
    mustChangePassword: false,
    createdAt: null,
    localePreference: null,
    phoneNumbers: []
  }
}

describe('useAdmissionsListPreferencesStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('hydration', () => {
    it('falls back to defaults when no user is logged in', () => {
      const store = useAdmissionsListPreferencesStore()
      expect(store.viewMode).toBe('cards')
      expect(store.groupBy).toBe('gender')
    })

    it('falls back to defaults on first visit (no stored value)', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(42))
      const store = useAdmissionsListPreferencesStore()
      expect(store.viewMode).toBe('cards')
      expect(store.groupBy).toBe('gender')
    })

    it('reads stored preferences keyed by user id', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.7`,
        JSON.stringify({ viewMode: 'table', groupBy: 'type' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(7))
      const store = useAdmissionsListPreferencesStore()
      expect(store.viewMode).toBe('table')
      expect(store.groupBy).toBe('type')
    })

    it('falls back to defaults when stored JSON is corrupt', () => {
      localStorage.setItem(`${STORAGE_KEY_PREFIX}.user.9`, '{not-valid-json')
      const auth = useAuthStore()
      auth.setUser(makeUser(9))
      const store = useAdmissionsListPreferencesStore()
      expect(store.viewMode).toBe('cards')
      expect(store.groupBy).toBe('gender')
    })

    it('falls back to defaults when stored payload fails schema validation', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.11`,
        JSON.stringify({ viewMode: 'unknown', groupBy: 'wat' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(11))
      const store = useAdmissionsListPreferencesStore()
      expect(store.viewMode).toBe('cards')
      expect(store.groupBy).toBe('gender')
    })

    it('uses different keys for different users', () => {
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.1`,
        JSON.stringify({ viewMode: 'table', groupBy: 'type' })
      )
      localStorage.setItem(
        `${STORAGE_KEY_PREFIX}.user.2`,
        JSON.stringify({ viewMode: 'cards', groupBy: 'none' })
      )
      const auth = useAuthStore()
      auth.setUser(makeUser(1))
      const store = useAdmissionsListPreferencesStore()
      expect(store.viewMode).toBe('table')
      expect(store.groupBy).toBe('type')

      auth.setUser(makeUser(2))
      // The watcher rehydrates after the user switches.
      expect(store.viewMode).toBe('cards')
      expect(store.groupBy).toBe('none')
    })
  })

  describe('persistence', () => {
    it('persists view mode changes to localStorage under the user-scoped key', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(5))
      const store = useAdmissionsListPreferencesStore()

      store.setViewMode('table')

      const raw = localStorage.getItem(`${STORAGE_KEY_PREFIX}.user.5`)
      expect(raw).not.toBeNull()
      expect(JSON.parse(raw!)).toEqual({ viewMode: 'table', groupBy: 'gender' })
    })

    it('persists groupBy changes to localStorage', () => {
      const auth = useAuthStore()
      auth.setUser(makeUser(5))
      const store = useAdmissionsListPreferencesStore()

      store.setGroupBy('type')

      const raw = localStorage.getItem(`${STORAGE_KEY_PREFIX}.user.5`)
      expect(JSON.parse(raw!)).toEqual({ viewMode: 'cards', groupBy: 'type' })
    })

    it('does not write to localStorage before a user has been hydrated', () => {
      const store = useAdmissionsListPreferencesStore()
      store.setViewMode('table')
      // No user → no key was created.
      expect(localStorage.getItem(`${STORAGE_KEY_PREFIX}.user.null`)).toBeNull()
    })
  })
})
