import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleStore } from '@/stores/locale'
import { getCurrentLocale, type SupportedLocale } from '@/i18n'

export function useLocale() {
  const { t } = useI18n()
  const localeStore = useLocaleStore()

  const currentLocale = computed(() => getCurrentLocale())

  const localeOptions = computed(() =>
    localeStore.supportedLocales.map(code => ({
      code,
      label: t(`locale.${code}`)
    }))
  )

  async function setLocale(newLocale: SupportedLocale) {
    await localeStore.changeLocale(newLocale)
  }

  return {
    t,
    currentLocale,
    localeOptions,
    setLocale,
    loading: computed(() => localeStore.loading)
  }
}
