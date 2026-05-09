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
import InputNumber from 'primevue/inputnumber'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import PayrollPaymentDialog from '@/components/treasury/PayrollPaymentDialog.vue'
import { useTreasuryEmployeeStore } from '@/stores/treasuryEmployee'
import { useBankAccountStore } from '@/stores/bankAccount'
import { useAuthStore } from '@/stores/auth'
import { EmployeeType, EmployeePaymentType, PayrollStatus } from '@/types/treasury'
import type { PayrollEntry, SalaryHistory } from '@/types/treasury'
import { formatCurrency, toApiDate } from '@/utils/format'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { updateSalarySchema, type UpdateSalaryFormData } from '@/validation/treasury'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const employeeStore = useTreasuryEmployeeStore()
const bankAccountStore = useBankAccountStore()
const authStore = useAuthStore()

const canConfigure = computed(() => authStore.hasPermission('treasury:configure'))
const canWrite = computed(() => authStore.hasPermission('treasury:write'))

const employeeId = computed(() => Number(route.params.id))
const employee = computed(() => employeeStore.currentEmployee)
const selectedYear = ref(new Date().getFullYear())

// Payroll payment dialog
const showPayrollPayment = ref(false)
const selectedEntry = ref<PayrollEntry | null>(null)

// Update salary dialog
const showSalaryDialog = ref(false)
const salaryLoading = ref(false)
const localSalaryDate = ref<Date | null>(new Date())

// Contractor payment dialog
const showContractorPayment = ref(false)
const contractorLoading = ref(false)
const contractorAmount = ref<number | null>(null)
const contractorDate = ref<Date | null>(new Date())
const contractorInvoice = ref('')
const contractorNotes = ref('')

const { defineField, handleSubmit, errors, resetForm } = useForm<UpdateSalaryFormData>({
  validationSchema: toTypedSchema(updateSalarySchema),
  initialValues: {
    newSalary: undefined as unknown as number,
    effectiveFrom: '',
    notes: ''
  }
})

const [newSalary] = defineField('newSalary')
const [effectiveFrom] = defineField('effectiveFrom')
const [salaryNotes] = defineField('notes')

onMounted(async () => {
  await loadAll()
})

async function loadAll() {
  try {
    await Promise.all([
      employeeStore.fetchEmployee(employeeId.value),
      bankAccountStore.fetchActiveBankAccounts()
    ])
    await loadPayroll()
    if (employee.value?.employeeType === EmployeeType.PAYROLL) {
      await employeeStore.fetchSalaryHistory(employeeId.value)
    }
    await employeeStore.fetchPaymentHistory(employeeId.value)
  } catch (error) {
    showError(error)
  }
}

async function loadPayroll() {
  try {
    await employeeStore.fetchPayroll(employeeId.value, selectedYear.value)
  } catch (error) {
    showError(error)
  }
}

async function generatePayroll() {
  try {
    await employeeStore.generatePayroll(employeeId.value, { year: selectedYear.value })
    showSuccess('treasury.payroll.generated')
    await loadPayroll()
  } catch (error) {
    showError(error)
  }
}

function openPayEntry(entry: PayrollEntry) {
  selectedEntry.value = entry
  showPayrollPayment.value = true
}

async function onEntryPaid() {
  await loadPayroll()
  await employeeStore.fetchPaymentHistory(employeeId.value)
}

function openSalaryDialog() {
  resetForm({
    values: {
      newSalary: employee.value?.baseSalary ?? (undefined as unknown as number),
      effectiveFrom: '',
      notes: ''
    }
  })
  localSalaryDate.value = new Date()
  showSalaryDialog.value = true
}

const submitSalaryUpdate = handleSubmit(async formValues => {
  salaryLoading.value = true
  try {
    await employeeStore.updateSalary(employeeId.value, {
      newSalary: formValues.newSalary,
      effectiveFrom: formValues.effectiveFrom,
      notes: formValues.notes || null
    })
    showSuccess('treasury.employee.salaryUpdated')
    showSalaryDialog.value = false
    await employeeStore.fetchSalaryHistory(employeeId.value)
    await loadPayroll()
  } catch (error) {
    showError(error)
  } finally {
    salaryLoading.value = false
  }
})

async function submitContractorPayment() {
  if (!contractorAmount.value || !contractorDate.value || !contractorInvoice.value) return
  contractorLoading.value = true
  try {
    await employeeStore.recordContractorPayment(employeeId.value, {
      amount: contractorAmount.value,
      paymentDate: toApiDate(contractorDate.value),
      invoiceNumber: contractorInvoice.value,
      notes: contractorNotes.value || null
    })
    showSuccess('treasury.employee.contractorPaymentRecorded')
    showContractorPayment.value = false
    contractorAmount.value = null
    contractorInvoice.value = ''
    contractorNotes.value = ''
    await employeeStore.fetchPaymentHistory(employeeId.value)
  } catch (error) {
    showError(error)
  } finally {
    contractorLoading.value = false
  }
}

function payrollStatusSeverity(status: PayrollStatus): string {
  switch (status) {
    case PayrollStatus.PAID:
      return 'success'
    case PayrollStatus.CANCELLED:
      return 'secondary'
    default:
      return 'warn'
  }
}

function salaryHistoryLabel(history: SalaryHistory): string {
  const from = history.effectiveFrom
  const to = history.effectiveTo ?? t('common.present')
  return `${from} → ${to}`
}

function goBack() {
  router.push({ name: 'treasury-employees' })
}
</script>

<template>
  <div class="employee-payroll-view">
    <div class="page-header">
      <div class="header-left">
        <Button icon="pi pi-arrow-left" severity="secondary" text @click="goBack" />
        <h1 class="page-title">{{ employee?.fullName || '...' }}</h1>
      </div>
    </div>

    <!-- Employee Info Card -->
    <Card class="employee-header-card" v-if="employee">
      <template #content>
        <div class="employee-info-grid">
          <div class="info-item">
            <span class="info-label">{{ t('treasury.employee.employeeType') }}</span>
            <Tag
              :value="t(`treasury.employee.types.${employee.employeeType}`)"
              :severity="
                employee.employeeType === EmployeeType.PAYROLL
                  ? 'info'
                  : employee.employeeType === EmployeeType.CONTRACTOR
                    ? 'warn'
                    : 'success'
              "
            />
          </div>
          <div class="info-item" v-if="employee.position">
            <span class="info-label">{{ t('treasury.employee.position') }}</span>
            <span class="info-value">{{ employee.position }}</span>
          </div>
          <div class="info-item" v-if="employee.baseSalary != null">
            <span class="info-label">{{ t('treasury.employee.baseSalary') }}</span>
            <span class="info-value">{{ formatCurrency(employee.baseSalary) }}</span>
          </div>
          <div class="info-item" v-if="employee.hireDate">
            <span class="info-label">{{ t('treasury.employee.hireDate') }}</span>
            <span class="info-value">{{ employee.hireDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('treasury.employee.active') }}</span>
            <Tag
              :value="employee.active ? t('common.yes') : t('common.no')"
              :severity="employee.active ? 'success' : 'secondary'"
            />
          </div>
          <div class="info-item" v-if="employee.indemnizacionLiability != null">
            <span class="info-label">{{ t('treasury.employee.indemnizacion.title') }}</span>
            <span class="info-value indemnizacion">
              {{ formatCurrency(employee.indemnizacionLiability) }}
            </span>
          </div>
        </div>
      </template>
    </Card>

    <!-- PAYROLL: Salary History Panel -->
    <Card v-if="employee?.employeeType === EmployeeType.PAYROLL" class="section-card">
      <template #header>
        <div class="section-header">
          <h2>{{ t('treasury.employee.salaryHistoryTitle') }}</h2>
          <Button
            v-if="canConfigure"
            icon="pi pi-pencil"
            :label="t('treasury.employee.updateSalary')"
            severity="secondary"
            outlined
            size="small"
            @click="openSalaryDialog"
          />
        </div>
      </template>
      <template #content>
        <DataTable
          :value="employeeStore.salaryHistory"
          :loading="employeeStore.loading"
          data-key="id"
          striped-rows
          size="small"
        >
          <template #empty>
            <div class="text-center p-3">{{ t('treasury.employee.noSalaryHistory') }}</div>
          </template>
          <Column :header="t('treasury.employee.baseSalary')">
            <template #body="{ data }">{{ formatCurrency(data.baseSalary) }}</template>
          </Column>
          <Column :header="t('treasury.employee.effectivePeriod')">
            <template #body="{ data }">{{ salaryHistoryLabel(data) }}</template>
          </Column>
          <Column field="notes" :header="t('treasury.income.notes')">
            <template #body="{ data }">{{ data.notes || '—' }}</template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- PAYROLL: Payroll Schedule Panel -->
    <Card v-if="employee?.employeeType === EmployeeType.PAYROLL" class="section-card">
      <template #header>
        <div class="section-header">
          <h2>{{ t('treasury.payroll.title') }}</h2>
          <div class="payroll-controls">
            <InputNumber
              v-model="selectedYear"
              :min="2020"
              :max="2100"
              :show-buttons="true"
              :step="1"
              style="width: 120px"
              @input="loadPayroll"
            />
            <Button
              v-if="canConfigure"
              icon="pi pi-calendar-plus"
              :label="t('treasury.payroll.generateFor', { year: selectedYear })"
              severity="secondary"
              outlined
              size="small"
              @click="generatePayroll"
            />
          </div>
        </div>
      </template>
      <template #content>
        <DataTable
          :value="employeeStore.payrollEntries"
          :loading="employeeStore.loading"
          data-key="id"
          striped-rows
          size="small"
        >
          <template #empty>
            <div class="text-center p-3">{{ t('treasury.payroll.empty') }}</div>
          </template>
          <Column field="periodLabel" :header="t('treasury.payroll.period')" />
          <Column :header="t('treasury.payroll.grossAmount')" style="width: 120px">
            <template #body="{ data }">{{ formatCurrency(data.grossAmount) }}</template>
          </Column>
          <Column field="dueDate" :header="t('treasury.payroll.dueDate')" style="width: 110px" />
          <Column :header="t('treasury.payroll.status')" style="width: 110px">
            <template #body="{ data }">
              <Tag
                :value="t(`treasury.payroll.statuses.${data.status}`)"
                :severity="payrollStatusSeverity(data.status)"
              />
            </template>
          </Column>
          <Column field="paidDate" :header="t('treasury.payroll.paidDate')" style="width: 110px">
            <template #body="{ data }">{{ data.paidDate || '—' }}</template>
          </Column>
          <Column :header="t('common.actions')" style="width: 80px">
            <template #body="{ data }">
              <Button
                v-if="canWrite && data.status === PayrollStatus.PENDING"
                icon="pi pi-dollar"
                severity="success"
                text
                rounded
                size="small"
                v-tooltip.top="t('treasury.payroll.pay')"
                @click="openPayEntry(data)"
              />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- CONTRACTOR: Contractor Payments Panel -->
    <Card v-if="employee?.employeeType === EmployeeType.CONTRACTOR" class="section-card">
      <template #header>
        <div class="section-header">
          <h2>{{ t('treasury.employee.contractorPayments') }}</h2>
          <Button
            v-if="canWrite"
            icon="pi pi-plus"
            :label="t('treasury.employee.recordPayment')"
            severity="secondary"
            outlined
            size="small"
            @click="showContractorPayment = true"
          />
        </div>
      </template>
      <template #content>
        <DataTable
          :value="
            employeeStore.paymentHistory.filter(
              p => p.type === EmployeePaymentType.CONTRACTOR_PAYMENT
            )
          "
          :loading="employeeStore.loading"
          data-key="relatedEntityId"
          striped-rows
          size="small"
        >
          <template #empty>
            <div class="text-center p-3">{{ t('treasury.employee.noPayments') }}</div>
          </template>
          <Column field="date" :header="t('treasury.payment.paymentDate')" style="width: 110px" />
          <Column :header="t('treasury.payment.amount')" style="width: 120px">
            <template #body="{ data }">{{ formatCurrency(data.amount) }}</template>
          </Column>
          <Column field="reference" :header="t('treasury.expense.invoiceNumber')">
            <template #body="{ data }">{{ data.reference || '—' }}</template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Payment History Panel -->
    <Card class="section-card">
      <template #header>
        <div class="section-header">
          <h2>{{ t('treasury.employee.paymentHistoryTitle') }}</h2>
        </div>
      </template>
      <template #content>
        <DataTable
          :value="employeeStore.paymentHistory"
          :loading="employeeStore.loading"
          data-key="relatedEntityId"
          striped-rows
          size="small"
        >
          <template #empty>
            <div class="text-center p-3">{{ t('treasury.employee.noPayments') }}</div>
          </template>
          <Column field="date" :header="t('common.date')" style="width: 110px" />
          <Column :header="t('common.type')" style="width: 150px">
            <template #body="{ data }">
              {{ t(`treasury.employee.paymentTypes.${data.type}`) }}
            </template>
          </Column>
          <Column :header="t('treasury.payment.amount')" style="width: 120px">
            <template #body="{ data }">{{ formatCurrency(data.amount) }}</template>
          </Column>
          <Column field="reference" :header="t('common.reference')">
            <template #body="{ data }">{{ data.reference || '—' }}</template>
          </Column>
          <Column field="status" :header="t('treasury.expense.status')" style="width: 100px" />
        </DataTable>
      </template>
    </Card>

    <!-- Payroll Payment Dialog -->
    <PayrollPaymentDialog
      v-if="selectedEntry"
      v-model:visible="showPayrollPayment"
      :entry="selectedEntry"
      @paid="onEntryPaid"
    />

    <!-- Update Salary Dialog -->
    <Dialog
      v-model:visible="showSalaryDialog"
      :header="t('treasury.employee.updateSalary')"
      modal
      :style="{ width: '420px' }"
    >
      <form @submit.prevent="submitSalaryUpdate" class="salary-form">
        <div class="form-field">
          <label>{{ t('treasury.employee.newSalary') }} *</label>
          <InputNumber
            v-model="newSalary"
            :min="0.01"
            :max-fraction-digits="2"
            :class="{ 'p-invalid': errors.newSalary }"
          />
          <Message v-if="errors.newSalary" severity="error" :closable="false">
            {{ errors.newSalary }}
          </Message>
        </div>
        <div class="form-field">
          <label>{{ t('treasury.employee.effectiveFrom') }} *</label>
          <DatePicker
            v-model="localSalaryDate"
            :class="{ 'p-invalid': errors.effectiveFrom }"
            @update:model-value="val => (effectiveFrom = val ? toApiDate(val as Date) : '')"
          />
          <Message v-if="errors.effectiveFrom" severity="error" :closable="false">
            {{ errors.effectiveFrom }}
          </Message>
        </div>
        <div class="form-field">
          <label>{{ t('treasury.income.notes') }}</label>
          <Textarea v-model="salaryNotes" rows="2" />
        </div>
        <div class="form-actions">
          <Button
            type="button"
            :label="t('common.cancel')"
            severity="secondary"
            outlined
            @click="showSalaryDialog = false"
          />
          <Button type="submit" :label="t('common.save')" :loading="salaryLoading" />
        </div>
      </form>
    </Dialog>

    <!-- Contractor Payment Dialog -->
    <Dialog
      v-model:visible="showContractorPayment"
      :header="t('treasury.employee.recordPayment')"
      modal
      :style="{ width: '460px' }"
    >
      <div class="contractor-form">
        <div class="form-field">
          <label>{{ t('treasury.payment.amount') }} *</label>
          <InputNumber v-model="contractorAmount" :min="0.01" :max-fraction-digits="2" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.expense.invoiceNumber') }} *</label>
          <InputText v-model="contractorInvoice" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.expense.expenseDate') }} *</label>
          <DatePicker v-model="contractorDate" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.income.notes') }}</label>
          <Textarea v-model="contractorNotes" rows="2" />
        </div>
        <div class="form-actions">
          <Button
            :label="t('common.cancel')"
            severity="secondary"
            outlined
            @click="showContractorPayment = false"
          />
          <Button
            :label="t('common.save')"
            :loading="contractorLoading"
            :disabled="!contractorAmount || !contractorInvoice || !contractorDate"
            @click="submitContractorPayment"
          />
        </div>
      </div>
    </Dialog>
  </div>
</template>

<style scoped>
.employee-payroll-view {
  max-width: 1200px;
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

.employee-header-card {
  margin-bottom: 1.5rem;
}

.employee-info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-label {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-weight: 500;
}

.info-value.indemnizacion {
  color: var(--p-orange-500);
  font-size: 1.1rem;
}

.section-card {
  margin-bottom: 1.5rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem 0;
}

.section-header h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
}

.payroll-controls {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.salary-form,
.contractor-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
