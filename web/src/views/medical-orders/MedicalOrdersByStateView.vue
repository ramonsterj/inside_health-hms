<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import MultiSelect from 'primevue/multiselect'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useAuthStore } from '@/stores/auth'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import {
  MedicalOrderStatus,
  MedicalOrderCategory,
  RESULTS_BEARING_CATEGORIES,
  canDiscontinueStatus,
  canUploadResultDocument
} from '@/types/medicalRecord'
import type { MedicalOrderListItemResponse } from '@/types/medicalRecord'
import { formatDateTime, getFullName } from '@/utils/format'
import MedicalOrderStateBadge from '@/components/medical-record/MedicalOrderStateBadge.vue'
import MedicalOrderDocumentUploadDialog from '@/components/medical-record/MedicalOrderDocumentUploadDialog.vue'
import MedicalOrderRejectDialog from '@/components/medical-record/MedicalOrderRejectDialog.vue'
import MedicalOrderEmergencyAuthorizeDialog from '@/components/medical-record/MedicalOrderEmergencyAuthorizeDialog.vue'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const authStore = useAuthStore()
const orderStore = useMedicalOrderStore()
const { showError, showSuccess } = useErrorHandler()

const PAGE_SIZE = 20

const rows = ref(PAGE_SIZE)
const first = ref(0)
const totalRecords = ref(0)
const items = ref<MedicalOrderListItemResponse[]>([])
const loading = ref(false)

// Default to the action-needed buckets: pending authorization + outstanding labs.
const statusFilter = ref<MedicalOrderStatus[]>([
  MedicalOrderStatus.SOLICITADO,
  MedicalOrderStatus.AUTORIZADO,
  MedicalOrderStatus.EN_PROCESO
])
const categoryFilter = ref<MedicalOrderCategory[]>([])

const showUploadDialog = ref(false)
const showRejectDialog = ref(false)
const showEmergencyDialog = ref(false)
const activeOrder = ref<MedicalOrderListItemResponse | null>(null)

const canAuthorize = computed(() => authStore.hasPermission('medical-order:authorize'))
const canEmergencyAuthorize = computed(() =>
  authStore.hasPermission('medical-order:emergency-authorize')
)
const canMarkInProgress = computed(() =>
  authStore.hasPermission('medical-order:mark-in-progress')
)
const canDiscontinue = computed(() => authStore.hasPermission('medical-order:discontinue'))
const canUploadDocument = computed(() => authStore.hasPermission('medical-order:upload-document'))

const statusOptions = computed(() =>
  Object.values(MedicalOrderStatus).map((status) => ({
    label: t(`medicalRecord.medicalOrder.statuses.${status}`),
    value: status
  }))
)

const categoryOptions = computed(() =>
  Object.values(MedicalOrderCategory).map((category) => ({
    label: t(`medicalRecord.medicalOrder.categories.${category}`),
    value: category
  }))
)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const result = await orderStore.fetchOrdersByStatus({
      status: statusFilter.value.length > 0 ? statusFilter.value : undefined,
      category: categoryFilter.value.length > 0 ? categoryFilter.value : undefined,
      page: Math.floor(first.value / rows.value),
      size: rows.value
    })
    items.value = result.content
    totalRecords.value = result.page.totalElements
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  first.value = 0
  void load()
}

function clearFilters() {
  statusFilter.value = []
  categoryFilter.value = []
  applyFilters()
}

function onPageChange(event: { first: number; rows: number }) {
  first.value = event.first
  rows.value = event.rows
  void load()
}

function isAuthorizableRow(row: MedicalOrderListItemResponse): boolean {
  return canAuthorize.value && row.status === MedicalOrderStatus.SOLICITADO
}

function isEmergencyAuthorizeRow(row: MedicalOrderListItemResponse): boolean {
  return canEmergencyAuthorize.value && row.status === MedicalOrderStatus.SOLICITADO
}

function isMarkInProgressRow(row: MedicalOrderListItemResponse): boolean {
  return (
    canMarkInProgress.value &&
    row.status === MedicalOrderStatus.AUTORIZADO &&
    RESULTS_BEARING_CATEGORIES.includes(row.category)
  )
}

function isDiscontinueRow(row: MedicalOrderListItemResponse): boolean {
  return canDiscontinue.value && canDiscontinueStatus(row.status)
}

function isUploadResultRow(row: MedicalOrderListItemResponse): boolean {
  return canUploadDocument.value && canUploadResultDocument(row.category, row.status)
}

function openAdmission(row: MedicalOrderListItemResponse) {
  router.push({ name: 'admission-detail', params: { id: row.admissionId } })
}

function confirmAuthorize(row: MedicalOrderListItemResponse) {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmAuthorize'),
    header: t('common.confirm'),
    icon: 'pi pi-check-circle',
    acceptClass: 'p-button-success',
    accept: () => doAuthorize(row)
  })
}

async function doAuthorize(row: MedicalOrderListItemResponse) {
  try {
    await orderStore.authorizeMedicalOrder(row.admissionId, row.id)
    showSuccess('medicalRecord.medicalOrder.transitions.authorized')
    await load()
  } catch (error) {
    showError(error)
  }
}

function confirmMarkInProgress(row: MedicalOrderListItemResponse) {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmMarkInProgress'),
    header: t('common.confirm'),
    icon: 'pi pi-clock',
    accept: () => doMarkInProgress(row)
  })
}

async function doMarkInProgress(row: MedicalOrderListItemResponse) {
  try {
    await orderStore.markInProgress(row.admissionId, row.id)
    showSuccess('medicalRecord.medicalOrder.transitions.markedInProgress')
    await load()
  } catch (error) {
    showError(error)
  }
}

function openEmergencyDialog(row: MedicalOrderListItemResponse) {
  activeOrder.value = row
  showEmergencyDialog.value = true
}

async function onEmergencyAuthorized() {
  showEmergencyDialog.value = false
  await load()
}

function confirmDiscontinue(row: MedicalOrderListItemResponse) {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmDiscontinue'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-warning',
    accept: () => doDiscontinue(row)
  })
}

async function doDiscontinue(row: MedicalOrderListItemResponse) {
  try {
    await orderStore.discontinueMedicalOrder(row.admissionId, row.id)
    showSuccess('medicalRecord.medicalOrder.discontinued')
    await load()
  } catch (error) {
    showError(error)
  }
}

function openRejectDialog(row: MedicalOrderListItemResponse) {
  activeOrder.value = row
  showRejectDialog.value = true
}

async function onRejected() {
  await load()
}

function openUploadDialog(row: MedicalOrderListItemResponse) {
  activeOrder.value = row
  showUploadDialog.value = true
}

async function onDocumentUploaded() {
  showUploadDialog.value = false
  await load()
}
</script>

<template>
  <div class="orders-by-state">
    <header class="page-header">
      <h1 class="page-title">{{ t('medicalRecord.medicalOrder.byState.title') }}</h1>
      <p class="page-subtitle">{{ t('medicalRecord.medicalOrder.byState.subtitle') }}</p>
    </header>

    <Card>
      <template #content>
        <div class="filters-row">
          <MultiSelect
            v-model="statusFilter"
            :options="statusOptions"
            option-label="label"
            option-value="value"
            display="chip"
            :placeholder="t('medicalRecord.medicalOrder.byState.filters.anyStatus')"
            class="filter-status"
            :showClear="true"
            @change="applyFilters"
          />
          <MultiSelect
            v-model="categoryFilter"
            :options="categoryOptions"
            option-label="label"
            option-value="value"
            display="chip"
            :placeholder="t('medicalRecord.medicalOrder.byState.filters.anyCategory')"
            class="filter-category"
            :showClear="true"
            @change="applyFilters"
          />
          <Button
            text
            severity="secondary"
            :label="t('medicalRecord.medicalOrder.byState.filters.clear')"
            icon="pi pi-filter-slash"
            @click="clearFilters"
          />
        </div>
      </template>
    </Card>

    <Card class="results-card">
      <template #content>
        <DataTable
          :value="items"
          :loading="loading"
          :paginator="true"
          v-model:rows="rows"
          v-model:first="first"
          :totalRecords="totalRecords"
          :lazy="true"
          @page="onPageChange"
          :rowsPerPageOptions="[10, 20, 50]"
          dataKey="id"
          stripedRows
          scrollable
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('medicalRecord.medicalOrder.byState.empty') }}
            </div>
          </template>

          <Column :header="t('medicalRecord.medicalOrder.byState.columns.patient')">
            <template #body="{ data }">
              {{ getFullName(data.patientFirstName, data.patientLastName) }}
            </template>
          </Column>

          <Column :header="t('medicalRecord.medicalOrder.byState.columns.category')">
            <template #body="{ data }">
              {{ t(`medicalRecord.medicalOrder.categories.${data.category}`) }}
            </template>
          </Column>

          <Column :header="t('medicalRecord.medicalOrder.byState.columns.summary')">
            <template #body="{ data }">
              <div class="summary-cell">
                <div v-if="data.medication" class="summary-medication">
                  {{ data.medication }}
                  <span v-if="data.dosage" class="summary-dosage">— {{ data.dosage }}</span>
                </div>
                <div v-else-if="data.summary" class="summary-text">{{ data.summary }}</div>
                <div v-else class="summary-empty">—</div>
              </div>
            </template>
          </Column>

          <Column :header="t('medicalRecord.medicalOrder.byState.columns.status')" style="width: 180px">
            <template #body="{ data }">
              <MedicalOrderStateBadge :status="data.status" />
            </template>
          </Column>

          <Column :header="t('medicalRecord.medicalOrder.byState.columns.requestedBy')" style="width: 160px">
            <template #body="{ data }">
              <span v-if="data.createdBy">
                {{ getFullName(data.createdBy.firstName, data.createdBy.lastName) }}
              </span>
              <span v-else>—</span>
            </template>
          </Column>

          <Column :header="t('medicalRecord.medicalOrder.byState.columns.requestedAt')" style="width: 160px">
            <template #body="{ data }">
              {{ formatDateTime(data.createdAt) }}
            </template>
          </Column>

          <Column
            :header="t('medicalRecord.medicalOrder.byState.columns.documents')"
            style="width: 80px"
          >
            <template #body="{ data }">
              <span class="doc-count">
                <i class="pi pi-paperclip"></i>
                {{ data.documentCount }}
              </span>
            </template>
          </Column>

          <Column :header="t('medicalRecord.medicalOrder.byState.columns.actions')" style="width: 240px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  v-if="isAuthorizableRow(data)"
                  icon="pi pi-check"
                  severity="success"
                  text
                  rounded
                  v-tooltip.top="t('medicalRecord.medicalOrder.actions.authorize')"
                  @click="confirmAuthorize(data)"
                />
                <Button
                  v-if="isAuthorizableRow(data)"
                  icon="pi pi-times"
                  severity="danger"
                  text
                  rounded
                  v-tooltip.top="t('medicalRecord.medicalOrder.actions.reject')"
                  @click="openRejectDialog(data)"
                />
                <Button
                  v-if="isEmergencyAuthorizeRow(data)"
                  icon="pi pi-bolt"
                  severity="warn"
                  text
                  rounded
                  v-tooltip.top="t('medicalRecord.medicalOrder.actions.emergencyAuthorize')"
                  @click="openEmergencyDialog(data)"
                />
                <Button
                  v-if="isMarkInProgressRow(data)"
                  icon="pi pi-play"
                  severity="warn"
                  text
                  rounded
                  v-tooltip.top="t('medicalRecord.medicalOrder.actions.markInProgress')"
                  @click="confirmMarkInProgress(data)"
                />
                <Button
                  v-if="isUploadResultRow(data)"
                  icon="pi pi-upload"
                  severity="info"
                  text
                  rounded
                  v-tooltip.top="t('medicalRecord.medicalOrder.actions.uploadResults')"
                  @click="openUploadDialog(data)"
                />
                <Button
                  v-if="isDiscontinueRow(data)"
                  icon="pi pi-ban"
                  severity="warn"
                  text
                  rounded
                  v-tooltip.top="t('medicalRecord.medicalOrder.discontinue')"
                  @click="confirmDiscontinue(data)"
                />
                <Button
                  icon="pi pi-external-link"
                  severity="secondary"
                  text
                  rounded
                  v-tooltip.top="t('medicalRecord.medicalOrder.byState.openAdmission')"
                  @click="openAdmission(data)"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Reject Dialog -->
    <MedicalOrderRejectDialog
      v-if="activeOrder"
      v-model:visible="showRejectDialog"
      :admissionId="activeOrder.admissionId"
      :orderId="activeOrder.id"
      @rejected="onRejected"
    />

    <!-- Emergency Authorize Dialog -->
    <MedicalOrderEmergencyAuthorizeDialog
      v-if="activeOrder"
      v-model:visible="showEmergencyDialog"
      :admissionId="activeOrder.admissionId"
      :orderId="activeOrder.id"
      @authorized="onEmergencyAuthorized"
    />

    <!-- Document Upload Dialog (auto-transitions to RESULTADOS_RECIBIDOS when uploading) -->
    <MedicalOrderDocumentUploadDialog
      v-if="activeOrder"
      v-model:visible="showUploadDialog"
      :admissionId="activeOrder.admissionId"
      :orderId="activeOrder.id"
      @uploaded="onDocumentUploaded"
    />
  </div>
</template>

<style scoped>
.orders-by-state {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
}

.page-subtitle {
  margin: 0;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.filters-row {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.filter-status,
.filter-category {
  min-width: 18rem;
}

.summary-cell {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  font-size: 0.875rem;
}

.summary-medication {
  font-weight: 600;
}

.summary-dosage {
  font-weight: 400;
  color: var(--p-text-muted-color);
}

.summary-text {
  color: var(--p-text-color);
}

.summary-empty {
  color: var(--p-text-muted-color);
}

.doc-count {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}
</style>
