<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTreasuryReportStore } from '@/stores/treasuryReport'
import { formatCurrency } from '@/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import Button from 'primevue/button'

const { t } = useI18n()
const store = useTreasuryReportStore()

const formatTenure = (days: number) => {
  const years = Math.floor(days / 365)
  const months = Math.floor((days % 365) / 30)
  if (years > 0 && months > 0) return `${years}y ${months}m`
  if (years > 0) return `${years}y`
  return `${months}m`
}

onMounted(() => {
  store.fetchIndemnizacion()
})
</script>

<template>
  <div class="indemnizacion-report">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('treasury.report.indemnizacion.title') }}</h1>
      </div>
      <div class="header-actions">
        <Button icon="pi pi-refresh" :label="t('treasury.report.common.refresh')" outlined @click="store.fetchIndemnizacion()" />
      </div>
    </div>

    <!-- Summary Banner -->
    <div class="summary-banner">
      <div class="summary-item">
        <div class="summary-icon" style="background: var(--p-blue-50); color: var(--p-blue-500)">
          <i class="pi pi-calendar" />
        </div>
        <div class="summary-content">
          <span class="summary-label">{{ t('treasury.report.indemnizacion.asOfDate') }}</span>
          <span class="summary-value">{{ store.indemnizacion?.asOfDate ?? '-' }}</span>
        </div>
      </div>

      <div class="summary-divider" />

      <div class="summary-item">
        <div class="summary-icon" style="background: var(--p-red-50); color: var(--p-red-500)">
          <i class="pi pi-exclamation-circle" />
        </div>
        <div class="summary-content">
          <span class="summary-label">{{ t('treasury.report.indemnizacion.grandTotal') }}</span>
          <span class="summary-value liability">{{ formatCurrency(store.indemnizacion?.grandTotal) }}</span>
        </div>
      </div>

      <div class="summary-divider" />

      <div class="summary-item">
        <div class="summary-icon" style="background: var(--p-teal-50); color: var(--p-teal-500)">
          <i class="pi pi-users" />
        </div>
        <div class="summary-content">
          <span class="summary-label">{{ t('treasury.report.common.employees') }}</span>
          <span class="summary-value">{{ store.indemnizacion?.employees?.length ?? 0 }}</span>
        </div>
      </div>
    </div>

    <!-- Employee Table -->
    <Card>
      <template #content>
        <DataTable
          :value="store.indemnizacion?.employees ?? []"
          :loading="store.loading"
          stripedRows
          responsiveLayout="scroll"
          sortField="liability"
          :sortOrder="-1"
          paginator
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20, 50]"
        >
          <template #empty>{{ t('treasury.report.indemnizacion.empty') }}</template>
          <Column field="fullName" :header="t('treasury.report.common.name')" sortable />
          <Column field="position" :header="t('treasury.report.compensation.position')" sortable />
          <Column field="hireDate" :header="t('treasury.report.indemnizacion.hireDate')" sortable style="width: 130px" />
          <Column field="tenureDays" :header="t('treasury.report.indemnizacion.tenure')" sortable style="width: 100px">
            <template #body="{ data }">
              <span class="tenure-badge">{{ formatTenure(data.tenureDays) }}</span>
            </template>
          </Column>
          <Column field="currentSalary" :header="t('treasury.report.indemnizacion.salary')" sortable style="text-align: right">
            <template #body="{ data }">{{ formatCurrency(data.currentSalary) }}</template>
          </Column>
          <Column field="liability" :header="t('treasury.report.indemnizacion.liability')" sortable style="text-align: right">
            <template #body="{ data }">
              <span class="liability-amount">{{ formatCurrency(data.liability) }}</span>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.indemnizacion-report {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
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
  gap: 0.5rem;
}

/* Summary Banner */
.summary-banner {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 1.25rem 2rem;
  background: var(--p-surface-card);
  border: 1px solid var(--p-surface-border);
  border-radius: var(--p-border-radius);
  margin-bottom: 1.5rem;
  gap: 1.5rem;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 0.75rem;
  font-size: 1.15rem;
  flex-shrink: 0;
}

.summary-content {
  display: flex;
  flex-direction: column;
}

.summary-label {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
  font-weight: 500;
}

.summary-value {
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--p-text-color);
}

.summary-value.liability {
  color: var(--p-red-500);
}

.summary-divider {
  width: 1px;
  height: 2.5rem;
  background: var(--p-surface-border);
}

.tenure-badge {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  background: var(--p-surface-100);
  border-radius: 0.35rem;
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--p-text-muted-color);
}

.liability-amount {
  font-weight: 700;
  color: var(--p-red-500);
}

@media (max-width: 768px) {
  .summary-banner {
    flex-direction: column;
    gap: 1rem;
  }

  .summary-divider {
    width: 100%;
    height: 1px;
  }
}
</style>
