import { ref, readonly } from 'vue'
import axios from 'axios'
import { tokenStorage } from '@/utils/tokenStorage'
import type { ApiResponse, AuthResponse } from '@/types'

// =============================================================================
// Configuration Constants
// =============================================================================

/** How often to update activity timestamp (throttle window) */
const ACTIVITY_THROTTLE_MS = 5_000 // 5 seconds

/** User considered "active" if activity within this window */
const ACTIVITY_THRESHOLD_MS = 2 * 60_000 // 2 minutes

/** Start proactive refresh this long before token expiry */
const REFRESH_BUFFER_MS = 90_000 // 90 seconds

// =============================================================================
// Module-level state (singleton pattern)
// =============================================================================

/**
 * Module-level state for singleton pattern.
 * All consumers of useSessionExpiration() share this same state.
 */
const sessionExpired = ref(false)
const intendedRoute = ref<string | null>(null)
let monitoringInitialized = false
let expirationTimeoutId: ReturnType<typeof setTimeout> | null = null

// Activity tracking state
let lastActivityTimestamp: number = Date.now()
let lastActivityUpdate: number = 0
let activityListenersAttached = false
let proactiveRefreshTimeoutId: ReturnType<typeof setTimeout> | null = null
let isProactiveRefreshing = false

/**
 * Decodes a JWT token and returns the payload.
 * JWTs are base64url encoded - we need to decode the middle part (payload).
 */
function decodeJwtPayload(token: string): { exp?: number } | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null

    // Base64url decode the payload (second part)
    // parts[1] is guaranteed to exist after the length check above
    const payload = parts[1] as string
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )

    return JSON.parse(jsonPayload)
  } catch {
    return null
  }
}

/**
 * Gets the expiration timestamp (in milliseconds) from the access token.
 * Returns null if token is invalid or doesn't have expiration.
 */
function getTokenExpirationMs(): number | null {
  const token = tokenStorage.getAccessToken()
  if (!token) return null

  const payload = decodeJwtPayload(token)
  if (!payload?.exp) return null

  // JWT exp is in seconds, convert to milliseconds
  return payload.exp * 1000
}

/**
 * Checks if the access token is expired or missing.
 */
function isTokenExpired(): boolean {
  const expirationMs = getTokenExpirationMs()
  if (expirationMs === null) {
    // No token or invalid token - check if we had tokens (session expired)
    return !tokenStorage.hasTokens()
  }
  return Date.now() >= expirationMs
}

// =============================================================================
// Activity Tracking
// =============================================================================

/**
 * Updates last activity timestamp (throttled).
 * Only updates if enough time has passed since last update to avoid performance issues.
 */
function updateActivity(): void {
  const now = Date.now()
  if (now - lastActivityUpdate >= ACTIVITY_THROTTLE_MS) {
    lastActivityTimestamp = now
    lastActivityUpdate = now
  }
}

/**
 * Returns true if user has been active within the activity threshold.
 */
function isUserActive(): boolean {
  return Date.now() - lastActivityTimestamp < ACTIVITY_THRESHOLD_MS
}

/**
 * Attaches activity event listeners to document.
 * Only attaches once (singleton pattern).
 */
function attachActivityListeners(): void {
  if (activityListenersAttached) return
  activityListenersAttached = true

  const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart']
  events.forEach(event => {
    document.addEventListener(event, updateActivity, { passive: true })
  })
}

// =============================================================================
// Proactive Token Refresh
// =============================================================================

/**
 * Attempts to silently refresh the token before it expires.
 * Only refreshes if user has been recently active.
 */
async function attemptProactiveRefresh(): Promise<void> {
  // Guard against concurrent refresh attempts
  if (isProactiveRefreshing) return
  if (sessionExpired.value) return
  if (!tokenStorage.hasTokens()) return

  // Only refresh if user was recently active
  if (!isUserActive()) {
    // User is inactive - let the session expire naturally
    return
  }

  isProactiveRefreshing = true

  try {
    const refreshToken = tokenStorage.getRefreshToken()
    if (!refreshToken) {
      return
    }

    // Use axios directly to avoid circular dependency with api.ts
    const response = await axios.post<ApiResponse<AuthResponse>>(
      '/api/auth/refresh',
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' } }
    )

    if (response.data.success && response.data.data) {
      const { accessToken, refreshToken: newRefreshToken } = response.data.data
      tokenStorage.setTokens(accessToken, newRefreshToken)

      // Reschedule monitoring with new token
      scheduleExpirationCheck()
    }
  } catch {
    // Proactive refresh failed - will show modal when token actually expires
    // Don't trigger modal here; let the final expiration check handle it
  } finally {
    isProactiveRefreshing = false
  }
}

// =============================================================================
// Expiration Monitoring
// =============================================================================

/**
 * Proactively checks token expiration and triggers the modal if expired.
 * Only triggers if user has tokens (was logged in) and they're now expired.
 */
function checkAndTriggerExpiration(): void {
  // Only check if we have tokens stored (user was logged in)
  if (!tokenStorage.hasTokens()) return

  // Check if already showing the modal
  if (sessionExpired.value) return

  if (isTokenExpired()) {
    const currentPath = window.location.pathname + window.location.search
    // Clear tokens first
    tokenStorage.clearTokens()
    // Trigger the modal
    sessionExpired.value = true
    intendedRoute.value = currentPath
  }
}

/**
 * Schedules proactive refresh and expiration checks.
 * - Proactive refresh: Attempts silent refresh ~90 seconds before expiry (if user active)
 * - Expiration check: Shows modal when token actually expires
 */
function scheduleExpirationCheck(): void {
  // Clear any existing timeouts
  if (expirationTimeoutId) {
    clearTimeout(expirationTimeoutId)
    expirationTimeoutId = null
  }
  if (proactiveRefreshTimeoutId) {
    clearTimeout(proactiveRefreshTimeoutId)
    proactiveRefreshTimeoutId = null
  }

  const expirationMs = getTokenExpirationMs()
  if (expirationMs === null) return

  const timeUntilExpiration = expirationMs - Date.now()

  // If already expired, check now
  if (timeUntilExpiration <= 0) {
    checkAndTriggerExpiration()
    return
  }

  // Cap at 24 hours to avoid setTimeout overflow issues
  const maxTimeout = 24 * 60 * 60 * 1000

  // Schedule proactive refresh (before token expires)
  const timeUntilProactiveRefresh = timeUntilExpiration - REFRESH_BUFFER_MS
  if (timeUntilProactiveRefresh > 0) {
    const proactiveTimeout = Math.min(timeUntilProactiveRefresh, maxTimeout)
    proactiveRefreshTimeoutId = setTimeout(() => {
      attemptProactiveRefresh()
    }, proactiveTimeout)
  } else if (timeUntilExpiration > 0) {
    // Token expires soon but not yet - attempt refresh now if active
    attemptProactiveRefresh()
  }

  // Schedule final expiration check (with a small buffer after expiry)
  const expirationTimeout = Math.min(timeUntilExpiration + 1000, maxTimeout)
  expirationTimeoutId = setTimeout(() => {
    checkAndTriggerExpiration()
  }, expirationTimeout)
}

/**
 * Handles visibility change - checks token when user returns to tab.
 */
function handleVisibilityChange(): void {
  if (document.visibilityState === 'visible') {
    checkAndTriggerExpiration()
  }
}

/**
 * Initializes proactive token expiration monitoring and activity tracking.
 * Only initializes once (singleton pattern).
 */
function initializeMonitoring(): void {
  if (monitoringInitialized) return
  monitoringInitialized = true

  // Attach activity listeners for tracking user interaction
  attachActivityListeners()

  // Check on visibility change (when user returns to tab)
  document.addEventListener('visibilitychange', handleVisibilityChange)

  // Schedule expiration check based on token expiry time
  scheduleExpirationCheck()

  // Also check periodically (every 60 seconds) as a backup
  setInterval(() => {
    checkAndTriggerExpiration()
    scheduleExpirationCheck()
  }, 60000)
}

/**
 * Composable for managing session expiration state.
 *
 * This uses a singleton pattern - the state is shared across all components
 * that use this composable. This is necessary because:
 * 1. The API service (outside Vue context) needs to trigger expiration
 * 2. The modal component needs to read the state
 * 3. Multiple concurrent 401 errors should only show one modal
 *
 * @example
 * // In api.ts interceptor:
 * import { useSessionExpiration } from '@/composables/useSessionExpiration'
 * const { triggerSessionExpired } = useSessionExpiration()
 * triggerSessionExpired(window.location.pathname + window.location.search)
 *
 * @example
 * // In SessionExpiredModal.vue:
 * const { sessionExpired, intendedRoute, resetSessionExpired } = useSessionExpiration()
 */
export function useSessionExpiration() {
  // Initialize monitoring on first use
  initializeMonitoring()

  /**
   * Triggers the session expiration flow.
   * Guards against duplicate calls - if already expired, does nothing.
   *
   * @param route - The full path (including query params) to redirect to after login
   */
  function triggerSessionExpired(route: string): void {
    // Guard against duplicate triggers from concurrent 401 responses
    if (sessionExpired.value) {
      return
    }

    intendedRoute.value = route
    sessionExpired.value = true
  }

  /**
   * Resets the session expiration state.
   * Called after the user acknowledges the modal and is redirected to login.
   */
  function resetSessionExpired(): void {
    sessionExpired.value = false
    intendedRoute.value = null
  }

  return {
    /** Whether the session has expired (read-only to prevent external mutation) */
    sessionExpired: readonly(sessionExpired),
    /** The route to redirect to after login (read-only) */
    intendedRoute: readonly(intendedRoute),
    /** Trigger session expiration - shows the modal */
    triggerSessionExpired,
    /** Reset state - called when user clicks login button */
    resetSessionExpired,
    /** Manually check token expiration (useful after login to reschedule) */
    scheduleExpirationCheck,
    /** Record user activity - call this on API requests to track activity */
    recordActivity: updateActivity
  }
}
