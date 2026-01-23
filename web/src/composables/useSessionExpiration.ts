import { ref, readonly } from 'vue'
import { tokenStorage } from '@/utils/tokenStorage'

/**
 * Module-level state for singleton pattern.
 * All consumers of useSessionExpiration() share this same state.
 */
const sessionExpired = ref(false)
const intendedRoute = ref<string | null>(null)
let monitoringInitialized = false
let expirationTimeoutId: ReturnType<typeof setTimeout> | null = null

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
 * Schedules a check for when the token expires.
 * This ensures we proactively show the modal right when the token expires.
 */
function scheduleExpirationCheck(): void {
  // Clear any existing timeout
  if (expirationTimeoutId) {
    clearTimeout(expirationTimeoutId)
    expirationTimeoutId = null
  }

  const expirationMs = getTokenExpirationMs()
  if (expirationMs === null) return

  const timeUntilExpiration = expirationMs - Date.now()

  // If already expired, check now
  if (timeUntilExpiration <= 0) {
    checkAndTriggerExpiration()
    return
  }

  // Schedule check for when token expires (with a small buffer)
  // Cap at 24 hours to avoid setTimeout overflow issues
  const maxTimeout = 24 * 60 * 60 * 1000
  const timeout = Math.min(timeUntilExpiration + 1000, maxTimeout)

  expirationTimeoutId = setTimeout(() => {
    checkAndTriggerExpiration()
  }, timeout)
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
 * Initializes proactive token expiration monitoring.
 * Only initializes once (singleton pattern).
 */
function initializeMonitoring(): void {
  if (monitoringInitialized) return
  monitoringInitialized = true

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
    scheduleExpirationCheck
  }
}
