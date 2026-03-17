import { computed, ref, onUnmounted, toValue, type MaybeRefOrGetter } from 'vue'
import { useI18n } from 'vue-i18n'

const OVERDUE_HOURS = 8
const WARNING_HOURS = 4
const TICK_INTERVAL_MS = 60_000

export function useVitalsFreshness(recordedAt: MaybeRefOrGetter<string | null>) {
  const { t } = useI18n()
  const now = ref(Date.now())

  const timer = setInterval(() => {
    now.value = Date.now()
  }, TICK_INTERVAL_MS)

  onUnmounted(() => clearInterval(timer))

  const hoursSince = computed(() => {
    const ts = toValue(recordedAt)
    if (!ts) return null
    const diff = now.value - new Date(ts).getTime()
    if (isNaN(diff)) return null
    return diff / (1000 * 60 * 60)
  })

  const freshnessClass = computed(() => {
    const h = hoursSince.value
    if (h == null) return 'freshness-critical'
    if (h > OVERDUE_HOURS) return 'freshness-critical'
    if (h > WARNING_HOURS) return 'freshness-warning'
    return 'freshness-ok'
  })

  const freshnessLabel = computed(() => {
    const h = hoursSince.value
    if (h == null) return '--'
    return t('kardex.vitals.hoursAgo', { hours: Math.round(h) })
  })

  return { freshnessClass, freshnessLabel }
}
