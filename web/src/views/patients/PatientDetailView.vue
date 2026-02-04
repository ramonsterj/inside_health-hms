<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import ProgressSpinner from 'primevue/progressspinner'
import { usePatientStore } from '@/stores/patient'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { showError } = useErrorHandler()
const patientStore = usePatientStore()
const authStore = useAuthStore()

const patientId = computed(() => Number(route.params.id))
const patient = computed(() => patientStore.currentPatient)
const loading = ref(false)
const showIdDocumentDialog = ref(false)
const idDocumentUrl = ref<string | null>(null)

const canEdit = computed(() => authStore.hasPermission('patient:update'))
const canViewId = computed(() => authStore.hasPermission('patient:view-id'))
const canAdmit = computed(() => authStore.hasPermission('admission:create'))

onMounted(async () => {
  await loadPatient()
})

async function loadPatient() {
  loading.value = true
  try {
    await patientStore.fetchPatient(patientId.value)
  } catch (error) {
    showError(error)
    router.push({ name: 'patients' })
  } finally {
    loading.value = false
  }
}

function editPatient() {
  router.push({ name: 'patient-edit', params: { id: patientId.value } })
}

function admitPatient() {
  router.push({ name: 'admission-create', query: { patientId: patientId.value } })
}

function goBack() {
  router.push({ name: 'patients' })
}

async function viewIdDocument() {
  try {
    const blob = await patientStore.downloadIdDocument(patientId.value)
    idDocumentUrl.value = URL.createObjectURL(blob)
    showIdDocumentDialog.value = true
  } catch (error) {
    showError(error)
  }
}

async function downloadIdDocument() {
  try {
    const blob = await patientStore.downloadIdDocument(patientId.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `patient-${patientId.value}-id-document`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (error) {
    showError(error)
  }
}

function closeIdDocumentDialog() {
  showIdDocumentDialog.value = false
  if (idDocumentUrl.value) {
    URL.revokeObjectURL(idDocumentUrl.value)
    idDocumentUrl.value = null
  }
}

function formatDate(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

function formatUserName(
  user: { firstName: string | null; lastName: string | null; username: string } | null
): string {
  if (!user) return '-'
  if (user.firstName || user.lastName) {
    return `${user.firstName || ''} ${user.lastName || ''}`.trim()
  }
  return user.username
}
</script>

<template>
  <div class="patient-detail-page">
    <div v-if="loading" class="loading-container">
      <ProgressSpinner />
    </div>

    <template v-else-if="patient">
      <div class="page-header">
        <div class="header-left">
          <Button icon="pi pi-arrow-left" severity="secondary" text rounded @click="goBack" />
          <h1 class="page-title">{{ patient.firstName }} {{ patient.lastName }}</h1>
        </div>
        <div class="header-actions">
          <Button
            v-if="canAdmit"
            icon="pi pi-user-plus"
            :label="t('patient.admitPatient')"
            @click="admitPatient"
          />
          <Button
            v-if="canEdit"
            icon="pi pi-pencil"
            :label="t('common.edit')"
            severity="secondary"
            @click="editPatient"
          />
        </div>
      </div>

      <div class="detail-grid">
        <Card class="detail-card">
          <template #title>{{ t('patient.generalInfo') }}</template>
          <template #content>
            <div class="info-grid">
              <div class="info-item">
                <label>{{ t('patient.firstName') }}</label>
                <span>{{ patient.firstName }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.lastName') }}</label>
                <span>{{ patient.lastName }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.age') }}</label>
                <span>{{ patient.age }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.sex') }}</label>
                <span>{{ t(`patient.sexOptions.${patient.sex}`) }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.gender') }}</label>
                <span>{{ patient.gender }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.maritalStatus') }}</label>
                <span>{{ t(`patient.maritalStatusOptions.${patient.maritalStatus}`) }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.religion') }}</label>
                <span>{{ patient.religion }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.educationLevel') }}</label>
                <span>{{ t(`patient.educationLevelOptions.${patient.educationLevel}`) }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.occupation') }}</label>
                <span>{{ patient.occupation }}</span>
              </div>
              <div class="info-item full-width">
                <label>{{ t('patient.address') }}</label>
                <span>{{ patient.address }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.email') }}</label>
                <span>{{ patient.email }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.idDocumentNumber') }}</label>
                <span>{{ patient.idDocumentNumber || '-' }}</span>
              </div>
              <div v-if="patient.notes" class="info-item full-width">
                <label>{{ t('patient.notes') }}</label>
                <span>{{ patient.notes }}</span>
              </div>
            </div>
          </template>
        </Card>

        <Card class="detail-card">
          <template #title>{{ t('patient.emergencyContacts') }}</template>
          <template #content>
            <div
              v-for="contact in patient.emergencyContacts"
              :key="contact.id"
              class="contact-card"
            >
              <div class="contact-name">{{ contact.name }}</div>
              <div class="contact-details">
                <span>{{ contact.relationship }}</span>
                <span>{{ contact.phone }}</span>
              </div>
            </div>
          </template>
        </Card>

        <Card v-if="canViewId" class="detail-card">
          <template #title>{{ t('patient.idDocument') }}</template>
          <template #content>
            <div v-if="patient.hasIdDocument" class="id-document-actions">
              <Button
                icon="pi pi-eye"
                :label="t('patient.viewIdDocument')"
                @click="viewIdDocument"
              />
              <Button
                icon="pi pi-download"
                :label="t('common.download')"
                severity="secondary"
                @click="downloadIdDocument"
              />
            </div>
            <div v-else class="no-document">
              <p>{{ t('patient.noIdDocument') }}</p>
            </div>
          </template>
        </Card>

        <Card class="detail-card">
          <template #title>{{ t('patient.auditInfo') }}</template>
          <template #content>
            <div class="info-grid">
              <div class="info-item">
                <label>{{ t('patient.registeredBy') }}</label>
                <span>{{ formatUserName(patient.createdBy) }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('common.createdAt') }}</label>
                <span>{{ formatDate(patient.createdAt) }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('patient.lastModifiedBy') }}</label>
                <span>{{ formatUserName(patient.updatedBy) }}</span>
              </div>
              <div class="info-item">
                <label>{{ t('common.updatedAt') }}</label>
                <span>{{ formatDate(patient.updatedAt) }}</span>
              </div>
            </div>
          </template>
        </Card>
      </div>
    </template>

    <!-- ID Document Preview Dialog -->
    <Dialog
      v-model:visible="showIdDocumentDialog"
      :header="t('patient.idDocument')"
      :modal="true"
      :style="{ width: '80vw', maxWidth: '800px' }"
      :breakpoints="{ '640px': '95vw' }"
      @hide="closeIdDocumentDialog"
    >
      <div class="document-preview">
        <img v-if="idDocumentUrl" :src="idDocumentUrl" alt="ID Document" class="document-image" />
      </div>
      <template #footer>
        <Button :label="t('common.close')" @click="closeIdDocumentDialog" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.patient-detail-page {
  max-width: 1200px;
  margin: 0 auto;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
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

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.detail-card {
  height: fit-content;
}

.detail-card:first-child {
  grid-column: span 2;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-item label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-color-secondary);
  text-transform: uppercase;
}

.info-item span {
  font-size: 0.875rem;
}

.contact-card {
  padding: 0.75rem;
  border: 1px solid var(--surface-border);
  border-radius: 6px;
  margin-bottom: 0.5rem;
}

.contact-name {
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.contact-details {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.id-document-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.no-document {
  text-align: center;
  padding: 1rem;
}

.no-document p {
  color: var(--text-color-secondary);
  margin-bottom: 1rem;
}

.document-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.document-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-card:first-child {
    grid-column: span 1;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item.full-width {
    grid-column: span 1;
  }
}
</style>
