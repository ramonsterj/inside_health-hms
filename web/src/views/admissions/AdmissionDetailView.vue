<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useAdmissionStore } from '@/stores/admission'
import { useAuthStore } from '@/stores/auth'
import { AdmissionStatus } from '@/types/admission'
import type { ConsultingPhysician } from '@/types/admission'
import AddConsultingPhysicianDialog from '@/components/admissions/AddConsultingPhysicianDialog.vue'
import AdmissionTypeBadge from '@/components/admissions/AdmissionTypeBadge.vue'
import DocumentList from '@/components/documents/DocumentList.vue'
import DocumentUploadDialog from '@/components/documents/DocumentUploadDialog.vue'
import DocumentViewer from '@/components/documents/DocumentViewer.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const admissionStore = useAdmissionStore()
const authStore = useAuthStore()

const loading = ref(false)
const showAddConsultingPhysicianDialog = ref(false)
const showDocumentUploadDialog = ref(false)

const admissionId = computed(() => Number(route.params.id))
const admission = computed(() => admissionStore.currentAdmission)

const canUpdate = computed(() => authStore.hasPermission('admission:update'))
const canDelete = computed(() => authStore.hasPermission('admission:delete'))

const existingConsultingPhysicianIds = computed(
  () => admission.value?.consultingPhysicians.map(cp => cp.physician.id) || []
)

onMounted(async () => {
  await Promise.all([loadAdmission(), admissionStore.fetchDoctors()])
})

async function loadAdmission() {
  loading.value = true
  try {
    await admissionStore.fetchAdmission(admissionId.value)
  } catch (error) {
    showError(error)
    router.push({ name: 'admissions' })
  } finally {
    loading.value = false
  }
}

function editAdmission() {
  router.push({ name: 'admission-edit', params: { id: admissionId.value } })
}

function confirmDischarge() {
  confirm.require({
    message: t('admission.confirmDischarge'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    accept: () => {
      dischargePatient()
    }
  })
}

async function dischargePatient() {
  confirm.close()
  loading.value = true
  try {
    await admissionStore.dischargePatient(admissionId.value)
    showSuccess('admission.discharged')
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function confirmDelete() {
  confirm.require({
    message: t('admission.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteAdmission()
  })
}

async function deleteAdmission() {
  loading.value = true
  try {
    await admissionStore.deleteAdmission(admissionId.value)
    showSuccess('admission.deleted')
    router.push({ name: 'admissions' })
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function getFullName(firstName: string | null, lastName: string | null): string {
  return `${firstName || ''} ${lastName || ''}`.trim()
}

function formatDateTime(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

function getStatusSeverity(status: AdmissionStatus): 'success' | 'secondary' {
  return status === AdmissionStatus.ACTIVE ? 'success' : 'secondary'
}

function getContrastColor(hexColor: string): string {
  const r = parseInt(hexColor.slice(1, 3), 16)
  const g = parseInt(hexColor.slice(3, 5), 16)
  const b = parseInt(hexColor.slice(5, 7), 16)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return luminance > 0.5 ? '#000000' : '#FFFFFF'
}

function formatDate(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString()
}

function formatDoctorName(doctor: {
  salutation: string | null
  firstName: string | null
  lastName: string | null
}): string {
  const salutationLabel = doctor.salutation ? t(`user.salutations.${doctor.salutation}`) : ''
  return `${salutationLabel} ${getFullName(doctor.firstName, doctor.lastName)}`.trim()
}

async function handleConsultingPhysicianAdded() {
  showAddConsultingPhysicianDialog.value = false
  showSuccess('admission.consultingPhysicians.added')
  await admissionStore.fetchAdmission(admissionId.value)
}

function confirmRemoveConsultingPhysician(cp: ConsultingPhysician) {
  confirm.require({
    message: t('admission.consultingPhysicians.confirmRemove'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => removeConsultingPhysician(cp.id)
  })
}

async function removeConsultingPhysician(consultingPhysicianId: number) {
  loading.value = true
  try {
    await admissionStore.removeConsultingPhysician(admissionId.value, consultingPhysicianId)
    showSuccess('admission.consultingPhysicians.removed')
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function handleDocumentUploaded() {
  showDocumentUploadDialog.value = false
}
</script>

<template>
  <div class="admission-detail-page">
    <ConfirmDialog />

    <div class="page-header">
      <div class="header-left">
        <Button icon="pi pi-arrow-left" text rounded @click="router.push({ name: 'admissions' })" />
        <h1 class="page-title">{{ t('admission.details') }}</h1>
      </div>
      <div class="header-actions" v-if="admission">
        <Button
          v-if="canUpdate && admission.status === AdmissionStatus.ACTIVE"
          icon="pi pi-pencil"
          :label="t('common.edit')"
          severity="secondary"
          @click="editAdmission"
        />
        <Button
          v-if="canUpdate && admission.status === AdmissionStatus.ACTIVE"
          icon="pi pi-sign-out"
          :label="t('admission.discharge')"
          severity="warning"
          @click="confirmDischarge"
        />
        <Button
          v-if="canDelete"
          icon="pi pi-trash"
          :label="t('common.delete')"
          severity="danger"
          outlined
          @click="confirmDelete"
        />
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
    </div>

    <div v-else-if="admission" class="detail-grid">
      <Card>
        <template #title>{{ t('admission.patientInfo') }}</template>
        <template #content>
          <div class="info-row">
            <span class="info-label">{{ t('admission.patient') }}</span>
            <span class="info-value">
              {{ getFullName(admission.patient.firstName, admission.patient.lastName) }}
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('patient.idDocumentNumber') }}</span>
            <span class="info-value">{{ admission.patient.idDocumentNumber || '-' }}</span>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>{{ t('admission.admissionInfo') }}</template>
        <template #content>
          <div class="info-row">
            <span class="info-label">{{ t('admission.status') }}</span>
            <Tag
              :value="t(`admission.statuses.${admission.status}`)"
              :severity="getStatusSeverity(admission.status)"
            />
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('admission.type') }}</span>
            <AdmissionTypeBadge :type="admission.type" />
          </div>
          <div v-if="admission.triageCode" class="info-row">
            <span class="info-label">{{ t('admission.triageCode') }}</span>
            <span
              class="triage-badge"
              :style="{
                backgroundColor: admission.triageCode.color,
                color: getContrastColor(admission.triageCode.color)
              }"
            >
              {{ admission.triageCode.code }}
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('admission.room') }}</span>
            <span class="info-value">{{ admission.room?.number || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('admission.treatingPhysician') }}</span>
            <span class="info-value">{{ formatDoctorName(admission.treatingPhysician) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('admission.admissionDate') }}</span>
            <span class="info-value">{{ formatDateTime(admission.admissionDate) }}</span>
          </div>
          <div v-if="admission.dischargeDate" class="info-row">
            <span class="info-label">{{ t('admission.dischargeDate') }}</span>
            <span class="info-value">{{ formatDateTime(admission.dischargeDate) }}</span>
          </div>
        </template>
      </Card>

      <!-- Consulting Physicians Section -->
      <Card class="full-width">
        <template #title>
          <div class="card-title-with-action">
            <span>{{ t('admission.consultingPhysicians.title') }}</span>
            <Button
              v-if="canUpdate && admission.status === AdmissionStatus.ACTIVE"
              icon="pi pi-plus"
              :label="t('admission.consultingPhysicians.add')"
              size="small"
              @click="showAddConsultingPhysicianDialog = true"
            />
          </div>
        </template>
        <template #content>
          <div v-if="admission.consultingPhysicians.length === 0" class="empty-section">
            <p>{{ t('admission.consultingPhysicians.empty') }}</p>
          </div>
          <DataTable
            v-else
            :value="admission.consultingPhysicians"
            class="consulting-physicians-table"
          >
            <Column :header="t('admission.consultingPhysicians.physician')">
              <template #body="{ data }">
                {{ formatDoctorName(data.physician) }}
              </template>
            </Column>
            <Column :header="t('admission.consultingPhysicians.reason')">
              <template #body="{ data }">
                {{ data.reason || '-' }}
              </template>
            </Column>
            <Column :header="t('admission.consultingPhysicians.requestedDate')">
              <template #body="{ data }">
                {{ formatDate(data.requestedDate) }}
              </template>
            </Column>
            <Column :header="t('admission.consultingPhysicians.addedBy')">
              <template #body="{ data }">
                <div class="added-info">
                  <span>{{ data.createdBy?.username || '-' }}</span>
                  <small>{{ formatDateTime(data.createdAt) }}</small>
                </div>
              </template>
            </Column>
            <Column
              v-if="canUpdate && admission.status === AdmissionStatus.ACTIVE"
              :header="t('common.actions')"
            >
              <template #body="{ data }">
                <Button
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  size="small"
                  @click="confirmRemoveConsultingPhysician(data)"
                />
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>

      <!-- Documents Section -->
      <Card class="full-width documents-card">
        <template #content>
          <DocumentList :admissionId="admissionId" @upload="showDocumentUploadDialog = true" />
        </template>
      </Card>

      <Card class="full-width">
        <template #title>{{ t('common.auditInfo') }}</template>
        <template #content>
          <div class="audit-grid">
            <div class="info-row">
              <span class="info-label">{{ t('common.createdAt') }}</span>
              <span class="info-value">{{ formatDateTime(admission.createdAt) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('common.createdBy') }}</span>
              <span class="info-value">{{ admission.createdBy?.username || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('common.updatedAt') }}</span>
              <span class="info-value">{{ formatDateTime(admission.updatedAt) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">{{ t('common.updatedBy') }}</span>
              <span class="info-value">{{ admission.updatedBy?.username || '-' }}</span>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <!-- Add Consulting Physician Dialog -->
    <AddConsultingPhysicianDialog
      v-if="admission"
      v-model:visible="showAddConsultingPhysicianDialog"
      :admissionId="admissionId"
      :treatingPhysicianId="admission.treatingPhysician.id"
      :existingPhysicianIds="existingConsultingPhysicianIds"
      @added="handleConsultingPhysicianAdded"
    />

    <!-- Document Upload Dialog -->
    <DocumentUploadDialog
      v-model:visible="showDocumentUploadDialog"
      :admissionId="admissionId"
      @uploaded="handleDocumentUploaded"
    />

    <!-- Document Viewer Modal -->
    <DocumentViewer />
  </div>
</template>

<style scoped>
.admission-detail-page {
  max-width: 900px;
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

.loading-container {
  display: flex;
  justify-content: center;
  padding: 4rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.full-width {
  grid-column: span 2;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--p-surface-border);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-weight: 500;
  color: var(--p-text-muted-color);
}

.info-value {
  font-weight: 500;
}

.triage-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

.audit-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.card-title-with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.empty-section {
  text-align: center;
  padding: 1rem;
  color: var(--p-text-muted-color);
}

.empty-section p {
  margin: 0;
}

.consulting-physicians-table {
  margin-top: 0.5rem;
}

.added-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.added-info small {
  color: var(--p-text-muted-color);
  font-size: 0.75rem;
}

.documents-card :deep(.p-card-content) {
  padding: 0;
}
</style>
