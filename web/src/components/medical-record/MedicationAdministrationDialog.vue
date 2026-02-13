<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { useConfirm } from 'primevue/useconfirm'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import {
  createMedicationAdministrationSchema,
  type MedicationAdministrationFormData
} from '@/validation/medicationAdministration'
import { useMedicationAdministrationStore } from '@/stores/medicationAdministration'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { AdministrationStatus } from '@/types/medicationAdministration'

const props = defineProps<{
  visible: boolean
  admissionId: number
  orderId: number
  medicationName?: string | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const confirm = useConfirm()
const { showError } = useErrorHandler()
const administrationStore = useMedicationAdministrationStore()

const loading = ref(false)

const statusOptions = [
  { label: t('medicationAdministration.statuses.GIVEN'), value: AdministrationStatus.GIVEN },
  { label: t('medicationAdministration.statuses.MISSED'), value: AdministrationStatus.MISSED },
  { label: t('medicationAdministration.statuses.REFUSED'), value: AdministrationStatus.REFUSED },
  { label: t('medicationAdministration.statuses.HELD'), value: AdministrationStatus.HELD }
]

const { defineField, handleSubmit, errors, resetForm } =
  useForm<MedicationAdministrationFormData>({
    validationSchema: toTypedSchema(createMedicationAdministrationSchema),
    initialValues: {
      status: AdministrationStatus.GIVEN,
      notes: ''
    }
  })

const [status] = defineField('status')
const [notes] = defineField('notes')

watch(
  () => props.visible,
  newValue => {
    if (newValue) {
      resetForm()
    }
  }
)

const onSubmit = handleSubmit(async formValues => {
  // If status is GIVEN, show confirmation dialog
  if (formValues.status === AdministrationStatus.GIVEN) {
    confirm.require({
      message: t('medicationAdministration.confirmGiven'),
      header: t('common.confirm'),
      icon: 'pi pi-exclamation-triangle',
      acceptClass: 'p-button-success',
      accept: () => submitAdministration(formValues)
    })
  } else {
    await submitAdministration(formValues)
  }
})

async function submitAdministration(formValues: MedicationAdministrationFormData) {
  loading.value = true
  try {
    const data = {
      status: formValues.status,
      notes: formValues.notes || undefined
    }

    await administrationStore.createAdministration(props.admissionId, props.orderId, data)
    emit('saved')
    closeDialog()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function closeDialog() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="t('medicationAdministration.administer')"
    :modal="true"
    :closable="!loading"
    :style="{ width: '500px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form @submit="onSubmit" class="administration-form">
      <div v-if="medicationName" class="medication-info">
        <strong>{{ medicationName }}</strong>
      </div>

      <!-- Status -->
      <div class="form-field">
        <label for="status">{{ t('medicationAdministration.status') }} *</label>
        <Select
          id="status"
          v-model="status"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          :class="{ 'p-invalid': errors.status }"
          class="w-full"
        />
        <Message v-if="errors.status" severity="error" :closable="false">
          {{ errors.status }}
        </Message>
      </div>

      <!-- Notes -->
      <div class="form-field">
        <label for="notes">{{ t('medicationAdministration.notes') }}</label>
        <Textarea
          id="notes"
          v-model="notes"
          rows="4"
          :class="{ 'p-invalid': errors.notes }"
          class="w-full"
        />
        <Message v-if="errors.notes" severity="error" :closable="false">
          {{ errors.notes }}
        </Message>
      </div>
    </form>

    <template #footer>
      <div class="dialog-footer">
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          :disabled="loading"
          @click="closeDialog"
        />
        <Button :label="t('common.save')" :loading="loading" @click="onSubmit" />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.administration-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.medication-info {
  padding: 0.75rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
  border-left: 4px solid var(--p-primary-color);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 500;
  color: var(--p-text-color);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
