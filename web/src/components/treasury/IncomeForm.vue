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
import Message from 'primevue/message'
import { useIncomeStore } from '@/stores/income'
import { useBankAccountStore } from '@/stores/bankAccount'
import { IncomeCategory } from '@/types/treasury'
import type { Income } from '@/types/treasury'
import { formatCurrency, toApiDate } from '@/utils/format'
import { createIncomeSchema, type CreateIncomeFormData } from '@/validation/treasury'

const props = defineProps<{
  visible: boolean
  income?: Income | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const incomeStore = useIncomeStore()
const bankAccountStore = useBankAccountStore()

const loading = ref(false)
const localIncomeDate = ref<Date | null>(null)
const isEditing = computed(() => !!props.income)

const categoryOptions = computed(() =>
  Object.values(IncomeCategory).map(v => ({
    label: t(`treasury.income.categories.${v}`),
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

const invoiceOptions = computed(() =>
  incomeStore.invoices.map(inv => ({
    label: `${inv.invoiceNumber} — ${inv.patientName} (${formatCurrency(inv.totalAmount)})`,
    value: inv.id
  }))
)

const { defineField, handleSubmit, errors, resetForm } = useForm<CreateIncomeFormData>({
  validationSchema: toTypedSchema(createIncomeSchema),
  initialValues: {
    description: '',
    category: IncomeCategory.OTHER_INCOME,
    amount: undefined as unknown as number,
    incomeDate: '',
    reference: '',
    bankAccountId: undefined as unknown as number,
    invoiceId: undefined as unknown as number,
    notes: ''
  }
})

const [description] = defineField('description')
const [category] = defineField('category')
const [amount] = defineField('amount')
const [incomeDate] = defineField('incomeDate')
const [reference] = defineField('reference')
const [bankAccountId] = defineField('bankAccountId')
const [invoiceId] = defineField('invoiceId')
const [notes] = defineField('notes')

onMounted(async () => {
  await bankAccountStore.fetchActiveBankAccounts()
})

watch(localIncomeDate, val => {
  incomeDate.value = val ? toApiDate(val) : ''
})

watch(
  () => props.visible,
  async visible => {
    if (visible) {
      await incomeStore.searchInvoices()
      if (props.income) {
        resetForm({
          values: {
            description: props.income.description,
            category: props.income.category,
            amount: props.income.amount,
            incomeDate: props.income.incomeDate,
            reference: props.income.reference ?? '',
            bankAccountId: props.income.bankAccountId,
            invoiceId: props.income.invoiceId ?? (undefined as unknown as number),
            notes: props.income.notes ?? ''
          }
        })
        localIncomeDate.value = props.income.incomeDate
          ? new Date(props.income.incomeDate + 'T00:00:00')
          : null
      } else {
        resetForm()
        localIncomeDate.value = null
      }
    }
  }
)

const onSubmit = handleSubmit(async formValues => {
  loading.value = true
  try {
    const data = {
      ...formValues,
      reference: formValues.reference || null,
      invoiceId: formValues.invoiceId || null,
      notes: formValues.notes || null
    }
    if (isEditing.value && props.income) {
      await incomeStore.updateIncome(props.income.id, data)
      showSuccess('treasury.income.updated')
    } else {
      await incomeStore.createIncome(data)
      showSuccess('treasury.income.created')
    }
    emit('saved')
    onHide()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function onHide() {
  resetForm()
  localIncomeDate.value = null
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="onHide"
    :header="isEditing ? t('treasury.income.edit') : t('treasury.income.new')"
    modal
    :style="{ width: '560px' }"
    :breakpoints="{ '768px': '95vw' }"
  >
    <form @submit.prevent="onSubmit" class="income-form">
      <div class="form-field">
        <label for="inc-invoice">{{ t('treasury.income.invoice') }}</label>
        <Select
          id="inc-invoice"
          v-model="invoiceId"
          :options="invoiceOptions"
          option-label="label"
          option-value="value"
          filter
          :filter-fields="['label']"
          :filter-placeholder="t('treasury.income.invoiceSearchPlaceholder')"
          :placeholder="t('treasury.income.invoicePlaceholder')"
          :class="{ 'p-invalid': errors.invoiceId }"
          reset-filter-on-hide
        />
        <Message v-if="errors.invoiceId" severity="error" :closable="false">
          {{ errors.invoiceId }}
        </Message>
      </div>

      <div class="form-field">
        <label for="inc-description">{{ t('treasury.income.description') }} *</label>
        <InputText
          id="inc-description"
          v-model="description"
          :class="{ 'p-invalid': errors.description }"
        />
        <Message v-if="errors.description" severity="error" :closable="false">
          {{ errors.description }}
        </Message>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="inc-category">{{ t('treasury.income.category') }} *</label>
          <Select
            id="inc-category"
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

        <div class="form-field">
          <label for="inc-amount">{{ t('treasury.income.amount') }} *</label>
          <InputNumber
            id="inc-amount"
            v-model="amount"
            :min="0.01"
            :max-fraction-digits="2"
            :class="{ 'p-invalid': errors.amount }"
          />
          <Message v-if="errors.amount" severity="error" :closable="false">
            {{ errors.amount }}
          </Message>
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="inc-date">{{ t('treasury.income.incomeDate') }} *</label>
          <DatePicker
            id="inc-date"
            v-model="localIncomeDate"
            :class="{ 'p-invalid': errors.incomeDate }"
          />
          <Message v-if="errors.incomeDate" severity="error" :closable="false">
            {{ errors.incomeDate }}
          </Message>
        </div>

        <div class="form-field">
          <label for="inc-reference">{{ t('treasury.income.reference') }}</label>
          <InputText id="inc-reference" v-model="reference" />
        </div>
      </div>

      <div class="form-field">
        <label for="inc-bank">{{ t('treasury.income.bankAccount') }} *</label>
        <Select
          id="inc-bank"
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
        <label for="inc-notes">{{ t('treasury.income.notes') }}</label>
        <Textarea id="inc-notes" v-model="notes" rows="2" />
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
.income-form {
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
