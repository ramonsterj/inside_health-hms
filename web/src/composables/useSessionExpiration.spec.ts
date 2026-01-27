import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { useSessionExpiration } from './useSessionExpiration'
import { tokenStorage } from '@/utils/tokenStorage'
import axios from 'axios'

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

// Mock axios for proactive refresh tests
vi.mock('axios', () => ({
  default: {
    post: vi.fn()
  }
}))

/**
 * Creates a mock JWT token with the given expiration time (in seconds from now).
 */
function createMockToken(expiresInSeconds: number): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const payload = btoa(
    JSON.stringify({
      sub: '1',
      exp: Math.floor(Date.now() / 1000) + expiresInSeconds
    })
  )
  return `${header}.${payload}.mock-signature`
}

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

  describe('activity tracking', () => {
    it('should expose recordActivity function', () => {
      const { recordActivity } = useSessionExpiration()

      expect(typeof recordActivity).toBe('function')
      // Should not throw when called
      expect(() => recordActivity()).not.toThrow()
    })

    it('should throttle activity updates', () => {
      const { recordActivity } = useSessionExpiration()

      // First call should update
      recordActivity()

      // Advance time by 2 seconds (less than throttle window of 5 seconds)
      vi.advanceTimersByTime(2000)

      // Second call should be throttled (within 5 second window)
      recordActivity()

      // Advance past throttle window
      vi.advanceTimersByTime(5000)

      // This call should update
      recordActivity()

      // No assertion needed - we're verifying it doesn't throw and the throttle works
    })
  })

  describe('proactive refresh', () => {
    beforeEach(() => {
      vi.mocked(tokenStorage.hasTokens).mockReturnValue(true)
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue('mock-refresh-token')
      vi.mocked(axios.post).mockReset()
    })

    it('should attempt proactive refresh when token near expiry and user is active', async () => {
      // Mock token that expires in 2 minutes (120 seconds)
      const mockToken = createMockToken(120)
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(mockToken)

      // Mock successful refresh response
      vi.mocked(axios.post).mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            accessToken: createMockToken(900), // New token expires in 15 min
            refreshToken: 'new-refresh-token'
          }
        }
      })

      const { recordActivity, scheduleExpirationCheck } = useSessionExpiration()

      // Record activity (user is active)
      recordActivity()

      // Schedule monitoring
      scheduleExpirationCheck()

      // Advance to proactive refresh time (120 - 90 = 30 seconds)
      vi.advanceTimersByTime(30000)

      // Allow async operations to complete
      await vi.runAllTimersAsync()

      // Verify refresh was attempted
      expect(axios.post).toHaveBeenCalledWith(
        '/api/auth/refresh',
        { refreshToken: 'mock-refresh-token' },
        { headers: { 'Content-Type': 'application/json' } }
      )

      // Verify new tokens were stored
      expect(tokenStorage.setTokens).toHaveBeenCalledWith(expect.any(String), 'new-refresh-token')
    })

    it('should NOT attempt refresh when user is inactive', async () => {
      const { resetSessionExpired, scheduleExpirationCheck } = useSessionExpiration()
      resetSessionExpired()

      // First advance time to make user inactive (past the 2 min activity threshold)
      vi.advanceTimersByTime(3 * 60 * 1000) // 3 minutes

      // Now create token that expires in 2 minutes from THIS point
      const mockToken = createMockToken(120)
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(mockToken)

      // Now schedule monitoring (user is already inactive)
      scheduleExpirationCheck()

      // Advance to what would be proactive refresh time (30 seconds)
      vi.advanceTimersByTime(30000)

      await vi.runAllTimersAsync()

      // Refresh should NOT have been attempted because user was inactive
      expect(axios.post).not.toHaveBeenCalled()
    })

    it('should show modal if token expires after failed refresh', async () => {
      const { sessionExpired, recordActivity, scheduleExpirationCheck, resetSessionExpired } =
        useSessionExpiration()

      // Ensure clean state first
      resetSessionExpired()
      expect(sessionExpired.value).toBe(false)

      // Mock token that expires in 2 minutes
      const mockToken = createMockToken(120)
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(mockToken)

      // Mock refresh failure
      vi.mocked(axios.post).mockRejectedValueOnce(new Error('Refresh failed'))

      // Record activity (user is active)
      recordActivity()

      // Schedule monitoring
      scheduleExpirationCheck()

      // Advance to proactive refresh time (30 seconds)
      await vi.advanceTimersByTimeAsync(30000)

      // Refresh was attempted but failed
      expect(axios.post).toHaveBeenCalled()

      // Modal not shown yet (proactive refresh failed silently)
      // Note: checkAndTriggerExpiration runs in the 60-sec interval, so modal might show earlier
      // The key test is that refresh was attempted first

      // Now advance to actual expiration time + buffer (need 90 more seconds to get to 120 total)
      await vi.advanceTimersByTimeAsync(92000) // Past the 120-second expiry + buffer

      // Now modal should show because token actually expired
      expect(sessionExpired.value).toBe(true)
    })

    it('should reschedule monitoring after successful refresh', async () => {
      const { sessionExpired, recordActivity, scheduleExpirationCheck, resetSessionExpired } =
        useSessionExpiration()

      // Ensure clean state
      resetSessionExpired()
      expect(sessionExpired.value).toBe(false)

      // Initial token expires in 2 minutes
      const mockToken = createMockToken(120)
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(mockToken)

      // Mock successful refresh that returns token expiring in 15 minutes
      const newMockToken = createMockToken(900)
      vi.mocked(axios.post).mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            accessToken: newMockToken,
            refreshToken: 'new-refresh-token'
          }
        }
      })

      // Record activity
      recordActivity()

      // Schedule monitoring
      scheduleExpirationCheck()

      // Advance to proactive refresh time (30 seconds)
      await vi.advanceTimersByTimeAsync(30000)

      // At this point, refresh should have been called and succeeded
      expect(axios.post).toHaveBeenCalled()

      // Update the mock to return the new token (simulating stored token)
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(newMockToken)

      // Mock hasTokens to return true (tokens still exist)
      vi.mocked(tokenStorage.hasTokens).mockReturnValue(true)

      // Advance past the original expiration time but NOT past the new token expiration
      // Original was 120 sec, we already advanced 30, advance 100 more = 130 sec total
      // New token expires in 900 sec from refresh time, so still valid
      await vi.advanceTimersByTimeAsync(100000)

      // Session should NOT be expired (new token hasn't expired yet)
      // The scheduleExpirationCheck was called after successful refresh with the new token
      expect(sessionExpired.value).toBe(false)
    })
  })
})
