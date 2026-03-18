<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { settleDoctorFeeSchema, type SettleDoctorFeeFormData } from '@/validation/treasury'
import { useDoctorFeeStore } from '@/stores/doctorFee'
import { useBankAccountStore } from '@/stores/bankAccount'
import type { DoctorFee } from '@/types/treasury'
import { formatCurrency } from '@/utils/format'

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
const bankAccountStore = useBankAccountStore()
const submitLoading = ref(false)
const localPaymentDate = ref<Date | null>(new Date())

const { defineField, handleSubmit, errors, resetForm } = useForm<SettleDoctorFeeFormData>({
  validationSchema: toTypedSchema(settleDoctorFeeSchema),
  initialValues: {
    bankAccountId: undefined as unknown as number,
    paymentDate: new Date().toISOString().substring(0, 10),
    notes: ''
  }
})

const [bankAccountId] = defineField('bankAccountId')
const [paymentDate] = defineField('paymentDate')
const [notes] = defineField('notes')

const bankAccountOptions = computed(() =>
  bankAccountStore.activeBankAccounts.map(a => ({
    label: a.name,
    value: a.id
  }))
)

watch(localPaymentDate, val => {
  paymentDate.value = val ? (val as Date).toISOString().substring(0, 10) : ''
})

function onShow() {
  resetForm({
    values: {
      bankAccountId: undefined as unknown as number,
      paymentDate: new Date().toISOString().substring(0, 10),
      notes: ''
    }
  })
  localPaymentDate.value = new Date()
  bankAccountStore.fetchActiveBankAccounts()
}

function onHide() {
  emit('update:visible', false)
}

const submitForm = handleSubmit(async formValues => {
  submitLoading.value = true
  try {
    await doctorFeeStore.settleDoctorFee(props.employeeId, props.fee.id, {
      bankAccountId: formValues.bankAccountId,
      paymentDate: formValues.paymentDate,
      notes: formValues.notes || null
    })
    showSuccess('treasury.doctorFee.settled')
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
    :header="t('treasury.doctorFee.settle')"
    modal
    :style="{ width: '480px' }"
    @show="onShow"
  >
    <div class="settle-form">
      <!-- Fee Summary -->
      <div class="fee-summary">
        <div class="summary-row">
          <span>{{ t('treasury.doctorFee.grossAmount') }}</span>
          <span>{{ formatCurrency(fee.grossAmount) }}</span>
        </div>
        <div class="summary-row">
          <span>{{ t('treasury.doctorFee.commissionPct') }}</span>
          <span>{{ fee.commissionPct }}%</span>
        </div>
        <div class="summary-row net-row">
          <span>{{ t('treasury.doctorFee.netAmount') }}</span>
          <span class="net-value">{{ formatCurrency(fee.netAmount) }}</span>
        </div>
      </div>

      <form @submit.prevent="submitForm" class="form-fields">
        <div class="form-field">
          <label>{{ t('treasury.payment.bankAccount') }} *</label>
          <Select
            v-model="bankAccountId"
            :options="bankAccountOptions"
            option-label="label"
            option-value="value"
            :class="{ 'p-invalid': errors.bankAccountId }"
          />
          <Message v-if="errors.bankAccountId" severity="error" :closable="false">
            {{ errors.bankAccountId }}
          </Message>
        </div>

        <div class="form-field">
          <label>{{ t('treasury.payment.paymentDate') }} *</label>
          <DatePicker
            v-model="localPaymentDate"
            date-format="yy-mm-dd"
            :class="{ 'p-invalid': errors.paymentDate }"
          />
          <Message v-if="errors.paymentDate" severity="error" :closable="false">
            {{ errors.paymentDate }}
          </Message>
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
          <Button
            type="submit"
            :label="t('treasury.doctorFee.confirmSettle')"
            severity="success"
            :loading="submitLoading"
          />
        </div>
      </form>
    </div>
  </Dialog>
</template>

<style scoped>
.settle-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.fee-summary {
  background: var(--p-surface-100);
  border-radius: var(--p-border-radius);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
}

.net-row {
  border-top: 1px solid var(--p-surface-300);
  padding-top: 0.5rem;
  font-weight: 600;
}

.net-value {
  color: var(--p-primary-color);
  font-size: 1.05rem;
}

.form-fields {
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
