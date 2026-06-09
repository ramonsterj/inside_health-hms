<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SelectButton from 'primevue/selectbutton'
import {
  useAdmissionsListPreferencesStore,
  type AdmissionsListGroupBy
} from '@/stores/admissionsListPreferences'

const { t } = useI18n()
const preferences = useAdmissionsListPreferencesStore()

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

// Secondary options exclude the current primary value so the two levels can
// never select the same dimension from the UI.
const secondaryGroupByOptions = computed(() =>
  groupByOptions.value.filter(
    option => option.value === 'none' || option.value !== preferences.primaryGroupBy
  )
)

const primaryGroupBy = computed<AdmissionsListGroupBy>({
  get: () => preferences.primaryGroupBy,
  set: value => {
    if (value) preferences.setPrimaryGroupBy(value)
  }
})

const secondaryGroupBy = computed<AdmissionsListGroupBy>({
  get: () => preferences.secondaryGroupBy,
  set: value => {
    if (value) preferences.setSecondaryGroupBy(value)
  }
})

// The second level only makes sense once a primary dimension is chosen.
const showSecondary = computed(() => preferences.primaryGroupBy !== 'none')
</script>

<template>
  <div class="admissions-list-toolbar">
    <div class="toolbar-field">
      <label class="toolbar-label">{{ t('admission.listView.groupBy') }}</label>
      <SelectButton
        v-model="primaryGroupBy"
        :options="groupByOptions"
        optionLabel="label"
        optionValue="value"
        :allowEmpty="false"
        :aria-label="t('admission.listView.groupBy')"
      />
    </div>
    <div v-if="showSecondary" class="toolbar-field">
      <label class="toolbar-label">{{ t('admission.listView.thenBy') }}</label>
      <SelectButton
        v-model="secondaryGroupBy"
        :options="secondaryGroupByOptions"
        optionLabel="label"
        optionValue="value"
        :allowEmpty="false"
        :aria-label="t('admission.listView.thenBy')"
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
