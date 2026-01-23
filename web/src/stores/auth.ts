import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import { tokenStorage } from '@/utils/tokenStorage'
import { useLocaleStore } from '@/stores/locale'
import type {
  User,
  LoginRequest,
  ApiResponse,
  AuthResponse,
  UsernameAvailabilityResponse
} from '@/types'
import type { ForceChangePasswordFormData } from '@/validation/user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => !!user.value && tokenStorage.hasTokens())
  const isAdmin = computed(() => user.value?.roles?.includes('ADMIN') ?? false)
  const mustChangePassword = computed(() => user.value?.mustChangePassword ?? false)

  function hasPermission(permission: string): boolean {
    if (!user.value) return false
    // Admins have all permissions
    if (user.value.roles?.includes('ADMIN')) return true
    return user.value.permissions?.includes(permission) ?? false
  }

  async function login(credentials: LoginRequest): Promise<void> {
    loading.value = true
    try {
      const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', credentials)
      if (response.data.success && response.data.data) {
        const { accessToken, refreshToken, user: userData } = response.data.data
        tokenStorage.setTokens(accessToken, refreshToken)
        user.value = userData
        // Initialize locale from user's stored preference
        const localeStore = useLocaleStore()
        localeStore.initFromUser(userData.localePreference)
      } else {
        throw new Error(response.data.message || 'Login failed')
      }
    } finally {
      loading.value = false
    }
  }

  async function changePasswordForced(data: ForceChangePasswordFormData): Promise<void> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<User>>('/users/me/password', {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword
      })
      if (response.data.success && response.data.data) {
        // Update user with mustChangePassword = false
        user.value = response.data.data
      } else if (response.data.success) {
        // If no user data returned, just update the flag locally
        if (user.value) {
          user.value = { ...user.value, mustChangePassword: false }
        }
      } else {
        throw new Error(response.data.message || 'Password change failed')
      }
    } finally {
      loading.value = false
    }
  }

  async function refreshToken(): Promise<void> {
    const refresh = tokenStorage.getRefreshToken()
    if (!refresh) {
      throw new Error('No refresh token')
    }

    const response = await api.post<ApiResponse<AuthResponse>>('/auth/refresh', {
      refreshToken: refresh
    })

    if (response.data.success && response.data.data) {
      const { accessToken, refreshToken: newRefresh, user: userData } = response.data.data
      tokenStorage.setTokens(accessToken, newRefresh)
      user.value = userData
      // Initialize locale from user's stored preference
      const localeStore = useLocaleStore()
      localeStore.initFromUser(userData.localePreference)
    } else {
      throw new Error('Token refresh failed')
    }
  }

  async function logout(): Promise<void> {
    try {
      await api.post('/auth/logout')
    } catch {
      // Ignore errors during logout
    } finally {
      tokenStorage.clearTokens()
      user.value = null
    }
  }

  async function fetchCurrentUser(): Promise<void> {
    if (!tokenStorage.hasTokens()) {
      return
    }

    try {
      const response = await api.get<ApiResponse<User>>('/users/me')
      if (response.data.success && response.data.data) {
        user.value = response.data.data
        // Initialize locale from user's stored preference
        const localeStore = useLocaleStore()
        localeStore.initFromUser(response.data.data.localePreference)
      }
    } catch {
      tokenStorage.clearTokens()
      user.value = null
    }
  }

  function setUser(userData: User) {
    user.value = userData
  }

  async function checkUsernameAvailability(
    username: string
  ): Promise<UsernameAvailabilityResponse> {
    const response = await api.get<ApiResponse<UsernameAvailabilityResponse>>(
      '/auth/check-username',
      { params: { username } }
    )
    if (response.data.success && response.data.data) {
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to check username availability')
  }

  return {
    user,
    loading,
    isAuthenticated,
    isAdmin,
    mustChangePassword,
    hasPermission,
    login,
    changePasswordForced,
    refreshToken,
    logout,
    fetchCurrentUser,
    setUser,
    checkUsernameAvailability
  }
})
