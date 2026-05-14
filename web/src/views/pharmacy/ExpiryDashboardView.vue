<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Select from 'primevue/select'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import { useExpiryReportStore } from '@/stores/expiryReport'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { formatDate } from '@/utils/format'
import ExpiryStatusChip from '@/components/pharmacy/ExpiryStatusChip.vue'
import { LotExpiryStatus, MedicationSection } from '@/types/pharmacy'

const { t } = useI18n()
const store = useExpiryReportStore()
const { showError } = useErrorHandler()

const windowDays = ref(90)
const urgentWindow = ref(30)
const section = ref<MedicationSection | null>(null)
const controlled = ref<boolean | null>(null)

const sectionOptions = computed(() => [
  { label: t('common.all'), value: null },
  ...Object.values(MedicationSection).map(s => ({ label: t(`pharmacy.section.${s}`), value: s }))
])
const controlledOptions = computed(() => [
  { label: t('common.all'), value: null },
  { label: t('common.yes'), value: true },
  { label: t('common.no'), value: false }
])

onMounted(() => load())

async function load() {
  try {
    await store.fetch({
      window: windowDays.value,
      urgentWindow: urgentWindow.value,
      section: section.value ?? undefined,
      controlled: controlled.value ?? undefined
    })
  } catch (e) {
    showError(e)
  }
}
</script>

<template>
  <div class="expiry-dashboard">
    <div class="page-header">
      <h1>{{ t('pharmacy.expiry.dashboard') }}</h1>
    </div>

    <Card class="mb-3">
      <template #content>
        <div class="filters">
          <div>
            <label>{{ t('pharmacy.expiry.window') }}</label>
            <InputNumber v-model="windowDays" :min="1" :max="365" />
          </div>
          <div>
            <label>{{ t('pharmacy.expiry.urgentWindow') }}</label>
            <InputNumber v-model="urgentWindow" :min="1" :max="365" />
          </div>
          <div>
            <label>{{ t('pharmacy.medication.section') }}</label>
            <Select
              v-model="section"
              :options="sectionOptions"
              optionLabel="label"
              optionValue="value"
            />
          </div>
          <div>
            <label>{{ t('pharmacy.medication.controlled') }}</label>
            <Select
              v-model="controlled"
              :options="controlledOptions"
              optionLabel="label"
              optionValue="value"
            />
          </div>
          <Button icon="pi pi-search" :label="t('common.search')" @click="load" />
        </div>
      </template>
    </Card>

    <Card v-if="store.report" class="mb-3">
      <template #content>
        <div class="totals">
          <div v-for="status in Object.values(LotExpiryStatus)" :key="status" class="total-cell">
            <ExpiryStatusChip :status="status" />
            <span class="count">{{ store.report.totals[status] || 0 }}</span>
          </div>
        </div>
      </template>
    </Card>

    <Card>
      <template #content>
        <DataTable
          :value="store.report?.items || []"
          :loading="store.loading"
          dataKey="lotId"
          paginator
          :rows="25"
          :rowsPerPageOptions="[25, 50, 100]"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('pharmacy.expiry.empty') }}</div>
          </template>
          <Column :header="t('pharmacy.lot.status')">
            <template #body="{ data }">
              <ExpiryStatusChip :status="data.status" />
            </template>
          </Column>
          <Column field="sku" :header="t('pharmacy.medication.sku')" />
          <Column field="genericName" :header="t('pharmacy.medication.genericName')" />
          <Column field="strength" :header="t('pharmacy.medication.strength')" />
          <Column :header="t('pharmacy.medication.section')">
            <template #body="{ data }">
              {{ data.section ? t(`pharmacy.section.${data.section}`) : '-' }}
            </template>
          </Column>
          <Column field="lotNumber" :header="t('pharmacy.lot.lotNumber')" />
          <Column :header="t('pharmacy.lot.expirationDate')">
            <template #body="{ data }">{{ formatDate(data.expirationDate) }}</template>
          </Column>
          <Column field="daysToExpiry" :header="t('pharmacy.expiry.daysToExpiry')" />
          <Column field="quantityOnHand" :header="t('pharmacy.lot.quantityOnHand')" />
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.expiry-dashboard {
  max-width: 1400px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 1rem;
}
.filters {
  display: flex;
  gap: 1rem;
  align-items: end;
  flex-wrap: wrap;
}
.filters > div {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.totals {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}
.total-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.count {
  font-weight: 600;
  font-size: 1.2rem;
}
.mb-3 {
  margin-bottom: 1rem;
}
</style>
