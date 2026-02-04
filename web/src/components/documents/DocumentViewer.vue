<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import { useDocumentStore } from '@/stores/document'
import { useErrorHandler } from '@/composables/useErrorHandler'

const { t } = useI18n()
const { showError } = useErrorHandler()
const documentStore = useDocumentStore()

const documentUrl = ref<string | null>(null)
const loading = ref(false)

const visible = computed({
  get: () => !!documentStore.viewerDocument,
  set: (value: boolean) => {
    if (!value) {
      closeViewer()
    }
  }
})
const document = computed(() => documentStore.viewerDocument)
const isImage = computed(() => document.value?.contentType?.startsWith('image/') ?? false)

watch(
  () => documentStore.viewerDocument,
  async newDoc => {
    if (newDoc && documentStore.viewerAdmissionId) {
      // For PDFs, open directly in new tab - no dialog needed
      if (newDoc.contentType === 'application/pdf') {
        await openPdfInNewTab()
      } else {
        await loadDocument()
      }
    } else {
      clearUrl()
    }
  }
)

async function openPdfInNewTab() {
  if (!document.value || !documentStore.viewerAdmissionId) return

  loading.value = true
  try {
    const blob = await documentStore.downloadDocument(
      documentStore.viewerAdmissionId,
      document.value.id
    )
    const url = window.URL.createObjectURL(blob)
    window.open(url, '_blank')
    // Close the viewer since we opened in new tab
    closeViewer()
    // Revoke after a delay to ensure the new tab has loaded
    setTimeout(() => window.URL.revokeObjectURL(url), 5000)
  } catch (error) {
    showError(error)
    closeViewer()
  } finally {
    loading.value = false
  }
}

async function loadDocument() {
  if (!document.value || !documentStore.viewerAdmissionId) return

  loading.value = true
  try {
    const blob = await documentStore.downloadDocument(
      documentStore.viewerAdmissionId,
      document.value.id
    )
    documentUrl.value = window.URL.createObjectURL(blob)
  } catch (error) {
    showError(error)
    closeViewer()
  } finally {
    loading.value = false
  }
}

function clearUrl() {
  if (documentUrl.value) {
    window.URL.revokeObjectURL(documentUrl.value)
    documentUrl.value = null
  }
}

function closeViewer() {
  clearUrl()
  documentStore.clearViewerDocument()
}

function downloadDocument() {
  if (!document.value || !documentUrl.value) return

  const link = window.document.createElement('a')
  link.href = documentUrl.value
  link.download = document.value.displayName
  link.click()
}
</script>

<template>
  <!-- Dialog only shown for non-PDF documents (images) -->
  <Dialog
    v-model:visible="visible"
    :header="document?.displayName || t('document.viewer')"
    modal
    :style="{ width: '90vw', maxWidth: '1200px' }"
    :contentStyle="{ height: '80vh', padding: 0 }"
    :pt="{ footer: { style: 'padding: 1rem 1.5rem' } }"
  >
    <template #header>
      <div class="viewer-header">
        <span class="document-title">{{ document?.displayName }}</span>
        <div class="viewer-actions">
          <Button
            icon="pi pi-download"
            :label="t('document.download')"
            severity="secondary"
            outlined
            size="small"
            @click="downloadDocument"
          />
        </div>
      </div>
    </template>

    <div class="viewer-content">
      <div v-if="loading" class="loading-container">
        <ProgressSpinner />
      </div>

      <template v-else-if="documentUrl">
        <div v-if="isImage" class="image-viewer">
          <img :src="documentUrl" :alt="document?.displayName" />
        </div>

        <div v-else class="unsupported">
          <i class="pi pi-file"></i>
          <p>{{ t('document.unsupportedType') }}</p>
          <Button :label="t('document.download')" icon="pi pi-download" @click="downloadDocument" />
        </div>
      </template>
    </div>

    <template #footer>
      <Button :label="t('common.close')" severity="secondary" @click="closeViewer" />
    </template>
  </Dialog>
</template>

<style scoped>
.viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.document-title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 1rem;
}

.viewer-actions {
  display: flex;
  gap: 0.5rem;
}

.viewer-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-ground);
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.image-viewer {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  padding: 1rem;
}

.image-viewer img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  text-align: center;
  color: var(--text-color-secondary);
  width: 100%;
  height: 100%;
}

.unsupported i {
  font-size: 3rem;
  margin-bottom: 1rem;
}
</style>
