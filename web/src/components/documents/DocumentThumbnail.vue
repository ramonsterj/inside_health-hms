<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import type { AdmissionDocument } from '@/types/document'
import { useDocumentStore } from '@/stores/document'

const { t } = useI18n()
const documentStore = useDocumentStore()

const props = defineProps<{
  document: AdmissionDocument
  admissionId: number
  canDelete?: boolean
}>()

const emit = defineEmits<{
  view: []
  download: []
  delete: []
}>()

const thumbnailBlobUrl = ref<string | null>(null)
const thumbnailLoading = ref(false)

const hasThumbnail = computed(() => props.document.hasThumbnail && props.document.thumbnailUrl)

onMounted(async () => {
  if (hasThumbnail.value) {
    thumbnailLoading.value = true
    try {
      const blob = await documentStore.getThumbnail(props.admissionId, props.document.id)
      thumbnailBlobUrl.value = URL.createObjectURL(blob)
    } catch {
      // Silently fail - will show placeholder
    } finally {
      thumbnailLoading.value = false
    }
  }
})

onUnmounted(() => {
  if (thumbnailBlobUrl.value) {
    URL.revokeObjectURL(thumbnailBlobUrl.value)
  }
})

const placeholderIcon = computed(() => {
  if (props.document.contentType === 'application/pdf') {
    return 'pi pi-file-pdf'
  }
  return 'pi pi-image'
})

const localizedTypeName = computed(() => {
  const code = props.document.documentType.code
  return t(`document.types.${code}`, props.document.documentType.name)
})

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <div class="document-thumbnail" @click="emit('view')">
    <div class="thumbnail-image">
      <div v-if="thumbnailLoading" class="thumbnail-loading">
        <i class="pi pi-spin pi-spinner"></i>
      </div>
      <img v-else-if="thumbnailBlobUrl" :src="thumbnailBlobUrl" :alt="document.displayName" />
      <div v-else class="thumbnail-placeholder">
        <i :class="placeholderIcon"></i>
      </div>
    </div>

    <div class="thumbnail-info">
      <div class="document-name" :title="document.displayName">
        {{ document.displayName }}
      </div>
      <div class="document-meta">
        <span class="document-type">{{ localizedTypeName }}</span>
        <span class="document-size">{{ formatFileSize(document.fileSize) }}</span>
      </div>
    </div>

    <div class="thumbnail-actions" @click.stop>
      <Button
        icon="pi pi-eye"
        severity="secondary"
        text
        rounded
        size="small"
        @click="emit('view')"
        v-tooltip.top="t('document.view')"
      />
      <Button
        icon="pi pi-download"
        severity="secondary"
        text
        rounded
        size="small"
        @click="emit('download')"
        v-tooltip.top="t('document.download')"
      />
      <Button
        v-if="canDelete"
        icon="pi pi-trash"
        severity="danger"
        text
        rounded
        size="small"
        @click="emit('delete')"
        v-tooltip.top="t('document.delete')"
      />
    </div>
  </div>
</template>

<style scoped>
.document-thumbnail {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--surface-card);
}

.document-thumbnail:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.thumbnail-image {
  height: 120px;
  background: var(--surface-ground);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.thumbnail-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.thumbnail-placeholder,
.thumbnail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-color-secondary);
}

.thumbnail-placeholder i {
  font-size: 2.5rem;
}

.thumbnail-info {
  flex: 1;
  padding: 0.5rem;
  min-height: 3.5rem;
}

.document-name {
  font-weight: 500;
  font-size: 0.875rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 0.25rem;
}

.document-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.thumbnail-actions {
  display: flex;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.375rem;
  border-top: 1px solid var(--surface-border);
}
</style>
