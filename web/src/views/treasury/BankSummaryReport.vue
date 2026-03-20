<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTreasuryReportStore } from '@/stores/treasuryReport'
import { formatCurrency } from '@/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import type { BankAccountSummaryItem } from '@/types/treasury'

const { t } = useI18n()
const store = useTreasuryReportStore()
const expandedRows = ref<BankAccountSummaryItem[]>([])

function getAccountTypeSeverity(type: string) {
  switch (type) {
    case 'CHECKING': return 'info'
    case 'SAVINGS': return 'success'
    case 'PETTY_CASH': return 'warn'
    default: return 'secondary'
  }
}

onMounted(() => {
  store.fetchBankSummary()
})
</script>

<template>
  <div class="bank-summary">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.report.bankSummary.title') }}</h1>
    </div>

    <!-- Total Balance Hero -->
    <div class="hero-card">
      <div class="hero-icon">
        <i class="pi pi-building-columns" />
      </div>
      <div class="hero-content">
        <span class="hero-label">{{ t('treasury.report.bankSummary.totalBalance') }}</span>
        <span class="hero-value">{{ formatCurrency(store.bankSummary?.totalBookBalance) }}</span>
      </div>
    </div>

    <!-- Accounts Table -->
    <Card>
      <template #content>
        <DataTable
          v-model:expandedRows="expandedRows"
          :value="store.bankSummary?.accounts ?? []"
          :loading="store.loading"
          dataKey="bankAccountId"
          stripedRows
          responsiveLayout="scroll"
          paginator
          :rows="10"
          :rowsPerPageOptions="[5, 10, 20]"
        >
          <template #empty>{{ t('treasury.report.bankSummary.empty') }}</template>
          <Column expander style="width: 3rem" />
          <Column field="name" :header="t('treasury.report.bankSummary.accountName')" sortable />
          <Column field="bankName" :header="t('treasury.report.bankSummary.bankName')" sortable />
          <Column field="accountType" :header="t('treasury.report.bankSummary.accountType')" sortable style="width: 130px">
            <template #body="{ data }">
              <Tag :value="t(`treasury.bankAccount.accountTypes.${data.accountType}`)" :severity="getAccountTypeSeverity(data.accountType)" />
            </template>
          </Column>
          <Column field="openingBalance" :header="t('treasury.report.bankSummary.openingBalance')" style="text-align: right">
            <template #body="{ data }">{{ formatCurrency(data.openingBalance) }}</template>
          </Column>
          <Column field="bookBalance" :header="t('treasury.report.bankSummary.bookBalance')" style="text-align: right">
            <template #body="{ data }">
              <span class="font-bold">{{ formatCurrency(data.bookBalance) }}</span>
            </template>
          </Column>
          <Column field="lastStatementDate" :header="t('treasury.report.bankSummary.lastStatement')" style="width: 140px">
            <template #body="{ data }">{{ data.lastStatementDate ?? '-' }}</template>
          </Column>
          <Column field="active" :header="t('treasury.report.bankSummary.status')" style="width: 100px">
            <template #body="{ data }">
              <Tag :value="data.active ? t('common.active') : t('common.inactive')" :severity="data.active ? 'success' : 'danger'" />
            </template>
          </Column>

          <template #expansion="{ data }">
            <div class="expansion-content">
              <h4 class="expansion-title">
                <i class="pi pi-history" />
                {{ t('treasury.report.bankSummary.recentTransactions') }}
              </h4>
              <DataTable :value="data.recentTransactions" stripedRows paginator :rows="5" :rowsPerPageOptions="[5, 10, 20]">
                <template #empty>{{ t('treasury.report.bankSummary.noTransactions') }}</template>
                <Column field="type" :header="t('treasury.report.common.type')" style="width: 130px">
                  <template #body="{ data: txn }">
                    <Tag :value="t(`treasury.report.bankSummary.transactionTypes.${txn.type}`)" :severity="txn.type === 'INCOME' ? 'success' : 'warn'" />
                  </template>
                </Column>
                <Column field="date" :header="t('treasury.report.common.date')" style="width: 120px" />
                <Column field="description" :header="t('treasury.report.common.description')" />
                <Column field="amount" :header="t('treasury.report.common.amount')" style="text-align: right; width: 140px">
                  <template #body="{ data: txn }">
                    <span class="font-semibold" :class="txn.amount >= 0 ? 'text-green' : 'text-red'">
                      {{ formatCurrency(txn.amount) }}
                    </span>
                  </template>
                </Column>
                <Column field="reference" :header="t('treasury.report.common.reference')" style="width: 160px">
                  <template #body="{ data: txn }">
                    <span class="ref-text">{{ txn.reference ?? '-' }}</span>
                  </template>
                </Column>
              </DataTable>
            </div>
          </template>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.bank-summary {
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

.hero-card {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 1.5rem 2rem;
  background: var(--p-surface-card);
  border: 1px solid var(--p-surface-border);
  border-radius: var(--p-border-radius);
  margin-bottom: 1.5rem;
}

.hero-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 1rem;
  background: var(--p-primary-50);
  color: var(--p-primary-color);
  font-size: 1.5rem;
  flex-shrink: 0;
}

.hero-content {
  display: flex;
  flex-direction: column;
}

.hero-label {
  font-size: 0.9rem;
  color: var(--p-text-muted-color);
  font-weight: 500;
}

.hero-value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--p-primary-color);
  line-height: 1.3;
}

.expansion-content {
  padding: 0.5rem 1rem 0.5rem 2rem;
}

.expansion-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 0.75rem 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--p-text-muted-color);
}

.text-green {
  color: var(--p-green-500);
}

.text-red {
  color: var(--p-red-500);
}

.ref-text {
  font-family: monospace;
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
}
</style>
