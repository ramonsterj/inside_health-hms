<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import type { VitalSignDateRange } from '@/types/nursing'

const props = defineProps<{
  modelValue: VitalSignDateRange
}>()

const emit = defineEmits<{
  'update:modelValue': [value: VitalSignDateRange]
  'filter-change': []
}>()

const { t } = useI18n()

const fromDate = ref<Date | null>(
  props.modelValue.fromDate ? new Date(props.modelValue.fromDate) : null
)
const toDate = ref<Date | null>(props.modelValue.toDate ? new Date(props.modelValue.toDate) : null)

// Quick preset options
type PresetValue = 'last7Days' | 'last30Days' | 'allTime'
const presetOptions = computed(() => [
  { label: t('nursing.vitalSigns.filter.last7Days'), value: 'last7Days' as PresetValue },
  { label: t('nursing.vitalSigns.filter.last30Days'), value: 'last30Days' as PresetValue },
  { label: t('nursing.vitalSigns.filter.allTime'), value: 'allTime' as PresetValue }
])

const selectedPreset = ref<PresetValue | null>(null)

function applyPreset(preset: PresetValue | null) {
  if (!preset) return

  const now = new Date()
  switch (preset) {
    case 'last7Days':
      fromDate.value = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      toDate.value = now
      break
    case 'last30Days':
      fromDate.value = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
      toDate.value = now
      break
    case 'allTime':
      fromDate.value = null
      toDate.value = null
      break
  }
  applyFilter()
}

function formatDateForApi(date: Date | null): string | null {
  if (!date) return null
  return date.toISOString().split('T')[0] ?? null
}

function onManualDateChange() {
  selectedPreset.value = null
}

function applyFilter() {
  const range: VitalSignDateRange = {
    fromDate: formatDateForApi(fromDate.value),
    toDate: formatDateForApi(toDate.value)
  }
  emit('update:modelValue', range)
  emit('filter-change')
}

function clearFilter() {
  fromDate.value = null
  toDate.value = null
  selectedPreset.value = null
  const range: VitalSignDateRange = { fromDate: null, toDate: null }
  emit('update:modelValue', range)
  emit('filter-change')
}

// Watch for preset changes
watch(selectedPreset, applyPreset)

// Sync props to local state
watch(
  () => props.modelValue,
  newVal => {
    fromDate.value = newVal.fromDate ? new Date(newVal.fromDate) : null
    toDate.value = newVal.toDate ? new Date(newVal.toDate) : null
  },
  { immediate: true }
)
</script>

<template>
  <div class="vital-sign-date-filter">
    <div class="filter-row">
      <div class="preset-section">
        <SelectButton
          v-model="selectedPreset"
          :options="presetOptions"
          optionLabel="label"
          optionValue="value"
        />
      </div>
      <div class="date-inputs">
        <div class="date-field">
          <label>{{ t('nursing.vitalSigns.filter.from') }}</label>
          <DatePicker
            v-model="fromDate"
            :placeholder="t('nursing.vitalSigns.filter.from')"
            dateFormat="yy-mm-dd"
            showIcon
            :maxDate="toDate || undefined"
            @date-select="onManualDateChange"
          />
        </div>
        <div class="date-field">
          <label>{{ t('nursing.vitalSigns.filter.to') }}</label>
          <DatePicker
            v-model="toDate"
            :placeholder="t('nursing.vitalSigns.filter.to')"
            dateFormat="yy-mm-dd"
            showIcon
            :minDate="fromDate || undefined"
            @date-select="onManualDateChange"
          />
        </div>
      </div>
      <div class="filter-actions">
        <Button
          :label="t('nursing.vitalSigns.filter.apply')"
          icon="pi pi-check"
          size="small"
          @click="applyFilter"
        />
        <Button
          :label="t('nursing.vitalSigns.filter.clear')"
          icon="pi pi-times"
          severity="secondary"
          text
          size="small"
          @click="clearFilter"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.vital-sign-date-filter {
  padding: 0.75rem;
  background: var(--p-surface-50);
  border-radius: 0.5rem;
  margin-bottom: 1rem;
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
}

.preset-section {
  flex: 0 0 auto;
}

.date-inputs {
  display: flex;
  gap: 1rem;
}

.date-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.date-field label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--p-text-muted-color);
}

.filter-actions {
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
  margin-left: auto;
}

@media (max-width: 1024px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .date-inputs {
    flex-direction: row;
  }

  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .date-inputs {
    flex-direction: column;
  }
}
</style>
