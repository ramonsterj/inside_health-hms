<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import FileUpload from 'primevue/fileupload'
import { useBankStatementStore } from '@/stores/bankStatement'
import { toApiDate } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  bankAccountId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  uploaded: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const store = useBankStatementStore()

const statementDate = ref<Date | null>(null)
const selectedFile = ref<File | null>(null)

function onFileSelect(event: { files: File[] }) {
  selectedFile.value = event.files[0] || null
}

function onFileClear() {
  selectedFile.value = null
}

async function submit() {
  if (!selectedFile.value || !statementDate.value) return
  try {
    await store.uploadStatement(
      props.bankAccountId,
      selectedFile.value,
      toApiDate(statementDate.value)!
    )
    showSuccess('treasury.reconciliation.uploaded')
    close()
    emit('uploaded')
  } catch (error) {
    showError(error)
  }
}

function close() {
  emit('update:visible', false)
  statementDate.value = null
  selectedFile.value = null
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    :header="t('treasury.reconciliation.uploadTitle')"
    :style="{ width: '450px' }"
    modal
  >
    <div class="upload-form">
      <div class="form-field">
        <label>{{ t('treasury.reconciliation.statementDate') }}</label>
        <DatePicker v-model="statementDate" class="w-full" />
      </div>
      <div class="form-field">
        <label>{{ t('treasury.reconciliation.file') }}</label>
        <FileUpload
          mode="basic"
          accept=".xlsx,.csv"
          :max-file-size="10000000"
          :auto="false"
          choose-label="Choose File"
          @select="onFileSelect"
          @clear="onFileClear"
        />
      </div>
    </div>
    <template #footer>
      <Button :label="t('common.cancel')" severity="secondary" @click="close" />
      <Button
        :label="t('treasury.reconciliation.upload')"
        :disabled="!selectedFile || !statementDate"
        :loading="store.loading"
        @click="submit"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.upload-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.w-full {
  width: 100%;
}
</style>
