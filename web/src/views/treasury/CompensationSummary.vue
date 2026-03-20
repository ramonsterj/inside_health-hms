<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTreasuryReportStore } from '@/stores/treasuryReport'
import { formatCurrency } from '@/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

const { t } = useI18n()
const store = useTreasuryReportStore()

const currentYear = new Date().getFullYear()
const selectedYear = ref(currentYear)
const yearOptions = Array.from({ length: 5 }, (_, i) => ({
  label: String(currentYear - 2 + i),
  value: currentYear - 2 + i
}))

function getTypeSeverity(type: string) {
  switch (type) {
    case 'PAYROLL': return 'info'
    case 'CONTRACTOR': return 'warn'
    case 'DOCTOR': return 'success'
    default: return 'secondary'
  }
}

async function loadReport() {
  await store.fetchCompensation(selectedYear.value)
}

onMounted(() => {
  loadReport()
})
</script>

<template>
  <div class="compensation-summary">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('treasury.report.compensation.title') }}</h1>
      </div>
      <div class="header-actions">
        <div class="filter-field">
          <label>{{ t('treasury.report.compensation.year') }}</label>
          <Select v-model="selectedYear" :options="yearOptions" optionLabel="label" optionValue="value" @change="loadReport" style="width: 120px" />
        </div>
      </div>
    </div>

    <!-- KPI Cards -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-green-50); color: var(--p-green-500)">
          <i class="pi pi-check-circle" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.compensation.totalYtd') }}</span>
          <span class="kpi-value" style="color: var(--p-green-500)">{{ formatCurrency(store.compensation?.totalYtdPayments) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-orange-50); color: var(--p-orange-500)">
          <i class="pi pi-clock" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.compensation.totalPending') }}</span>
          <span class="kpi-value" style="color: var(--p-orange-500)">{{ formatCurrency(store.compensation?.totalPending) }}</span>
        </div>
      </div>
    </div>

    <!-- Employee Table -->
    <Card>
      <template #content>
        <DataTable
          :value="store.compensation?.employees ?? []"
          :loading="store.loading"
          stripedRows
          responsiveLayout="scroll"
          sortField="fullName"
          :sortOrder="1"
          paginator
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20, 50]"
        >
          <template #empty>{{ t('treasury.report.compensation.empty') }}</template>
          <Column field="fullName" :header="t('treasury.report.common.name')" sortable />
          <Column field="employeeType" :header="t('treasury.report.common.type')" sortable style="width: 130px">
            <template #body="{ data }">
              <Tag :value="t(`treasury.employee.types.${data.employeeType}`)" :severity="getTypeSeverity(data.employeeType)" />
            </template>
          </Column>
          <Column field="position" :header="t('treasury.report.compensation.position')" sortable />
          <Column field="compensation" :header="t('treasury.report.compensation.compensation')" sortable style="text-align: right">
            <template #body="{ data }">{{ formatCurrency(data.compensation) }}</template>
          </Column>
          <Column field="ytdPayments" :header="t('treasury.report.compensation.ytdPayments')" sortable style="text-align: right">
            <template #body="{ data }">
              <span class="font-semibold" style="color: var(--p-green-500)">{{ formatCurrency(data.ytdPayments) }}</span>
            </template>
          </Column>
          <Column field="pendingAmount" :header="t('treasury.report.compensation.pending')" sortable style="text-align: right">
            <template #body="{ data }">
              <span class="font-semibold" :style="{ color: data.pendingAmount > 0 ? 'var(--p-orange-500)' : 'var(--p-text-muted-color)' }">
                {{ formatCurrency(data.pendingAmount) }}
              </span>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.compensation-summary {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-title {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.filter-field label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--p-text-muted-color);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: var(--p-surface-card);
  border: 1px solid var(--p-surface-border);
  border-radius: var(--p-border-radius);
}

.kpi-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
  font-weight: 500;
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 700;
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
