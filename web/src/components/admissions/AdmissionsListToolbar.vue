<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SelectButton from 'primevue/selectbutton'
import {
  useAdmissionsListPreferencesStore,
  type AdmissionsListViewMode,
  type AdmissionsListGroupBy
} from '@/stores/admissionsListPreferences'

const { t } = useI18n()
const preferences = useAdmissionsListPreferencesStore()

const viewModeOptions = computed(() => [
  { label: t('admission.listView.viewModes.cards'), value: 'cards' as AdmissionsListViewMode },
  { label: t('admission.listView.viewModes.table'), value: 'table' as AdmissionsListViewMode }
])

const groupByOptions = computed(() => [
  { label: t('admission.listView.groupByOptions.none'), value: 'none' as AdmissionsListGroupBy },
  {
    label: t('admission.listView.groupByOptions.gender'),
    value: 'gender' as AdmissionsListGroupBy
  },
  { label: t('admission.listView.groupByOptions.type'), value: 'type' as AdmissionsListGroupBy },
  {
    label: t('admission.listView.groupByOptions.triage'),
    value: 'triage' as AdmissionsListGroupBy
  }
])

const viewMode = computed<AdmissionsListViewMode>({
  get: () => preferences.viewMode,
  set: value => {
    if (value) preferences.setViewMode(value)
  }
})

const groupBy = computed<AdmissionsListGroupBy>({
  get: () => preferences.groupBy,
  set: value => {
    if (value) preferences.setGroupBy(value)
  }
})
</script>

<template>
  <div class="admissions-list-toolbar">
    <div class="toolbar-field">
      <label class="toolbar-label">{{ t('admission.listView.view') }}</label>
      <SelectButton
        v-model="viewMode"
        :options="viewModeOptions"
        optionLabel="label"
        optionValue="value"
        :allowEmpty="false"
        :aria-label="t('admission.listView.view')"
      />
    </div>
    <div class="toolbar-field">
      <label class="toolbar-label">{{ t('admission.listView.groupBy') }}</label>
      <SelectButton
        v-model="groupBy"
        :options="groupByOptions"
        optionLabel="label"
        optionValue="value"
        :allowEmpty="false"
        :aria-label="t('admission.listView.groupBy')"
      />
    </div>
  </div>
</template>

<style scoped>
.admissions-list-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  align-items: flex-end;
}

.toolbar-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.toolbar-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
</style>
