<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { createWarehouseChargeSchema } from '@/validation/warehouse'
import { useWarehouseStore } from '@/stores/warehouse'
import { useWarehouseChargeStore } from '@/stores/warehouseCharge'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { useAdmissionStore } from '@/stores/admission'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { AdmissionStatus } from '@/types/admission'

const props = defineProps<{
  visible: boolean
  warehouseId?: number | null
}>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const { t } = useI18n()
const warehouseStore = useWarehouseStore()
const chargeStore = useWarehouseChargeStore()
const itemStore = useInventoryItemStore()
const admissionStore = useAdmissionStore()
const { showError, showSuccess } = useErrorHandler()

const submitting = ref(false)

// Deactivated warehouses are hidden from operation dropdowns (AC-12).
const warehouseOptions = computed(() =>
  warehouseStore.warehouses
    .filter(w => w.active)
    .map(w => ({ label: `${w.code} - ${w.name}`, value: w.id }))
)
const itemOptions = computed(() =>
  itemStore.items.map(i => ({
    label: i.sku ? `${i.name} (${i.sku})` : i.name,
    value: i.id
  }))
)
const admissionOptions = computed(() =>
  admissionStore.admissions.map(a => {
    const patient = `${a.patient.firstName} ${a.patient.lastName}`.trim()
    const room = a.room ? ` · ${t('room.numberPrefix')}${a.room.number}` : ''
    return { label: `${patient}${room}`, value: a.id }
  })
)

const { defineField, handleSubmit, errors, resetForm, setFieldValue } = useForm({
  validationSchema: toTypedSchema(createWarehouseChargeSchema),
  initialValues: {
    warehouseId: 0,
    itemId: 0,
    lotId: null,
    admissionId: 0,
    quantity: 1,
    reason: '',
    notes: ''
  }
})

const [selectedWarehouseId] = defineField('warehouseId')
const [itemId] = defineField('itemId')
const [admissionId] = defineField('admissionId')
const [quantity] = defineField('quantity')
const [reason] = defineField('reason')
const [notes] = defineField('notes')

watch(
  () => props.visible,
  async v => {
    if (v) {
      resetForm()
      if (props.warehouseId) {
        setFieldValue('warehouseId', props.warehouseId)
      }
      if (warehouseStore.warehouses.length === 0) {
        await warehouseStore.fetchWarehouses().catch(() => undefined)
      }
      await Promise.all([
        itemStore.fetchItems(0, 200).catch(() => undefined),
        admissionStore.fetchAdmissions(0, 200, AdmissionStatus.ACTIVE).catch(() => undefined)
      ])
    }
  }
)

const onSubmit = handleSubmit(async values => {
  submitting.value = true
  try {
    await chargeStore.createCharge({
      warehouseId: values.warehouseId,
      itemId: values.itemId,
      lotId: values.lotId ?? null,
      admissionId: values.admissionId,
      quantity: values.quantity,
      reason: values.reason,
      notes: values.notes || null
    })
    showSuccess('warehouse.charge.created')
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
    :header="t('warehouse.charge.new')"
    modal
    :style="{ width: '560px' }"
  >
    <form @submit.prevent="onSubmit" class="form">
      <div class="field">
        <label>{{ t('warehouse.charge.warehouse') }}</label>
        <Select
          v-model="selectedWarehouseId"
          :options="warehouseOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('warehouse.transfer.selectWarehouse')"
          filter
        />
        <Message v-if="errors.warehouseId" severity="error" :closable="false">
          {{ errors.warehouseId }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.charge.item') }}</label>
        <Select
          v-model="itemId"
          :options="itemOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('warehouse.transfer.selectItem')"
          filter
        />
        <Message v-if="errors.itemId" severity="error" :closable="false">
          {{ errors.itemId }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.charge.admission') }}</label>
        <Select
          v-model="admissionId"
          :options="admissionOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('warehouse.charge.selectAdmission')"
          filter
        />
        <Message v-if="errors.admissionId" severity="error" :closable="false">
          {{ errors.admissionId }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.charge.quantity') }}</label>
        <InputNumber v-model="quantity" :min="1" showButtons />
        <Message v-if="errors.quantity" severity="error" :closable="false">
          {{ errors.quantity }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.charge.reason') }}</label>
        <InputText v-model="reason" />
        <Message v-if="errors.reason" severity="error" :closable="false">
          {{ errors.reason }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.charge.notes') }}</label>
        <Textarea v-model="notes" rows="2" />
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
