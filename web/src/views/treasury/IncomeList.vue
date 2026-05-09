<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import DatePicker from 'primevue/datepicker'
import IncomeForm from '@/components/treasury/IncomeForm.vue'
import { useIncomeStore } from '@/stores/income'
import { useBankAccountStore } from '@/stores/bankAccount'
import { useAuthStore } from '@/stores/auth'
import { IncomeCategory } from '@/types/treasury'
import type { Income } from '@/types/treasury'
import { formatCurrency, toApiDate } from '@/utils/format'

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const incomeStore = useIncomeStore()
const bankAccountStore = useBankAccountStore()
const authStore = useAuthStore()

const canWrite = computed(() => authStore.hasPermission('treasury:write'))
const canDelete = computed(() => authStore.hasPermission('treasury:delete'))

const currentPage = ref(0)
const pageSize = ref(20)

const filterCategory = ref<string | null>(null)
const filterBankAccount = ref<number | null>(null)
const filterFrom = ref<Date | null>(null)
const filterTo = ref<Date | null>(null)
const filterSearch = ref<string>('')

const showForm = ref(false)
const selectedIncome = ref<Income | null>(null)

const categoryOptions = computed(() => [
  { label: t('treasury.income.filters.allCategories'), value: null },
  ...Object.values(IncomeCategory).map(v => ({
    label: t(`treasury.income.categories.${v}`),
    value: v
  }))
])

const bankAccountOptions = computed(() => [
  { label: t('treasury.income.filters.allAccounts'), value: null },
  ...bankAccountStore.activeBankAccounts.map(a => ({
    label: a.name,
    value: a.id
  }))
])

onMounted(async () => {
  await bankAccountStore.fetchActiveBankAccounts()
  await loadIncomes()
})

async function loadIncomes() {
  try {
    await incomeStore.fetchIncomes(currentPage.value, pageSize.value, {
      category: filterCategory.value ?? undefined,
      bankAccountId: filterBankAccount.value ?? undefined,
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
  loadIncomes()
}

function applyFilters() {
  currentPage.value = 0
  loadIncomes()
}

function clearFilters() {
  filterCategory.value = null
  filterBankAccount.value = null
  filterFrom.value = null
  filterTo.value = null
  filterSearch.value = ''
  currentPage.value = 0
  loadIncomes()
}

function openCreate() {
  selectedIncome.value = null
  showForm.value = true
}

function openEdit(income: Income) {
  selectedIncome.value = income
  showForm.value = true
}

function onSaved() {
  loadIncomes()
}

function confirmDelete(income: Income) {
  confirm.require({
    message: t('treasury.income.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteIncome(income.id)
  })
}

async function deleteIncome(id: number) {
  try {
    await incomeStore.deleteIncome(id)
    showSuccess('treasury.income.deleted')
    loadIncomes()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="income-list">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.income.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canWrite"
          icon="pi pi-plus"
          :label="t('treasury.income.new')"
          @click="openCreate"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadIncomes"
          :loading="incomeStore.loading"
        />
      </div>
    </div>

    <!-- Filters -->
    <Card class="filters-card">
      <template #content>
        <div class="filters-grid">
          <div class="filter-field">
            <label>{{ t('treasury.income.category') }}</label>
            <Select
              v-model="filterCategory"
              :options="categoryOptions"
              option-label="label"
              option-value="value"
            />
          </div>
          <div class="filter-field">
            <label>{{ t('treasury.income.bankAccount') }}</label>
            <Select
              v-model="filterBankAccount"
              :options="bankAccountOptions"
              option-label="label"
              option-value="value"
            />
          </div>
          <div class="filter-field">
            <label>{{ t('common.from') }}</label>
            <DatePicker v-model="filterFrom" />
          </div>
          <div class="filter-field">
            <label>{{ t('common.to') }}</label>
            <DatePicker v-model="filterTo" />
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
          :value="incomeStore.incomes"
          :loading="incomeStore.loading"
          data-key="id"
          striped-rows
          lazy
          :total-records="incomeStore.totalIncomes"
          paginator
          :rows="pageSize"
          :rows-per-page-options="[10, 20, 50]"
          @page="onPage"
        >
          <template #empty>
            <div class="text-center p-4">{{ t('treasury.income.empty') }}</div>
          </template>

          <Column field="description" :header="t('treasury.income.description')" />

          <Column :header="t('treasury.income.category')" style="width: 130px">
            <template #body="{ data }">
              {{ t(`treasury.income.categories.${data.category}`) }}
            </template>
          </Column>

          <Column
            field="incomeDate"
            :header="t('treasury.income.incomeDate')"
            style="width: 110px"
          />

          <Column :header="t('treasury.income.amount')" style="width: 110px">
            <template #body="{ data }">{{ formatCurrency(data.amount) }}</template>
          </Column>

          <Column
            field="bankAccountName"
            :header="t('treasury.income.bankAccount')"
            style="width: 150px"
          />

          <Column field="reference" :header="t('treasury.income.reference')" style="width: 120px">
            <template #body="{ data }">{{ data.reference || '—' }}</template>
          </Column>

          <Column :header="t('common.actions')" style="width: 100px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  v-if="canWrite"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  v-tooltip.top="t('common.edit')"
                  @click="openEdit(data)"
                />
                <Button
                  v-if="canDelete"
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

    <IncomeForm v-model:visible="showForm" :income="selectedIncome" @saved="onSaved" />
  </div>
</template>

<style scoped>
.income-list {
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
</style>
