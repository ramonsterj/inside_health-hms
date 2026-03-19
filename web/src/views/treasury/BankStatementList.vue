<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import BankStatementUpload from '@/components/treasury/BankStatementUpload.vue'
import ColumnMappingForm from '@/components/treasury/ColumnMappingForm.vue'
import { useBankStatementStore } from '@/stores/bankStatement'
import { useBankAccountStore } from '@/stores/bankAccount'
import { BankStatementStatus } from '@/types/treasury'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const store = useBankStatementStore()
const bankAccountStore = useBankAccountStore()

const bankAccountId = computed(() => Number(route.params.bankAccountId))
const showUpload = ref(false)
const showMapping = ref(false)

onMounted(async () => {
  try {
    await bankAccountStore.fetchBankAccount(bankAccountId.value)
    await store.fetchStatements(bankAccountId.value)
    await store.fetchColumnMapping(bankAccountId.value)
  } catch (error) {
    showError(error)
  }
})

function openUpload() {
  if (!store.columnMapping) {
    showMapping.value = true
    return
  }
  showUpload.value = true
}

function onUploaded() {
  store.fetchStatements(bankAccountId.value)
}

function onMappingSaved() {
  store.fetchColumnMapping(bankAccountId.value)
}

function goToReconcile(statementId: number) {
  router.push({
    name: 'bank-statement-reconcile',
    params: { bankAccountId: bankAccountId.value, statementId }
  })
}

function confirmDelete(statementId: number) {
  confirm.require({
    message: t('treasury.reconciliation.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteStatement(statementId)
  })
}

async function deleteStatement(statementId: number) {
  try {
    await store.deleteStatement(bankAccountId.value, statementId)
    showSuccess('treasury.reconciliation.deleted')
    store.fetchStatements(bankAccountId.value)
  } catch (error) {
    showError(error)
  }
}

function statusSeverity(status: BankStatementStatus): string {
  return status === BankStatementStatus.COMPLETED ? 'success' : 'warn'
}

function progressPercent(data: { totalRows: number; matchedCount: number; acknowledgedCount: number }): number {
  if (data.totalRows === 0) return 0
  return Math.round(((data.matchedCount + data.acknowledgedCount) / data.totalRows) * 100)
}
</script>

<template>
  <div class="bank-statement-list">
    <div class="page-header">
      <div class="header-left">
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          rounded
          @click="router.push({ name: 'treasury-bank-accounts' })"
        />
        <h1 class="page-title">
          {{ t('treasury.reconciliation.title') }}
          <span v-if="bankAccountStore.currentBankAccount" class="account-name">
            — {{ bankAccountStore.currentBankAccount.name }}
          </span>
        </h1>
      </div>
      <div class="header-actions">
        <Button
          icon="pi pi-cog"
          :label="t('treasury.reconciliation.columnMapping')"
          severity="secondary"
          outlined
          @click="showMapping = true"
        />
        <Button
          icon="pi pi-upload"
          :label="t('treasury.reconciliation.upload')"
          @click="openUpload"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="store.fetchStatements(bankAccountId)"
          :loading="store.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="store.statements"
          :loading="store.loading"
          data-key="id"
          striped-rows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('treasury.reconciliation.empty') }}</div>
          </template>

          <Column field="fileName" :header="t('treasury.reconciliation.fileName')" />

          <Column field="statementDate" :header="t('treasury.reconciliation.statementDate')" style="width: 130px" />

          <Column :header="t('treasury.reconciliation.status')" style="width: 130px">
            <template #body="{ data }">
              <Tag
                :value="t(`treasury.reconciliation.statuses.${data.status}`)"
                :severity="statusSeverity(data.status)"
              />
            </template>
          </Column>

          <Column :header="t('treasury.reconciliation.progress')" style="width: 200px">
            <template #body="{ data }">
              <ProgressBar :value="progressPercent(data)" :show-value="true" style="height: 20px" />
              <div class="progress-detail">
                {{ data.matchedCount + data.acknowledgedCount }} / {{ data.totalRows }}
              </div>
            </template>
          </Column>

          <Column :header="t('treasury.reconciliation.summary')" style="width: 200px">
            <template #body="{ data }">
              <div class="summary-badges">
                <Tag v-if="data.suggestedCount > 0" :value="`${data.suggestedCount} suggested`" severity="info" />
                <Tag v-if="data.unmatchedCount > 0" :value="`${data.unmatchedCount} unmatched`" severity="warn" />
              </div>
            </template>
          </Column>

          <Column :header="t('common.actions')" style="width: 120px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  icon="pi pi-search"
                  severity="info"
                  text
                  rounded
                  v-tooltip.top="t('treasury.reconciliation.reconcile')"
                  @click="goToReconcile(data.id)"
                />
                <Button
                  v-if="data.status !== 'COMPLETED'"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  v-tooltip.top="t('common.delete')"
                  @click="confirmDelete(data.id)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <BankStatementUpload
      v-model:visible="showUpload"
      :bank-account-id="bankAccountId"
      @uploaded="onUploaded"
    />

    <ColumnMappingForm
      v-model:visible="showMapping"
      :bank-account-id="bankAccountId"
      @saved="onMappingSaved"
    />
  </div>
</template>

<style scoped>
.bank-statement-list {
  max-width: 1100px;
  margin: 0 auto;
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
}

.account-name {
  font-weight: normal;
  font-size: 0.9em;
  color: var(--p-text-muted-color);
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.progress-detail {
  font-size: 0.8em;
  text-align: center;
  color: var(--p-text-muted-color);
  margin-top: 2px;
}

.summary-badges {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}
</style>
