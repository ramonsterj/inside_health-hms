<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Tag from 'primevue/tag'
import { AdministrationStatus } from '@/types/medicationAdministration'
import type { TagSeverity } from '@/types/billing'

const props = defineProps<{
  status: AdministrationStatus
}>()

const { t } = useI18n()

const statusSeverityMap: Record<AdministrationStatus, TagSeverity> = {
  [AdministrationStatus.GIVEN]: 'success',
  [AdministrationStatus.MISSED]: 'warn',
  [AdministrationStatus.REFUSED]: 'danger',
  [AdministrationStatus.HELD]: 'secondary'
}

const severity = computed<TagSeverity>(() => statusSeverityMap[props.status] || 'secondary')
const statusLabel = computed(() => t(`medicationAdministration.statuses.${props.status}`))
</script>

<template>
  <Tag :value="statusLabel" :severity="severity" />
</template>
