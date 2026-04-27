<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OccupancySummary } from '@/types/room'

const props = defineProps<{
  summary: OccupancySummary
}>()

const { t } = useI18n()

const occupancyPercentLabel = computed(() => `${props.summary.occupancyPercent.toFixed(1)}%`)
</script>

<template>
  <div class="summary-grid" role="status" :aria-label="t('bedOccupancy.title')">
    <div class="summary-card summary-card--total">
      <div class="summary-value">{{ summary.totalBeds }}</div>
      <div class="summary-label">{{ t('bedOccupancy.summary.total') }}</div>
    </div>
    <div class="summary-card summary-card--occupied">
      <div class="summary-value">{{ summary.occupiedBeds }}</div>
      <div class="summary-label">{{ t('bedOccupancy.summary.occupied') }}</div>
    </div>
    <div class="summary-card summary-card--free">
      <div class="summary-value">{{ summary.freeBeds }}</div>
      <div class="summary-label">{{ t('bedOccupancy.summary.free') }}</div>
    </div>
    <div class="summary-card summary-card--percent">
      <div class="summary-value">{{ occupancyPercentLabel }}</div>
      <div class="summary-label">{{ t('bedOccupancy.summary.percent') }}</div>
    </div>
  </div>
</template>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
}

.summary-card {
  background: var(--p-content-background, #fff);
  border: 1px solid var(--p-surface-border);
  border-radius: 0.5rem;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.summary-card--occupied {
  border-left: 4px solid var(--p-blue-500, #3b82f6);
}

.summary-card--free {
  border-left: 4px solid var(--p-green-500, #22c55e);
}

.summary-card--percent {
  border-left: 4px solid var(--p-primary-color);
}

.summary-card--total {
  border-left: 4px solid var(--p-surface-400, #94a3b8);
}

.summary-value {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
}

.summary-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-text-muted-color);
}
</style>
