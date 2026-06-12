<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import FileViewerDialog from '@/components/viewer/FileViewerDialog.vue'
import { useDocumentStore } from '@/stores/document'

const { t } = useI18n()
const documentStore = useDocumentStore()

const viewerDocument = computed(() => documentStore.viewerDocument)

const visible = computed({
  get: () => !!documentStore.viewerDocument,
  set: (value: boolean) => {
    if (!value) {
      documentStore.clearViewerDocument()
    }
  }
})

function fetchBlob(): Promise<Blob> {
  const admissionId = documentStore.viewerAdmissionId
  const doc = documentStore.viewerDocument
  if (!admissionId || !doc) {
    return Promise.reject(new Error('No document selected'))
  }
  return documentStore.downloadDocument(admissionId, doc.id)
}
</script>

<template>
  <FileViewerDialog
    v-model:visible="visible"
    :title="viewerDocument?.displayName || t('document.viewer.title')"
    :contentType="viewerDocument?.contentType"
    :downloadFileName="viewerDocument?.displayName"
    :fetchBlob="fetchBlob"
  />
</template>
