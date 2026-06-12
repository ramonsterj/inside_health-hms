<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import ImageViewerPane from './ImageViewerPane.vue'
import PdfViewerPane from './PdfViewerPane.vue'
import { useBlobObjectUrl } from '@/composables/useBlobObjectUrl'
import { useErrorHandler } from '@/composables/useErrorHandler'

const props = defineProps<{
  visible: boolean
  /** Dialog header; also the download filename when downloadFileName is not set. */
  title: string
  /** Known content type; falls back to the fetched blob's own type. */
  contentType?: string | null
  /** Called when the dialog opens; must return the document bytes (authenticated axios blob). */
  fetchBlob: () => Promise<Blob>
  downloadFileName?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()

const loading = ref(false)
const blob = ref<Blob | null>(null)
const { url: imageUrl, setBlob: setImageBlob } = useBlobObjectUrl()

const effectiveContentType = computed(() => props.contentType || blob.value?.type || '')
const isImage = computed(() => effectiveContentType.value.startsWith('image/'))
const isPdf = computed(() => effectiveContentType.value === 'application/pdf')

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})

watch(
  () => props.visible,
  open => {
    if (open) {
      load()
    } else {
      reset()
    }
  },
  { immediate: true }
)

async function load() {
  reset()
  loading.value = true
  try {
    blob.value = await props.fetchBlob()
    if (isImage.value) {
      setImageBlob(blob.value)
    }
  } catch (error) {
    showError(error)
    dialogVisible.value = false
  } finally {
    loading.value = false
  }
}

function reset() {
  blob.value = null
  setImageBlob(null)
}

function download() {
  if (!blob.value) return

  const url = URL.createObjectURL(blob.value)
  const link = window.document.createElement('a')
  link.href = url
  link.download = props.downloadFileName || props.title
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <Dialog
    v-model:visible="dialogVisible"
    modal
    :header="title"
    :style="{ width: '90vw', maxWidth: '1200px' }"
    :contentStyle="{ height: '80vh', padding: 0 }"
    :pt="{ footer: { style: 'padding: 1rem 1.5rem' } }"
  >
    <div class="viewer-content">
      <div v-if="loading" class="loading-container">
        <ProgressSpinner />
      </div>

      <template v-else-if="blob">
        <ImageViewerPane v-if="isImage && imageUrl" :src="imageUrl" :alt="title" />

        <PdfViewerPane v-else-if="isPdf" :blob="blob" />

        <div v-else class="unsupported">
          <i class="pi pi-file"></i>
          <p>{{ t('document.unsupportedType') }}</p>
          <Button :label="t('document.download')" icon="pi pi-download" @click="download" />
        </div>
      </template>
    </div>

    <template #footer>
      <Button
        icon="pi pi-download"
        :label="t('document.download')"
        severity="secondary"
        outlined
        :disabled="!blob"
        @click="download"
      />
      <Button :label="t('common.close')" severity="secondary" @click="dialogVisible = false" />
    </template>
  </Dialog>
</template>

<style scoped>
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
