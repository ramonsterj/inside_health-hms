<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { KardexVitalSignSummary } from '@/types'
import { formatShortDateTime } from '@/utils/format'
import { useVitalsFreshness } from '@/composables/useVitalsFreshness'

const props = defineProps<{
  vitalSigns: KardexVitalSignSummary | null
}>()

const { t, locale } = useI18n()

const { freshnessClass, freshnessLabel } =
  useVitalsFreshness(() => props.vitalSigns?.recordedAt ?? null)
</script>

<template>
  <div class="kardex-vitals">
    <h4>{{ t('kardex.vitals.title') }}</h4>

    <div v-if="!props.vitalSigns" class="empty-state">
      {{ t('kardex.vitals.empty') }}
    </div>

    <div v-else class="vitals-row">
      <div class="vital-item">
        <span class="vital-label">{{ t('kardex.vitals.bp') }}</span>
        <span class="vital-value">{{ props.vitalSigns.systolicBp }}/{{ props.vitalSigns.diastolicBp }}</span>
        <span class="vital-unit">{{ t('kardex.vitals.mmhg') }}</span>
      </div>
      <div class="vital-item">
        <span class="vital-label">{{ t('kardex.vitals.hr') }}</span>
        <span class="vital-value">{{ props.vitalSigns.heartRate }}</span>
        <span class="vital-unit">{{ t('kardex.vitals.bpm') }}</span>
      </div>
      <div class="vital-item">
        <span class="vital-label">{{ t('kardex.vitals.rr') }}</span>
        <span class="vital-value">{{ props.vitalSigns.respiratoryRate }}</span>
        <span class="vital-unit">{{ t('kardex.vitals.perMin') }}</span>
      </div>
      <div class="vital-item">
        <span class="vital-label">{{ t('kardex.vitals.temp') }}</span>
        <span class="vital-value">{{ props.vitalSigns.temperature }}</span>
        <span class="vital-unit">{{ t('kardex.vitals.celsius') }}</span>
      </div>
      <div class="vital-item">
        <span class="vital-label">{{ t('kardex.vitals.spo2') }}</span>
        <span class="vital-value">{{ props.vitalSigns.oxygenSaturation }}</span>
        <span class="vital-unit">{{ t('kardex.vitals.percent') }}</span>
      </div>
      <span class="vitals-separator">·</span>
      <span :class="['freshness', freshnessClass]">{{ freshnessLabel }}</span>
      <span class="vitals-separator">·</span>
      <span class="vitals-meta">
        {{ formatShortDateTime(props.vitalSigns.recordedAt, locale) }}
      </span>
      <span v-if="props.vitalSigns.recordedByName" class="vitals-separator">·</span>
      <span v-if="props.vitalSigns.recordedByName" class="vitals-meta">
        {{ props.vitalSigns.recordedByName }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.kardex-vitals h4 {
  margin: 0 0 0.75rem 0;
  font-size: 1rem;
}

.empty-state {
  color: var(--p-text-muted-color);
  font-style: italic;
  padding: 0.5rem 0;
}

.vitals-row {
  display: flex;
  align-items: baseline;
  gap: 1.25rem;
  flex-wrap: wrap;
}

.vital-item {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.vital-label {
  font-weight: 600;
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
}

.vital-value {
  font-size: 1.1rem;
  font-weight: 600;
}

.vital-unit {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.vitals-separator {
  color: var(--p-text-muted-color);
  font-size: 0.85rem;
}

.vitals-meta {
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
}

.freshness {
  font-weight: 600;
  font-size: 0.85rem;
}

.freshness-ok {
  color: var(--p-green-500);
}

.freshness-warning {
  color: var(--p-yellow-500);
}

.freshness-critical {
  color: var(--p-red-500);
}
</style>
