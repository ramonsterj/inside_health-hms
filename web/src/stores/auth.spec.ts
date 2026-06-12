import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from './auth'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('hasPermission', () => {
    it('returns true only when the permission is in the user permission list', () => {
      const auth = useAuthStore()
      auth.$patch({ user: { roles: ['MEDICO'], permissions: ['patient:read'] } } as never)

      expect(auth.hasPermission('patient:read')).toBe(true)
      expect(auth.hasPermission('patient:update')).toBe(false)
    })

    it('ignores roles — ADMINISTRADOR with no permissions has none (no admin bypass)', () => {
      const auth = useAuthStore()
      auth.$patch({ user: { roles: ['ADMINISTRADOR'], permissions: [] } } as never)

      expect(auth.hasPermission('user:read')).toBe(false)
    })

    it('returns false when there is no authenticated user', () => {
      const auth = useAuthStore()
      expect(auth.hasPermission('patient:read')).toBe(false)
    })
  })

  describe('isAdmin', () => {
    it('is true when the user carries the ADMINISTRADOR role', () => {
      const auth = useAuthStore()
      auth.$patch({ user: { roles: ['ADMINISTRADOR'], permissions: [] } } as never)
      expect(auth.isAdmin).toBe(true)
    })

    it('is false for non-admin roles', () => {
      const auth = useAuthStore()
      auth.$patch({ user: { roles: ['MEDICO'], permissions: [] } } as never)
      expect(auth.isAdmin).toBe(false)
    })
  })
})
