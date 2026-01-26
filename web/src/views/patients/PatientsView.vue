<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Dialog from 'primevue/dialog'
import FileUpload from 'primevue/fileupload'
import { usePatientStore } from '@/stores/patient'
import { useAuthStore } from '@/stores/auth'
import type { PatientSummary } from '@/types'
import { MAX_ID_DOCUMENT_SIZE, ACCEPTED_ID_DOCUMENT_TYPES } from '@/validation/patient'

const { t } = useI18n()
const router = useRouter()
const { showError, showSuccess } = useErrorHandler()
const patientStore = usePatientStore()
const authStore = useAuthStore()

const first = ref(0)
const rows = ref(20)
const searchQuery = ref('')
const searchTimeout = ref<ReturnType<typeof setTimeout> | null>(null)
const showUploadDialog = ref(false)
const selectedPatientForUpload = ref<PatientSummary | null>(null)
const uploadLoading = ref(false)

const canCreate = computed(() => authStore.hasPermission('patient:create'))
const canUploadId = computed(() => authStore.hasPermission('patient:upload-id'))
const canAdmit = computed(() => authStore.hasPermission('admission:create'))

onMounted(() => {
  loadPatients()
})

async function loadPatients() {
  try {
    const page = Math.floor(first.value / rows.value)
    await patientStore.fetchPatients(page, rows.value, searchQuery.value || null)
  } catch (error) {
    showError(error)
  }
}

function onSearch() {
  if (searchTimeout.value) {
    clearTimeout(searchTimeout.value)
  }
  searchTimeout.value = setTimeout(() => {
    first.value = 0
    loadPatients()
  }, 300)
}

function onPageChange() {
  loadPatients()
}

function viewPatient(patientId: number) {
  router.push({ name: 'patient-detail', params: { id: patientId } })
}

function editPatient(patientId: number) {
  router.push({ name: 'patient-edit', params: { id: patientId } })
}

function admitPatient(patientId: number) {
  router.push({ name: 'admission-create', query: { patientId: patientId.toString() } })
}

function createNewPatient() {
  router.push({ name: 'patient-create' })
}

function getFullName(firstName: string, lastName: string): string {
  return `${firstName} ${lastName}`.trim()
}

function openUploadDialog(patient: PatientSummary) {
  selectedPatientForUpload.value = patient
  showUploadDialog.value = true
}

function closeUploadDialog() {
  showUploadDialog.value = false
  selectedPatientForUpload.value = null
}

async function onFileUpload(event: { files: File | File[] }) {
  const files = Array.isArray(event.files) ? event.files : [event.files]
  const file = files[0]
  if (!file || !selectedPatientForUpload.value) return

  uploadLoading.value = true
  try {
    await patientStore.uploadIdDocument(selectedPatientForUpload.value.id, file)
    showSuccess('patient.idDocumentUploaded')
    closeUploadDialog()
    loadPatients()
  } catch (error) {
    showError(error)
  } finally {
    uploadLoading.value = false
  }
}
</script>

<template>
  <div class="patients-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('patient.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('patient.newPatient')"
          @click="createNewPatient"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadPatients"
          :loading="patientStore.loading"
        />
      </div>
    </div>

    <Card class="search-card">
      <template #content>
        <div class="search-bar">
          <IconField>
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="searchQuery"
              :placeholder="t('patient.search')"
              @input="onSearch"
              class="search-input"
            />
          </IconField>
        </div>
      </template>
    </Card>

    <Card>
      <template #content>
        <DataTable
          :value="patientStore.patients"
          :loading="patientStore.loading"
          :paginator="true"
          v-model:rows="rows"
          v-model:first="first"
          :totalRecords="patientStore.totalPatients"
          :lazy="true"
          @page="onPageChange"
          :rowsPerPageOptions="[10, 20, 50]"
          dataKey="id"
          stripedRows
          scrollable
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('patient.empty') }}
            </div>
          </template>

          <Column :header="t('patient.fullName')">
            <template #body="{ data }">
              {{ getFullName(data.firstName, data.lastName) }}
            </template>
          </Column>

          <Column field="age" :header="t('patient.age')" style="width: 80px" />

          <Column field="idDocumentNumber" :header="t('patient.idDocumentNumber')">
            <template #body="{ data }">
              {{ data.idDocumentNumber || '-' }}
            </template>
          </Column>

          <Column :header="t('common.actions')" style="width: 150px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  icon="pi pi-eye"
                  severity="info"
                  text
                  rounded
                  @click="viewPatient(data.id)"
                  v-tooltip.top="t('common.view')"
                />
                <Button
                  v-if="canCreate"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  @click="editPatient(data.id)"
                  v-tooltip.top="t('common.edit')"
                />
                <Button
                  v-if="canAdmit"
                  icon="pi pi-user-plus"
                  severity="success"
                  text
                  rounded
                  @click="admitPatient(data.id)"
                  v-tooltip.top="t('patient.actions.admit')"
                />
                <Button
                  v-if="canUploadId && !data.hasIdDocument"
                  icon="pi pi-id-card"
                  severity="warning"
                  text
                  rounded
                  @click="openUploadDialog(data)"
                  v-tooltip.top="t('patient.noIdDocument')"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <!-- Upload ID Document Dialog -->
    <Dialog
      v-model:visible="showUploadDialog"
      :header="t('patient.uploadIdDocument')"
      :modal="true"
      :closable="!uploadLoading"
      :style="{ width: '500px' }"
      :breakpoints="{ '640px': '90vw' }"
      @hide="closeUploadDialog"
    >
      <div v-if="selectedPatientForUpload" class="upload-dialog-content">
        <p class="upload-patient-name">
          {{ getFullName(selectedPatientForUpload.firstName, selectedPatientForUpload.lastName) }}
        </p>
        <FileUpload
          name="idDocument"
          :accept="ACCEPTED_ID_DOCUMENT_TYPES"
          :maxFileSize="MAX_ID_DOCUMENT_SIZE"
          :auto="true"
          customUpload
          @uploader="onFileUpload"
          :disabled="uploadLoading"
          :pt="{
            root: { class: 'upload-dropzone' },
            content: { class: 'upload-dropzone-content' }
          }"
        >
          <template #empty>
            <div class="dropzone-empty">
              <i class="pi pi-cloud-upload dropzone-icon" />
              <p class="dropzone-text">{{ t('patient.dragDropIdDocument') }}</p>
              <p class="dropzone-hint">{{ t('patient.idDocumentHint') }}</p>
            </div>
          </template>
        </FileUpload>
      </div>
    </Dialog>
  </div>
</template>

<style scoped>
.patients-page {
  max-width: 1200px;
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

.search-card {
  margin-bottom: 1rem;
}

.search-bar {
  display: flex;
  align-items: center;
}

.search-input {
  width: 300px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.upload-dialog-content {
  padding: 0.5rem 0;
}

.upload-patient-name {
  font-weight: 600;
  font-size: 1.1rem;
  margin-bottom: 1rem;
  text-align: center;
}

.dropzone-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  text-align: center;
}

.dropzone-icon {
  font-size: 3rem;
  color: var(--p-primary-color);
  margin-bottom: 1rem;
}

.dropzone-text {
  font-size: 1rem;
  font-weight: 500;
  margin: 0 0 0.5rem 0;
}

.dropzone-hint {
  font-size: 0.875rem;
  color: var(--p-text-muted-color);
  margin: 0;
}

:deep(.upload-dropzone) {
  border: 2px dashed var(--p-surface-border);
  border-radius: 8px;
  background: var(--p-surface-ground);
  transition:
    border-color 0.2s,
    background-color 0.2s;
}

:deep(.upload-dropzone:hover),
:deep(.upload-dropzone.p-fileupload-highlight) {
  border-color: var(--p-primary-color);
  background: var(--p-primary-50);
}

:deep(.upload-dropzone-content) {
  padding: 0;
}
</style>
