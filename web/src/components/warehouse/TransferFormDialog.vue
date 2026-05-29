<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { createTransferSchema } from '@/validation/warehouse'
import { useWarehouseStore } from '@/stores/warehouse'
import { useWarehouseTransferStore } from '@/stores/warehouseTransfer'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { useInventoryLotStore } from '@/stores/inventoryLot'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { formatDate } from '@/utils/format'

const props = defineProps<{
  visible: boolean
}>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const { t } = useI18n()
const warehouseStore = useWarehouseStore()
const transferStore = useWarehouseTransferStore()
const itemStore = useInventoryItemStore()
const lotStore = useInventoryLotStore()
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

const { defineField, handleSubmit, errors, resetForm } = useForm({
  validationSchema: toTypedSchema(createTransferSchema),
  initialValues: {
    sourceWarehouseId: 0,
    destinationWarehouseId: 0,
    itemId: 0,
    lotId: null,
    quantity: 1,
    notes: ''
  }
})

const [sourceWarehouseId] = defineField('sourceWarehouseId')
const [destinationWarehouseId] = defineField('destinationWarehouseId')
const [itemId] = defineField('itemId')
const [lotId] = defineField('lotId')
const [quantity] = defineField('quantity')
const [notes] = defineField('notes')

// Lot-tracked items require an explicit lot (the backend rejects a null lotId with
// error.warehouse.lot.required), so surface a lot picker once such an item is chosen.
const selectedItem = computed(() => itemStore.items.find(i => i.id === itemId.value) ?? null)
const isLotTracked = computed(() => selectedItem.value?.lotTrackingEnabled ?? false)
const lotOptions = computed(() =>
  lotStore.lots
    .filter(l => !l.recalled && l.quantityOnHand > 0)
    .map(l => ({
      label: `${l.lotNumber || '—'} · ${formatDate(l.expirationDate)} · ${l.quantityOnHand}`,
      value: l.id
    }))
)

watch(itemId, async id => {
  lotId.value = null
  lotStore.lots = []
  if (id && isLotTracked.value) {
    await lotStore.fetchByItem(id).catch(() => undefined)
  }
})

watch(
  () => props.visible,
  async v => {
    if (v) {
      resetForm()
      lotStore.lots = []
      if (warehouseStore.warehouses.length === 0) {
        await warehouseStore.fetchWarehouses().catch(() => undefined)
      }
      await itemStore.fetchItems(0, 200).catch(() => undefined)
    }
  }
)

const lotRequiredError = ref(false)

const onSubmit = handleSubmit(async values => {
  lotRequiredError.value = false
  if (isLotTracked.value && !values.lotId) {
    lotRequiredError.value = true
    return
  }
  submitting.value = true
  try {
    await transferStore.createTransfer({
      sourceWarehouseId: values.sourceWarehouseId,
      destinationWarehouseId: values.destinationWarehouseId,
      itemId: values.itemId,
      lotId: values.lotId ?? null,
      quantity: values.quantity,
      notes: values.notes || null
    })
    showSuccess('warehouse.transfer.created')
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
    :header="t('warehouse.transfer.new')"
    modal
    :style="{ width: '560px' }"
  >
    <form @submit.prevent="onSubmit" class="form">
      <div class="field">
        <label>{{ t('warehouse.transfer.source') }}</label>
        <Select
          v-model="sourceWarehouseId"
          :options="warehouseOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('warehouse.transfer.selectWarehouse')"
          filter
        />
        <Message v-if="errors.sourceWarehouseId" severity="error" :closable="false">
          {{ errors.sourceWarehouseId }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.transfer.destination') }}</label>
        <Select
          v-model="destinationWarehouseId"
          :options="warehouseOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('warehouse.transfer.selectWarehouse')"
          filter
        />
        <Message v-if="errors.destinationWarehouseId" severity="error" :closable="false">
          {{ errors.destinationWarehouseId }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.transfer.item') }}</label>
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
      <div v-if="isLotTracked" class="field">
        <label>{{ t('warehouse.transfer.lot') }}</label>
        <Select
          v-model="lotId"
          :options="lotOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('warehouse.transfer.selectLot')"
          :loading="lotStore.loading"
          filter
        />
        <Message v-if="lotRequiredError" severity="error" :closable="false">
          {{ t('warehouse.transfer.lotRequired') }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.transfer.quantity') }}</label>
        <InputNumber v-model="quantity" :min="1" showButtons />
        <Message v-if="errors.quantity" severity="error" :closable="false">
          {{ errors.quantity }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('warehouse.transfer.notes') }}</label>
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
