<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Tag from 'primevue/tag'
import { MedicalOrderStatus } from '@/types/medicalRecord'

const props = defineProps<{
  status: MedicalOrderStatus
}>()

const { t } = useI18n()

const SEVERITY_BY_STATUS: Record<MedicalOrderStatus, string> = {
  [MedicalOrderStatus.ACTIVA]: 'success',
  [MedicalOrderStatus.SOLICITADO]: 'info',
  [MedicalOrderStatus.NO_AUTORIZADO]: 'danger',
  [MedicalOrderStatus.AUTORIZADO]: 'success',
  [MedicalOrderStatus.EN_PROCESO]: 'warn',
  [MedicalOrderStatus.RESULTADOS_RECIBIDOS]: 'contrast',
  [MedicalOrderStatus.DESCONTINUADO]: 'secondary'
}

const severity = computed(() => SEVERITY_BY_STATUS[props.status])

const label = computed(() => t(`medicalRecord.medicalOrder.statuses.${props.status}`))
</script>

<template>
  <Tag :severity="severity" :value="label" />
</template>
