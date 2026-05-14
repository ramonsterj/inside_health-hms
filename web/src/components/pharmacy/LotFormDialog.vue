<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import DatePicker from 'primevue/datepicker'
import Textarea from 'primevue/textarea'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import { useInventoryLotStore } from '@/stores/inventoryLot'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useAuthStore } from '@/stores/auth'
import { toApiDate, fromApiDate } from '@/utils/format'
import type { InventoryLot } from '@/types/pharmacy'

const props = defineProps<{
  visible: boolean
  itemId: number
  lot?: InventoryLot | null
}>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const { t } = useI18n()
const authStore = useAuthStore()
const lotStore = useInventoryLotStore()
const { showError, showSuccess } = useErrorHandler()

const lotNumber = ref('')
const expirationDate = ref<Date | null>(null)
const quantityOnHand = ref(0)
const receivedAt = ref<Date | null>(null)
const supplier = ref('')
const notes = ref('')
const recalled = ref(false)
const recalledReason = ref('')
const submitting = ref(false)

const canRecall = authStore.hasPermission('inventory-lot:update')

watch(
  () => props.visible,
  v => {
    if (v) {
      if (props.lot) {
        lotNumber.value = props.lot.lotNumber ?? ''
        // Parse date-only API strings as local midnight (Guatemala is UTC-6,
        // so `new Date('yyyy-MM-dd')` would show the previous day).
        expirationDate.value = fromApiDate(props.lot.expirationDate)
        quantityOnHand.value = props.lot.quantityOnHand
        receivedAt.value = fromApiDate(props.lot.receivedAt)
        supplier.value = props.lot.supplier ?? ''
        notes.value = props.lot.notes ?? ''
        recalled.value = props.lot.recalled
        recalledReason.value = props.lot.recalledReason ?? ''
      } else {
        lotNumber.value = ''
        expirationDate.value = null
        quantityOnHand.value = 0
        receivedAt.value = new Date()
        supplier.value = ''
        notes.value = ''
        recalled.value = false
        recalledReason.value = ''
      }
    }
  }
)

async function onSubmit() {
  if (!expirationDate.value) {
    showError(new Error(t('validation.lot.expirationDate.required')))
    return
  }
  submitting.value = true
  try {
    if (props.lot) {
      await lotStore.updateLot(props.lot.id, {
        lotNumber: lotNumber.value || null,
        expirationDate: toApiDate(expirationDate.value)!,
        supplier: supplier.value || null,
        notes: notes.value || null,
        recalled: recalled.value,
        recalledReason: recalledReason.value || null
      })
      showSuccess('pharmacy.lot.updated')
    } else {
      await lotStore.createLot(props.itemId, {
        lotNumber: lotNumber.value || null,
        expirationDate: toApiDate(expirationDate.value)!,
        quantityOnHand: quantityOnHand.value,
        receivedAt: toApiDate(receivedAt.value ?? new Date())!,
        supplier: supplier.value || null,
        notes: notes.value || null
      })
      showSuccess('pharmacy.lot.created')
    }
    emit('saved')
    emit('update:visible', false)
  } catch (e) {
    showError(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="lot ? t('pharmacy.lot.edit') : t('pharmacy.lot.new')"
    modal
    :style="{ width: '520px' }"
  >
    <div class="form">
      <div class="field">
        <label>{{ t('pharmacy.lot.lotNumber') }}</label>
        <InputText v-model="lotNumber" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.lot.expirationDate') }}</label>
        <DatePicker v-model="expirationDate" />
      </div>
      <div class="field" v-if="!lot">
        <label>{{ t('pharmacy.lot.quantityOnHand') }}</label>
        <InputNumber v-model="quantityOnHand" :min="0" />
      </div>
      <div class="field" v-if="!lot">
        <label>{{ t('pharmacy.lot.receivedAt') }}</label>
        <DatePicker v-model="receivedAt" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.lot.supplier') }}</label>
        <InputText v-model="supplier" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.lot.notes') }}</label>
        <Textarea v-model="notes" rows="2" />
      </div>
      <div v-if="lot && canRecall" class="field">
        <label>{{ t('pharmacy.lot.recalled') }}</label>
        <ToggleSwitch v-model="recalled" />
      </div>
      <div v-if="lot && recalled" class="field">
        <label>{{ t('pharmacy.lot.recalledReason') }}</label>
        <Textarea v-model="recalledReason" rows="2" />
      </div>
    </div>
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
