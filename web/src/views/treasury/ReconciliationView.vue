<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import { useBankStatementStore } from '@/stores/bankStatement'
import { useBankAccountStore } from '@/stores/bankAccount'
import {
  MatchStatus,
  BankStatementStatus,
  ExpenseCategory,
  IncomeCategory
} from '@/types/treasury'
import type { BankStatementRow } from '@/types/treasury'
import { formatCurrency } from '@/utils/format'
import { toApiDate } from '@/utils/format'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { showError, showSuccess } = useErrorHandler()
const store = useBankStatementStore()
const bankAccountStore = useBankAccountStore()

const bankAccountId = computed(() => Number(route.params.bankAccountId))
const statementId = computed(() => Number(route.params.statementId))

const showAcknowledgeDialog = ref(false)
const showCreateExpenseDialog = ref(false)
const showCreateIncomeDialog = ref(false)
const selectedRow = ref<BankStatementRow | null>(null)

// Acknowledge form
const acknowledgeReason = ref('')
const acknowledgeNonLedger = ref(false)

// Create expense form
const expenseForm = ref({
  supplierName: '',
  category: null as ExpenseCategory | null,
  description: '',
  amount: null as number | null,
  expenseDate: null as Date | null,
  invoiceNumber: '',
  notes: ''
})

// Create income form
const incomeForm = ref({
  description: '',
  category: null as IncomeCategory | null,
  amount: null as number | null,
  incomeDate: null as Date | null,
  reference: '',
  notes: ''
})

const expenseCategories = Object.values(ExpenseCategory).map((v) => ({
  label: t(`treasury.expense.categories.${v}`),
  value: v
}))

const incomeCategories = Object.values(IncomeCategory).map((v) => ({
  label: t(`treasury.income.categories.${v}`),
  value: v
}))

const canComplete = computed(() => {
  if (!store.currentStatement) return false
  return (
    store.currentStatement.status !== BankStatementStatus.COMPLETED &&
    store.currentStatement.unmatchedCount === 0 &&
    store.currentStatement.suggestedCount === 0
  )
})

const isCompleted = computed(
  () => store.currentStatement?.status === BankStatementStatus.COMPLETED
)

onMounted(async () => {
  try {
    await bankAccountStore.fetchBankAccount(bankAccountId.value)
    await store.fetchStatement(bankAccountId.value, statementId.value)
    await store.fetchRows(bankAccountId.value, statementId.value)
  } catch (error) {
    showError(error)
  }
})

function rowClass(data: BankStatementRow): string {
  switch (data.matchStatus) {
    case MatchStatus.MATCHED:
      return 'row-matched'
    case MatchStatus.SUGGESTED:
      return 'row-suggested'
    case MatchStatus.UNMATCHED:
      return 'row-unmatched'
    case MatchStatus.ACKNOWLEDGED:
      return 'row-acknowledged'
    default:
      return ''
  }
}

function statusSeverity(status: MatchStatus): string {
  switch (status) {
    case MatchStatus.MATCHED:
      return 'success'
    case MatchStatus.SUGGESTED:
      return 'info'
    case MatchStatus.UNMATCHED:
      return 'warn'
    case MatchStatus.ACKNOWLEDGED:
      return 'secondary'
    default:
      return 'info'
  }
}

async function onConfirmMatch(row: BankStatementRow) {
  try {
    await store.confirmMatch(bankAccountId.value, statementId.value, row.id)
    showSuccess('treasury.reconciliation.matchConfirmed')
    refreshStatement()
  } catch (error) {
    showError(error)
  }
}

async function onRejectMatch(row: BankStatementRow) {
  try {
    await store.rejectMatch(bankAccountId.value, statementId.value, row.id)
    showSuccess('treasury.reconciliation.matchRejected')
    refreshStatement()
  } catch (error) {
    showError(error)
  }
}

function openAcknowledge(row: BankStatementRow) {
  selectedRow.value = row
  acknowledgeReason.value = ''
  acknowledgeNonLedger.value = false
  showAcknowledgeDialog.value = true
}

async function submitAcknowledge() {
  if (!selectedRow.value || !acknowledgeReason.value.trim()) return
  try {
    await store.acknowledgeRow(bankAccountId.value, statementId.value, selectedRow.value.id, {
      reason: acknowledgeReason.value.trim(),
      nonLedger: acknowledgeNonLedger.value
    })
    showSuccess('treasury.reconciliation.rowAcknowledged')
    showAcknowledgeDialog.value = false
    refreshStatement()
  } catch (error) {
    showError(error)
  }
}

function openCreateExpense(row: BankStatementRow) {
  selectedRow.value = row
  expenseForm.value = {
    supplierName: row.description || '',
    category: null,
    description: '',
    amount: row.debitAmount || 0,
    expenseDate: row.transactionDate ? new Date(row.transactionDate + 'T00:00:00') : null,
    invoiceNumber: row.reference || '',
    notes: ''
  }
  showCreateExpenseDialog.value = true
}

async function submitCreateExpense() {
  if (!selectedRow.value || !expenseForm.value.category || !expenseForm.value.expenseDate) return
  try {
    await store.createExpenseFromRow(
      bankAccountId.value,
      statementId.value,
      selectedRow.value.id,
      {
        supplierName: expenseForm.value.supplierName,
        category: expenseForm.value.category,
        description: expenseForm.value.description || undefined,
        amount: expenseForm.value.amount || 0,
        expenseDate: toApiDate(expenseForm.value.expenseDate)!,
        invoiceNumber: expenseForm.value.invoiceNumber,
        notes: expenseForm.value.notes || undefined
      }
    )
    showSuccess('treasury.reconciliation.expenseCreated')
    showCreateExpenseDialog.value = false
    refreshStatement()
  } catch (error) {
    showError(error)
  }
}

function openCreateIncome(row: BankStatementRow) {
  selectedRow.value = row
  incomeForm.value = {
    description: row.description || '',
    category: null,
    amount: row.creditAmount || 0,
    incomeDate: row.transactionDate ? new Date(row.transactionDate + 'T00:00:00') : null,
    reference: row.reference || '',
    notes: ''
  }
  showCreateIncomeDialog.value = true
}

async function submitCreateIncome() {
  if (!selectedRow.value || !incomeForm.value.category || !incomeForm.value.incomeDate) return
  try {
    await store.createIncomeFromRow(
      bankAccountId.value,
      statementId.value,
      selectedRow.value.id,
      {
        description: incomeForm.value.description,
        category: incomeForm.value.category,
        amount: incomeForm.value.amount || 0,
        incomeDate: toApiDate(incomeForm.value.incomeDate)!,
        reference: incomeForm.value.reference || undefined,
        notes: incomeForm.value.notes || undefined
      }
    )
    showSuccess('treasury.reconciliation.incomeCreated')
    showCreateIncomeDialog.value = false
    refreshStatement()
  } catch (error) {
    showError(error)
  }
}

async function onComplete() {
  try {
    await store.completeStatement(bankAccountId.value, statementId.value)
    showSuccess('treasury.reconciliation.completed')
  } catch (error) {
    showError(error)
  }
}

async function refreshStatement() {
  await store.fetchStatement(bankAccountId.value, statementId.value)
}
</script>

<template>
  <div class="reconciliation-view">
    <div class="page-header">
      <div class="header-left">
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          rounded
          @click="
            router.push({
              name: 'bank-statements',
              params: { bankAccountId: bankAccountId }
            })
          "
        />
        <h1 class="page-title">{{ t('treasury.reconciliation.reconcile') }}</h1>
      </div>
      <div class="header-actions">
        <Button
          v-if="canComplete"
          icon="pi pi-check"
          :label="t('treasury.reconciliation.complete')"
          severity="success"
          @click="onComplete"
        />
        <Button
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="store.fetchRows(bankAccountId, statementId)"
          :loading="store.loading"
        />
      </div>
    </div>

    <!-- Summary -->
    <Card class="mb-3" v-if="store.currentStatement">
      <template #content>
        <div class="summary-row">
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.reconciliation.fileName') }}</span>
            <span>{{ store.currentStatement.fileName }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.reconciliation.statementDate') }}</span>
            <span>{{ store.currentStatement.statementDate }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.reconciliation.totalRows') }}</span>
            <span>{{ store.currentStatement.totalRows }}</span>
          </div>
          <div class="summary-item">
            <Tag :value="`${store.currentStatement.matchedCount} ${t('treasury.reconciliation.matchStatuses.MATCHED')}`" severity="success" />
            <Tag
              v-if="store.currentStatement.suggestedCount > 0"
              :value="`${store.currentStatement.suggestedCount} ${t('treasury.reconciliation.matchStatuses.SUGGESTED')}`"
              severity="info"
            />
            <Tag
              v-if="store.currentStatement.unmatchedCount > 0"
              :value="`${store.currentStatement.unmatchedCount} ${t('treasury.reconciliation.matchStatuses.UNMATCHED')}`"
              severity="warn"
            />
            <Tag
              v-if="store.currentStatement.acknowledgedCount > 0"
              :value="`${store.currentStatement.acknowledgedCount} ${t('treasury.reconciliation.matchStatuses.ACKNOWLEDGED')}`"
              severity="secondary"
            />
          </div>
          <div class="summary-item" v-if="isCompleted">
            <Tag :value="t('treasury.reconciliation.statuses.COMPLETED')" severity="success" />
          </div>
        </div>
      </template>
    </Card>

    <!-- Rows DataTable -->
    <Card>
      <template #content>
        <DataTable
          :value="store.rows"
          :loading="store.loading"
          data-key="id"
          striped-rows
          :row-class="rowClass"
          scrollable
          scroll-height="600px"
        >
          <template #empty>
            <div class="text-center p-4">{{ t('treasury.reconciliation.noRows') }}</div>
          </template>

          <Column field="rowNumber" header="#" style="width: 50px" />

          <Column
            field="transactionDate"
            :header="t('treasury.reconciliation.transactionDate')"
            style="width: 110px"
          />

          <Column
            field="description"
            :header="t('treasury.reconciliation.description')"
            style="min-width: 200px"
          >
            <template #body="{ data }">{{ data.description || '—' }}</template>
          </Column>

          <Column
            field="reference"
            :header="t('treasury.reconciliation.reference')"
            style="width: 120px"
          >
            <template #body="{ data }">{{ data.reference || '—' }}</template>
          </Column>

          <Column :header="t('treasury.reconciliation.debit')" style="width: 120px">
            <template #body="{ data }">
              <span v-if="data.debitAmount" class="text-red-500">{{
                formatCurrency(data.debitAmount)
              }}</span>
              <span v-else>—</span>
            </template>
          </Column>

          <Column :header="t('treasury.reconciliation.credit')" style="width: 120px">
            <template #body="{ data }">
              <span v-if="data.creditAmount" class="text-green-600">{{
                formatCurrency(data.creditAmount)
              }}</span>
              <span v-else>—</span>
            </template>
          </Column>

          <Column :header="t('treasury.reconciliation.matchStatusLabel')" style="width: 130px">
            <template #body="{ data }">
              <Tag
                :value="t(`treasury.reconciliation.matchStatuses.${data.matchStatus}`)"
                :severity="statusSeverity(data.matchStatus)"
              />
            </template>
          </Column>

          <Column :header="t('treasury.reconciliation.matchedTo')" style="min-width: 180px">
            <template #body="{ data }">
              <span v-if="data.matchedEntityDescription">{{ data.matchedEntityDescription }}</span>
              <span v-else-if="data.acknowledgedReason" class="text-muted">{{
                data.acknowledgedReason
              }}</span>
              <span v-else>—</span>
            </template>
          </Column>

          <Column
            :header="t('common.actions')"
            style="width: 160px"
            frozen
            align-frozen="right"
          >
            <template #body="{ data }">
              <div class="action-buttons" v-if="!isCompleted">
                <!-- SUGGESTED actions -->
                <template v-if="data.matchStatus === 'SUGGESTED'">
                  <Button
                    icon="pi pi-check"
                    severity="success"
                    text
                    rounded
                    size="small"
                    v-tooltip.top="t('treasury.reconciliation.confirm')"
                    @click="onConfirmMatch(data)"
                  />
                  <Button
                    icon="pi pi-times"
                    severity="danger"
                    text
                    rounded
                    size="small"
                    v-tooltip.top="t('treasury.reconciliation.reject')"
                    @click="onRejectMatch(data)"
                  />
                </template>
                <!-- UNMATCHED actions -->
                <template v-if="data.matchStatus === 'UNMATCHED'">
                  <Button
                    v-if="data.debitAmount"
                    icon="pi pi-plus"
                    severity="warn"
                    text
                    rounded
                    size="small"
                    v-tooltip.top="t('treasury.reconciliation.createExpense')"
                    @click="openCreateExpense(data)"
                  />
                  <Button
                    v-if="data.creditAmount"
                    icon="pi pi-plus"
                    severity="success"
                    text
                    rounded
                    size="small"
                    v-tooltip.top="t('treasury.reconciliation.createIncome')"
                    @click="openCreateIncome(data)"
                  />
                  <Button
                    icon="pi pi-eye-slash"
                    severity="secondary"
                    text
                    rounded
                    size="small"
                    v-tooltip.top="t('treasury.reconciliation.acknowledge')"
                    @click="openAcknowledge(data)"
                  />
                </template>
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Acknowledge Dialog -->
    <Dialog
      v-model:visible="showAcknowledgeDialog"
      :header="t('treasury.reconciliation.acknowledgeTitle')"
      :style="{ width: '400px' }"
      modal
    >
      <div class="form-field">
        <label>{{ t('treasury.reconciliation.reason') }}</label>
        <Textarea v-model="acknowledgeReason" rows="3" class="w-full" />
      </div>
      <div class="form-field flex items-center gap-2">
        <Checkbox v-model="acknowledgeNonLedger" :binary="true" input-id="nonLedger" />
        <label for="nonLedger">{{ t('treasury.reconciliation.nonLedger') }}</label>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          @click="showAcknowledgeDialog = false"
        />
        <Button
          :label="t('common.save')"
          :disabled="!acknowledgeReason.trim()"
          @click="submitAcknowledge"
        />
      </template>
    </Dialog>

    <!-- Create Expense Dialog -->
    <Dialog
      v-model:visible="showCreateExpenseDialog"
      :header="t('treasury.reconciliation.createExpense')"
      :style="{ width: '500px' }"
      modal
    >
      <div class="form-grid">
        <div class="form-field">
          <label>{{ t('treasury.expense.supplierName') }}</label>
          <InputText v-model="expenseForm.supplierName" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.expense.category') }}</label>
          <Select
            v-model="expenseForm.category"
            :options="expenseCategories"
            option-label="label"
            option-value="value"
            :placeholder="t('treasury.expense.category')"
            class="w-full"
          />
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>{{ t('treasury.expense.amount') }}</label>
            <InputNumber
              v-model="expenseForm.amount"
              mode="currency"
              currency="GTQ"
              locale="es-GT"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label>{{ t('treasury.expense.expenseDate') }}</label>
            <DatePicker v-model="expenseForm.expenseDate" date-format="yy-mm-dd" class="w-full" />
          </div>
        </div>
        <div class="form-field">
          <label>{{ t('treasury.expense.invoiceNumber') }}</label>
          <InputText v-model="expenseForm.invoiceNumber" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('common.notes') }}</label>
          <Textarea v-model="expenseForm.notes" rows="2" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          @click="showCreateExpenseDialog = false"
        />
        <Button
          :label="t('common.save')"
          :disabled="!expenseForm.supplierName || !expenseForm.category || !expenseForm.expenseDate"
          @click="submitCreateExpense"
        />
      </template>
    </Dialog>

    <!-- Create Income Dialog -->
    <Dialog
      v-model:visible="showCreateIncomeDialog"
      :header="t('treasury.reconciliation.createIncome')"
      :style="{ width: '500px' }"
      modal
    >
      <div class="form-grid">
        <div class="form-field">
          <label>{{ t('treasury.income.description') }}</label>
          <InputText v-model="incomeForm.description" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.income.category') }}</label>
          <Select
            v-model="incomeForm.category"
            :options="incomeCategories"
            option-label="label"
            option-value="value"
            :placeholder="t('treasury.income.category')"
            class="w-full"
          />
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>{{ t('treasury.income.amount') }}</label>
            <InputNumber
              v-model="incomeForm.amount"
              mode="currency"
              currency="GTQ"
              locale="es-GT"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label>{{ t('treasury.income.incomeDate') }}</label>
            <DatePicker v-model="incomeForm.incomeDate" date-format="yy-mm-dd" class="w-full" />
          </div>
        </div>
        <div class="form-field">
          <label>{{ t('treasury.income.reference') }}</label>
          <InputText v-model="incomeForm.reference" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('common.notes') }}</label>
          <Textarea v-model="incomeForm.notes" rows="2" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          @click="showCreateIncomeDialog = false"
        />
        <Button
          :label="t('common.save')"
          :disabled="!incomeForm.description || !incomeForm.category || !incomeForm.incomeDate"
          @click="submitCreateIncome"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.reconciliation-view {
  max-width: 1400px;
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

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.mb-3 {
  margin-bottom: 1rem;
}

.summary-row {
  display: flex;
  gap: 2rem;
  align-items: center;
  flex-wrap: wrap;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.summary-label {
  font-size: 0.8em;
  color: var(--p-text-muted-color);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.15rem;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-row .form-field {
  flex: 1;
}

.w-full {
  width: 100%;
}

.text-red-500 {
  color: var(--p-red-500);
}

.text-green-600 {
  color: var(--p-green-600);
}

.text-muted {
  color: var(--p-text-muted-color);
  font-style: italic;
}

:deep(.row-matched) {
  background-color: color-mix(in srgb, var(--p-green-500) 8%, transparent) !important;
}

:deep(.row-suggested) {
  background-color: color-mix(in srgb, var(--p-blue-500) 8%, transparent) !important;
}

:deep(.row-unmatched) {
  background-color: color-mix(in srgb, var(--p-yellow-500) 8%, transparent) !important;
}

:deep(.row-acknowledged) {
  background-color: color-mix(in srgb, var(--p-surface-400) 8%, transparent) !important;
}
</style>
