<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { useConfirm } from 'primevue/useconfirm'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import InputNumber from 'primevue/inputnumber'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import {
  createMedicationAdministrationSchema,
  type MedicationAdministrationFormData
} from '@/validation/medicationAdministration'
import { useMedicationAdministrationStore } from '@/stores/medicationAdministration'
import { useErrorHandler } from '@/composables/useErrorHandler'
import {
  AdministrationStatus,
  type CreateMedicationAdministrationRequest
} from '@/types/medicationAdministration'
import { useAuthStore } from '@/stores/auth'
import { usePharmacyStore } from '@/stores/pharmacy'
import { useInventoryLotStore } from '@/stores/inventoryLot'
import ExpiryStatusChip from '@/components/pharmacy/ExpiryStatusChip.vue'
import { type InventoryLot } from '@/types/pharmacy'
import { formatDate } from '@/utils/format'
import { lotExpiryStatusFromDate } from '@/utils/expiry'
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  admissionId: number
  orderId: number
  medicationName?: string | null
  inventoryItemId?: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const confirm = useConfirm()
const { showError } = useErrorHandler()
const administrationStore = useMedicationAdministrationStore()
const authStore = useAuthStore()
const pharmacyStore = usePharmacyStore()
const lotStore = useInventoryLotStore()

const loading = ref(false)
const quantity = ref(1)
const fefoLot = ref<InventoryLot | null>(null)
const overrideLotId = ref<number | null>(null)

const canOverrideLot = authStore.hasPermission('inventory-lot:update')

const lotOptions = computed(() => [
  { label: t('pharmacy.fefo.auto'), value: null },
  ...lotStore.lots
    .filter(l => !l.recalled && l.quantityOnHand > 0)
    .map(l => ({
      label: `${l.lotNumber || '—'} · ${formatDate(l.expirationDate)} · ${l.quantityOnHand}`,
      value: l.id
    }))
])

const statusOptions = [
  { label: t('medicationAdministration.statuses.GIVEN'), value: AdministrationStatus.GIVEN },
  { label: t('medicationAdministration.statuses.MISSED'), value: AdministrationStatus.MISSED },
  { label: t('medicationAdministration.statuses.REFUSED'), value: AdministrationStatus.REFUSED },
  { label: t('medicationAdministration.statuses.HELD'), value: AdministrationStatus.HELD }
]

const { defineField, handleSubmit, errors, resetForm } = useForm<MedicationAdministrationFormData>({
  validationSchema: toTypedSchema(createMedicationAdministrationSchema),
  initialValues: {
    status: AdministrationStatus.GIVEN,
    notes: ''
  }
})

const [status] = defineField('status')
const [notes] = defineField('notes')

watch(
  () => props.visible,
  async newValue => {
    if (newValue) {
      resetForm()
      quantity.value = 1
      overrideLotId.value = null
      fefoLot.value = null
      await refreshFefo()
      if (canOverrideLot && props.inventoryItemId) {
        await lotStore.fetchByItem(props.inventoryItemId).catch(() => undefined)
      }
    }
  }
)

async function refreshFefo() {
  if (!props.inventoryItemId) return
  try {
    fefoLot.value = await pharmacyStore.fefoPreview(props.inventoryItemId, quantity.value || 1)
  } catch {
    fefoLot.value = null
  }
}

function lotStatus(lot: InventoryLot) {
  return lotExpiryStatusFromDate(lot.expirationDate)
}

const onSubmit = handleSubmit(async formValues => {
  // If status is GIVEN, show confirmation dialog
  if (formValues.status === AdministrationStatus.GIVEN) {
    confirm.require({
      message: t('medicationAdministration.confirmGiven'),
      header: t('common.confirm'),
      icon: 'pi pi-exclamation-triangle',
      acceptClass: 'p-button-success',
      accept: () => submitAdministration(formValues)
    })
  } else {
    await submitAdministration(formValues)
  }
})

async function submitAdministration(formValues: MedicationAdministrationFormData) {
  loading.value = true
  try {
    const data: CreateMedicationAdministrationRequest = {
      status: formValues.status,
      notes: formValues.notes || undefined
    }
    if (formValues.status === AdministrationStatus.GIVEN) {
      data.quantity = quantity.value
      if (canOverrideLot && overrideLotId.value) {
        data.lotId = overrideLotId.value
      }
    }

    await administrationStore.createAdministration(props.admissionId, props.orderId, data)
    emit('saved')
    closeDialog()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function closeDialog() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="t('medicationAdministration.administer')"
    :modal="true"
    :closable="!loading"
    :style="{ width: '500px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form @submit="onSubmit" class="administration-form">
      <div v-if="medicationName" class="medication-info">
        <strong>{{ medicationName }}</strong>
      </div>

      <!-- Status -->
      <div class="form-field">
        <label for="status">{{ t('medicationAdministration.status') }} *</label>
        <Select
          id="status"
          v-model="status"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          :class="{ 'p-invalid': errors.status }"
          class="w-full"
        />
        <Message v-if="errors.status" severity="error" :closable="false">
          {{ errors.status }}
        </Message>
      </div>

      <!-- Quantity (GIVEN only) -->
      <div v-if="status === AdministrationStatus.GIVEN" class="form-field">
        <label for="quantity">{{ t('pharmacy.medication.quantity') }}</label>
        <InputNumber id="quantity" v-model="quantity" :min="1" :max="999" @blur="refreshFefo" />
      </div>

      <!-- FEFO preview -->
      <div v-if="status === AdministrationStatus.GIVEN && fefoLot" class="form-field">
        <label>{{ t('pharmacy.fefo.previewLabel') }}</label>
        <div class="fefo-row">
          <ExpiryStatusChip :status="lotStatus(fefoLot)" />
          <span>{{ fefoLot.lotNumber || '—' }}</span>
          <span class="muted">{{ formatDate(fefoLot.expirationDate) }}</span>
        </div>
      </div>

      <!-- Admin lot override (admin only) -->
      <div
        v-if="status === AdministrationStatus.GIVEN && canOverrideLot && inventoryItemId"
        class="form-field"
      >
        <label for="overrideLot">{{ t('pharmacy.fefo.overrideLabel') }}</label>
        <Select
          id="overrideLot"
          v-model="overrideLotId"
          :options="lotOptions"
          optionLabel="label"
          optionValue="value"
          class="w-full"
        />
      </div>

      <!-- Notes -->
      <div class="form-field">
        <label for="notes">{{ t('medicationAdministration.notes') }}</label>
        <Textarea
          id="notes"
          v-model="notes"
          rows="4"
          :class="{ 'p-invalid': errors.notes }"
          class="w-full"
        />
        <Message v-if="errors.notes" severity="error" :closable="false">
          {{ errors.notes }}
        </Message>
      </div>
    </form>

    <template #footer>
      <div class="dialog-footer">
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          :disabled="loading"
          @click="closeDialog"
        />
        <Button :label="t('common.save')" :loading="loading" @click="onSubmit" />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.administration-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.medication-info {
  padding: 0.75rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
  border-left: 4px solid var(--p-primary-color);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 500;
  color: var(--p-text-color);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.fefo-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.muted {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}
</style>
