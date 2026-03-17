<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import Tag from 'primevue/tag'
import { formatDateTime, formatDate } from '@/utils/format'
import { MedicalOrderStatus, MedicalOrderCategory } from '@/types/medicalRecord'
import type { MedicalOrderResponse } from '@/types/medicalRecord'
import type { MedicalOrderDocument } from '@/types/document'
import { useAuthStore } from '@/stores/auth'
import { useMedicalOrderDocumentStore } from '@/stores/medicalOrderDocument'
import { useErrorHandler } from '@/composables/useErrorHandler'
import DocumentThumbnail from '@/components/documents/DocumentThumbnail.vue'
import MedicationAdministrationDialog from './MedicationAdministrationDialog.vue'
import MedicationAdministrationHistory from './MedicationAdministrationHistory.vue'
import MedicalOrderDocumentUploadDialog from './MedicalOrderDocumentUploadDialog.vue'
import MedicalOrderDocumentViewer from './MedicalOrderDocumentViewer.vue'

const props = defineProps<{
  order: MedicalOrderResponse
  canEdit: boolean
}>()

const emit = defineEmits<{
  edit: []
  discontinue: []
  documentCountChanged: []
}>()

const { t } = useI18n()
const confirm = useConfirm()
const authStore = useAuthStore()
const documentStore = useMedicalOrderDocumentStore()
const { showError, showSuccess } = useErrorHandler()

const URL_REVOCATION_DELAY_MS = 5000

const showAdministrationDialog = ref(false)
const showHistory = ref(false)
const showUploadDialog = ref(false)
const showAttachments = ref(false)
const attachmentsLoaded = ref(false)
const historyRef = ref<InstanceType<typeof MedicationAdministrationHistory> | null>(null)

const isActive = computed(() => props.order.status === MedicalOrderStatus.ACTIVE)
const isMedicationCategory = computed(
  () => props.order.category === MedicalOrderCategory.MEDICAMENTOS
)
const canAdminister = computed(
  () =>
    authStore.hasPermission('medication-administration:create') &&
    isMedicationCategory.value &&
    isActive.value &&
    props.order.inventoryItemId !== null
)
const canUploadDocument = computed(() => authStore.hasPermission('medical-order:upload-document'))
const canDeleteDocument = computed(() => authStore.hasPermission('medical-order:delete-document'))

const statusSeverity = computed(() => (isActive.value ? 'success' : 'secondary'))

const documents = computed(() => documentStore.getDocuments(props.order.id))
const localDocumentCount = computed(() =>
  attachmentsLoaded.value ? documents.value.length : props.order.documentCount
)

const authorName = computed(() => {
  if (!props.order.createdBy) return '-'
  const staff = props.order.createdBy
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
})

const discontinuedByName = computed(() => {
  if (!props.order.discontinuedBy) return '-'
  const staff = props.order.discontinuedBy
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
})

function confirmDiscontinue() {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmDiscontinue'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-warning',
    accept: () => emit('discontinue')
  })
}

function openAdministrationDialog() {
  showAdministrationDialog.value = true
}

function onAdministrationSaved() {
  showAdministrationDialog.value = false
  if (showHistory.value) {
    historyRef.value?.refresh()
  }
}

function toggleHistory() {
  showHistory.value = !showHistory.value
}

async function toggleAttachments() {
  showAttachments.value = !showAttachments.value
  if (showAttachments.value && !attachmentsLoaded.value) {
    try {
      await documentStore.fetchDocuments(props.order.admissionId, props.order.id)
      attachmentsLoaded.value = true
    } catch (error) {
      showError(error)
    }
  }
}

async function onDocumentUploaded() {
  // Refresh documents list
  try {
    await documentStore.fetchDocuments(props.order.admissionId, props.order.id)
    attachmentsLoaded.value = true
    showAttachments.value = true
  } catch (error) {
    showError(error)
  }
  emit('documentCountChanged')
}

function thumbnailFetcher(documentId: number): Promise<Blob> {
  return documentStore.getThumbnail(props.order.admissionId, props.order.id, documentId)
}

function viewDocument(doc: MedicalOrderDocument) {
  documentStore.setViewerDocument(props.order.admissionId, props.order.id, doc)
}

async function downloadDocument(doc: MedicalOrderDocument) {
  try {
    const blob = await documentStore.downloadDocument(
      props.order.admissionId,
      props.order.id,
      doc.id
    )
    const url = window.URL.createObjectURL(blob)
    const link = window.document.createElement('a')
    link.href = url
    link.download = doc.displayName
    link.click()
    setTimeout(() => window.URL.revokeObjectURL(url), URL_REVOCATION_DELAY_MS)
  } catch (error) {
    showError(error)
  }
}

function confirmDeleteDocument(doc: MedicalOrderDocument) {
  confirm.require({
    message: t('medicalRecord.medicalOrder.documents.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteDocument(doc)
  })
}

async function deleteDocument(doc: MedicalOrderDocument) {
  try {
    await documentStore.deleteDocument(props.order.admissionId, props.order.id, doc.id)
    showSuccess('medicalRecord.medicalOrder.documents.deleted')
    emit('documentCountChanged')
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <Card class="medical-order-card" :class="{ discontinued: !isActive }">
    <template #content>
      <div class="order-content">
        <!-- Header Row -->
        <div class="order-header">
          <div class="order-main-info">
            <!-- Medication Name (prominent for medication orders) -->
            <span v-if="isMedicationCategory && order.medication" class="medication-name">
              {{ order.medication }}
            </span>
            <!-- Dosage & Route -->
            <div v-if="isMedicationCategory" class="medication-details">
              <span v-if="order.dosage" class="dosage">{{ order.dosage }}</span>
              <span v-if="order.route" class="route">
                {{ t(`medicalRecord.medicalOrder.routes.${order.route}`) }}
              </span>
              <span v-if="order.frequency" class="frequency">{{ order.frequency }}</span>
            </div>
            <!-- Schedule -->
            <span v-if="order.schedule" class="schedule">{{ order.schedule }}</span>
          </div>
          <Tag
            :value="t(`medicalRecord.medicalOrder.statuses.${order.status}`)"
            :severity="statusSeverity"
          />
        </div>

        <!-- Observations -->
        <div v-if="order.observations" class="observations">
          <label>{{ t('medicalRecord.medicalOrder.fields.observations') }}:</label>
          <span>{{ order.observations }}</span>
        </div>

        <!-- Dates Row -->
        <div class="dates-row">
          <div class="date-item">
            <label>{{ t('medicalRecord.medicalOrder.fields.startDate') }}:</label>
            <span>{{ formatDate(order.startDate) }}</span>
          </div>
          <div v-if="order.endDate" class="date-item">
            <label>{{ t('medicalRecord.medicalOrder.fields.endDate') }}:</label>
            <span>{{ formatDate(order.endDate) }}</span>
          </div>
        </div>

        <!-- Created By -->
        <div class="meta-row">
          <span class="created-info">
            {{ t('medicalRecord.medicalOrder.orderedBy') }}: {{ authorName }}
            <span class="date-small">({{ formatDateTime(order.createdAt) }})</span>
          </span>
        </div>

        <!-- Discontinued Info -->
        <div v-if="!isActive && order.discontinuedAt" class="discontinued-info">
          <i class="pi pi-ban"></i>
          {{ t('medicalRecord.medicalOrder.discontinuedBy') }}: {{ discontinuedByName }}
          <span class="date-small">({{ formatDateTime(order.discontinuedAt) }})</span>
        </div>

        <!-- Actions -->
        <div class="order-actions">
          <Button
            v-if="canEdit"
            icon="pi pi-pencil"
            :label="t('common.edit')"
            text
            size="small"
            @click="emit('edit')"
          />
          <Button
            v-if="canEdit && isActive"
            icon="pi pi-ban"
            :label="t('medicalRecord.medicalOrder.discontinue')"
            text
            size="small"
            severity="warning"
            @click="confirmDiscontinue"
          />
          <Button
            v-if="canUploadDocument"
            icon="pi pi-paperclip"
            :label="t('medicalRecord.medicalOrder.documents.attach')"
            text
            size="small"
            @click="showUploadDialog = true"
          />
          <Button
            v-if="canAdminister"
            icon="pi pi-plus"
            :label="t('medicationAdministration.administer')"
            text
            size="small"
            severity="success"
            @click="openAdministrationDialog"
          />
          <Button
            v-if="isMedicationCategory && order.inventoryItemId"
            icon="pi pi-history"
            :label="showHistory ? t('common.collapse') : t('medicationAdministration.history')"
            text
            size="small"
            @click="toggleHistory"
          />
          <Button
            v-if="localDocumentCount > 0"
            icon="pi pi-images"
            :label="
              showAttachments
                ? t('common.collapse')
                : t('medicalRecord.medicalOrder.documents.count', { count: localDocumentCount })
            "
            text
            size="small"
            severity="info"
            @click="toggleAttachments"
          >
            <template #icon>
              <i class="pi pi-images" style="margin-right: 0.25rem"></i>
              <Badge v-if="!showAttachments" :value="localDocumentCount" severity="info" />
            </template>
          </Button>
        </div>

        <!-- Document Attachments -->
        <div v-if="showAttachments" class="attachments-section">
          <div v-if="documentStore.loading" class="attachments-loading">
            <i class="pi pi-spin pi-spinner"></i>
          </div>
          <div v-else-if="documents.length === 0" class="attachments-empty">
            {{ t('medicalRecord.medicalOrder.documents.empty') }}
          </div>
          <div v-else class="attachments-grid">
            <DocumentThumbnail
              v-for="doc in documents"
              :key="doc.id"
              :document="doc"
              :admissionId="order.admissionId"
              :canDelete="canDeleteDocument"
              :thumbnailFetcher="thumbnailFetcher"
              @view="viewDocument(doc)"
              @download="downloadDocument(doc)"
              @delete="confirmDeleteDocument(doc)"
            />
          </div>
        </div>

        <!-- Administration History -->
        <MedicationAdministrationHistory
          v-if="showHistory && order.inventoryItemId"
          ref="historyRef"
          :admissionId="order.admissionId"
          :orderId="order.id"
        />
      </div>
    </template>
  </Card>

  <!-- Administration Dialog -->
  <MedicationAdministrationDialog
    v-model:visible="showAdministrationDialog"
    :admissionId="order.admissionId"
    :orderId="order.id"
    :medicationName="order.medication"
    @saved="onAdministrationSaved"
  />

  <!-- Document Upload Dialog -->
  <MedicalOrderDocumentUploadDialog
    v-model:visible="showUploadDialog"
    :admissionId="order.admissionId"
    :orderId="order.id"
    @uploaded="onDocumentUploaded"
  />

  <!-- Document Viewer -->
  <MedicalOrderDocumentViewer />
</template>

<style scoped>
.medical-order-card {
  background: var(--p-surface-card);
  border-left: 4px solid var(--p-green-500);
}

.medical-order-card.discontinued {
  border-left-color: var(--p-gray-400);
  opacity: 0.8;
}

.order-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.order-main-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.medication-name {
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--p-text-color);
}

.medication-details {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-size: 0.875rem;
}

.dosage {
  font-weight: 600;
  color: var(--p-primary-color);
}

.route {
  color: var(--p-text-muted-color);
}

.frequency {
  color: var(--p-text-color);
}

.schedule {
  font-size: 0.875rem;
  color: var(--p-text-muted-color);
}

.observations {
  padding: 0.5rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
  font-size: 0.875rem;
}

.observations label {
  font-weight: 500;
  margin-right: 0.25rem;
}

.dates-row {
  display: flex;
  gap: 1.5rem;
  font-size: 0.875rem;
}

.date-item label {
  font-weight: 500;
  margin-right: 0.25rem;
  color: var(--p-text-muted-color);
}

.meta-row {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.date-small {
  font-size: 0.7rem;
  margin-left: 0.25rem;
}

.discontinued-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--p-orange-600);
  padding: 0.5rem;
  background: var(--p-orange-50);
  border-radius: var(--p-border-radius);
}

.discontinued-info i {
  font-size: 0.875rem;
}

.order-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
}

.attachments-section {
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--p-surface-border);
}

.attachments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 0.75rem;
}

.attachments-loading {
  display: flex;
  justify-content: center;
  padding: 1rem;
  color: var(--p-text-muted-color);
}

.attachments-empty {
  text-align: center;
  padding: 1rem;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}
</style>
