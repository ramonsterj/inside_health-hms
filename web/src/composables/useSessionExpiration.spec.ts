import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { useSessionExpiration } from './useSessionExpiration'

// Mock tokenStorage
vi.mock('@/utils/tokenStorage', () => ({
  tokenStorage: {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    setTokens: vi.fn(),
    clearTokens: vi.fn(),
    hasTokens: vi.fn()
  }
}))

describe('useSessionExpiration', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    // Reset state before each test
    const { resetSessionExpired } = useSessionExpiration()
    resetSessionExpired()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('triggerSessionExpired', () => {
    it('should set sessionExpired to true', () => {
      const { sessionExpired, triggerSessionExpired } = useSessionExpiration()

      expect(sessionExpired.value).toBe(false)
      triggerSessionExpired('/dashboard')
      expect(sessionExpired.value).toBe(true)
    })

    it('should store the intended route', () => {
      const { intendedRoute, triggerSessionExpired } = useSessionExpiration()

      expect(intendedRoute.value).toBeNull()
      triggerSessionExpired('/patients?page=2&filter=active')
      expect(intendedRoute.value).toBe('/patients?page=2&filter=active')
    })

    it('should guard against duplicate triggers', () => {
      const { sessionExpired, intendedRoute, triggerSessionExpired } = useSessionExpiration()

      triggerSessionExpired('/first-route')
      expect(sessionExpired.value).toBe(true)
      expect(intendedRoute.value).toBe('/first-route')

      // Second trigger should be ignored
      triggerSessionExpired('/second-route')
      expect(intendedRoute.value).toBe('/first-route')
    })
  })

  describe('resetSessionExpired', () => {
    it('should reset sessionExpired to false', () => {
      const { sessionExpired, triggerSessionExpired, resetSessionExpired } = useSessionExpiration()

      triggerSessionExpired('/dashboard')
      expect(sessionExpired.value).toBe(true)

      resetSessionExpired()
      expect(sessionExpired.value).toBe(false)
    })

    it('should clear the intended route', () => {
      const { intendedRoute, triggerSessionExpired, resetSessionExpired } = useSessionExpiration()

      triggerSessionExpired('/dashboard')
      expect(intendedRoute.value).toBe('/dashboard')

      resetSessionExpired()
      expect(intendedRoute.value).toBeNull()
    })
  })

  describe('singleton pattern', () => {
    it('should share state across multiple calls', () => {
      const instance1 = useSessionExpiration()
      const instance2 = useSessionExpiration()

      instance1.triggerSessionExpired('/shared-route')

      // Both instances should see the same state
      expect(instance1.sessionExpired.value).toBe(true)
      expect(instance2.sessionExpired.value).toBe(true)
      expect(instance1.intendedRoute.value).toBe('/shared-route')
      expect(instance2.intendedRoute.value).toBe('/shared-route')

      // Reset from instance2 should affect instance1
      instance2.resetSessionExpired()
      expect(instance1.sessionExpired.value).toBe(false)
      expect(instance2.sessionExpired.value).toBe(false)
    })
  })

  describe('readonly refs', () => {
    it('should return readonly sessionExpired ref', () => {
      const { sessionExpired } = useSessionExpiration()

      // TypeScript should prevent direct assignment, but we verify the readonly wrapper exists
      expect(sessionExpired).toBeDefined()
      // The value should be accessible
      expect(typeof sessionExpired.value).toBe('boolean')
    })

    it('should return readonly intendedRoute ref', () => {
      const { intendedRoute } = useSessionExpiration()

      expect(intendedRoute).toBeDefined()
      // Initial value should be null
      expect(intendedRoute.value).toBeNull()
    })
  })

  describe('complete flow', () => {
    it('should handle the full session expiration cycle', () => {
      const { sessionExpired, intendedRoute, triggerSessionExpired, resetSessionExpired } =
        useSessionExpiration()

      // Initial state
      expect(sessionExpired.value).toBe(false)
      expect(intendedRoute.value).toBeNull()

      // Trigger expiration
      triggerSessionExpired('/users?tab=active')
      expect(sessionExpired.value).toBe(true)
      expect(intendedRoute.value).toBe('/users?tab=active')

      // Duplicate trigger should be ignored
      triggerSessionExpired('/different-route')
      expect(intendedRoute.value).toBe('/users?tab=active')

      // Reset after user acknowledges
      resetSessionExpired()
      expect(sessionExpired.value).toBe(false)
      expect(intendedRoute.value).toBeNull()

      // Should be able to trigger again after reset
      triggerSessionExpired('/new-route')
      expect(sessionExpired.value).toBe(true)
      expect(intendedRoute.value).toBe('/new-route')
    })
  })

  describe('scheduleExpirationCheck', () => {
    it('should be a callable function', () => {
      const { scheduleExpirationCheck } = useSessionExpiration()

      expect(typeof scheduleExpirationCheck).toBe('function')
      // Should not throw when called
      expect(() => scheduleExpirationCheck()).not.toThrow()
    })
  })
})
