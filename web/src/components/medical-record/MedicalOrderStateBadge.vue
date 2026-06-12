<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Tag from 'primevue/tag'
import { MedicalOrderStatus, SEVERITY_BY_STATUS } from '@/types/medicalRecord'

const props = defineProps<{
  status: MedicalOrderStatus
}>()

const { t } = useI18n()

const severity = computed(() => SEVERITY_BY_STATUS[props.status])

const label = computed(() => t(`medicalRecord.medicalOrder.statuses.${props.status}`))
</script>

<template>
  <Tag :severity="severity" :value="label" />
</template>
