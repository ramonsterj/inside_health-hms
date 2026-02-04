<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import FileUpload, { type FileUploadUploaderEvent } from 'primevue/fileupload'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Message from 'primevue/message'
import { useDocumentStore } from '@/stores/document'
import { useDocumentTypeStore } from '@/stores/documentType'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { uploadDocumentSchema, type UploadDocumentFormData } from '@/validation/document'

const { t } = useI18n()
const { showError, showSuccess, setFieldErrorsFromResponse } = useErrorHandler()
const documentStore = useDocumentStore()
const documentTypeStore = useDocumentTypeStore()

const props = defineProps<{
  admissionId: number
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  uploaded: []
}>()

const selectedFile = ref<File | null>(null)

const { defineField, handleSubmit, errors, resetForm, setErrors, setFieldValue } =
  useForm<UploadDocumentFormData>({
    validationSchema: toTypedSchema(uploadDocumentSchema),
    initialValues: {
      file: undefined as unknown as File,
      documentTypeId: 0,
      displayName: undefined
    }
  })

const [documentTypeId] = defineField('documentTypeId')
const [displayName] = defineField('displayName')

const documentTypeOptions = computed(() =>
  documentTypeStore.documentTypeSummaries.map(dt => ({
    label: t(`document.types.${dt.code}`, dt.name),
    value: dt.id
  }))
)

const isVisible = computed({
  get: () => props.visible,
  set: value => emit('update:visible', value)
})

onMounted(async () => {
  if (documentTypeStore.documentTypeSummaries.length === 0) {
    try {
      await documentTypeStore.fetchDocumentTypeSummaries()
    } catch (error) {
      showError(error)
    }
  }
})

function onFileSelect(event: FileUploadUploaderEvent) {
  const files = Array.isArray(event.files) ? event.files : [event.files]
  const file = files[0]
  if (file) {
    selectedFile.value = file
    setFieldValue('file', file)
  }
}

function clearFile() {
  selectedFile.value = null
  setFieldValue('file', undefined as unknown as File)
}

const onSubmit = handleSubmit(async values => {
  try {
    await documentStore.uploadDocument(
      props.admissionId,
      values.file,
      values.documentTypeId,
      values.displayName || undefined
    )
    showSuccess('document.uploaded')
    closeDialog()
    emit('uploaded')
  } catch (error) {
    if (!setFieldErrorsFromResponse(setErrors, error)) {
      showError(error)
    }
  }
})

function closeDialog() {
  isVisible.value = false
  resetForm()
  selectedFile.value = null
}
</script>

<template>
  <Dialog
    v-model:visible="isVisible"
    :header="t('document.uploadTitle')"
    modal
    :style="{ width: '500px' }"
    @hide="closeDialog"
  >
    <form @submit="onSubmit" class="upload-form">
      <div class="form-field">
        <label>{{ t('document.file') }} *</label>
        <FileUpload
          v-if="!selectedFile"
          name="document"
          accept=".pdf,.jpg,.jpeg,.png"
          :maxFileSize="25 * 1024 * 1024"
          :auto="true"
          customUpload
          @uploader="onFileSelect"
          :class="{ 'p-invalid': errors.file }"
          :pt="{
            root: { class: 'upload-dropzone' },
            content: { class: 'upload-dropzone-content' }
          }"
        >
          <template #empty>
            <div class="dropzone-empty">
              <i class="pi pi-cloud-upload dropzone-icon" />
              <p class="dropzone-text">{{ t('document.dragDropFile') }}</p>
              <p class="dropzone-hint">{{ t('document.fileHint') }}</p>
            </div>
          </template>
        </FileUpload>
        <div v-else class="selected-file">
          <div class="file-info">
            <i class="pi pi-file"></i>
            <span>{{ selectedFile.name }}</span>
          </div>
          <Button
            icon="pi pi-times"
            severity="secondary"
            text
            rounded
            size="small"
            @click="clearFile"
          />
        </div>
        <Message v-if="errors.file" severity="error" :closable="false">
          {{ errors.file }}
        </Message>
      </div>

      <div class="form-field">
        <label for="documentTypeId">{{ t('document.type') }} *</label>
        <Select
          id="documentTypeId"
          v-model="documentTypeId"
          :options="documentTypeOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('document.selectType')"
          :class="{ 'p-invalid': errors.documentTypeId }"
        />
        <Message v-if="errors.documentTypeId" severity="error" :closable="false">
          {{ errors.documentTypeId }}
        </Message>
      </div>

      <div class="form-field">
        <label for="displayName">{{ t('document.displayName') }}</label>
        <InputText
          id="displayName"
          v-model="displayName"
          :placeholder="t('document.displayNamePlaceholder')"
          :class="{ 'p-invalid': errors.displayName }"
        />
        <small class="field-help">{{ t('document.displayNameHelp') }}</small>
        <Message v-if="errors.displayName" severity="error" :closable="false">
          {{ errors.displayName }}
        </Message>
      </div>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="closeDialog"
        />
        <Button
          type="submit"
          :label="t('document.upload')"
          icon="pi pi-upload"
          :loading="documentStore.uploading"
        />
      </div>
    </form>
  </Dialog>
</template>

<style scoped>
.upload-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 500;
}

.selected-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--surface-ground);
  border-radius: 6px;
  border: 1px solid var(--surface-border);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  overflow: hidden;
}

.file-info span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.field-help {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.dropzone-empty {
  padding: 2rem;
  text-align: center;
}

.dropzone-icon {
  font-size: 3rem;
  color: var(--p-primary-color);
  margin-bottom: 1rem;
}

.dropzone-text {
  font-size: 1rem;
  font-weight: 500;
  margin: 0 0 0.5rem 0;
}

.dropzone-hint {
  font-size: 0.875rem;
  color: var(--p-text-muted-color);
  margin: 0;
}

:deep(.upload-dropzone) {
  border: 2px dashed var(--p-surface-border);
  border-radius: 8px;
  background: var(--p-surface-ground);
  transition:
    border-color 0.2s,
    background-color 0.2s;
}

:deep(.upload-dropzone:hover),
:deep(.upload-dropzone.p-fileupload-highlight) {
  border-color: var(--p-primary-color);
  background: var(--p-primary-50);
}

:deep(.upload-dropzone-content) {
  padding: 0;
}
</style>
