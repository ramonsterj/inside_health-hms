import { createI18n } from 'vue-i18n'
import type { Ref } from 'vue'
import en from './locales/en.json'
import es from './locales/es.json'

export type MessageSchema = typeof en

export const SUPPORTED_LOCALES = ['en', 'es'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

function getInitialLocale(): SupportedLocale {
  // Check localStorage first
  const stored = localStorage.getItem('locale')
  if (stored && SUPPORTED_LOCALES.includes(stored as SupportedLocale)) {
    return stored as SupportedLocale
  }

  // Check browser language
  const browserLang = navigator.language.split('-')[0]
  if (SUPPORTED_LOCALES.includes(browserLang as SupportedLocale)) {
    return browserLang as SupportedLocale
  }

  return 'en'
}

export const i18n = createI18n<[MessageSchema], SupportedLocale>({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'en',
  messages: {
    en,
    es
  }
})

// Cast to Ref since legacy: false uses composition API mode
const localeRef = i18n.global.locale as unknown as Ref<SupportedLocale>

export function setLocale(locale: SupportedLocale) {
  localeRef.value = locale
  localStorage.setItem('locale', locale)
  document.documentElement.setAttribute('lang', locale)
}

export function getCurrentLocale(): SupportedLocale {
  return localeRef.value
}

export default i18n
