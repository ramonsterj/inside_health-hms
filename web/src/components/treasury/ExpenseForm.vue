<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import ToggleSwitch from 'primevue/toggleswitch'
import Message from 'primevue/message'
import { useExpenseStore } from '@/stores/expense'
import { useBankAccountStore } from '@/stores/bankAccount'
import { ExpenseCategory } from '@/types/treasury'
import { createExpenseSchema, type CreateExpenseFormData } from '@/validation/treasury'
import { toApiDate } from '@/utils/format'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  created: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const expenseStore = useExpenseStore()
const bankAccountStore = useBankAccountStore()

const loading = ref(false)
const invoiceFile = ref<File | null>(null)

// Local Date refs for DatePicker components (DatePicker requires Date | null, form schema uses strings)
const localExpenseDate = ref<Date | null>(null)
const localDueDate = ref<Date | null>(null)
const localPaymentDate = ref<Date | null>(null)

const categoryOptions = computed(() =>
  Object.values(ExpenseCategory).map(v => ({
    label: t(`treasury.expense.categories.${v}`),
    value: v
  }))
)

const bankAccountOptions = computed(() =>
  bankAccountStore.activeBankAccounts.map(a => ({
    label: a.isPettyCash
      ? `${a.name} (${t('treasury.bankAccount.pettyCashBadge')})`
      : `${a.name}${a.maskedAccountNumber ? ` — ${a.maskedAccountNumber}` : ''}`,
    value: a.id
  }))
)

const { defineField, handleSubmit, errors, resetForm } = useForm<CreateExpenseFormData>({
  validationSchema: toTypedSchema(createExpenseSchema),
  initialValues: {
    supplierName: '',
    category: ExpenseCategory.OTHER,
    description: '',
    amount: undefined as unknown as number,
    expenseDate: '',
    invoiceNumber: '',
    dueDate: '',
    isPaid: false,
    paymentDate: '',
    bankAccountId: null,
    notes: ''
  }
})

const [supplierName] = defineField('supplierName')
const [category] = defineField('category')
const [description] = defineField('description')
const [amount] = defineField('amount')
const [expenseDate] = defineField('expenseDate')
const [invoiceNumber] = defineField('invoiceNumber')
const [dueDate] = defineField('dueDate')
const [isPaid] = defineField('isPaid')
const [paymentDate] = defineField('paymentDate')
const [bankAccountId] = defineField('bankAccountId')
const [notes] = defineField('notes')

onMounted(async () => {
  await bankAccountStore.fetchActiveBankAccounts()
})

watch(localExpenseDate, val => {
  expenseDate.value = val ? val.toISOString().substring(0, 10) : ''
})
watch(localDueDate, val => {
  dueDate.value = val ? val.toISOString().substring(0, 10) : ''
})
watch(localPaymentDate, val => {
  paymentDate.value = val ? val.toISOString().substring(0, 10) : ''
})

watch(
  () => props.visible,
  visible => {
    if (visible) {
      resetForm()
      invoiceFile.value = null
      localExpenseDate.value = null
      localDueDate.value = null
      localPaymentDate.value = null
    }
  }
)

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  invoiceFile.value = input.files?.[0] ?? null
}

const onSubmit = handleSubmit(async formValues => {
  loading.value = true
  try {
    const data = {
      ...formValues,
      expenseDate: toApiDate(formValues.expenseDate as unknown as Date | string) ?? '',
      dueDate: toApiDate(formValues.dueDate as unknown as Date | string),
      paymentDate: toApiDate(formValues.paymentDate as unknown as Date | string),
      description: formValues.description || null,
      bankAccountId: formValues.bankAccountId ?? null,
      notes: formValues.notes || null
    }
    await expenseStore.createExpense(data, invoiceFile.value ?? undefined)
    showSuccess('treasury.expense.created')
    emit('created')
    onHide()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function onHide() {
  resetForm()
  invoiceFile.value = null
  localExpenseDate.value = null
  localDueDate.value = null
  localPaymentDate.value = null
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="onHide"
    :header="t('treasury.expense.new')"
    modal
    :style="{ width: '600px' }"
    :breakpoints="{ '768px': '95vw' }"
  >
    <form @submit.prevent="onSubmit" class="expense-form">
      <div class="form-row">
        <div class="form-field">
          <label for="exp-supplier">{{ t('treasury.expense.supplierName') }} *</label>
          <InputText
            id="exp-supplier"
            v-model="supplierName"
            :class="{ 'p-invalid': errors.supplierName }"
          />
          <Message v-if="errors.supplierName" severity="error" :closable="false">
            {{ errors.supplierName }}
          </Message>
        </div>

        <div class="form-field">
          <label for="exp-category">{{ t('treasury.expense.category') }} *</label>
          <Select
            id="exp-category"
            v-model="category"
            :options="categoryOptions"
            option-label="label"
            option-value="value"
            :class="{ 'p-invalid': errors.category }"
          />
          <Message v-if="errors.category" severity="error" :closable="false">
            {{ errors.category }}
          </Message>
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="exp-amount">{{ t('treasury.expense.amount') }} *</label>
          <InputNumber
            id="exp-amount"
            v-model="amount"
            :min="0.01"
            :max-fraction-digits="2"
            :class="{ 'p-invalid': errors.amount }"
          />
          <Message v-if="errors.amount" severity="error" :closable="false">
            {{ errors.amount }}
          </Message>
        </div>

        <div class="form-field">
          <label for="exp-date">{{ t('treasury.expense.expenseDate') }} *</label>
          <DatePicker
            id="exp-date"
            v-model="localExpenseDate"
            date-format="yy-mm-dd"
            :class="{ 'p-invalid': errors.expenseDate }"
          />
          <Message v-if="errors.expenseDate" severity="error" :closable="false">
            {{ errors.expenseDate }}
          </Message>
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="exp-invoice">{{ t('treasury.expense.invoiceNumber') }} *</label>
          <InputText
            id="exp-invoice"
            v-model="invoiceNumber"
            :class="{ 'p-invalid': errors.invoiceNumber }"
          />
          <Message v-if="errors.invoiceNumber" severity="error" :closable="false">
            {{ errors.invoiceNumber }}
          </Message>
        </div>

        <div class="form-field">
          <label for="exp-invoice-file">{{ t('treasury.expense.invoiceDocument') }}</label>
          <input
            id="exp-invoice-file"
            ref="fileInputRef"
            type="file"
            accept=".pdf,.jpg,.jpeg,.png"
            class="file-input"
            @change="onFileChange"
          />
          <small class="hint">{{ t('treasury.expense.invoiceDocumentHint') }}</small>
        </div>
      </div>

      <div class="form-field">
        <label for="exp-description">{{ t('treasury.expense.description') }}</label>
        <Textarea id="exp-description" v-model="description" rows="2" />
      </div>

      <div class="form-field is-paid-toggle">
        <label>{{ t('treasury.expense.isPaid') }}</label>
        <ToggleSwitch v-model="isPaid" />
      </div>

      <div v-if="isPaid" class="form-row">
        <div class="form-field">
          <label for="exp-pay-date">{{ t('treasury.expense.paymentDate') }} *</label>
          <DatePicker
            id="exp-pay-date"
            v-model="localPaymentDate"
            date-format="yy-mm-dd"
            :class="{ 'p-invalid': errors.paymentDate }"
          />
          <Message v-if="errors.paymentDate" severity="error" :closable="false">
            {{ errors.paymentDate }}
          </Message>
        </div>

        <div class="form-field">
          <label for="exp-bank-account">{{ t('treasury.payment.bankAccount') }} *</label>
          <Select
            id="exp-bank-account"
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
      </div>

      <div v-if="!isPaid" class="form-field">
        <label for="exp-due-date">{{ t('treasury.expense.dueDate') }} *</label>
        <DatePicker
          id="exp-due-date"
          v-model="localDueDate"
          date-format="yy-mm-dd"
          :class="{ 'p-invalid': errors.dueDate }"
        />
        <Message v-if="errors.dueDate" severity="error" :closable="false">
          {{ errors.dueDate }}
        </Message>
      </div>

      <div class="form-field">
        <label for="exp-notes">{{ t('treasury.expense.notes') }}</label>
        <Textarea id="exp-notes" v-model="notes" rows="2" />
      </div>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="onHide"
        />
        <Button type="submit" :label="t('common.save')" :loading="loading" />
      </div>
    </form>
  </Dialog>
</template>

<style scoped>
.expense-form {
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

.is-paid-toggle {
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
}

.file-input {
  padding: 0.4rem 0;
  font-size: 0.9rem;
}

.hint {
  color: var(--p-text-muted-color);
  font-size: 0.8rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

@media (max-width: 600px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
