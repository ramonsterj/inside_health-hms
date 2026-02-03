<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Tag from 'primevue/tag'
import { AdmissionType } from '@/types/admission'

const props = defineProps<{
  type: AdmissionType
}>()

const { t } = useI18n()

const label = computed(() => t(`admission.types.${props.type}`))

const severity = computed((): 'info' | 'success' | 'warn' | 'danger' | 'secondary' | 'contrast' => {
  switch (props.type) {
    case AdmissionType.HOSPITALIZATION:
      return 'info'
    case AdmissionType.AMBULATORY:
      return 'secondary'
    case AdmissionType.ELECTROSHOCK_THERAPY:
      return 'warn'
    case AdmissionType.KETAMINE_INFUSION:
      return 'contrast'
    case AdmissionType.EMERGENCY:
      return 'danger'
    default:
      return 'secondary'
  }
})
</script>

<template>
  <Tag :value="label" :severity="severity" />
</template>
