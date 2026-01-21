import { ref, watch, type Ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

export interface UsernameAvailabilityState {
  isChecking: boolean
  isAvailable: boolean | null
  message: string | null
  error: string | null
}

function debounce<T extends (...args: Parameters<T>) => void>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  return (...args: Parameters<T>) => {
    if (timeoutId) clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn(...args), delay)
  }
}

export function useUsernameAvailability(
  username: Ref<string>,
  options: { debounceMs?: number; minLength?: number } = {}
) {
  const { debounceMs = 300, minLength = 3 } = options
  const authStore = useAuthStore()

  const state = ref<UsernameAvailabilityState>({
    isChecking: false,
    isAvailable: null,
    message: null,
    error: null
  })

  const checkAvailability = debounce(async (value: string) => {
    if (!value || value.length < minLength) {
      state.value = { isChecking: false, isAvailable: null, message: null, error: null }
      return
    }

    state.value.isChecking = true
    state.value.error = null

    try {
      const result = await authStore.checkUsernameAvailability(value)
      state.value.isAvailable = result.available
      state.value.message = result.message
    } catch (err) {
      state.value.error = err instanceof Error ? err.message : 'Check failed'
      state.value.isAvailable = null
      state.value.message = null
    } finally {
      state.value.isChecking = false
    }
  }, debounceMs)

  watch(username, newValue => {
    if (!newValue || newValue.length < minLength) {
      state.value = { isChecking: false, isAvailable: null, message: null, error: null }
    } else {
      state.value.isChecking = true
    }
    checkAvailability(newValue)
  })

  function reset() {
    state.value = { isChecking: false, isAvailable: null, message: null, error: null }
  }

  return { state, reset }
}
