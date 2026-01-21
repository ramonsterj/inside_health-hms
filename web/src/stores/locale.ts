import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import { SUPPORTED_LOCALES, setLocale as setI18nLocale, type SupportedLocale } from '@/i18n'

export const useLocaleStore = defineStore('locale', () => {
  const loading = ref(false)

  const supportedLocales = computed(() => [...SUPPORTED_LOCALES])

  async function changeLocale(newLocale: SupportedLocale, syncToBackend = true) {
    loading.value = true
    try {
      // Update frontend locale
      setI18nLocale(newLocale)

      // Sync with backend if user is authenticated
      if (syncToBackend) {
        try {
          await api.put('/users/me/locale', null, {
            params: { locale: newLocale }
          })
        } catch {
          // Silently fail - user might not be authenticated
        }
      }
    } finally {
      loading.value = false
    }
  }

  function initFromUser(userLocale: string | null | undefined) {
    if (userLocale && SUPPORTED_LOCALES.includes(userLocale as SupportedLocale)) {
      setI18nLocale(userLocale as SupportedLocale)
    }
  }

  return {
    supportedLocales,
    loading,
    changeLocale,
    initFromUser
  }
})
