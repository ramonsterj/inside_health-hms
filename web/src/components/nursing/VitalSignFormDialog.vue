<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import DatePicker from 'primevue/datepicker'
import Textarea from 'primevue/textarea'
import { vitalSignSchema, type VitalSignFormData } from '@/validation/nursing'
import { useVitalSignStore } from '@/stores/vitalSign'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { VitalSignResponse } from '@/types/nursing'

const props = defineProps<{
  admissionId: number
  vitalSignToEdit?: VitalSignResponse | null
}>()

const visible = defineModel<boolean>('visible', { required: true })

const emit = defineEmits<{
  saved: []
}>()

const { t } = useI18n()
const vitalSignStore = useVitalSignStore()
const { showError } = useErrorHandler()

const isEdit = computed(() => !!props.vitalSignToEdit)

const { handleSubmit, resetForm, errors, defineField, meta, setFieldError } =
  useForm<VitalSignFormData>({
    validationSchema: toTypedSchema(vitalSignSchema)
  })

const [recordedAtDate, recordedAtDateAttrs] = defineField('recordedAt')
const [systolicBp, systolicBpAttrs] = defineField('systolicBp')
const [diastolicBp, diastolicBpAttrs] = defineField('diastolicBp')
const [heartRate, heartRateAttrs] = defineField('heartRate')
const [respiratoryRate, respiratoryRateAttrs] = defineField('respiratoryRate')
const [temperature, temperatureAttrs] = defineField('temperature')
const [oxygenSaturation, oxygenSaturationAttrs] = defineField('oxygenSaturation')
const [other, otherAttrs] = defineField('other')

// Local date state for the DatePicker
const localRecordedAt = ref<Date | null>(null)

const saving = ref(false)

watch(visible, newValue => {
  if (newValue) {
    if (props.vitalSignToEdit) {
      // Edit mode: populate form
      const vs = props.vitalSignToEdit
      localRecordedAt.value = vs.recordedAt ? new Date(vs.recordedAt) : null
      resetForm({
        values: {
          recordedAt: vs.recordedAt || '',
          systolicBp: vs.systolicBp,
          diastolicBp: vs.diastolicBp,
          heartRate: vs.heartRate,
          respiratoryRate: vs.respiratoryRate,
          temperature: vs.temperature,
          oxygenSaturation: vs.oxygenSaturation,
          other: vs.other || ''
        }
      })
    } else {
      // Create mode: clear form
      localRecordedAt.value = null
      resetForm({
        values: {
          recordedAt: '',
          systolicBp: undefined as unknown as number,
          diastolicBp: undefined as unknown as number,
          heartRate: undefined as unknown as number,
          respiratoryRate: undefined as unknown as number,
          temperature: undefined as unknown as number,
          oxygenSaturation: undefined as unknown as number,
          other: ''
        }
      })
    }
  }
})

// Sync local date to form field
watch(localRecordedAt, newVal => {
  if (newVal) {
    recordedAtDate.value = newVal.toISOString()
  } else {
    recordedAtDate.value = ''
  }
})

const onSubmit = handleSubmit(async values => {
  // Additional validation: recordedAt cannot be in the future
  if (values.recordedAt) {
    const recordedDate = new Date(values.recordedAt)
    if (recordedDate > new Date()) {
      setFieldError('recordedAt', 'validation.nursing.vitalSign.recordedAt.future')
      return
    }
  }

  saving.value = true
  try {
    const payload = {
      recordedAt: values.recordedAt || null,
      systolicBp: values.systolicBp,
      diastolicBp: values.diastolicBp,
      heartRate: values.heartRate,
      respiratoryRate: values.respiratoryRate,
      temperature: values.temperature,
      oxygenSaturation: values.oxygenSaturation,
      other: values.other || null
    }

    if (isEdit.value && props.vitalSignToEdit) {
      await vitalSignStore.updateVitalSign(props.admissionId, props.vitalSignToEdit.id, payload)
    } else {
      await vitalSignStore.createVitalSign(props.admissionId, payload)
    }
    visible.value = false
    emit('saved')
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
})

function handleCancel() {
  visible.value = false
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    :header="isEdit ? t('nursing.vitalSigns.edit') : t('nursing.vitalSigns.add')"
    modal
    :style="{ width: '60vw' }"
    :breakpoints="{ '960px': '80vw', '640px': '95vw' }"
    :closable="!saving"
    :closeOnEscape="!saving"
  >
    <form @submit.prevent="onSubmit" class="vital-sign-form">
      <!-- Recorded At -->
      <div class="form-field full-width">
        <label for="recordedAt">{{ t('nursing.vitalSigns.fields.recordedAt') }}</label>
        <DatePicker
          id="recordedAt"
          v-model="localRecordedAt"
          v-bind="recordedAtDateAttrs"
          showTime
          hourFormat="24"
          :showIcon="true"
          :maxDate="new Date()"
          :placeholder="t('nursing.vitalSigns.placeholders.recordedAt')"
          :class="{ 'p-invalid': errors.recordedAt }"
        />
        <small class="field-hint">{{ t('nursing.vitalSigns.hints.recordedAt') }}</small>
        <small v-if="errors.recordedAt" class="p-error">
          {{ t(errors.recordedAt) }}
        </small>
      </div>

      <!-- Blood Pressure Row -->
      <div class="form-row">
        <div class="form-field">
          <label for="systolicBp">{{ t('nursing.vitalSigns.fields.systolicBp') }} *</label>
          <div class="input-with-unit">
            <InputNumber
              id="systolicBp"
              v-model="systolicBp"
              v-bind="systolicBpAttrs"
              :min="60"
              :max="250"
              :useGrouping="false"
              :class="{ 'p-invalid': errors.systolicBp }"
            />
            <span class="unit">{{ t('nursing.vitalSigns.units.mmHg') }}</span>
          </div>
          <small v-if="errors.systolicBp" class="p-error">
            {{ t(errors.systolicBp) }}
          </small>
        </div>

        <div class="form-field">
          <label for="diastolicBp">{{ t('nursing.vitalSigns.fields.diastolicBp') }} *</label>
          <div class="input-with-unit">
            <InputNumber
              id="diastolicBp"
              v-model="diastolicBp"
              v-bind="diastolicBpAttrs"
              :min="30"
              :max="150"
              :useGrouping="false"
              :class="{ 'p-invalid': errors.diastolicBp }"
            />
            <span class="unit">{{ t('nursing.vitalSigns.units.mmHg') }}</span>
          </div>
          <small v-if="errors.diastolicBp" class="p-error">
            {{ t(errors.diastolicBp) }}
          </small>
        </div>
      </div>

      <!-- Heart Rate and Respiratory Rate -->
      <div class="form-row">
        <div class="form-field">
          <label for="heartRate">{{ t('nursing.vitalSigns.fields.heartRate') }} *</label>
          <div class="input-with-unit">
            <InputNumber
              id="heartRate"
              v-model="heartRate"
              v-bind="heartRateAttrs"
              :min="20"
              :max="250"
              :useGrouping="false"
              :class="{ 'p-invalid': errors.heartRate }"
            />
            <span class="unit">{{ t('nursing.vitalSigns.units.bpm') }}</span>
          </div>
          <small v-if="errors.heartRate" class="p-error">
            {{ t(errors.heartRate) }}
          </small>
        </div>

        <div class="form-field">
          <label for="respiratoryRate"
            >{{ t('nursing.vitalSigns.fields.respiratoryRate') }} *</label
          >
          <div class="input-with-unit">
            <InputNumber
              id="respiratoryRate"
              v-model="respiratoryRate"
              v-bind="respiratoryRateAttrs"
              :min="5"
              :max="60"
              :useGrouping="false"
              :class="{ 'p-invalid': errors.respiratoryRate }"
            />
            <span class="unit">{{ t('nursing.vitalSigns.units.breathsPerMin') }}</span>
          </div>
          <small v-if="errors.respiratoryRate" class="p-error">
            {{ t(errors.respiratoryRate) }}
          </small>
        </div>
      </div>

      <!-- Temperature and Oxygen Saturation -->
      <div class="form-row">
        <div class="form-field">
          <label for="temperature">{{ t('nursing.vitalSigns.fields.temperature') }} *</label>
          <div class="input-with-unit">
            <InputNumber
              id="temperature"
              v-model="temperature"
              v-bind="temperatureAttrs"
              :min="30.0"
              :max="45.0"
              :minFractionDigits="1"
              :maxFractionDigits="1"
              :step="0.1"
              :useGrouping="false"
              :class="{ 'p-invalid': errors.temperature }"
            />
            <span class="unit">{{ t('nursing.vitalSigns.units.celsius') }}</span>
          </div>
          <small v-if="errors.temperature" class="p-error">
            {{ t(errors.temperature) }}
          </small>
        </div>

        <div class="form-field">
          <label for="oxygenSaturation"
            >{{ t('nursing.vitalSigns.fields.oxygenSaturation') }} *</label
          >
          <div class="input-with-unit">
            <InputNumber
              id="oxygenSaturation"
              v-model="oxygenSaturation"
              v-bind="oxygenSaturationAttrs"
              :min="50"
              :max="100"
              :useGrouping="false"
              :class="{ 'p-invalid': errors.oxygenSaturation }"
            />
            <span class="unit">{{ t('nursing.vitalSigns.units.percent') }}</span>
          </div>
          <small v-if="errors.oxygenSaturation" class="p-error">
            {{ t(errors.oxygenSaturation) }}
          </small>
        </div>
      </div>

      <!-- Other observations -->
      <div class="form-field full-width">
        <label for="other">{{ t('nursing.vitalSigns.fields.other') }}</label>
        <Textarea
          id="other"
          v-model="other"
          v-bind="otherAttrs"
          rows="3"
          :maxlength="1000"
          :placeholder="t('nursing.vitalSigns.placeholders.other')"
          :class="{ 'p-invalid': errors.other }"
          autoResize
        />
        <small v-if="errors.other" class="p-error">
          {{ t(errors.other) }}
        </small>
      </div>
    </form>

    <template #footer>
      <Button
        :label="t('common.cancel')"
        severity="secondary"
        text
        :disabled="saving"
        @click="handleCancel"
      />
      <Button
        :label="t('common.save')"
        icon="pi pi-check"
        :loading="saving"
        :disabled="!meta.valid"
        @click="onSubmit"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.vital-sign-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-field.full-width {
  grid-column: 1 / -1;
}

.form-field label {
  font-weight: 600;
  font-size: 0.9rem;
}

.input-with-unit {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.input-with-unit :deep(.p-inputnumber) {
  flex: 1;
}

.unit {
  font-size: 0.9rem;
  color: var(--p-text-muted-color);
  white-space: nowrap;
}

.field-hint {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
}

:deep(.p-textarea) {
  width: 100%;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
