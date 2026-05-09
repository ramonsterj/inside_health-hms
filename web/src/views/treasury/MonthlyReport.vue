<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTreasuryReportStore } from '@/stores/treasuryReport'
import { formatCurrency, toApiDate } from '@/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'

const { t } = useI18n()
const store = useTreasuryReportStore()

const now = new Date()
const fromDate = ref(new Date(now.getFullYear(), now.getMonth(), 1))
const toDate = ref(new Date())

async function loadReport() {
  await store.fetchMonthlyReport(toApiDate(fromDate.value), toApiDate(toDate.value))
}

onMounted(() => {
  loadReport()
})
</script>

<template>
  <div class="monthly-report">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.report.monthly.title') }}</h1>
    </div>

    <!-- Filters -->
    <Card class="filter-card">
      <template #content>
        <div class="filters">
          <div class="filter-field">
            <label>{{ t('treasury.report.common.from') }}</label>
            <DatePicker v-model="fromDate" />
          </div>
          <div class="filter-field">
            <label>{{ t('treasury.report.common.to') }}</label>
            <DatePicker v-model="toDate" />
          </div>
          <Button :label="t('treasury.report.common.filter')" icon="pi pi-search" @click="loadReport" class="filter-btn" />
        </div>
      </template>
    </Card>

    <!-- Net Balance Banner -->
    <div class="net-balance-banner">
      <div class="balance-row">
        <div class="balance-item">
          <div class="balance-icon" style="background: var(--p-red-50); color: var(--p-red-500)">
            <i class="pi pi-arrow-down-left" />
          </div>
          <div class="balance-detail">
            <span class="balance-label">{{ t('treasury.report.monthly.totalExpenses') }}</span>
            <span class="balance-amount expense">{{ formatCurrency(store.monthlyReport?.totalExpenses) }}</span>
          </div>
        </div>
        <div class="balance-divider" />
        <div class="balance-item">
          <div class="balance-icon" style="background: var(--p-green-50); color: var(--p-green-500)">
            <i class="pi pi-arrow-up-right" />
          </div>
          <div class="balance-detail">
            <span class="balance-label">{{ t('treasury.report.monthly.totalIncome') }}</span>
            <span class="balance-amount income">{{ formatCurrency(store.monthlyReport?.totalIncome) }}</span>
          </div>
        </div>
        <div class="balance-divider" />
        <div class="balance-item">
          <div
            class="balance-icon"
            :style="{
              background: (store.monthlyReport?.netBalance ?? 0) >= 0 ? 'var(--p-green-50)' : 'var(--p-red-50)',
              color: (store.monthlyReport?.netBalance ?? 0) >= 0 ? 'var(--p-green-500)' : 'var(--p-red-500)'
            }"
          >
            <i class="pi pi-equals" />
          </div>
          <div class="balance-detail">
            <span class="balance-label">{{ t('treasury.report.monthly.netBalance') }}</span>
            <span class="balance-amount" :class="(store.monthlyReport?.netBalance ?? 0) >= 0 ? 'income' : 'expense'">
              {{ formatCurrency(store.monthlyReport?.netBalance) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Category Breakdowns -->
    <div class="tables-grid">
      <Card>
        <template #title>
          <div class="card-title-with-icon">
            <i class="pi pi-arrow-down-left" style="color: var(--p-red-500)" />
            {{ t('treasury.report.monthly.expensesByCategory') }}
          </div>
        </template>
        <template #content>
          <DataTable :value="store.monthlyReport?.expensesByCategory ?? []" :loading="store.loading" stripedRows paginator :rows="10" :rowsPerPageOptions="[5, 10, 20]">
            <template #empty>{{ t('treasury.report.monthly.noData') }}</template>
            <Column field="label" :header="t('treasury.report.common.category')">
              <template #body="{ data }">{{ t(`treasury.expense.categories.${data.category}`) }}</template>
            </Column>
            <Column field="count" :header="t('treasury.report.common.count')" style="width: 80px; text-align: center" />
            <Column field="total" :header="t('treasury.report.common.total')" style="text-align: right">
              <template #body="{ data }">
                <span class="font-semibold">{{ formatCurrency(data.total) }}</span>
              </template>
            </Column>
          </DataTable>
          <div class="table-footer">
            <span class="footer-label">{{ t('treasury.report.monthly.totalExpenses') }}</span>
            <span class="footer-value expense">{{ formatCurrency(store.monthlyReport?.totalExpenses) }}</span>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>
          <div class="card-title-with-icon">
            <i class="pi pi-arrow-up-right" style="color: var(--p-green-500)" />
            {{ t('treasury.report.monthly.incomeByCategory') }}
          </div>
        </template>
        <template #content>
          <DataTable :value="store.monthlyReport?.incomeByCategory ?? []" :loading="store.loading" stripedRows paginator :rows="10" :rowsPerPageOptions="[5, 10, 20]">
            <template #empty>{{ t('treasury.report.monthly.noData') }}</template>
            <Column field="label" :header="t('treasury.report.common.category')">
              <template #body="{ data }">{{ t(`treasury.income.categories.${data.category}`) }}</template>
            </Column>
            <Column field="count" :header="t('treasury.report.common.count')" style="width: 80px; text-align: center" />
            <Column field="total" :header="t('treasury.report.common.total')" style="text-align: right">
              <template #body="{ data }">
                <span class="font-semibold">{{ formatCurrency(data.total) }}</span>
              </template>
            </Column>
          </DataTable>
          <div class="table-footer">
            <span class="footer-label">{{ t('treasury.report.monthly.totalIncome') }}</span>
            <span class="footer-value income">{{ formatCurrency(store.monthlyReport?.totalIncome) }}</span>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.monthly-report {
  max-width: 1200px;
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
  margin-bottom: 1.5rem;
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

/* Net Balance Banner */
.net-balance-banner {
  padding: 1.25rem 1.5rem;
  border-radius: var(--p-border-radius);
  margin-bottom: 1.5rem;
  border: 1px solid var(--p-surface-border);
  background: var(--p-surface-card);
}

.balance-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 1.5rem;
}

.balance-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.balance-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 0.75rem;
  font-size: 1.1rem;
  flex-shrink: 0;
}

.balance-detail {
  display: flex;
  flex-direction: column;
}

.balance-label {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
  font-weight: 500;
}

.balance-amount {
  font-size: 1.35rem;
  font-weight: 700;
}

.balance-amount.expense {
  color: var(--p-red-500);
}

.balance-amount.income {
  color: var(--p-green-500);
}

.balance-divider {
  width: 1px;
  height: 2.5rem;
  background: var(--p-surface-border);
}

/* Tables Grid */
.tables-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
}

.card-title-with-icon {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0 0 0;
  margin-top: 0.75rem;
  border-top: 2px solid var(--p-surface-border);
}

.footer-label {
  font-weight: 600;
  font-size: 0.9rem;
}

.footer-value {
  font-weight: 700;
  font-size: 1.1rem;
}

.footer-value.expense {
  color: var(--p-red-500);
}

.footer-value.income {
  color: var(--p-green-500);
}

@media (max-width: 768px) {
  .tables-grid {
    grid-template-columns: 1fr;
  }

  .balance-row {
    flex-direction: column;
    gap: 1rem;
  }

  .balance-divider {
    width: 100%;
    height: 1px;
  }
}
</style>
