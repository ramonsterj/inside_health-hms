<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import ToggleSwitch from 'primevue/toggleswitch'
import { useBankStatementStore } from '@/stores/bankStatement'
import { StatementFileType } from '@/types/treasury'

const props = defineProps<{
  visible: boolean
  bankAccountId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const store = useBankStatementStore()

const fileTypeOptions = [
  { label: 'XLSX', value: StatementFileType.XLSX },
  { label: 'CSV', value: StatementFileType.CSV }
]

const form = ref({
  fileType: StatementFileType.XLSX as StatementFileType,
  hasHeader: true,
  dateColumn: '',
  descriptionColumn: '',
  referenceColumn: '',
  debitColumn: '',
  creditColumn: '',
  balanceColumn: '',
  dateFormat: 'dd/MM/yyyy',
  skipRows: 0
})

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      await store.fetchColumnMapping(props.bankAccountId)
      if (store.columnMapping) {
        form.value = {
          fileType: store.columnMapping.fileType,
          hasHeader: store.columnMapping.hasHeader,
          dateColumn: store.columnMapping.dateColumn,
          descriptionColumn: store.columnMapping.descriptionColumn || '',
          referenceColumn: store.columnMapping.referenceColumn || '',
          debitColumn: store.columnMapping.debitColumn,
          creditColumn: store.columnMapping.creditColumn,
          balanceColumn: store.columnMapping.balanceColumn || '',
          dateFormat: store.columnMapping.dateFormat,
          skipRows: store.columnMapping.skipRows
        }
      }
    }
  }
)

async function submit() {
  try {
    await store.saveColumnMapping(props.bankAccountId, {
      fileType: form.value.fileType,
      hasHeader: form.value.hasHeader,
      dateColumn: form.value.dateColumn,
      descriptionColumn: form.value.descriptionColumn || undefined,
      referenceColumn: form.value.referenceColumn || undefined,
      debitColumn: form.value.debitColumn,
      creditColumn: form.value.creditColumn,
      balanceColumn: form.value.balanceColumn || undefined,
      dateFormat: form.value.dateFormat,
      skipRows: form.value.skipRows
    })
    showSuccess('treasury.reconciliation.mappingSaved')
    emit('update:visible', false)
    emit('saved')
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    :header="t('treasury.reconciliation.columnMappingTitle')"
    :style="{ width: '550px' }"
    modal
  >
    <div class="mapping-form">
      <div class="form-row">
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.fileType') }}</label>
          <Select
            v-model="form.fileType"
            :options="fileTypeOptions"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.hasHeader') }}</label>
          <ToggleSwitch v-model="form.hasHeader" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.dateColumn') }} *</label>
          <InputText v-model="form.dateColumn" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.dateFormatLabel') }}</label>
          <InputText v-model="form.dateFormat" class="w-full" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.debitColumn') }} *</label>
          <InputText v-model="form.debitColumn" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.creditColumn') }} *</label>
          <InputText v-model="form.creditColumn" class="w-full" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.descriptionColumn') }}</label>
          <InputText v-model="form.descriptionColumn" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.referenceColumn') }}</label>
          <InputText v-model="form.referenceColumn" class="w-full" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.balanceColumn') }}</label>
          <InputText v-model="form.balanceColumn" class="w-full" />
        </div>
        <div class="form-field">
          <label>{{ t('treasury.reconciliation.skipRows') }}</label>
          <InputNumber v-model="form.skipRows" :min="0" class="w-full" />
        </div>
      </div>
    </div>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        severity="secondary"
        @click="emit('update:visible', false)"
      />
      <Button
        :label="t('common.save')"
        :disabled="!form.dateColumn || !form.debitColumn || !form.creditColumn"
        :loading="store.loading"
        @click="submit"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.mapping-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-row .form-field {
  flex: 1;
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
