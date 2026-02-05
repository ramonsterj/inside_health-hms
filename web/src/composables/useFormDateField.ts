import { ref, watch, type Ref } from 'vue'

export function useFormDateField(formField: Ref<string | undefined>) {
  const datePickerValue = ref<Date | null>(null)

  // Sync form string → Date for picker
  watch(
    () => formField.value,
    val => {
      datePickerValue.value = val ? new Date(val) : null
    },
    { immediate: true }
  )

  // Sync picker Date → form string
  watch(datePickerValue, val => {
    formField.value = val ? val.toISOString().split('T')[0] : ''
  })

  return datePickerValue
}
