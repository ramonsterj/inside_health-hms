<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { createDoctorFeeSchema, type CreateDoctorFeeFormData } from '@/validation/treasury'
import { useDoctorFeeStore } from '@/stores/doctorFee'
import { DoctorFeeBillingType } from '@/types/treasury'
import type { TreasuryEmployee } from '@/types/treasury'
import { formatCurrency, toApiDate } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  employee: TreasuryEmployee
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const doctorFeeStore = useDoctorFeeStore()
const submitLoading = ref(false)
const localFeeDate = ref<Date | null>(new Date())

const { defineField, handleSubmit, errors, resetForm } =
  useForm<CreateDoctorFeeFormData>({
    validationSchema: toTypedSchema(createDoctorFeeSchema),
    initialValues: {
      billingType: undefined as unknown as DoctorFeeBillingType,
      grossAmount: undefined as unknown as number,
      commissionPct: props.employee.hospitalCommissionPct,
      feeDate: toApiDate(new Date()),
      patientChargeId: null,
      description: '',
      notes: ''
    }
  })

const [billingType] = defineField('billingType')
const [grossAmount] = defineField('grossAmount')
const [commissionPct] = defineField('commissionPct')
const [feeDate] = defineField('feeDate')
const [patientChargeId] = defineField('patientChargeId')
const [description] = defineField('description')
const [notes] = defineField('notes')

const billingTypeOptions = computed(() =>
  Object.values(DoctorFeeBillingType).map(v => ({
    label: t(`treasury.doctorFee.billingTypes.${v}`),
    value: v
  }))
)

const computedNetAmount = computed(() => {
  if (!grossAmount.value || grossAmount.value <= 0) return null
  const pct = commissionPct.value ?? 0
  const net = grossAmount.value - (grossAmount.value * pct) / 100
  return Math.max(0, net)
})

watch(localFeeDate, val => {
  feeDate.value = val ? toApiDate(val as Date) : ''
})

function onShow() {
  resetForm({
    values: {
      billingType: undefined as unknown as DoctorFeeBillingType,
      grossAmount: undefined as unknown as number,
      commissionPct: props.employee.hospitalCommissionPct,
      feeDate: toApiDate(new Date()),
      patientChargeId: null,
      description: '',
      notes: ''
    }
  })
  localFeeDate.value = new Date()
}

function onHide() {
  emit('update:visible', false)
}

const submitForm = handleSubmit(async formValues => {
  submitLoading.value = true
  try {
    await doctorFeeStore.createDoctorFee(props.employee.id, {
      billingType: formValues.billingType,
      grossAmount: formValues.grossAmount,
      commissionPct: formValues.commissionPct ?? null,
      feeDate: formValues.feeDate,
      patientChargeId: formValues.patientChargeId ?? null,
      description: formValues.description || null,
      notes: formValues.notes || null
    })
    showSuccess('treasury.doctorFee.created')
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
    :header="t('treasury.doctorFee.new')"
    modal
    :style="{ width: '520px' }"
    @show="onShow"
  >
    <form @submit.prevent="submitForm" class="doctor-fee-form">
      <div class="form-row">
        <div class="form-field">
          <label>{{ t('treasury.doctorFee.billingType') }} *</label>
          <Select
            v-model="billingType"
            :options="billingTypeOptions"
            option-label="label"
            option-value="value"
            :class="{ 'p-invalid': errors.billingType }"
          />
          <Message v-if="errors.billingType" severity="error" :closable="false">
            {{ errors.billingType }}
          </Message>
        </div>
        <div class="form-field">
          <label>{{ t('treasury.doctorFee.feeDate') }} *</label>
          <DatePicker
            v-model="localFeeDate"
            :class="{ 'p-invalid': errors.feeDate }"
          />
          <Message v-if="errors.feeDate" severity="error" :closable="false">
            {{ errors.feeDate }}
          </Message>
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label>{{ t('treasury.doctorFee.grossAmount') }} *</label>
          <InputNumber
            v-model="grossAmount"
            :min="0.01"
            :max-fraction-digits="2"
            :class="{ 'p-invalid': errors.grossAmount }"
          />
          <Message v-if="errors.grossAmount" severity="error" :closable="false">
            {{ errors.grossAmount }}
          </Message>
        </div>
        <div class="form-field">
          <label>{{ t('treasury.doctorFee.commissionPct') }}</label>
          <InputNumber
            v-model="commissionPct"
            :min="0"
            :max="100"
            :max-fraction-digits="2"
            suffix="%"
          />
        </div>
      </div>

      <div v-if="computedNetAmount !== null" class="net-amount-preview">
        <span class="net-label">{{ t('treasury.doctorFee.netAmount') }}:</span>
        <span class="net-value">{{ formatCurrency(computedNetAmount) }}</span>
      </div>

      <div class="form-field">
        <label>{{ t('treasury.doctorFee.patientChargeId') }}</label>
        <InputNumber v-model="patientChargeId" :min="1" :use-grouping="false" />
      </div>

      <div class="form-field">
        <label>{{ t('treasury.doctorFee.description') }}</label>
        <InputText v-model="description" :maxlength="500" />
      </div>

      <div class="form-field">
        <label>{{ t('treasury.income.notes') }}</label>
        <Textarea v-model="notes" rows="2" />
      </div>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="onHide"
        />
        <Button type="submit" :label="t('common.save')" :loading="submitLoading" />
      </div>
    </form>
  </Dialog>
</template>

<style scoped>
.doctor-fee-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
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

.net-amount-preview {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--p-surface-100);
  border-radius: var(--p-border-radius);
}

.net-label {
  font-weight: 500;
  font-size: 0.9rem;
}

.net-value {
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--p-primary-color);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
