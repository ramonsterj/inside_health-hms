<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTreasuryReportStore } from '@/stores/treasuryReport'
import { formatCurrency } from '@/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const { t } = useI18n()
const store = useTreasuryReportStore()
const windowDays = ref(30)

async function loadReport() {
  await store.fetchUpcomingPayments(windowDays.value)
}

onMounted(() => {
  loadReport()
})
</script>

<template>
  <div class="upcoming-payments">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.report.upcoming.title') }}</h1>
    </div>

    <!-- Filter -->
    <Card class="filter-card">
      <template #content>
        <div class="filters">
          <div class="filter-field">
            <label>{{ t('treasury.report.upcoming.windowDays') }}</label>
            <InputNumber v-model="windowDays" :min="1" :max="365" style="width: 120px" />
          </div>
          <Button :label="t('treasury.report.common.filter')" icon="pi pi-search" @click="loadReport" class="filter-btn" />
        </div>
      </template>
    </Card>

    <!-- KPI Cards -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-blue-50); color: var(--p-blue-500)">
          <i class="pi pi-wallet" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.upcoming.totalAmount') }}</span>
          <span class="kpi-value">{{ formatCurrency(store.upcomingPayments?.totalAmount) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-orange-50); color: var(--p-orange-500)">
          <i class="pi pi-file" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.upcoming.expenses') }}</span>
          <span class="kpi-value">{{ store.upcomingPayments?.expenseCount ?? 0 }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-teal-50); color: var(--p-teal-500)">
          <i class="pi pi-users" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.upcoming.payroll') }}</span>
          <span class="kpi-value">{{ store.upcomingPayments?.payrollCount ?? 0 }}</span>
        </div>
      </div>
    </div>

    <!-- Items Table -->
    <Card>
      <template #content>
        <DataTable
          :value="store.upcomingPayments?.items ?? []"
          :loading="store.loading"
          stripedRows
          responsiveLayout="scroll"
          sortField="dueDate"
          :sortOrder="1"
          paginator
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20, 50]"
        >
          <template #empty>{{ t('treasury.report.upcoming.empty') }}</template>
          <Column field="type" :header="t('treasury.report.common.type')" sortable style="width: 110px">
            <template #body="{ data }">
              <Tag :value="t(`treasury.report.upcoming.types.${data.type}`)" :severity="data.type === 'EXPENSE' ? 'warn' : 'info'" />
            </template>
          </Column>
          <Column field="description" :header="t('treasury.report.common.description')" sortable />
          <Column field="supplierName" :header="t('treasury.report.common.supplierEmployee')" sortable>
            <template #body="{ data }">{{ data.supplierName || data.employeeName || '-' }}</template>
          </Column>
          <Column field="category" :header="t('treasury.report.common.category')" sortable style="width: 140px">
            <template #body="{ data }">{{ data.category ? t(`treasury.expense.categories.${data.category}`) : '-' }}</template>
          </Column>
          <Column field="amount" :header="t('treasury.report.common.amount')" sortable style="text-align: right; width: 140px">
            <template #body="{ data }">
              <span class="font-semibold">{{ formatCurrency(data.amount) }}</span>
            </template>
          </Column>
          <Column field="dueDate" :header="t('treasury.report.common.dueDate')" sortable style="width: 130px" />
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.upcoming-payments {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
}

.filter-card {
  margin-bottom: 1rem;
}

.filters {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  flex-wrap: wrap;
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

.filter-btn {
  align-self: flex-end;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
  color: var(--p-text-color);
}

@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
