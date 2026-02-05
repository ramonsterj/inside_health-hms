<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import { medicalOrderSchema, type MedicalOrderFormData } from '@/validation/medicalRecord'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useFormDateField } from '@/composables/useFormDateField'
import { MedicalOrderCategory, AdministrationRoute } from '@/types/medicalRecord'
import type { MedicalOrderResponse } from '@/types/medicalRecord'
import RichTextEditor from '@/components/common/RichTextEditor.vue'

const props = defineProps<{
  visible: boolean
  admissionId: number
  order?: MedicalOrderResponse | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()
const medicalOrderStore = useMedicalOrderStore()

const loading = ref(false)
const isEditMode = computed(() => !!props.order)

// Category options
const categoryOptions = computed(() =>
  Object.values(MedicalOrderCategory).map(cat => ({
    label: t(`medicalRecord.medicalOrder.categories.${cat}`),
    value: cat
  }))
)

// Route options
const routeOptions = computed(() => [
  { label: '-', value: null },
  ...Object.values(AdministrationRoute).map(route => ({
    label: t(`medicalRecord.medicalOrder.routes.${route}`),
    value: route
  }))
])

const { defineField, handleSubmit, errors, values, setValues, resetForm } =
  useForm<MedicalOrderFormData>({
    validationSchema: toTypedSchema(medicalOrderSchema),
    initialValues: {
      category: MedicalOrderCategory.ORDENES_MEDICAS,
      startDate: new Date().toISOString().split('T')[0],
      endDate: '',
      medication: '',
      dosage: '',
      route: null,
      frequency: '',
      schedule: '',
      observations: ''
    }
  })

const [category] = defineField('category')
const [startDate] = defineField('startDate')
const [endDate] = defineField('endDate')
const [medication] = defineField('medication')
const [dosage] = defineField('dosage')
const [route] = defineField('route')
const [frequency] = defineField('frequency')
const [schedule] = defineField('schedule')
const [observations] = defineField('observations')

// Show medication fields when MEDICAMENTOS category is selected
const showMedicationFields = computed(() => values.category === MedicalOrderCategory.MEDICAMENTOS)

// Use composable for type-safe Date ↔ string synchronization
const startDatePicker = useFormDateField(startDate)
const endDatePicker = useFormDateField(endDate)

watch(
  () => props.visible,
  newValue => {
    if (newValue) {
      if (props.order) {
        setValues({
          category: props.order.category,
          startDate: props.order.startDate,
          endDate: props.order.endDate || '',
          medication: props.order.medication || '',
          dosage: props.order.dosage || '',
          route: props.order.route || null,
          frequency: props.order.frequency || '',
          schedule: props.order.schedule || '',
          observations: props.order.observations || ''
        })
      } else {
        resetForm()
      }
    }
  }
)

const onSubmit = handleSubmit(async formValues => {
  loading.value = true
  try {
    // Convert empty strings to null for API
    const data = {
      category: formValues.category,
      startDate: formValues.startDate,
      endDate: formValues.endDate || null,
      medication: formValues.medication || null,
      dosage: formValues.dosage || null,
      route: formValues.route || null,
      frequency: formValues.frequency || null,
      schedule: formValues.schedule || null,
      observations: formValues.observations || null
    }

    if (isEditMode.value && props.order) {
      await medicalOrderStore.updateMedicalOrder(props.admissionId, props.order.id, data)
    } else {
      await medicalOrderStore.createMedicalOrder(props.admissionId, data)
    }
    emit('saved')
    closeDialog()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function closeDialog() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="
      isEditMode
        ? t('medicalRecord.medicalOrder.edit')
        : t('medicalRecord.medicalOrder.add')
    "
    :modal="true"
    :closable="!loading"
    :style="{ width: '600px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form @submit="onSubmit" class="medical-order-form">
      <!-- Category -->
      <div class="form-field">
        <label for="category">{{ t('medicalRecord.medicalOrder.fields.category') }} *</label>
        <Select
          id="category"
          v-model="category"
          :options="categoryOptions"
          optionLabel="label"
          optionValue="value"
          :class="{ 'p-invalid': errors.category }"
          class="w-full"
        />
        <Message v-if="errors.category" severity="error" :closable="false">
          {{ errors.category }}
        </Message>
      </div>

      <!-- Dates Row -->
      <div class="form-row">
        <div class="form-field">
          <label for="startDate">{{ t('medicalRecord.medicalOrder.fields.startDate') }} *</label>
          <DatePicker
            id="startDate"
            v-model="startDatePicker"
            dateFormat="yy-mm-dd"
            :class="{ 'p-invalid': errors.startDate }"
            class="w-full"
          />
          <Message v-if="errors.startDate" severity="error" :closable="false">
            {{ errors.startDate }}
          </Message>
        </div>
        <div class="form-field">
          <label for="endDate">{{ t('medicalRecord.medicalOrder.fields.endDate') }}</label>
          <DatePicker
            id="endDate"
            v-model="endDatePicker"
            dateFormat="yy-mm-dd"
            :minDate="startDatePicker || undefined"
            class="w-full"
          />
        </div>
      </div>

      <!-- Medication Fields (shown for MEDICAMENTOS category) -->
      <div v-if="showMedicationFields" class="medication-fields">
        <div class="form-field">
          <label for="medication">{{ t('medicalRecord.medicalOrder.fields.medication') }}</label>
          <InputText
            id="medication"
            v-model="medication"
            :placeholder="t('medicalRecord.medicalOrder.placeholders.medication')"
            :class="{ 'p-invalid': errors.medication }"
            class="w-full"
          />
          <Message v-if="errors.medication" severity="error" :closable="false">
            {{ errors.medication }}
          </Message>
        </div>

        <div class="form-row">
          <div class="form-field">
            <label for="dosage">{{ t('medicalRecord.medicalOrder.fields.dosage') }}</label>
            <InputText
              id="dosage"
              v-model="dosage"
              :placeholder="t('medicalRecord.medicalOrder.placeholders.dosage')"
              :class="{ 'p-invalid': errors.dosage }"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="route">{{ t('medicalRecord.medicalOrder.fields.route') }}</label>
            <Select
              id="route"
              v-model="route"
              :options="routeOptions"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-field">
            <label for="frequency">{{ t('medicalRecord.medicalOrder.fields.frequency') }}</label>
            <InputText
              id="frequency"
              v-model="frequency"
              :placeholder="t('medicalRecord.medicalOrder.placeholders.frequency')"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="schedule">{{ t('medicalRecord.medicalOrder.fields.schedule') }}</label>
            <InputText
              id="schedule"
              v-model="schedule"
              :placeholder="t('medicalRecord.medicalOrder.placeholders.schedule')"
              class="w-full"
            />
          </div>
        </div>
      </div>

      <!-- Schedule (for non-medication orders) -->
      <div v-else class="form-field">
        <label for="schedule">{{ t('medicalRecord.medicalOrder.fields.schedule') }}</label>
        <InputText
          id="schedule"
          v-model="schedule"
          :placeholder="t('medicalRecord.medicalOrder.placeholders.schedule')"
          class="w-full"
        />
      </div>

      <!-- Observations -->
      <div class="form-field">
        <RichTextEditor
          v-model="observations"
          :label="t('medicalRecord.medicalOrder.fields.observations')"
          :placeholder="t('medicalRecord.medicalOrder.placeholders.observations')"
          :rows="3"
        />
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
.medical-order-form {
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
  color: var(--p-text-color);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.medication-fields {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
