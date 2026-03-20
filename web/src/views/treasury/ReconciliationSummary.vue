<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTreasuryReportStore } from '@/stores/treasuryReport'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import ProgressBar from 'primevue/progressbar'
import Button from 'primevue/button'

const { t } = useI18n()
const store = useTreasuryReportStore()

function getCoverageSeverity(pct: number): string {
  if (pct >= 90) return 'success'
  if (pct >= 60) return 'warn'
  return 'danger'
}

onMounted(() => {
  store.fetchReconciliation()
})
</script>

<template>
  <div class="reconciliation-summary">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('treasury.report.reconciliationSummary.title') }}</h1>
      </div>
      <div class="header-actions">
        <Button icon="pi pi-refresh" :label="t('treasury.report.common.refresh')" outlined @click="store.fetchReconciliation()" />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="store.reconciliation?.accounts ?? []"
          :loading="store.loading"
          stripedRows
          responsiveLayout="scroll"
          paginator
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20]"
        >
          <template #empty>{{ t('treasury.report.reconciliationSummary.empty') }}</template>
          <Column field="bankAccountName" :header="t('treasury.report.reconciliationSummary.account')" sortable />
          <Column field="totalStatements" :header="t('treasury.report.reconciliationSummary.statements')" sortable style="width: 110px; text-align: center" />
          <Column field="totalRows" :header="t('treasury.report.reconciliationSummary.totalRows')" sortable style="width: 100px; text-align: center" />
          <Column field="matchedCount" :header="t('treasury.report.reconciliationSummary.matched')" sortable style="width: 110px; text-align: center">
            <template #body="{ data }">
              <span class="count-badge matched">{{ data.matchedCount }}</span>
            </template>
          </Column>
          <Column field="unmatchedCount" :header="t('treasury.report.reconciliationSummary.unmatched')" sortable style="width: 120px; text-align: center">
            <template #body="{ data }">
              <span class="count-badge" :class="data.unmatchedCount > 0 ? 'unmatched' : 'zero'">{{ data.unmatchedCount }}</span>
            </template>
          </Column>
          <Column field="acknowledgedCount" :header="t('treasury.report.reconciliationSummary.acknowledged')" sortable style="width: 130px; text-align: center">
            <template #body="{ data }">
              <span class="count-badge acknowledged">{{ data.acknowledgedCount }}</span>
            </template>
          </Column>
          <Column field="coveragePct" :header="t('treasury.report.reconciliationSummary.coverage')" sortable style="width: 180px">
            <template #body="{ data }">
              <div class="coverage-cell">
                <ProgressBar
                  :value="data.coveragePct"
                  :showValue="false"
                  style="height: 0.6rem; flex: 1"
                  :class="'coverage-' + getCoverageSeverity(data.coveragePct)"
                />
                <span class="coverage-text" :class="'text-' + getCoverageSeverity(data.coveragePct)">
                  {{ Math.round(data.coveragePct) }}%
                </span>
              </div>
            </template>
          </Column>
          <Column field="lastReconciliationDate" :header="t('treasury.report.reconciliationSummary.lastReconciliation')" sortable style="width: 160px">
            <template #body="{ data }">{{ data.lastReconciliationDate ?? '-' }}</template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.reconciliation-summary {
  max-width: 1400px;
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

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2rem;
  padding: 0.2rem 0.6rem;
  border-radius: 1rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.count-badge.matched {
  background: var(--p-green-50);
  color: var(--p-green-600);
}

.count-badge.unmatched {
  background: var(--p-red-50);
  color: var(--p-red-600);
}

.count-badge.acknowledged {
  background: var(--p-blue-50);
  color: var(--p-blue-600);
}

.count-badge.zero {
  background: var(--p-surface-100);
  color: var(--p-text-muted-color);
}

.coverage-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.coverage-text {
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.text-success {
  color: var(--p-green-500);
}

.text-warn {
  color: var(--p-orange-500);
}

.text-danger {
  color: var(--p-red-500);
}

:deep(.coverage-success .p-progressbar-value) {
  background: var(--p-green-500);
}

:deep(.coverage-warn .p-progressbar-value) {
  background: var(--p-orange-500);
}

:deep(.coverage-danger .p-progressbar-value) {
  background: var(--p-red-500);
}
</style>
