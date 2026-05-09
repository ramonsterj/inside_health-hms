<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import { useTreasuryEmployeeStore } from '@/stores/treasuryEmployee'
import { useBankAccountStore } from '@/stores/bankAccount'
import type { PayrollEntry } from '@/types/treasury'
import {
  recordPayrollPaymentSchema,
  type RecordPayrollPaymentFormData
} from '@/validation/treasury'
import { formatCurrency, toApiDate } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  entry: PayrollEntry
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  paid: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const employeeStore = useTreasuryEmployeeStore()
const bankAccountStore = useBankAccountStore()

const loading = ref(false)
const localPaymentDate = ref<Date | null>(null)

const bankAccountOptions = computed(() =>
  bankAccountStore.activeBankAccounts.map(a => ({
    label: a.isPettyCash
      ? `${a.name} (${t('treasury.bankAccount.pettyCashBadge')})`
      : `${a.name}${a.maskedAccountNumber ? ` — ${a.maskedAccountNumber}` : ''}`,
    value: a.id
  }))
)

const { defineField, handleSubmit, errors, resetForm } = useForm<RecordPayrollPaymentFormData>({
  validationSchema: toTypedSchema(recordPayrollPaymentSchema),
  initialValues: {
    paymentDate: '',
    bankAccountId: undefined as unknown as number,
    notes: ''
  }
})

const [paymentDate] = defineField('paymentDate')
const [bankAccountId] = defineField('bankAccountId')
const [notes] = defineField('notes')

onMounted(async () => {
  await bankAccountStore.fetchActiveBankAccounts()
})

watch(localPaymentDate, val => {
  paymentDate.value = val ? toApiDate(val) : ''
})

watch(
  () => props.visible,
  visible => {
    if (visible) {
      resetForm()
      localPaymentDate.value = new Date()
    }
  }
)

const onSubmit = handleSubmit(async formValues => {
  loading.value = true
  try {
    await employeeStore.payPayrollEntry(props.entry.id, {
      paymentDate: formValues.paymentDate,
      bankAccountId: formValues.bankAccountId,
      notes: formValues.notes || null
    })
    showSuccess('treasury.payroll.paid')
    emit('paid')
    onHide()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function onHide() {
  resetForm()
  localPaymentDate.value = null
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="onHide"
    :header="t('treasury.payroll.payEntry')"
    modal
    :style="{ width: '460px' }"
  >
    <div class="entry-summary">
      <div class="summary-row">
        <span class="summary-label">{{ t('treasury.payroll.period') }}</span>
        <span class="summary-value">{{ entry.periodLabel }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ t('treasury.payroll.employee') }}</span>
        <span class="summary-value">{{ entry.employeeName }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ t('treasury.payroll.grossAmount') }}</span>
        <span class="summary-value amount">{{ formatCurrency(entry.grossAmount) }}</span>
      </div>
    </div>

    <form @submit.prevent="onSubmit" class="payroll-payment-form">
      <div class="form-field">
        <label for="pp-date">{{ t('treasury.payroll.paymentDate') }} *</label>
        <DatePicker
          id="pp-date"
          v-model="localPaymentDate"
          :class="{ 'p-invalid': errors.paymentDate }"
        />
        <Message v-if="errors.paymentDate" severity="error" :closable="false">
          {{ errors.paymentDate }}
        </Message>
      </div>

      <div class="form-field">
        <label for="pp-bank">{{ t('treasury.payment.bankAccount') }} *</label>
        <Select
          id="pp-bank"
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
        <label for="pp-notes">{{ t('treasury.payment.notes') }}</label>
        <Textarea id="pp-notes" v-model="notes" rows="2" />
      </div>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="onHide"
        />
        <Button type="submit" :label="t('treasury.payroll.pay')" :loading="loading" />
      </div>
    </form>
  </Dialog>
</template>

<style scoped>
.entry-summary {
  background: var(--p-surface-50);
  border-radius: 6px;
  padding: 1rem;
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-label {
  color: var(--p-text-muted-color);
  font-size: 0.9rem;
}

.summary-value {
  font-weight: 500;
}

.summary-value.amount {
  color: var(--p-primary-color);
  font-size: 1.1rem;
}

.payroll-payment-form {
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
