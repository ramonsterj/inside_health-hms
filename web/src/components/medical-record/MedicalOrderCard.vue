<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import { formatDateTime, formatDate } from '@/utils/format'
import { sanitizeHtml } from '@/utils/sanitize'
import {
  MedicalOrderStatus,
  MedicalOrderCategory,
  RESULTS_BEARING_CATEGORIES,
  isTerminalStatus,
  canDiscontinueStatus,
  canUploadResultDocument,
  categoryRequiresAuthorization
} from '@/types/medicalRecord'
import type { MedicalOrderResponse } from '@/types/medicalRecord'
import type { MedicalOrderDocument } from '@/types/document'
import { useAuthStore } from '@/stores/auth'
import { useMedicalOrderDocumentStore } from '@/stores/medicalOrderDocument'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useErrorHandler } from '@/composables/useErrorHandler'
import DocumentThumbnail from '@/components/documents/DocumentThumbnail.vue'
import MedicalOrderStateBadge from './MedicalOrderStateBadge.vue'
import MedicationAdministrationDialog from './MedicationAdministrationDialog.vue'
import MedicationAdministrationHistory from './MedicationAdministrationHistory.vue'
import MedicalOrderDocumentUploadDialog from './MedicalOrderDocumentUploadDialog.vue'
import MedicalOrderDocumentViewer from './MedicalOrderDocumentViewer.vue'
import MedicalOrderRejectDialog from './MedicalOrderRejectDialog.vue'
import MedicalOrderEmergencyAuthorizeDialog from './MedicalOrderEmergencyAuthorizeDialog.vue'

const props = defineProps<{
  order: MedicalOrderResponse
  canEdit: boolean
}>()

const emit = defineEmits<{
  edit: []
  discontinue: []
  documentCountChanged: []
  refresh: []
}>()

const { t } = useI18n()
const confirm = useConfirm()
const authStore = useAuthStore()
const documentStore = useMedicalOrderDocumentStore()
const orderStore = useMedicalOrderStore()
const { showError, showSuccess } = useErrorHandler()

const URL_REVOCATION_DELAY_MS = 5000

const showAdministrationDialog = ref(false)
const showHistory = ref(false)
const showUploadDialog = ref(false)
const showAttachments = ref(false)
const attachmentsLoaded = ref(false)
const showRejectDialog = ref(false)
const showEmergencyDialog = ref(false)
const historyRef = ref<InstanceType<typeof MedicationAdministrationHistory> | null>(null)

const sanitizedObservations = computed(() => sanitizeHtml(props.order.observations))

const isAuthorized = computed(() => props.order.status === MedicalOrderStatus.AUTORIZADO)
const isTerminal = computed(() => isTerminalStatus(props.order.status))
const isMedicationCategory = computed(
  () => props.order.category === MedicalOrderCategory.MEDICAMENTOS
)
const isResultsBearingCategory = computed(() =>
  RESULTS_BEARING_CATEGORIES.includes(props.order.category)
)
const requiresAuthorization = computed(() => categoryRequiresAuthorization(props.order.category))

// Medication administration only makes sense for AUTORIZADO medication orders.
const canAdminister = computed(
  () =>
    authStore.hasPermission('medication-administration:create') &&
    isMedicationCategory.value &&
    isAuthorized.value &&
    props.order.inventoryItemId !== null
)
const canUploadDocument = computed(
  () =>
    authStore.hasPermission('medical-order:upload-document') &&
    canUploadResultDocument(props.order.category, props.order.status)
)
const canDeleteDocument = computed(() => authStore.hasPermission('medical-order:delete-document'))

// State-machine gating for the action buttons. Each button only shows when the order is in the
// right state AND the user holds the required permission.
const canAuthorize = computed(
  () =>
    authStore.hasPermission('medical-order:authorize') &&
    requiresAuthorization.value &&
    props.order.status === MedicalOrderStatus.SOLICITADO
)
const canReject = canAuthorize
const canEmergencyAuthorize = computed(
  () =>
    authStore.hasPermission('medical-order:emergency-authorize') &&
    requiresAuthorization.value &&
    props.order.status === MedicalOrderStatus.SOLICITADO
)
const canMarkInProgress = computed(
  () =>
    authStore.hasPermission('medical-order:mark-in-progress') &&
    isResultsBearingCategory.value &&
    isAuthorized.value
)
const canDiscontinue = computed(
  () =>
    authStore.hasPermission('medical-order:discontinue') &&
    canDiscontinueStatus(props.order.status)
)

// Per-category label for the "mark in progress" button.
const markInProgressLabel = computed(() => {
  switch (props.order.category) {
    case MedicalOrderCategory.LABORATORIOS:
      return t('medicalRecord.medicalOrder.actions.markInProgressLab')
    case MedicalOrderCategory.REFERENCIAS_MEDICAS:
      return t('medicalRecord.medicalOrder.actions.markInProgressReferral')
    case MedicalOrderCategory.PRUEBAS_PSICOMETRICAS:
      return t('medicalRecord.medicalOrder.actions.markInProgressTest')
    default:
      return t('medicalRecord.medicalOrder.actions.markInProgress')
  }
})

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

function staffDisplayName(staff: MedicalOrderResponse['discontinuedBy']): string {
  if (!staff) return '-'
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
}

const discontinuedByName = computed(() => staffDisplayName(props.order.discontinuedBy))
const authorizedByName = computed(() => staffDisplayName(props.order.authorizedBy))
const rejectedByName = computed(() => staffDisplayName(props.order.rejectedBy))
const inProgressByName = computed(() => staffDisplayName(props.order.inProgressBy))
const resultsReceivedByName = computed(() => staffDisplayName(props.order.resultsReceivedBy))
const emergencyByName = computed(() => staffDisplayName(props.order.emergencyBy))

function confirmDiscontinue() {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmDiscontinue'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-warning',
    accept: () => emit('discontinue')
  })
}

function confirmAuthorize() {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmAuthorize'),
    header: t('common.confirm'),
    icon: 'pi pi-check-circle',
    acceptClass: 'p-button-success',
    accept: doAuthorize
  })
}

async function doAuthorize() {
  try {
    await orderStore.authorizeMedicalOrder(props.order.admissionId, props.order.id)
    showSuccess('medicalRecord.medicalOrder.transitions.authorized')
    emit('refresh')
  } catch (error) {
    showError(error)
  }
}

function confirmMarkInProgress() {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmMarkInProgress'),
    header: t('common.confirm'),
    icon: 'pi pi-clock',
    accept: doMarkInProgress
  })
}

async function doMarkInProgress() {
  try {
    await orderStore.markInProgress(props.order.admissionId, props.order.id)
    showSuccess('medicalRecord.medicalOrder.transitions.markedInProgress')
    emit('refresh')
  } catch (error) {
    showError(error)
  }
}

function openRejectDialog() {
  showRejectDialog.value = true
}

function onRejected() {
  emit('refresh')
}

function openEmergencyDialog() {
  showEmergencyDialog.value = true
}

function onEmergencyAuthorized() {
  emit('refresh')
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
  <Card class="medical-order-card" :class="{ discontinued: isTerminal }">
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
          <MedicalOrderStateBadge :status="order.status" />
        </div>

        <!-- Observations -->
        <div v-if="order.observations" class="observations">
          <label>{{ t('medicalRecord.medicalOrder.fields.observations') }}:</label>
          <!-- eslint-disable-next-line vue/no-v-html -- content is sanitized via DOMPurify -->
          <div class="observations-content" v-html="sanitizedObservations"></div>
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

        <!-- Authorized Info -->
        <div v-if="order.authorizedAt" class="meta-row">
          <i class="pi pi-check-circle" style="color: var(--p-green-600); margin-right: 0.25rem"></i>
          {{ t('medicalRecord.medicalOrder.authorizedBy') }}: {{ authorizedByName }}
          <span class="date-small">({{ formatDateTime(order.authorizedAt) }})</span>
        </div>

        <!-- Emergency Authorization Info -->
        <div v-if="order.emergencyAuthorized" class="emergency-info">
          <i class="pi pi-bolt"></i>
          {{ t('medicalRecord.medicalOrder.emergencyAuthorize.banner') }}:
          {{
            order.emergencyReason
              ? t(`medicalRecord.medicalOrder.emergencyAuthorize.reasons.${order.emergencyReason}`)
              : '—'
          }}
          <div v-if="order.emergencyReasonNote" class="emergency-note">
            “{{ order.emergencyReasonNote }}”
          </div>
          <div class="date-small">
            {{ emergencyByName }} ({{ formatDateTime(order.emergencyAt) }})
          </div>
        </div>

        <!-- Rejection Info -->
        <div v-if="order.status === MedicalOrderStatus.NO_AUTORIZADO" class="rejection-info">
          <i class="pi pi-times-circle"></i>
          {{ t('medicalRecord.medicalOrder.rejectionReason') }}:
          {{ order.rejectionReason || '—' }}
          <div class="date-small">
            {{ rejectedByName }} ({{ formatDateTime(order.rejectedAt) }})
          </div>
        </div>

        <!-- In Progress Info -->
        <div v-if="order.inProgressAt" class="meta-row">
          <i class="pi pi-clock" style="color: var(--p-orange-600); margin-right: 0.25rem"></i>
          {{ t('medicalRecord.medicalOrder.inProgressBy') }}: {{ inProgressByName }}
          <span class="date-small">({{ formatDateTime(order.inProgressAt) }})</span>
        </div>

        <!-- Results Received Info -->
        <div v-if="order.resultsReceivedAt" class="meta-row">
          <i class="pi pi-inbox" style="color: var(--p-blue-600); margin-right: 0.25rem"></i>
          {{ t('medicalRecord.medicalOrder.resultsReceivedBy') }}: {{ resultsReceivedByName }}
          <span class="date-small">({{ formatDateTime(order.resultsReceivedAt) }})</span>
        </div>

        <!-- Discontinued Info -->
        <div v-if="order.discontinuedAt" class="discontinued-info">
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
            v-if="canAuthorize"
            icon="pi pi-check"
            :label="t('medicalRecord.medicalOrder.actions.authorize')"
            text
            size="small"
            severity="success"
            @click="confirmAuthorize"
          />
          <Button
            v-if="canReject"
            icon="pi pi-times"
            :label="t('medicalRecord.medicalOrder.actions.reject')"
            text
            size="small"
            severity="danger"
            @click="openRejectDialog"
          />
          <Button
            v-if="canEmergencyAuthorize"
            icon="pi pi-bolt"
            :label="t('medicalRecord.medicalOrder.actions.emergencyAuthorize')"
            text
            size="small"
            severity="warn"
            @click="openEmergencyDialog"
          />
          <Button
            v-if="canMarkInProgress"
            icon="pi pi-play"
            :label="markInProgressLabel"
            text
            size="small"
            severity="warn"
            @click="confirmMarkInProgress"
          />
          <Button
            v-if="canDiscontinue"
            icon="pi pi-ban"
            :label="t('medicalRecord.medicalOrder.discontinue')"
            text
            size="small"
            severity="warn"
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

  <!-- Reject Dialog -->
  <MedicalOrderRejectDialog
    v-model:visible="showRejectDialog"
    :admissionId="order.admissionId"
    :orderId="order.id"
    @rejected="onRejected"
  />

  <!-- Emergency Authorize Dialog -->
  <MedicalOrderEmergencyAuthorizeDialog
    v-model:visible="showEmergencyDialog"
    :admissionId="order.admissionId"
    :orderId="order.id"
    @authorized="onEmergencyAuthorized"
  />
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

.observations-content {
  display: inline-block;
  vertical-align: top;
  word-wrap: break-word;
}

.observations-content:deep(p) {
  margin: 0 0 0.5rem 0;
}

.observations-content:deep(p:last-child) {
  margin-bottom: 0;
}

.observations-content:deep(ul),
.observations-content:deep(ol) {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
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

.rejection-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: var(--p-red-700);
  padding: 0.5rem;
  background: var(--p-red-50);
  border-radius: var(--p-border-radius);
}

.rejection-info i {
  margin-right: 0.25rem;
}

.emergency-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: var(--p-orange-800);
  padding: 0.5rem;
  background: var(--p-orange-50);
  border-radius: var(--p-border-radius);
}

.emergency-info i {
  margin-right: 0.25rem;
}

.emergency-note {
  font-style: italic;
  font-size: 0.8125rem;
  color: var(--p-orange-700);
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
