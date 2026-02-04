<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import DocumentThumbnail from './DocumentThumbnail.vue'
import { useDocumentStore } from '@/stores/document'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { AdmissionDocument } from '@/types/document'

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const documentStore = useDocumentStore()
const authStore = useAuthStore()

const props = defineProps<{
  admissionId: number
}>()

const emit = defineEmits<{
  upload: []
}>()

const documents = computed(() => documentStore.getDocuments(props.admissionId))
const canUpload = computed(() => authStore.hasPermission('admission:upload-documents'))
const canDelete = computed(() => authStore.hasPermission('admission:delete-documents'))

onMounted(() => {
  loadDocuments()
})

async function loadDocuments() {
  try {
    await documentStore.fetchDocuments(props.admissionId)
  } catch (error) {
    showError(error)
  }
}

function viewDocument(document: AdmissionDocument) {
  documentStore.setViewerDocument(props.admissionId, document)
}

async function downloadDocument(document: AdmissionDocument) {
  try {
    const blob = await documentStore.downloadDocument(props.admissionId, document.id)
    const url = window.URL.createObjectURL(blob)
    const link = window.document.createElement('a')
    link.href = url
    link.download = document.displayName
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    showError(error)
  }
}

function confirmDelete(document: AdmissionDocument) {
  confirm.require({
    message: t('document.confirmDelete', { name: document.displayName }),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteDocument(document)
  })
}

async function deleteDocument(document: AdmissionDocument) {
  try {
    await documentStore.deleteDocument(props.admissionId, document.id)
    showSuccess('document.deleted')
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="document-list">
    <div class="document-list-header">
      <h3>{{ t('document.title') }}</h3>
      <div class="header-actions">
        <Button
          v-if="canUpload"
          icon="pi pi-upload"
          :label="t('document.upload')"
          size="small"
          @click="emit('upload')"
        />
        <Button
          icon="pi pi-refresh"
          size="small"
          severity="secondary"
          outlined
          @click="loadDocuments"
          :loading="documentStore.loading"
          v-tooltip.top="t('common.refresh')"
        />
      </div>
    </div>

    <div v-if="documentStore.loading && documents.length === 0" class="loading-container">
      <ProgressSpinner />
    </div>

    <div v-else-if="documents.length === 0" class="empty-state">
      <i class="pi pi-folder-open"></i>
      <p>{{ t('document.empty') }}</p>
      <Button
        v-if="canUpload"
        icon="pi pi-upload"
        :label="t('document.uploadFirst')"
        @click="emit('upload')"
      />
    </div>

    <div v-else class="document-grid">
      <DocumentThumbnail
        v-for="doc in documents"
        :key="doc.id"
        :document="doc"
        :admission-id="admissionId"
        :can-delete="canDelete"
        @view="viewDocument(doc)"
        @download="downloadDocument(doc)"
        @delete="confirmDelete(doc)"
      />
    </div>
  </div>
</template>

<style scoped>
.document-list {
  padding: 1rem;
}

.document-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.document-list-header h3 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  text-align: center;
  color: var(--text-color-secondary);
}

.empty-state i {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state p {
  margin-bottom: 1rem;
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 180px));
  grid-auto-rows: 1fr;
  gap: 1rem;
}
</style>
