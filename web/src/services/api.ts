import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { tokenStorage } from '@/utils/tokenStorage'
import { useSessionExpiration } from '@/composables/useSessionExpiration'
import type { ApiResponse, AuthResponse } from '@/types'

interface RetryableRequest extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor - attach access token
api.interceptors.request.use(
  config => {
    const token = tokenStorage.getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// Track if we're currently refreshing to prevent multiple refresh calls
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(promise => {
    if (error) {
      promise.reject(error)
    } else if (token) {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

/**
 * Handles session expiration by clearing tokens and triggering the session expired modal.
 * Uses window.location to get the current path since this code runs outside Vue Router context.
 */
function handleSessionExpiration(): void {
  // Clear tokens FIRST (security requirement)
  tokenStorage.clearTokens()

  // Get the composable and trigger session expiration
  // Note: This works because useSessionExpiration uses module-level state (singleton pattern)
  const { triggerSessionExpired } = useSessionExpiration()

  // Capture full path including query params using window.location
  // (We can't use Vue Router here as api.ts is outside Vue component context)
  const currentPath = window.location.pathname + window.location.search

  triggerSessionExpired(currentPath)
}

// Response interceptor - handle 401 and auto-refresh
api.interceptors.response.use(
  response => {
    // Record activity on successful API calls (user is interacting with the app)
    const { recordActivity } = useSessionExpiration()
    recordActivity()
    return response
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequest

    // If no config or already retried, reject
    if (!originalRequest) {
      return Promise.reject(error)
    }

    // If 401 and not a refresh request and not already retried
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/login')
    ) {
      if (isRefreshing) {
        // Queue the request while refreshing
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(api(originalRequest))
            },
            reject: (err: unknown) => {
              reject(err)
            }
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = tokenStorage.getRefreshToken()

      if (!refreshToken) {
        isRefreshing = false
        handleSessionExpiration()
        return Promise.reject(error)
      }

      try {
        const response = await axios.post<ApiResponse<AuthResponse>>(
          '/api/auth/refresh',
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } }
        )

        if (response.data.success && response.data.data) {
          const { accessToken, refreshToken: newRefreshToken } = response.data.data
          tokenStorage.setTokens(accessToken, newRefreshToken)

          // Reschedule monitoring with new token and record activity
          const { scheduleExpirationCheck, recordActivity } = useSessionExpiration()
          scheduleExpirationCheck()
          recordActivity()

          processQueue(null, accessToken)

          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return api(originalRequest)
        } else {
          throw new Error('Refresh failed')
        }
      } catch (refreshError) {
        processQueue(refreshError, null)
        handleSessionExpiration()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default api
