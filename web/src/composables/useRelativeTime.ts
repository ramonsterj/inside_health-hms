import { useI18n } from 'vue-i18n'

export function useRelativeTime() {
  const { t } = useI18n()

  function getRelativeTime(dateString: string): string {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMinutes = Math.floor(diffMs / (1000 * 60))
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

    if (diffMinutes < 1) {
      return t('common.time.justNow')
    } else if (diffMinutes < 60) {
      return t('common.time.minutesAgo', { count: diffMinutes })
    } else if (diffHours < 24) {
      return t('common.time.hoursAgo', { count: diffHours })
    } else if (diffDays === 1) {
      return t('common.time.yesterday')
    } else {
      return t('common.time.daysAgo', { count: diffDays })
    }
  }

  return { getRelativeTime }
}
