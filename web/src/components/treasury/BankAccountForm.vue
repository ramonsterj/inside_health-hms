<script setup lang="ts">
import { ref, computed, watch } from 'vue'
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
import Message from 'primevue/message'
import { useBankAccountStore } from '@/stores/bankAccount'
import type { BankAccount } from '@/types/treasury'
import { BankAccountType } from '@/types/treasury'
import {
  createBankAccountSchema,
  updateBankAccountSchema,
  type CreateBankAccountFormData
} from '@/validation/treasury'

const props = defineProps<{
  visible: boolean
  bankAccount?: BankAccount | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const bankAccountStore = useBankAccountStore()

const isEditMode = computed(() => !!props.bankAccount)
const loading = ref(false)

const accountTypeOptions = [
  { label: t('treasury.bankAccount.accountTypes.CHECKING'), value: BankAccountType.CHECKING },
  { label: t('treasury.bankAccount.accountTypes.SAVINGS'), value: BankAccountType.SAVINGS }
]

const schema = computed(() =>
  isEditMode.value ? updateBankAccountSchema : createBankAccountSchema
)

const { defineField, handleSubmit, errors, resetForm, setValues } =
  useForm<CreateBankAccountFormData>({
    validationSchema: computed(() => toTypedSchema(schema.value)),
    initialValues: {
      name: '',
      bankName: '',
      accountNumber: '',
      accountType: BankAccountType.CHECKING,
      currency: 'GTQ',
      openingBalance: 0,
      notes: ''
    }
  })

const [name] = defineField('name')
const [bankName] = defineField('bankName')
const [accountNumber] = defineField('accountNumber')
const [accountType] = defineField('accountType')
const [currency] = defineField('currency')
const [openingBalance] = defineField('openingBalance')
const [notes] = defineField('notes')

watch(
  () => props.bankAccount,
  account => {
    if (account) {
      setValues({
        name: account.name,
        bankName: account.bankName ?? '',
        accountNumber: '',
        accountType: account.accountType,
        currency: account.currency,
        openingBalance: account.openingBalance,
        notes: account.notes ?? ''
      })
    } else {
      resetForm()
    }
  }
)

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    const data = {
      ...values,
      bankName: values.bankName || null,
      accountNumber: values.accountNumber || null,
      notes: values.notes || null
    }
    if (isEditMode.value && props.bankAccount) {
      await bankAccountStore.updateBankAccount(props.bankAccount.id, data)
      showSuccess('treasury.bankAccount.updated')
    } else {
      await bankAccountStore.createBankAccount(data)
      showSuccess('treasury.bankAccount.created')
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
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="onHide"
    :header="isEditMode ? t('treasury.bankAccount.edit') : t('treasury.bankAccount.new')"
    modal
    :style="{ width: '520px' }"
  >
    <form @submit.prevent="onSubmit" class="bank-account-form">
      <div class="form-field">
        <label for="ba-name">{{ t('treasury.bankAccount.name') }} *</label>
        <InputText id="ba-name" v-model="name" :class="{ 'p-invalid': errors.name }" />
        <Message v-if="errors.name" severity="error" :closable="false">{{ errors.name }}</Message>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="ba-bankName">{{ t('treasury.bankAccount.bankName') }}</label>
          <InputText id="ba-bankName" v-model="bankName" />
        </div>

        <div class="form-field">
          <label for="ba-accountType">{{ t('treasury.bankAccount.accountType') }} *</label>
          <Select
            id="ba-accountType"
            v-model="accountType"
            :options="accountTypeOptions"
            option-label="label"
            option-value="value"
            :class="{ 'p-invalid': errors.accountType }"
          />
          <Message v-if="errors.accountType" severity="error" :closable="false">
            {{ errors.accountType }}
          </Message>
        </div>
      </div>

      <div class="form-row">
        <div class="form-field">
          <label for="ba-accountNumber">{{ t('treasury.bankAccount.accountNumber') }}</label>
          <InputText id="ba-accountNumber" v-model="accountNumber" />
        </div>

        <div class="form-field">
          <label for="ba-currency">{{ t('treasury.bankAccount.currency') }}</label>
          <InputText id="ba-currency" v-model="currency" maxlength="3" />
        </div>
      </div>

      <div class="form-field">
        <label for="ba-openingBalance">{{ t('treasury.bankAccount.openingBalance') }}</label>
        <InputNumber
          id="ba-openingBalance"
          v-model="openingBalance"
          :min="0"
          :max-fraction-digits="2"
          :disabled="isEditMode"
          :class="{ 'p-invalid': errors.openingBalance }"
        />
        <small v-if="isEditMode" class="hint">{{
          t('treasury.bankAccount.openingBalanceReadonly')
        }}</small>
        <Message v-if="errors.openingBalance" severity="error" :closable="false">
          {{ errors.openingBalance }}
        </Message>
      </div>

      <div class="form-field">
        <label for="ba-notes">{{ t('treasury.bankAccount.notes') }}</label>
        <Textarea id="ba-notes" v-model="notes" rows="2" />
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
.bank-account-form {
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
</style>
