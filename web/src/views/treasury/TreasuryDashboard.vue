<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useTreasuryReportStore } from '@/stores/treasuryReport'
import { formatCurrency } from '@/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import Button from 'primevue/button'

const { t } = useI18n()
const router = useRouter()
const store = useTreasuryReportStore()

const totalBalance = computed(() => {
  if (!store.dashboard) return 0
  return store.dashboard.bankBalances.reduce((sum, b) => sum + b.bookBalance, 0)
})

const reportLinks = [
  { label: 'nav.monthlyReport', icon: 'pi pi-chart-pie', route: 'treasury-monthly-report', color: 'var(--p-blue-500)' },
  { label: 'nav.upcomingPayments', icon: 'pi pi-calendar', route: 'treasury-upcoming-payments', color: 'var(--p-orange-500)' },
  { label: 'nav.bankSummary', icon: 'pi pi-building-columns', route: 'treasury-bank-summary', color: 'var(--p-teal-500)' },
  { label: 'nav.compensationSummary', icon: 'pi pi-users', route: 'treasury-compensation', color: 'var(--p-purple-500)' },
  { label: 'nav.indemnizacionReport', icon: 'pi pi-calculator', route: 'treasury-indemnizacion', color: 'var(--p-red-500)' },
  { label: 'nav.reconciliationSummary', icon: 'pi pi-check-square', route: 'treasury-reconciliation-summary', color: 'var(--p-cyan-500)' }
]

onMounted(() => {
  store.fetchDashboard()
})
</script>

<template>
  <div class="treasury-dashboard">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.report.dashboard.title') }}</h1>
    </div>

    <!-- KPI Cards -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-blue-50); color: var(--p-blue-500)">
          <i class="pi pi-wallet" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.dashboard.totalBalance') }}</span>
          <span class="kpi-value text-primary">{{ formatCurrency(totalBalance) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-orange-50); color: var(--p-orange-500)">
          <i class="pi pi-clock" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.dashboard.pendingPayables') }}</span>
          <span class="kpi-value" style="color: var(--p-orange-500)">
            {{ store.dashboard?.pendingPayablesCount ?? 0 }}
          </span>
          <span class="kpi-sub">{{ formatCurrency(store.dashboard?.pendingPayablesTotal) }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon" style="background: var(--p-red-50); color: var(--p-red-500)">
          <i class="pi pi-exclamation-triangle" />
        </div>
        <div class="kpi-content">
          <span class="kpi-label">{{ t('treasury.report.dashboard.next7Days') }}</span>
          <span class="kpi-value" style="color: var(--p-red-500)">
            {{ formatCurrency(store.dashboard?.next7DayTotal) }}
          </span>
          <span class="kpi-sub">
            {{ store.dashboard?.next7DayObligations?.length ?? 0 }} {{ t('treasury.report.dashboard.obligations') }}
          </span>
        </div>
      </div>
    </div>

    <!-- Bank Balances + Upcoming Obligations side by side -->
    <div class="content-grid">
      <Card class="section-card">
        <template #title>
          <div class="card-title-row">
            <span>{{ t('treasury.report.dashboard.bankBalances') }}</span>
            <Button
              icon="pi pi-arrow-right"
              text
              rounded
              size="small"
              @click="router.push({ name: 'treasury-bank-summary' })"
              v-tooltip.top="t('nav.bankSummary')"
            />
          </div>
        </template>
        <template #content>
          <DataTable :value="store.dashboard?.bankBalances ?? []" :loading="store.loading" stripedRows paginator :rows="5" :rowsPerPageOptions="[5, 10, 20]">
            <template #empty>{{ t('treasury.report.bankSummary.empty') }}</template>
            <Column field="name" :header="t('treasury.report.dashboard.accountName')" />
            <Column field="currency" :header="t('treasury.report.dashboard.currency')" style="width: 100px" />
            <Column field="bookBalance" :header="t('treasury.report.dashboard.balance')" style="text-align: right">
              <template #body="{ data }">
                <span class="font-semibold">{{ formatCurrency(data.bookBalance) }}</span>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>

      <Card class="section-card">
        <template #title>
          <div class="card-title-row">
            <span>{{ t('treasury.report.dashboard.upcomingObligations') }}</span>
            <Button
              icon="pi pi-arrow-right"
              text
              rounded
              size="small"
              @click="router.push({ name: 'treasury-upcoming-payments' })"
              v-tooltip.top="t('nav.upcomingPayments')"
            />
          </div>
        </template>
        <template #content>
          <DataTable :value="store.dashboard?.next7DayObligations ?? []" :loading="store.loading" stripedRows paginator :rows="5" :rowsPerPageOptions="[5, 10, 20]">
            <template #empty>{{ t('treasury.report.upcoming.empty') }}</template>
            <Column field="type" :header="t('treasury.report.common.type')" style="width: 100px">
              <template #body="{ data }">
                <Tag :value="t(`treasury.report.upcoming.types.${data.type}`)" :severity="data.type === 'EXPENSE' ? 'warn' : 'info'" />
              </template>
            </Column>
            <Column field="description" :header="t('treasury.report.common.description')" />
            <Column field="amount" :header="t('treasury.report.common.amount')" style="text-align: right">
              <template #body="{ data }">
                <span class="font-semibold">{{ formatCurrency(data.amount) }}</span>
              </template>
            </Column>
            <Column field="dueDate" :header="t('treasury.report.common.dueDate')" style="width: 120px" />
          </DataTable>
        </template>
      </Card>
    </div>

    <!-- Quick Links -->
    <h3 class="section-heading">{{ t('treasury.report.dashboard.reports') }}</h3>
    <div class="report-links-grid">
      <button
        v-for="link in reportLinks"
        :key="link.route"
        class="report-link-card"
        @click="router.push({ name: link.route })"
      >
        <i :class="link.icon" :style="{ color: link.color, fontSize: '1.5rem' }" />
        <span>{{ t(link.label) }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.treasury-dashboard {
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
  min-width: 0;
}

.kpi-label {
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
  font-weight: 500;
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.3;
}

.kpi-sub {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.section-card :deep(.p-card-title) {
  font-size: 1rem;
  font-weight: 600;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-heading {
  margin: 0 0 1rem 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.report-links-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.report-link-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--p-surface-card);
  border: 1px solid var(--p-surface-border);
  border-radius: var(--p-border-radius);
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--p-text-color);
  text-align: left;
}

.report-link-card:hover {
  border-color: var(--p-primary-color);
  background: var(--p-primary-50);
}

@media (max-width: 768px) {
  .kpi-grid,
  .content-grid,
  .report-links-grid {
    grid-template-columns: 1fr;
  }
}
</style>
