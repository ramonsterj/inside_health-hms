<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { updateDoctorFeeStatusSchema, type UpdateDoctorFeeStatusFormData } from '@/validation/treasury'
import { useDoctorFeeStore } from '@/stores/doctorFee'
import { DoctorFeeStatus } from '@/types/treasury'
import type { DoctorFee } from '@/types/treasury'

const props = defineProps<{
  visible: boolean
  fee: DoctorFee
  employeeId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const doctorFeeStore = useDoctorFeeStore()
const submitLoading = ref(false)
const invoiceFile = ref<File | null>(null)

const { defineField, handleSubmit, errors, resetForm } =
  useForm<UpdateDoctorFeeStatusFormData>({
    validationSchema: toTypedSchema(updateDoctorFeeStatusSchema),
    initialValues: {
      doctorInvoiceNumber: ''
    }
  })

const [doctorInvoiceNumber] = defineField('doctorInvoiceNumber')

function onShow() {
  resetForm({ values: { doctorInvoiceNumber: '' } })
  invoiceFile.value = null
}

function onHide() {
  emit('update:visible', false)
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  invoiceFile.value = input.files?.[0] ?? null
}

const submitForm = handleSubmit(async formValues => {
  submitLoading.value = true
  try {
    // Update status to INVOICED first (has validation — fail fast)
    await doctorFeeStore.updateDoctorFeeStatus(props.employeeId, props.fee.id, {
      status: DoctorFeeStatus.INVOICED,
      doctorInvoiceNumber: formValues.doctorInvoiceNumber
    })

    // Upload document after status is confirmed (UI handles missing doc on INVOICED fees)
    if (invoiceFile.value) {
      await doctorFeeStore.uploadInvoiceDocument(
        props.employeeId,
        props.fee.id,
        invoiceFile.value
      )
    }

    showSuccess('treasury.doctorFee.invoiced')
    emit('update:visible', false)
    emit('saved')
  } catch (error) {
    showError(error)
  } finally {
    submitLoading.value = false
  }
})
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="onHide"
    :header="t('treasury.doctorFee.submitInvoice')"
    modal
    :style="{ width: '440px' }"
    @show="onShow"
  >
    <form @submit.prevent="submitForm" class="invoice-form">
      <div class="form-field">
        <label>{{ t('treasury.doctorFee.doctorInvoiceNumber') }} *</label>
        <InputText
          v-model="doctorInvoiceNumber"
          :maxlength="100"
          :class="{ 'p-invalid': errors.doctorInvoiceNumber }"
        />
        <Message v-if="errors.doctorInvoiceNumber" severity="error" :closable="false">
          {{ errors.doctorInvoiceNumber }}
        </Message>
      </div>

      <div class="form-field">
        <label>{{ t('treasury.doctorFee.invoiceDocument') }}</label>
        <input type="file" accept=".pdf,.jpg,.jpeg,.png" @change="onFileChange" />
        <small class="file-hint">{{ t('treasury.doctorFee.invoiceDocumentHint') }}</small>
      </div>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="onHide"
        />
        <Button type="submit" :label="t('treasury.doctorFee.submitInvoice')" :loading="submitLoading" />
      </div>
    </form>
  </Dialog>
</template>

<style scoped>
.invoice-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-field label {
  font-weight: 500;
  font-size: 0.9rem;
}

.file-hint {
  color: var(--p-text-muted-color);
  font-size: 0.8rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
