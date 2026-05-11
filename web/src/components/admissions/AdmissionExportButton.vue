<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import { isAxiosError } from 'axios'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'

const props = defineProps<{ admissionId: number }>()

const { t } = useI18n()
const authStore = useAuthStore()
const { showError } = useErrorHandler()

const loading = ref(false)
const canExport = computed(() => authStore.hasPermission('admission:export-pdf'))

const FILENAME_REGEX = /filename="?([^"]+)"?/i

async function decodeBlobErrorBody(error: unknown): Promise<unknown> {
  if (!isAxiosError(error) || !(error.response?.data instanceof Blob)) {
    return error
  }
  try {
    const text = await error.response.data.text()
    error.response.data = text ? JSON.parse(text) : null
  } catch {
    error.response.data = null
  }
  return error
}

async function exportPdf() {
  loading.value = true
  try {
    const response = await api.get(`/v1/admissions/${props.admissionId}/export.pdf`, {
      responseType: 'blob'
    })

    const disposition = response.headers?.['content-disposition'] || ''
    const filenameMatch = FILENAME_REGEX.exec(disposition)
    const filename = filenameMatch?.[1] || `admission-${props.admissionId}.pdf`

    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)

    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    anchor.style.display = 'none'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)

    setTimeout(() => window.URL.revokeObjectURL(url), 5000)
  } catch (error) {
    showError(await decodeBlobErrorBody(error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Button
    v-if="canExport"
    icon="pi pi-file-pdf"
    :label="t('admission.export.button.label')"
    severity="secondary"
    :loading="loading"
    data-testid="admission-export-button"
    @click="exportPdf"
  />
</template>
