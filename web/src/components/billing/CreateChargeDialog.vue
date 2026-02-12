<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import { createChargeSchema } from '@/validation/billing'
import { useBillingStore } from '@/stores/billing'
import { ChargeType, type CreateChargeRequest } from '@/types/billing'

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

const chargeTypeOptions = computed(() => [
  { label: t('billing.chargeTypes.MEDICATION'), value: ChargeType.MEDICATION },
  { label: t('billing.chargeTypes.PROCEDURE'), value: ChargeType.PROCEDURE },
  { label: t('billing.chargeTypes.LAB'), value: ChargeType.LAB },
  { label: t('billing.chargeTypes.SERVICE'), value: ChargeType.SERVICE }
])

const { defineField, handleSubmit, errors, resetForm } = useForm<CreateChargeRequest>({
  validationSchema: toTypedSchema(createChargeSchema)
})

const [chargeType] = defineField('chargeType')
const [description] = defineField('description')
const [quantity] = defineField('quantity')
const [unitPrice] = defineField('unitPrice')

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  try {
    await billingStore.createCharge(props.admissionId, values as unknown as CreateChargeRequest)
    showSuccess('billing.chargeCreated')
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
    :header="t('billing.newCharge')"
    modal
    :style="{ width: '500px' }"
  >
    <form @submit="onSubmit" class="charge-form">
      <div class="form-field">
        <label for="chargeType">{{ t('billing.chargeType') }} *</label>
        <Select
          id="chargeType"
          v-model="chargeType"
          :options="chargeTypeOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('billing.selectChargeType')"
          :class="{ 'p-invalid': errors.chargeType }"
        />
        <Message v-if="errors.chargeType" severity="error" :closable="false">
          {{ errors.chargeType }}
        </Message>
      </div>

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

      <div class="form-row">
        <div class="form-field">
          <label for="quantity">{{ t('billing.quantity') }} *</label>
          <InputNumber
            id="quantity"
            v-model="quantity"
            :min="1"
            :class="{ 'p-invalid': errors.quantity }"
          />
          <Message v-if="errors.quantity" severity="error" :closable="false">
            {{ errors.quantity }}
          </Message>
        </div>

        <div class="form-field">
          <label for="unitPrice">{{ t('billing.unitPrice') }} *</label>
          <InputNumber
            id="unitPrice"
            v-model="unitPrice"
            :min="0"
            :minFractionDigits="2"
            :maxFractionDigits="2"
            mode="currency"
            currency="GTQ"
            :class="{ 'p-invalid': errors.unitPrice }"
          />
          <Message v-if="errors.unitPrice" severity="error" :closable="false">
            {{ errors.unitPrice }}
          </Message>
        </div>
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
.charge-form {
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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
