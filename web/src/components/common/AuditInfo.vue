<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { MedicalStaffResponse } from '@/types/medicalRecord'

defineProps<{
  createdAt: string | null
  createdBy: MedicalStaffResponse | null
  updatedAt: string | null
  updatedBy: MedicalStaffResponse | null
}>()

const { t } = useI18n()

function formatDateTime(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

function formatStaffName(staff: MedicalStaffResponse | null): string {
  if (!staff) return '-'
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
}
</script>

<template>
  <div class="audit-info">
    <div class="audit-grid">
      <div class="audit-item">
        <span class="audit-label">{{ t('common.createdAt') }}</span>
        <span class="audit-value">{{ formatDateTime(createdAt) }}</span>
      </div>
      <div class="audit-item">
        <span class="audit-label">{{ t('common.createdBy') }}</span>
        <span class="audit-value">{{ formatStaffName(createdBy) }}</span>
      </div>
      <div class="audit-item">
        <span class="audit-label">{{ t('common.updatedAt') }}</span>
        <span class="audit-value">{{ formatDateTime(updatedAt) }}</span>
      </div>
      <div class="audit-item">
        <span class="audit-label">{{ t('common.updatedBy') }}</span>
        <span class="audit-value">{{ formatStaffName(updatedBy) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.audit-info {
  padding: 1rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
}

.audit-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.audit-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.audit-label {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.audit-value {
  font-weight: 500;
  color: var(--p-text-color);
}

@media (max-width: 640px) {
  .audit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
