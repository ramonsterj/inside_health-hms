<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { createAdjustmentSchema, type CreateAdjustmentFormData } from '@/validation/billing'
import { useBillingStore } from '@/stores/billing'

const props = defineProps<{
  visible: boolean
  admissionId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  created: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const billingStore = useBillingStore()

const loading = ref(false)

const { defineField, handleSubmit, errors, resetForm } = useForm<CreateAdjustmentFormData>({
  validationSchema: toTypedSchema(createAdjustmentSchema),
  initialValues: {
    description: '',
    amount: undefined as unknown as number,
    reason: ''
  }
})

const [description] = defineField('description')
const [amount] = defineField('amount')
const [reason] = defineField('reason')

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  try {
    await billingStore.createAdjustment(props.admissionId, values)
    showSuccess('billing.adjustmentCreated')
    resetForm()
    emit('created')
    emit('update:visible', false)
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
    :header="t('billing.newAdjustment')"
    modal
    :style="{ width: '500px' }"
  >
    <form @submit="onSubmit" class="adjustment-form">
      <div class="form-field">
        <label for="description">{{ t('billing.description') }} *</label>
        <InputText
          id="description"
          v-model="description"
          :class="{ 'p-invalid': errors.description }"
        />
        <Message v-if="errors.description" severity="error" :closable="false">
          {{ errors.description }}
        </Message>
      </div>

      <div class="form-field">
        <label for="amount">{{ t('billing.amount') }} *</label>
        <InputNumber
          id="amount"
          v-model="amount"
          :maxFractionDigits="2"
          :minFractionDigits="2"
          mode="currency"
          currency="GTQ"
          :class="{ 'p-invalid': errors.amount }"
        />
        <small class="hint">{{ t('billing.adjustmentAmountHint') }}</small>
        <Message v-if="errors.amount" severity="error" :closable="false">
          {{ errors.amount }}
        </Message>
      </div>

      <div class="form-field">
        <label for="reason">{{ t('billing.reason') }} *</label>
        <Textarea
          id="reason"
          v-model="reason"
          rows="3"
          :class="{ 'p-invalid': errors.reason }"
        />
        <Message v-if="errors.reason" severity="error" :closable="false">
          {{ errors.reason }}
        </Message>
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
.adjustment-form {
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
}

.hint {
  color: var(--p-text-muted-color);
  font-size: 0.85rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
