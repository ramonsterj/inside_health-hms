<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { warehouseSchema } from '@/validation/warehouse'
import { useWarehouseStore } from '@/stores/warehouse'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { Warehouse } from '@/types/warehouse'

const props = defineProps<{
  visible: boolean
  warehouse?: Warehouse | null
}>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const { t } = useI18n()
const warehouseStore = useWarehouseStore()
const { showError, showSuccess } = useErrorHandler()

const submitting = ref(false)

const { defineField, handleSubmit, errors, resetForm, setValues } = useForm({
  validationSchema: toTypedSchema(warehouseSchema),
  initialValues: {
    code: '',
    name: '',
    description: '',
    active: true
  }
})

const [code] = defineField('code')
const [name] = defineField('name')
const [description] = defineField('description')
const [active] = defineField('active')

watch(
  () => props.visible,
  v => {
    if (v) {
      if (props.warehouse) {
        setValues({
          code: props.warehouse.code,
          name: props.warehouse.name,
          description: props.warehouse.description ?? '',
          active: props.warehouse.active
        })
      } else {
        resetForm()
      }
    }
  }
)

const onSubmit = handleSubmit(async values => {
  submitting.value = true
  try {
    if (props.warehouse) {
      await warehouseStore.updateWarehouse(props.warehouse.id, {
        name: values.name,
        description: values.description || null,
        active: values.active
      })
      showSuccess('warehouse.updated')
    } else {
      await warehouseStore.createWarehouse({
        code: values.code,
        name: values.name,
        description: values.description || null,
        active: values.active
      })
      showSuccess('warehouse.created')
    }
    emit('saved')
    emit('update:visible', false)
  } catch (e) {
    showError(e)
  } finally {
    submitting.value = false
  }
})
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="warehouse ? t('warehouse.edit') : t('warehouse.new')"
    modal
    :style="{ width: '520px' }"
  >
    <form @submit.prevent="onSubmit" class="form">
      <div class="field">
        <label>{{ t('warehouse.code') }}</label>
        <InputText v-model="code" :disabled="!!warehouse" />
        <Message v-if="errors.code" severity="error" :closable="false">{{ errors.code }}</Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.name') }}</label>
        <InputText v-model="name" />
        <Message v-if="errors.name" severity="error" :closable="false">{{ errors.name }}</Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.description') }}</label>
        <Textarea v-model="description" rows="2" />
        <Message v-if="errors.description" severity="error" :closable="false">
          {{ errors.description }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.active') }}</label>
        <ToggleSwitch v-model="active" />
      </div>
    </form>
    <template #footer>
      <Button :label="t('common.cancel')" text @click="$emit('update:visible', false)" />
      <Button :label="t('common.save')" :loading="submitting" @click="onSubmit" />
    </template>
  </Dialog>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
</style>
