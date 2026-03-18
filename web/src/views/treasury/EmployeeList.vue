<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import ToggleSwitch from 'primevue/toggleswitch'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import EmployeeForm from '@/components/treasury/EmployeeForm.vue'
import { useTreasuryEmployeeStore } from '@/stores/treasuryEmployee'
import { useAuthStore } from '@/stores/auth'
import { EmployeeType } from '@/types/treasury'
import type { TreasuryEmployee } from '@/types/treasury'
import { formatCurrency } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()
const { showError, showSuccess } = useErrorHandler()
const employeeStore = useTreasuryEmployeeStore()
const authStore = useAuthStore()

const canConfigure = computed(() => authStore.hasPermission('treasury:configure'))

const filterType = ref<string | null>(null)
const filterActive = ref(true)
const filterSearch = ref('')

const showForm = ref(false)
const selectedEmployee = ref<TreasuryEmployee | null>(null)

// Terminate dialog
const showTerminateDialog = ref(false)
const terminatingEmployee = ref<TreasuryEmployee | null>(null)
const terminationDate = ref<Date | null>(new Date())
const terminationReason = ref('')
const cancelPendingPayroll = ref(false)
const terminateLoading = ref(false)

const typeOptions = computed(() => [
  { label: t('treasury.employee.filters.allTypes'), value: null },
  ...Object.values(EmployeeType).map(v => ({
    label: t(`treasury.employee.types.${v}`),
    value: v
  }))
])

onMounted(() => loadEmployees())

async function loadEmployees() {
  try {
    await employeeStore.fetchEmployees({
      type: filterType.value ?? undefined,
      activeOnly: filterActive.value,
      search: filterSearch.value || undefined
    })
  } catch (error) {
    showError(error)
  }
}

function clearFilters() {
  filterType.value = null
  filterActive.value = true
  filterSearch.value = ''
  loadEmployees()
}

function openCreate() {
  selectedEmployee.value = null
  showForm.value = true
}

function openEdit(employee: TreasuryEmployee) {
  selectedEmployee.value = employee
  showForm.value = true
}

function onSaved() {
  loadEmployees()
}

function viewPayroll(employee: TreasuryEmployee) {
  router.push({ name: 'employee-payroll', params: { id: employee.id } })
}

function viewDoctorFees(employee: TreasuryEmployee) {
  router.push({ name: 'employee-doctor-fees', params: { id: employee.id } })
}

function openTerminate(employee: TreasuryEmployee) {
  terminatingEmployee.value = employee
  terminationDate.value = new Date()
  terminationReason.value = ''
  cancelPendingPayroll.value = false
  showTerminateDialog.value = true
}

async function submitTerminate() {
  if (!terminatingEmployee.value || !terminationDate.value) return
  terminateLoading.value = true
  try {
    await employeeStore.terminateEmployee(terminatingEmployee.value.id, {
      terminationDate: terminationDate.value.toISOString().substring(0, 10),
      terminationReason: terminationReason.value || null,
      cancelPendingPayroll: cancelPendingPayroll.value
    })
    showSuccess('treasury.employee.terminated')
    showTerminateDialog.value = false
    loadEmployees()
  } catch (error) {
    showError(error)
  } finally {
    terminateLoading.value = false
  }
}

function employeeTypeSeverity(type: EmployeeType): string {
  switch (type) {
    case EmployeeType.PAYROLL:
      return 'info'
    case EmployeeType.CONTRACTOR:
      return 'warn'
    case EmployeeType.DOCTOR:
      return 'success'
    default:
      return 'secondary'
  }
}

function compensationDisplay(employee: TreasuryEmployee): string {
  if (employee.employeeType === EmployeeType.PAYROLL && employee.baseSalary !== null) {
    return formatCurrency(employee.baseSalary)
  }
  if (employee.employeeType === EmployeeType.CONTRACTOR && employee.contractedRate !== null) {
    return formatCurrency(employee.contractedRate)
  }
  return '—'
}
</script>

<template>
  <div class="employee-list">
    <div class="page-header">
      <h1 class="page-title">{{ t('treasury.employee.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canConfigure"
          icon="pi pi-plus"
          :label="t('treasury.employee.new')"
          @click="openCreate"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadEmployees"
          :loading="employeeStore.loading"
        />
      </div>
    </div>

    <!-- Filters -->
    <Card class="filters-card">
      <template #content>
        <div class="filters-grid">
          <div class="filter-field">
            <label>{{ t('treasury.employee.employeeType') }}</label>
            <Select
              v-model="filterType"
              :options="typeOptions"
              option-label="label"
              option-value="value"
            />
          </div>
          <div class="filter-field filter-search">
            <label>{{ t('common.search') }}</label>
            <InputText
              v-model="filterSearch"
              :placeholder="t('common.search')"
              @keydown.enter="loadEmployees"
            />
          </div>
          <div class="filter-toggle">
            <label>{{ t('treasury.employee.activeOnly') }}</label>
            <ToggleSwitch v-model="filterActive" @change="loadEmployees" />
          </div>
          <div class="filter-actions">
            <Button :label="t('common.filter')" icon="pi pi-search" @click="loadEmployees" />
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
          :value="employeeStore.employees"
          :loading="employeeStore.loading"
          data-key="id"
          striped-rows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('treasury.employee.empty') }}</div>
          </template>

          <Column field="fullName" :header="t('treasury.employee.fullName')" />

          <Column :header="t('treasury.employee.employeeType')" style="width: 120px">
            <template #body="{ data }">
              <Tag
                :value="t(`treasury.employee.types.${data.employeeType}`)"
                :severity="employeeTypeSeverity(data.employeeType)"
              />
            </template>
          </Column>

          <Column field="position" :header="t('treasury.employee.position')" style="width: 130px">
            <template #body="{ data }">{{ data.position || '—' }}</template>
          </Column>

          <Column :header="t('treasury.employee.compensation')" style="width: 120px">
            <template #body="{ data }">{{ compensationDisplay(data) }}</template>
          </Column>

          <Column field="hireDate" :header="t('treasury.employee.hireDate')" style="width: 110px">
            <template #body="{ data }">{{ data.hireDate || '—' }}</template>
          </Column>

          <Column :header="t('treasury.employee.active')" style="width: 80px">
            <template #body="{ data }">
              <Tag
                :value="data.active ? t('common.yes') : t('common.no')"
                :severity="data.active ? 'success' : 'secondary'"
              />
            </template>
          </Column>

          <Column :header="t('common.actions')" style="width: 160px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  v-if="data.employeeType === EmployeeType.PAYROLL || data.employeeType === EmployeeType.CONTRACTOR"
                  icon="pi pi-calendar"
                  severity="info"
                  text
                  rounded
                  v-tooltip.top="data.employeeType === EmployeeType.PAYROLL ? t('treasury.payroll.title') : t('treasury.employee.contractorPayments')"
                  @click="viewPayroll(data)"
                />
                <Button
                  v-if="data.employeeType === EmployeeType.DOCTOR"
                  icon="pi pi-money-bill"
                  severity="success"
                  text
                  rounded
                  v-tooltip.top="t('treasury.doctorFee.title')"
                  @click="viewDoctorFees(data)"
                />
                <Button
                  v-if="canConfigure"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  v-tooltip.top="t('common.edit')"
                  @click="openEdit(data)"
                />
                <Button
                  v-if="canConfigure && data.active"
                  icon="pi pi-times-circle"
                  severity="danger"
                  text
                  rounded
                  v-tooltip.top="t('treasury.employee.terminate')"
                  @click="openTerminate(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Employee Form Dialog -->
    <EmployeeForm v-model:visible="showForm" :employee="selectedEmployee" @saved="onSaved" />

    <!-- Terminate Dialog -->
    <Dialog
      v-model:visible="showTerminateDialog"
      :header="t('treasury.employee.terminateTitle')"
      modal
      :style="{ width: '460px' }"
    >
      <div v-if="terminatingEmployee" class="terminate-form">
        <p class="terminate-warning">
          {{ t('treasury.employee.terminateWarning', { name: terminatingEmployee.fullName }) }}
        </p>

        <div class="form-field">
          <label>{{ t('treasury.employee.terminationDate') }} *</label>
          <DatePicker v-model="terminationDate" date-format="yy-mm-dd" />
        </div>

        <div class="form-field">
          <label>{{ t('treasury.employee.terminationReason') }}</label>
          <Textarea v-model="terminationReason" rows="2" />
        </div>

        <div
          v-if="terminatingEmployee.employeeType === EmployeeType.PAYROLL"
          class="form-field toggle-field"
        >
          <label>{{ t('treasury.employee.cancelPendingPayroll') }}</label>
          <ToggleSwitch v-model="cancelPendingPayroll" />
        </div>

        <Message severity="warn" :closable="false">
          {{ t('treasury.employee.terminateConfirm') }}
        </Message>

        <div class="form-actions">
          <Button
            :label="t('common.cancel')"
            severity="secondary"
            outlined
            @click="showTerminateDialog = false"
          />
          <Button
            :label="t('treasury.employee.terminate')"
            severity="danger"
            :loading="terminateLoading"
            @click="submitTerminate"
          />
        </div>
      </div>
    </Dialog>
  </div>
</template>

<style scoped>
.employee-list {
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

.filter-toggle {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.filter-toggle label {
  font-size: 0.85rem;
  font-weight: 500;
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

.terminate-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.terminate-warning {
  margin: 0;
  color: var(--p-text-color);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-field label {
  font-weight: 500;
  font-size: 0.9rem;
}

.toggle-field {
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
