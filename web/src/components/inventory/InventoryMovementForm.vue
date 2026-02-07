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
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { inventoryMovementSchema, type InventoryMovementFormData } from '@/validation/inventory'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { MovementType } from '@/types/inventoryItem'

const props = defineProps<{
  visible: boolean
  itemId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  created: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const itemStore = useInventoryItemStore()

const loading = ref(false)

const movementTypeOptions = computed(() => [
  { label: t('inventory.movement.types.ENTRY'), value: MovementType.ENTRY },
  { label: t('inventory.movement.types.EXIT'), value: MovementType.EXIT }
])

const { defineField, handleSubmit, errors, resetForm } = useForm<InventoryMovementFormData>({
  validationSchema: toTypedSchema(inventoryMovementSchema),
  initialValues: {
    type: MovementType.ENTRY,
    quantity: undefined as unknown as number,
    notes: ''
  }
})

const [type] = defineField('type')
const [quantity] = defineField('quantity')
const [notes] = defineField('notes')

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  try {
    await itemStore.createMovement(props.itemId, {
      ...values,
      notes: values.notes || null
    })
    showSuccess('inventory.movement.created')
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
    :header="t('inventory.movement.new')"
    modal
    :style="{ width: '450px' }"
  >
    <form @submit="onSubmit" class="movement-form">
      <div class="form-field">
        <label for="type">{{ t('inventory.movement.type') }} *</label>
        <Select
          id="type"
          v-model="type"
          :options="movementTypeOptions"
          optionLabel="label"
          optionValue="value"
          :class="{ 'p-invalid': errors.type }"
        />
        <Message v-if="errors.type" severity="error" :closable="false">
          {{ errors.type }}
        </Message>
      </div>

      <div class="form-field">
        <label for="quantity">{{ t('inventory.movement.quantity') }} *</label>
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
        <label for="notes">{{ t('inventory.movement.notes') }}</label>
        <Textarea id="notes" v-model="notes" rows="3" />
        <Message v-if="errors.notes" severity="error" :closable="false">
          {{ errors.notes }}
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
.movement-form {
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
