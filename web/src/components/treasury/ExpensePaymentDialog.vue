<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import DatePicker from 'primevue/datepicker'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { useExpenseStore } from '@/stores/expense'
import { useBankAccountStore } from '@/stores/bankAccount'
import type { Expense } from '@/types/treasury'
import { recordPaymentSchema, type RecordPaymentFormData } from '@/validation/treasury'
import { formatCurrency, toApiDate } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  expense: Expense
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  paid: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const expenseStore = useExpenseStore()
const bankAccountStore = useBankAccountStore()

const loading = ref(false)

// Local Date ref for DatePicker (DatePicker requires Date | null, form schema uses strings)
const localPaymentDate = ref<Date | null>(null)

const remainingAmount = computed(() => props.expense.remainingAmount)

const bankAccountOptions = computed(() =>
  bankAccountStore.activeBankAccounts.map(a => ({
    label: a.isPettyCash
      ? `${a.name} (${t('treasury.bankAccount.pettyCashBadge')})`
      : `${a.name}${a.maskedAccountNumber ? ` — ${a.maskedAccountNumber}` : ''}`,
    value: a.id
  }))
)

const { defineField, handleSubmit, errors, resetForm, setValues } = useForm<RecordPaymentFormData>({
  validationSchema: toTypedSchema(recordPaymentSchema),
  initialValues: {
    amount: undefined as unknown as number,
    paymentDate: '',
    bankAccountId: undefined as unknown as number,
    reference: '',
    notes: ''
  }
})

const [amount] = defineField('amount')
const [paymentDate] = defineField('paymentDate')
watch(localPaymentDate, val => {
  paymentDate.value = val ? val.toISOString().substring(0, 10) : ''
})
const [bankAccountId] = defineField('bankAccountId')
const [reference] = defineField('reference')
const [notes] = defineField('notes')

onMounted(async () => {
  await bankAccountStore.fetchActiveBankAccounts()
  setValues({ amount: remainingAmount.value })
})

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    await expenseStore.recordPayment(props.expense.id, {
      ...values,
      paymentDate: toApiDate(values.paymentDate as unknown as Date | string) ?? '',
      reference: values.reference || null,
      notes: values.notes || null
    })
    showSuccess('treasury.payment.created')
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
    :header="t('treasury.payment.record')"
    modal
    :style="{ width: '480px' }"
  >
    <div class="expense-summary">
      <div class="summary-row">
        <span class="summary-label">{{ t('treasury.expense.supplierName') }}:</span>
        <span>{{ expense.supplierName }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ t('treasury.expense.amount') }}:</span>
        <span>{{ formatCurrency(expense.amount) }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ t('treasury.expense.paidAmount') }}:</span>
        <span>{{ formatCurrency(expense.paidAmount) }}</span>
      </div>
      <div class="summary-row remaining">
        <span class="summary-label">{{ t('treasury.expense.remainingAmount') }}:</span>
        <span class="remaining-value">{{ formatCurrency(remainingAmount) }}</span>
      </div>
    </div>

    <form @submit.prevent="onSubmit" class="payment-form">
      <div class="form-field">
        <label for="pay-amount">{{ t('treasury.payment.amount') }} *</label>
        <InputNumber
          id="pay-amount"
          v-model="amount"
          :min="0.01"
          :max="remainingAmount"
          :max-fraction-digits="2"
          :class="{ 'p-invalid': errors.amount }"
        />
        <Message v-if="errors.amount" severity="error" :closable="false">
          {{ errors.amount }}
        </Message>
      </div>

      <div class="form-field">
        <label for="pay-date">{{ t('treasury.payment.paymentDate') }} *</label>
        <DatePicker
          id="pay-date"
          v-model="localPaymentDate"
          date-format="yy-mm-dd"
          :class="{ 'p-invalid': errors.paymentDate }"
        />
        <Message v-if="errors.paymentDate" severity="error" :closable="false">
          {{ errors.paymentDate }}
        </Message>
      </div>

      <div class="form-field">
        <label for="pay-account">{{ t('treasury.payment.bankAccount') }} *</label>
        <Select
          id="pay-account"
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
        <label for="pay-reference">{{ t('treasury.payment.reference') }}</label>
        <InputText id="pay-reference" v-model="reference" />
      </div>

      <div class="form-field">
        <label for="pay-notes">{{ t('treasury.payment.notes') }}</label>
        <Textarea id="pay-notes" v-model="notes" rows="2" />
      </div>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="onHide"
        />
        <Button type="submit" :label="t('treasury.payment.record')" :loading="loading" />
      </div>
    </form>
  </Dialog>
</template>

<style scoped>
.expense-summary {
  background: var(--p-surface-100);
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
}

.summary-label {
  color: var(--p-text-muted-color);
}

.remaining {
  font-weight: 600;
  border-top: 1px solid var(--p-surface-300);
  padding-top: 0.5rem;
  margin-top: 0.25rem;
}

.remaining-value {
  color: var(--p-orange-500);
}

.payment-form {
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
