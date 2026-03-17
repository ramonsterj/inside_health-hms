<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import ExpenseForm from '@/components/treasury/ExpenseForm.vue'
import ExpensePaymentDialog from '@/components/treasury/ExpensePaymentDialog.vue'
import { useExpenseStore } from '@/stores/expense'
import { useAuthStore } from '@/stores/auth'
import { ExpenseStatus, ExpenseCategory } from '@/types/treasury'
import type { Expense } from '@/types/treasury'
import { formatCurrency, toApiDate } from '@/utils/format'

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const expenseStore = useExpenseStore()
const authStore = useAuthStore()

// Permissions
const canWrite = computed(() => authStore.hasPermission('treasury:write'))
const canDelete = computed(() => authStore.hasPermission('treasury:delete'))

// Table state
const currentPage = ref(0)
const pageSize = ref(20)

// Filters
const filterStatus = ref<string | null>(null)
const filterCategory = ref<string | null>(null)
const filterFrom = ref<Date | null>(null)
const filterTo = ref<Date | null>(null)
const filterSearch = ref<string>('')

// Dialogs
const showExpenseForm = ref(false)
const showPaymentDialog = ref(false)
const selectedExpense = ref<Expense | null>(null)

// Payments list dialog
const showPaymentsList = ref(false)
const paymentsListExpense = ref<Expense | null>(null)

// File upload
const uploadInputRef = ref<HTMLInputElement | null>(null)
const uploadingForExpense = ref<Expense | null>(null)

const statusOptions = computed(() => [
  { label: t('treasury.expense.filters.allStatuses'), value: null },
  ...Object.values(ExpenseStatus).map(v => ({
    label: t(`treasury.expense.statuses.${v}`),
    value: v
  }))
])

const categoryOptions = computed(() => [
  { label: t('treasury.expense.filters.allCategories'), value: null },
  ...Object.values(ExpenseCategory).map(v => ({
    label: t(`treasury.expense.categories.${v}`),
    value: v
  }))
])

onMounted(() => loadExpenses())

async function loadExpenses() {
  try {
    await expenseStore.fetchExpenses(currentPage.value, pageSize.value, {
      status: filterStatus.value ?? undefined,
      category: filterCategory.value ?? undefined,
      from: toApiDate(filterFrom.value) ?? undefined,
      to: toApiDate(filterTo.value) ?? undefined,
      search: filterSearch.value || undefined
    })
  } catch (error) {
    showError(error)
  }
}

function onPage(event: { page: number; rows: number }) {
  currentPage.value = event.page
  pageSize.value = event.rows
  loadExpenses()
}

function applyFilters() {
  currentPage.value = 0
  loadExpenses()
}

function clearFilters() {
  filterStatus.value = null
  filterCategory.value = null
  filterFrom.value = null
  filterTo.value = null
  filterSearch.value = ''
  currentPage.value = 0
  loadExpenses()
}

function openCreate() {
  showExpenseForm.value = true
}

function onExpenseCreated() {
  loadExpenses()
}

function openPaymentDialog(expense: Expense) {
  selectedExpense.value = expense
  showPaymentDialog.value = true
}

function onPaymentRecorded() {
  loadExpenses()
}

async function openPaymentsList(expense: Expense) {
  paymentsListExpense.value = expense
  showPaymentsList.value = true
  try {
    await expenseStore.fetchPayments(expense.id)
  } catch (error) {
    showError(error)
  }
}

function triggerUpload(expense: Expense) {
  uploadingForExpense.value = expense
  uploadInputRef.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !uploadingForExpense.value) return
  try {
    await expenseStore.uploadInvoiceDocument(uploadingForExpense.value.id, file)
    showSuccess('treasury.expense.invoiceUploaded')
    loadExpenses()
  } catch (error) {
    showError(error)
  } finally {
    input.value = ''
    uploadingForExpense.value = null
  }
}

function confirmDelete(expense: Expense) {
  confirm.require({
    message: t('treasury.expense.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteExpense(expense.id)
  })
}

async function deleteExpense(id: number) {
  try {
    await expenseStore.deleteExpense(id)
    showSuccess('treasury.expense.deleted')
    loadExpenses()
  } catch (error) {
    showError(error)
  }
}

function statusSeverity(expense: Expense): string {
  if (expense.isOverdue) return 'danger'
  switch (expense.status) {
    case ExpenseStatus.PAID:
      return 'success'
    case ExpenseStatus.PARTIALLY_PAID:
      return 'info'
    case ExpenseStatus.CANCELLED:
      return 'secondary'
    default:
      return 'warn'
  }
}

function statusLabel(expense: Expense): string {
  if (expense.isOverdue) return t('treasury.expense.statuses.OVERDUE')
  return t(`treasury.expense.statuses.${expense.status}`)
}

function canRecordPayment(expense: Expense): boolean {
  return expense.status === ExpenseStatus.PENDING || expense.status === ExpenseStatus.PARTIALLY_PAID
}
</script>

<template>
  <div class="expense-list">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.expense.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canWrite"
          icon="pi pi-plus"
          :label="t('treasury.expense.new')"
          @click="openCreate"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadExpenses"
          :loading="expenseStore.loading"
        />
      </div>
    </div>

    <!-- Filters -->
    <Card class="filters-card">
      <template #content>
        <div class="filters-grid">
          <div class="filter-field">
            <label>{{ t('treasury.expense.status') }}</label>
            <Select
              v-model="filterStatus"
              :options="statusOptions"
              option-label="label"
              option-value="value"
            />
          </div>
          <div class="filter-field">
            <label>{{ t('treasury.expense.category') }}</label>
            <Select
              v-model="filterCategory"
              :options="categoryOptions"
              option-label="label"
              option-value="value"
            />
          </div>
          <div class="filter-field">
            <label>{{ t('common.from') }}</label>
            <DatePicker v-model="filterFrom" date-format="yy-mm-dd" />
          </div>
          <div class="filter-field">
            <label>{{ t('common.to') }}</label>
            <DatePicker v-model="filterTo" date-format="yy-mm-dd" />
          </div>
          <div class="filter-field filter-search">
            <label>{{ t('common.search') }}</label>
            <InputText
              v-model="filterSearch"
              :placeholder="t('common.search')"
              @keydown.enter="applyFilters"
            />
          </div>
          <div class="filter-actions">
            <Button :label="t('common.filter')" icon="pi pi-search" @click="applyFilters" />
            <Button
              :label="t('common.clear')"
              icon="pi pi-times"
              severity="secondary"
              outlined
              @click="clearFilters"
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- Table -->
    <Card>
      <template #content>
        <DataTable
          :value="expenseStore.expenses"
          :loading="expenseStore.loading"
          data-key="id"
          striped-rows
          lazy
          :total-records="expenseStore.totalExpenses"
          paginator
          :rows="pageSize"
          :rows-per-page-options="[10, 20, 50]"
          @page="onPage"
        >
          <template #empty>
            <div class="text-center p-4">{{ t('treasury.expense.empty') }}</div>
          </template>

          <Column field="supplierName" :header="t('treasury.expense.supplierName')" />

          <Column :header="t('treasury.expense.category')" style="width: 120px">
            <template #body="{ data }">
              {{ t(`treasury.expense.categories.${data.category}`) }}
            </template>
          </Column>

          <Column
            field="invoiceNumber"
            :header="t('treasury.expense.invoiceNumber')"
            style="width: 120px"
          />

          <Column
            field="expenseDate"
            :header="t('treasury.expense.expenseDate')"
            style="width: 110px"
          />

          <Column :header="t('treasury.expense.amount')" style="width: 110px">
            <template #body="{ data }">{{ formatCurrency(data.amount) }}</template>
          </Column>

          <Column :header="t('treasury.expense.paidAmount')" style="width: 100px">
            <template #body="{ data }">{{ formatCurrency(data.paidAmount) }}</template>
          </Column>

          <Column :header="t('treasury.expense.remainingAmount')" style="width: 110px">
            <template #body="{ data }">
              <span :class="data.remainingAmount > 0 ? 'text-orange' : ''">
                {{ formatCurrency(data.remainingAmount) }}
              </span>
            </template>
          </Column>

          <Column :header="t('treasury.expense.status')" style="width: 130px">
            <template #body="{ data }">
              <Tag :value="statusLabel(data)" :severity="statusSeverity(data)" />
            </template>
          </Column>

          <Column field="dueDate" :header="t('treasury.expense.dueDate')" style="width: 110px">
            <template #body="{ data }">{{ data.dueDate || '—' }}</template>
          </Column>

          <Column :header="t('common.actions')" style="width: 160px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  icon="pi pi-list"
                  severity="secondary"
                  text
                  rounded
                  v-tooltip.top="t('treasury.payment.title')"
                  @click="openPaymentsList(data)"
                />
                <Button
                  v-if="canWrite && canRecordPayment(data)"
                  icon="pi pi-dollar"
                  severity="success"
                  text
                  rounded
                  v-tooltip.top="t('treasury.payment.record')"
                  @click="openPaymentDialog(data)"
                />
                <Button
                  v-if="canWrite"
                  icon="pi pi-paperclip"
                  severity="secondary"
                  text
                  rounded
                  v-tooltip.top="t('treasury.expense.uploadInvoice')"
                  @click="triggerUpload(data)"
                />
                <Button
                  v-if="canDelete && data.status === 'PENDING'"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  v-tooltip.top="t('common.delete')"
                  @click="confirmDelete(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Hidden file input for invoice upload -->
    <input
      ref="uploadInputRef"
      type="file"
      accept=".pdf,.jpg,.jpeg,.png"
      style="display: none"
      @change="onFileSelected"
    />

    <!-- Create Expense Dialog -->
    <ExpenseForm v-model:visible="showExpenseForm" @created="onExpenseCreated" />

    <!-- Record Payment Dialog -->
    <ExpensePaymentDialog
      v-if="selectedExpense"
      v-model:visible="showPaymentDialog"
      :expense="selectedExpense"
      @paid="onPaymentRecorded"
    />

    <!-- Payments List Dialog -->
    <Dialog
      v-model:visible="showPaymentsList"
      :header="t('treasury.payment.title')"
      modal
      :style="{ width: '600px' }"
    >
      <DataTable
        :value="expenseStore.payments"
        :loading="expenseStore.loading"
        data-key="id"
        striped-rows
      >
        <template #empty>
          <div class="text-center p-4">{{ t('treasury.payment.empty') }}</div>
        </template>
        <Column field="paymentDate" :header="t('treasury.payment.paymentDate')" />
        <Column :header="t('treasury.payment.amount')">
          <template #body="{ data }">{{ formatCurrency(data.amount) }}</template>
        </Column>
        <Column :header="t('treasury.payment.bankAccount')">
          <template #body="{ data }">{{ data.bankAccountName }}</template>
        </Column>
        <Column field="reference" :header="t('treasury.payment.reference')">
          <template #body="{ data }">{{ data.reference || '—' }}</template>
        </Column>
      </DataTable>
    </Dialog>
  </div>
</template>

<style scoped>
.expense-list {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.filters-card {
  margin-bottom: 1rem;
}

.filters-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  min-width: 160px;
}

.filter-field label {
  font-size: 0.85rem;
  font-weight: 500;
}

.filter-search {
  min-width: 220px;
}

.filter-actions {
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
  padding-bottom: 0.1rem;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.text-orange {
  color: var(--p-orange-500);
}
</style>
