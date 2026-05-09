import { ref, watch, type Ref } from 'vue'
import { toApiDate } from '@/utils/format'

const DATE_ONLY_RE = /^(\d{4})-(\d{2})-(\d{2})$/

export function useFormDateField(formField: Ref<string | undefined>) {
  const datePickerValue = ref<Date | null>(null)

  watch(
    () => formField.value,
    val => {
      if (!val) {
        datePickerValue.value = null
        return
      }
      // For date-only strings, build a local-time Date so the picker shows the
      // intended day in any timezone (avoids the UTC-shift trap in Guatemala).
      const m = DATE_ONLY_RE.exec(val)
      datePickerValue.value = m
        ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
        : new Date(val)
    },
    { immediate: true }
  )

  watch(datePickerValue, val => {
    formField.value = toApiDate(val) ?? ''
  })

  return datePickerValue
}
