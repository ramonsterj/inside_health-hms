<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import ConfirmDialog from 'primevue/confirmdialog'
import DoctorFeeForm from '@/components/treasury/DoctorFeeForm.vue'
import DoctorFeeInvoiceDialog from '@/components/treasury/DoctorFeeInvoiceDialog.vue'
import DoctorFeeSettleDialog from '@/components/treasury/DoctorFeeSettleDialog.vue'
import { useDoctorFeeStore } from '@/stores/doctorFee'
import { useTreasuryEmployeeStore } from '@/stores/treasuryEmployee'
import { useAuthStore } from '@/stores/auth'
import { DoctorFeeStatus } from '@/types/treasury'
import type { DoctorFee } from '@/types/treasury'
import { formatCurrency } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const confirm = useConfirm()
const doctorFeeStore = useDoctorFeeStore()
const employeeStore = useTreasuryEmployeeStore()
const authStore = useAuthStore()

const canWrite = computed(() => authStore.hasPermission('treasury:write'))
const canDelete = computed(() => authStore.hasPermission('treasury:delete'))

const employeeId = computed(() => Number(route.params.id))
const employee = computed(() => employeeStore.currentEmployee)

const filterStatus = ref<string | null>(null)
const showForm = ref(false)
const showInvoiceDialog = ref(false)
const showSettleDialog = ref(false)
const selectedFee = ref<DoctorFee | null>(null)

// Upload document ref
const fileInput = ref<HTMLInputElement | null>(null)
const uploadingFeeId = ref<number | null>(null)

const statusOptions = computed(() => [
  { label: t('treasury.doctorFee.filters.allStatuses'), value: null },
  ...Object.values(DoctorFeeStatus).map(v => ({
    label: t(`treasury.doctorFee.statuses.${v}`),
    value: v
  }))
])

onMounted(async () => {
  try {
    await employeeStore.fetchEmployee(employeeId.value)
    await loadFees()
    await doctorFeeStore.fetchSummary(employeeId.value)
  } catch (error) {
    showError(error)
  }
})

async function loadFees() {
  try {
    await doctorFeeStore.fetchDoctorFees(employeeId.value, {
      status: filterStatus.value ?? undefined
    })
  } catch (error) {
    showError(error)
  }
}

async function refreshAll() {
  await loadFees()
  await doctorFeeStore.fetchSummary(employeeId.value)
}

function onSaved() {
  refreshAll()
}

function openInvoice(fee: DoctorFee) {
  selectedFee.value = fee
  showInvoiceDialog.value = true
}

function openSettle(fee: DoctorFee) {
  selectedFee.value = fee
  showSettleDialog.value = true
}

function triggerUpload(fee: DoctorFee) {
  uploadingFeeId.value = fee.id
  fileInput.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !uploadingFeeId.value) return
  try {
    await doctorFeeStore.uploadInvoiceDocument(
      employeeId.value,
      uploadingFeeId.value,
      file
    )
    showSuccess('treasury.doctorFee.documentUploaded')
    await loadFees()
  } catch (error) {
    showError(error)
  } finally {
    uploadingFeeId.value = null
    input.value = ''
  }
}

function confirmDelete(fee: DoctorFee) {
  confirm.require({
    message: t('treasury.doctorFee.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteFee(fee)
  })
}

async function deleteFee(fee: DoctorFee) {
  try {
    await doctorFeeStore.deleteDoctorFee(employeeId.value, fee.id)
    showSuccess('treasury.doctorFee.deleted')
    await refreshAll()
  } catch (error) {
    showError(error)
  }
}

function statusSeverity(status: DoctorFeeStatus): string {
  switch (status) {
    case DoctorFeeStatus.PENDING:
      return 'warn'
    case DoctorFeeStatus.INVOICED:
      return 'info'
    case DoctorFeeStatus.PAID:
      return 'success'
    default:
      return 'secondary'
  }
}

function goBack() {
  router.push({ name: 'treasury-employees' })
}
</script>

<template>
  <div class="doctor-fee-list">
    <ConfirmDialog />
    <input
      ref="fileInput"
      type="file"
      accept=".pdf,.jpg,.jpeg,.png"
      style="display: none"
      @change="onFileSelected"
    />

    <div class="page-header">
      <div class="header-left">
        <Button icon="pi pi-arrow-left" severity="secondary" text @click="goBack" />
        <h1 class="page-title">
          {{ t('treasury.doctorFee.title') }} — {{ employee?.fullName || '...' }}
        </h1>
      </div>
      <div class="header-actions">
        <Button
          v-if="canWrite"
          icon="pi pi-plus"
          :label="t('treasury.doctorFee.new')"
          @click="showForm = true"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          :loading="doctorFeeStore.loading"
          @click="refreshAll"
        />
      </div>
    </div>

    <!-- Summary Card -->
    <Card v-if="doctorFeeStore.summary" class="summary-card">
      <template #content>
        <div class="summary-grid">
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.doctorFee.totalFees') }}</span>
            <span class="summary-value">{{ doctorFeeStore.summary.totalFees }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.doctorFee.totalGross') }}</span>
            <span class="summary-value">{{ formatCurrency(doctorFeeStore.summary.totalGross) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.doctorFee.totalCommission') }}</span>
            <span class="summary-value commission">
              {{ formatCurrency(doctorFeeStore.summary.totalCommission) }}
            </span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.doctorFee.totalNet') }}</span>
            <span class="summary-value net">
              {{ formatCurrency(doctorFeeStore.summary.totalNet) }}
            </span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.doctorFee.amountPaid') }}</span>
            <span class="summary-value">{{ formatCurrency(doctorFeeStore.summary.amountPaid) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('treasury.doctorFee.outstandingBalance') }}</span>
            <span class="summary-value outstanding">
              {{ formatCurrency(doctorFeeStore.summary.outstandingBalance) }}
            </span>
          </div>
          <div class="summary-item">
            <Tag :value="`${t('treasury.doctorFee.statuses.PENDING')}: ${doctorFeeStore.summary.pendingCount}`" severity="warn" />
          </div>
          <div class="summary-item">
            <Tag :value="`${t('treasury.doctorFee.statuses.INVOICED')}: ${doctorFeeStore.summary.invoicedCount}`" severity="info" />
          </div>
          <div class="summary-item">
            <Tag :value="`${t('treasury.doctorFee.statuses.PAID')}: ${doctorFeeStore.summary.paidCount}`" severity="success" />
          </div>
        </div>
      </template>
    </Card>

    <!-- Filters -->
    <Card class="filters-card">
      <template #content>
        <div class="filters-grid">
          <div class="filter-field">
            <label>{{ t('treasury.doctorFee.status') }}</label>
            <Select
              v-model="filterStatus"
              :options="statusOptions"
              option-label="label"
              option-value="value"
              @change="loadFees"
            />
          </div>
          <div class="filter-actions">
            <Button :label="t('common.filter')" icon="pi pi-search" @click="loadFees" />
            <Button
              :label="t('common.clear')"
              icon="pi pi-times"
              severity="secondary"
              outlined
              @click="filterStatus = null; loadFees()"
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- Table -->
    <Card>
      <template #content>
        <DataTable
          :value="doctorFeeStore.doctorFees"
          :loading="doctorFeeStore.loading"
          data-key="id"
          striped-rows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('treasury.doctorFee.empty') }}</div>
          </template>

          <Column field="feeDate" :header="t('treasury.doctorFee.feeDate')" style="width: 110px" />

          <Column :header="t('treasury.doctorFee.billingType')" style="width: 140px">
            <template #body="{ data }">
              <Tag
                :value="t(`treasury.doctorFee.billingTypes.${data.billingType}`)"
                :severity="data.billingType === 'HOSPITAL_BILLED' ? 'info' : 'secondary'"
              />
            </template>
          </Column>

          <Column :header="t('treasury.doctorFee.grossAmount')" style="width: 120px">
            <template #body="{ data }">{{ formatCurrency(data.grossAmount) }}</template>
          </Column>

          <Column :header="t('treasury.doctorFee.commissionPct')" style="width: 100px">
            <template #body="{ data }">{{ data.commissionPct }}%</template>
          </Column>

          <Column :header="t('treasury.doctorFee.netAmount')" style="width: 120px">
            <template #body="{ data }">
              <span class="net-amount">{{ formatCurrency(data.netAmount) }}</span>
            </template>
          </Column>

          <Column :header="t('treasury.expense.status')" style="width: 110px">
            <template #body="{ data }">
              <Tag
                :value="t(`treasury.doctorFee.statuses.${data.status}`)"
                :severity="statusSeverity(data.status)"
              />
            </template>
          </Column>

          <Column field="doctorInvoiceNumber" :header="t('treasury.doctorFee.invoiceNumber')" style="width: 120px">
            <template #body="{ data }">{{ data.doctorInvoiceNumber || '—' }}</template>
          </Column>

          <Column :header="t('common.actions')" style="width: 160px">
            <template #body="{ data }">
              <div class="action-buttons">
                <!-- PENDING actions -->
                <Button
                  v-if="canWrite && data.status === DoctorFeeStatus.PENDING"
                  icon="pi pi-file-edit"
                  severity="info"
                  text
                  rounded
                  size="small"
                  v-tooltip.top="t('treasury.doctorFee.submitInvoice')"
                  @click="openInvoice(data)"
                />
                <Button
                  v-if="canDelete && data.status === DoctorFeeStatus.PENDING"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  size="small"
                  v-tooltip.top="t('common.delete')"
                  @click="confirmDelete(data)"
                />
                <!-- INVOICED actions -->
                <Button
                  v-if="canWrite && data.status === DoctorFeeStatus.INVOICED && !data.invoiceDocumentPath"
                  icon="pi pi-upload"
                  severity="secondary"
                  text
                  rounded
                  size="small"
                  v-tooltip.top="t('treasury.doctorFee.uploadDocument')"
                  @click="triggerUpload(data)"
                />
                <Button
                  v-if="canWrite && data.status === DoctorFeeStatus.INVOICED"
                  icon="pi pi-dollar"
                  severity="success"
                  text
                  rounded
                  size="small"
                  v-tooltip.top="t('treasury.doctorFee.settle')"
                  @click="openSettle(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Dialogs -->
    <DoctorFeeForm
      v-if="employee"
      v-model:visible="showForm"
      :employee="employee"
      @saved="onSaved"
    />

    <DoctorFeeInvoiceDialog
      v-if="selectedFee"
      v-model:visible="showInvoiceDialog"
      :fee="selectedFee"
      :employee-id="employeeId"
      @saved="onSaved"
    />

    <DoctorFeeSettleDialog
      v-if="selectedFee"
      v-model:visible="showSettleDialog"
      :fee="selectedFee"
      :employee-id="employeeId"
      @saved="onSaved"
    />
  </div>
</template>

<style scoped>
.doctor-fee-list {
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

.summary-card {
  margin-bottom: 1rem;
}

.summary-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  align-items: center;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.summary-label {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-value {
  font-weight: 600;
  font-size: 1rem;
}

.summary-value.commission {
  color: var(--p-orange-500);
}

.summary-value.net {
  color: var(--p-primary-color);
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
  min-width: 180px;
}

.filter-field label {
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

.net-amount {
  font-weight: 600;
  color: var(--p-primary-color);
}
</style>
