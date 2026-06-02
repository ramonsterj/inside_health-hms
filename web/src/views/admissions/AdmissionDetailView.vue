<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import Button from 'primevue/button'
import AuditInfo from '@/components/common/AuditInfo.vue'
import { useAdmissionStore } from '@/stores/admission'
import { useAuthStore } from '@/stores/auth'
import { AdmissionStatus } from '@/types/admission'
import AddConsultingPhysicianDialog from '@/components/admissions/AddConsultingPhysicianDialog.vue'
import AdmissionExportButton from '@/components/admissions/AdmissionExportButton.vue'
import DocumentUploadDialog from '@/components/documents/DocumentUploadDialog.vue'
import DocumentViewer from '@/components/documents/DocumentViewer.vue'
import AdmissionHeroHeader from '@/components/medical-record/AdmissionHeroHeader.vue'
import MedicalRecordHub from '@/components/medical-record/MedicalRecordHub.vue'

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
const canViewBilling = computed(() => authStore.hasPermission('billing:read'))
const canViewInvoice = computed(() => authStore.hasPermission('invoice:read'))
const canViewMedicalRecord = computed(
  () =>
    authStore.hasPermission('clinical-history:read') ||
    authStore.hasPermission('clinical-history:create') ||
    authStore.hasPermission('progress-note:read') ||
    authStore.hasPermission('progress-note:create') ||
    authStore.hasPermission('medical-order:read') ||
    authStore.hasPermission('medical-order:create') ||
    authStore.hasPermission('psychotherapy-activity:read') ||
    authStore.hasPermission('psychotherapy-activity:create') ||
    authStore.hasPermission('nursing-note:read') ||
    authStore.hasPermission('nursing-note:create') ||
    authStore.hasPermission('vital-sign:read') ||
    authStore.hasPermission('vital-sign:create') ||
    authStore.hasPermission('admission:read') ||
    authStore.hasPermission('admission:upload-documents')
)

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

async function handleConsultingPhysicianAdded() {
  showAddConsultingPhysicianDialog.value = false
  showSuccess('admission.consultingPhysicians.added')
  await admissionStore.fetchAdmission(admissionId.value)
}

function confirmRemoveConsultingPhysicianById(id: number) {
  confirm.require({
    message: t('admission.consultingPhysicians.confirmRemove'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => removeConsultingPhysician(id)
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
    <div class="page-header">
      <div class="header-left">
        <Button icon="pi pi-arrow-left" text rounded @click="router.back()" />
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
        <AdmissionExportButton :admission-id="admissionId" />
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
      <AdmissionHeroHeader :admission="admission" :admissionStatus="admission.status" />

      <!-- Medical Record Section (includes Documents, Consulting Physicians, Nursing, etc.) -->
      <MedicalRecordHub
        v-if="canViewMedicalRecord"
        :admissionId="admissionId"
        :admission="admission"
        :admissionType="admission.type"
        :admissionStatus="admission.status"
        :consultingPhysicians="admission.consultingPhysicians"
        @upload-document="showDocumentUploadDialog = true"
        @add-consulting-physician="showAddConsultingPhysicianDialog = true"
        @remove-consulting-physician="confirmRemoveConsultingPhysicianById"
      />

      <!-- Billing Section -->
      <Card v-if="canViewBilling">
        <template #title>{{ t('billing.title') }}</template>
        <template #content>
          <div class="billing-links">
            <Button
              icon="pi pi-list"
              :label="t('billing.charges')"
              severity="secondary"
              outlined
              @click="router.push({ name: 'admission-charges', params: { id: admissionId } })"
            />
            <Button
              icon="pi pi-chart-bar"
              :label="t('billing.balance')"
              severity="secondary"
              outlined
              @click="router.push({ name: 'admission-balance', params: { id: admissionId } })"
            />
            <Button
              v-if="canViewInvoice"
              icon="pi pi-file"
              :label="t('billing.invoice')"
              severity="secondary"
              outlined
              @click="router.push({ name: 'admission-invoice', params: { id: admissionId } })"
            />
          </div>
        </template>
      </Card>

      <AuditInfo
        :createdAt="admission.createdAt"
        :createdByName="admission.createdBy?.username || '-'"
        :updatedAt="admission.updatedAt"
        :updatedByName="admission.updatedBy?.username || '-'"
      />
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

.loading-container {
  display: flex;
  justify-content: center;
  padding: 4rem;
}

.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.billing-links {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
</style>
