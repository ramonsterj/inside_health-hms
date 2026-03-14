<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { formatDate, formatTime } from '@/utils/format'

defineProps<{
  createdAt: string | null
  createdByName: string
  updatedAt: string | null
  updatedByName: string
}>()

const { t } = useI18n()
</script>

<template>
  <div class="audit-line">
    <span>{{ t('common.auditCreated', { date: formatDate(createdAt), time: formatTime(createdAt), user: createdByName }) }}</span>
    <span class="audit-separator">&mdash;</span>
    <span>{{ t('common.auditUpdated', { date: formatDate(updatedAt), time: formatTime(updatedAt), user: updatedByName }) }}</span>
  </div>
</template>

<style scoped>
.audit-line {
  margin: 0;
  padding: 1rem 1.25rem;
  font-size: 0.925rem;
  color: var(--p-text-color);
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: var(--p-border-radius);
}

.audit-separator {
  margin: 0 0.5rem;
  color: var(--p-text-muted-color);
}
</style>
