<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import MultiSelect from 'primevue/multiselect'
import InputText from 'primevue/inputtext'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import { medicalOrderSchema, type MedicalOrderFormData } from '@/validation/medicalRecord'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { usePharmacyStore } from '@/stores/pharmacy'
import { useLabCatalogStore } from '@/stores/labCatalog'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useFormDateField } from '@/composables/useFormDateField'
import { toApiDate } from '@/utils/format'
import {
  MedicalOrderCategory,
  MedicalOrderStatus,
  AdministrationRoute
} from '@/types/medicalRecord'
import { InventoryKind } from '@/types/inventoryItem'
import type { MedicalOrderResponse } from '@/types/medicalRecord'
import RichTextEditor from '@/components/common/RichTextEditor.vue'

const props = defineProps<{
  visible: boolean
  admissionId: number
  order?: MedicalOrderResponse | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()
const medicalOrderStore = useMedicalOrderStore()
const inventoryItemStore = useInventoryItemStore()
const pharmacyStore = usePharmacyStore()
const labCatalogStore = useLabCatalogStore()

const loading = ref(false)
const isEditMode = computed(() => !!props.order)

// Inventory item selector is shown for these billable categories. LABORATORIOS is
// deliberately excluded — labs use the provider + multi-test model instead.
const inventoryItemCategories = [
  MedicalOrderCategory.MEDICAMENTOS,
  MedicalOrderCategory.CUIDADOS_ESPECIALES,
  MedicalOrderCategory.REFERENCIAS_MEDICAS,
  MedicalOrderCategory.PRUEBAS_PSICOMETRICAS,
  MedicalOrderCategory.ACTIVIDAD_FISICA
]

// Categories whose catalog the inventory picker can load.
const billableCategories = [...inventoryItemCategories]

// Apply-panel UI state.
const selectedPanelId = ref<number | null>(null)
const panelNotice = ref<{ provider: string; tests: string } | null>(null)

// Category options
const categoryOptions = computed(() =>
  Object.values(MedicalOrderCategory).map(cat => ({
    label: t(`medicalRecord.medicalOrder.categories.${cat}`),
    value: cat
  }))
)

// Inventory item options.
// For MEDICAMENTOS we show the pharmacy catalog (DRUG items, accessible to
// doctors / nurses / chief nurses via `medication:read`) because the legacy
// admin inventory endpoint is gated by `inventory-item:read`, which only
// ADMINISTRADOR holds today.
const inventoryItemOptions = computed(() => {
  if (values.category === MedicalOrderCategory.MEDICAMENTOS) {
    return [
      { label: '-', value: null },
      ...pharmacyStore.items.map(med => ({
        label: medicationLabel(med),
        value: med.itemId
      }))
    ]
  }
  return [
    { label: '-', value: null },
    ...inventoryItemStore.items
      .filter(item => item.kind !== InventoryKind.DRUG)
      .map(item => ({
        label: item.name,
        value: item.id
      }))
  ]
})

function medicationLabel(med: {
  name: string
  genericName: string
  commercialName: string | null
  strength: string | null
}): string {
  const parts: string[] = []
  parts.push(med.genericName || med.name)
  if (med.strength) parts.push(med.strength)
  if (med.commercialName && med.commercialName !== med.genericName) {
    parts.push(`(${med.commercialName})`)
  }
  return parts.join(' ')
}

// Route options
const routeOptions = computed(() => [
  { label: '-', value: null },
  ...Object.values(AdministrationRoute).map(route => ({
    label: t(`medicalRecord.medicalOrder.routes.${route}`),
    value: route
  }))
])

const { defineField, handleSubmit, errors, values, setValues, setFieldValue, resetForm } =
  useForm<MedicalOrderFormData>({
    validationSchema: toTypedSchema(medicalOrderSchema),
    initialValues: {
      category: MedicalOrderCategory.ORDENES_MEDICAS,
      startDate: toApiDate(new Date()),
      endDate: '',
      medication: '',
      dosage: '',
      route: null,
      frequency: '',
      schedule: '',
      observations: '',
      inventoryItemId: null,
      labProviderId: null,
      labProviderTestIds: []
    }
  })

const [category] = defineField('category')
const [startDate] = defineField('startDate')
const [endDate] = defineField('endDate')
const [medication] = defineField('medication')
const [dosage] = defineField('dosage')
const [route] = defineField('route')
const [frequency] = defineField('frequency')
const [schedule] = defineField('schedule')
const [observations] = defineField('observations')
const [inventoryItemId] = defineField('inventoryItemId')
const [labProviderId] = defineField('labProviderId')
const [labProviderTestIds] = defineField('labProviderTestIds')

// Show medication fields when MEDICAMENTOS category is selected
const showMedicationFields = computed(() => values.category === MedicalOrderCategory.MEDICAMENTOS)

// Show inventory item selector for billable categories (LABORATORIOS uses the lab fields)
const showInventoryItemSelector = computed(() =>
  inventoryItemCategories.includes(values.category as MedicalOrderCategory)
)

// Lab provider + multi-test model for LABORATORIOS.
const showLabFields = computed(() => values.category === MedicalOrderCategory.LABORATORIOS)

// One order = one provider, so lab fields are locked after the order leaves SOLICITADO (AC14).
const labFieldsEditable = computed(
  () => !isEditMode.value || props.order?.status === MedicalOrderStatus.SOLICITADO
)

const providerOptions = computed(() =>
  labCatalogStore.providers.filter(p => p.active).map(p => ({ label: p.name, value: p.id }))
)

// Prices are intentionally NOT surfaced in the ordering form — pricing is a
// finance/billing concern, not a clinical one. The label is just the test name.
const labTestOptions = computed(() =>
  labCatalogStore.getProviderTests(values.labProviderId).map(pt => ({
    label: pt.displayName,
    value: pt.id
  }))
)

const panelOptions = computed(() =>
  labCatalogStore.panels.filter(p => p.active).map(p => ({ label: p.name, value: p.id }))
)

onMounted(() => {
  loadCatalog(values.category as MedicalOrderCategory)
  if (values.category === MedicalOrderCategory.LABORATORIOS) loadLabCatalog()
})

// Load lab providers + panels (non-blocking, like loadCatalog). The order form only
// needs active providers/panels for selection.
async function loadLabCatalog(): Promise<void> {
  try {
    await Promise.all([labCatalogStore.fetchProviders(true), labCatalogStore.fetchPanels()])
  } catch {
    // Pickers are optional — don't block the form on a transient catalog fetch failure.
  }
}

// Re-fetch the appropriate catalog whenever the user switches category, so
// the picker is populated with the right kind of items (medications vs
// supplies/services) the next time the dropdown opens. Also drop any
// previously chosen inventory item that no longer belongs to the new
// catalog — otherwise a medication selected under MEDICAMENTOS could
// remain attached when the user switches to LABORATORIOS / REFERENCIAS /
// PRUEBAS_PSICOMETRICAS, and the backend would happily bill against it.
watch(
  () => values.category,
  async (newCategory, oldCategory) => {
    if (!newCategory) return

    // Category-switch cleanup: leaving LABORATORIOS clears lab fields; entering it
    // clears the inventory item (the two billing models are mutually exclusive).
    if (oldCategory === MedicalOrderCategory.LABORATORIOS && newCategory !== oldCategory) {
      setFieldValue('labProviderId', null)
      setFieldValue('labProviderTestIds', [])
      selectedPanelId.value = null
      panelNotice.value = null
    }
    if (newCategory === MedicalOrderCategory.LABORATORIOS) {
      setFieldValue('inventoryItemId', null)
      await loadLabCatalog()
      return
    }

    await loadCatalog(newCategory as MedicalOrderCategory)
    const currentId = values.inventoryItemId
    if (currentId == null) return
    const opts = inventoryItemOptions.value
    // Only the placeholder means the catalog is empty (fetch failed or
    // nothing registered yet). Don't drop the selection in that case —
    // a transient failure shouldn't silently disconnect an already-billed
    // link when editing an existing order.
    const hasRealOptions = opts.some(opt => opt.value != null)
    if (!hasRealOptions) return
    const stillValid = opts.some(opt => opt.value === currentId)
    if (!stillValid) setFieldValue('inventoryItemId', null)
  }
)

// When the provider changes, fetch its tests and prune any selected test that the new
// provider does not offer (one order = one provider, AC7). `hydratingProvider` suppresses
// the prune during edit-mode hydration so we don't race the explicit id set.
const hydratingProvider = ref(false)
watch(
  () => values.labProviderId,
  async (providerId, previous) => {
    panelNotice.value = null
    if (providerId == null) return
    try {
      await labCatalogStore.fetchProviderTests(providerId, true)
    } catch {
      // Non-blocking — an empty test list just shows no options.
    }
    if (hydratingProvider.value || previous == null) return
    // Provider actually changed — drop selections not offered by the new provider.
    const valid = new Set(labCatalogStore.getProviderTests(providerId).map(pt => pt.id))
    const pruned = (values.labProviderTestIds ?? []).filter(id => valid.has(id))
    setFieldValue('labProviderTestIds', pruned)
  }
)

async function applyPanel(): Promise<void> {
  const panelId = selectedPanelId.value
  const providerId = values.labProviderId
  if (panelId == null || providerId == null) return
  try {
    const resolution = await labCatalogStore.resolvePanel(panelId, providerId)
    // Merge matched provider-test ids into the current selection (additive, idempotent).
    const merged = new Set(values.labProviderTestIds ?? [])
    resolution.matched.forEach(m => merged.add(m.labProviderTestId))
    setFieldValue('labProviderTestIds', Array.from(merged))

    const provider = labCatalogStore.providers.find(p => p.id === providerId)
    panelNotice.value = resolution.unmatchedTests.length
      ? {
          provider: provider?.name ?? '',
          tests: resolution.unmatchedTests.map(u => u.name).join(', ')
        }
      : null
  } catch (error) {
    showError(error)
  }
}

async function loadCatalog(category: MedicalOrderCategory): Promise<void> {
  if (!billableCategories.includes(category)) return
  try {
    if (category === MedicalOrderCategory.MEDICAMENTOS) {
      // Pull the whole medication catalog so the in-dropdown filter can
      // search by generic, commercial, or SKU without a round-trip per
      // keystroke. ~615 SKUs today; bump if the catalog grows materially.
      await pharmacyStore.fetchMedications(0, 1000)
    } else {
      await inventoryItemStore.fetchItems(0, 1000)
    }
  } catch {
    // Picker is optional — fall back to an empty list rather than blocking
    // the order form on a transient catalog fetch failure.
  }
}

// Use composable for type-safe Date ↔ string synchronization
const startDatePicker = useFormDateField(startDate)
const endDatePicker = useFormDateField(endDate)

watch(
  () => props.visible,
  async newValue => {
    if (!newValue) return
    selectedPanelId.value = null
    panelNotice.value = null
    if (props.order) {
      const labProviderId = props.order.labProvider?.id ?? null
      setValues({
        category: props.order.category,
        startDate: props.order.startDate,
        endDate: props.order.endDate || '',
        medication: props.order.medication || '',
        dosage: props.order.dosage || '',
        route: props.order.route || null,
        frequency: props.order.frequency || '',
        schedule: props.order.schedule || '',
        observations: props.order.observations || '',
        inventoryItemId: props.order.inventoryItemId || null,
        labProviderId,
        labProviderTestIds: []
      })

      if (props.order.category === MedicalOrderCategory.LABORATORIOS) {
        await loadLabCatalog()
        if (labProviderId != null) {
          // Hydrate the provider's tests first, then set the selected ids so the
          // provider watcher's prune doesn't race the assignment.
          hydratingProvider.value = true
          try {
            await labCatalogStore.fetchProviderTests(labProviderId, true)
          } catch {
            // Non-blocking.
          }
          setFieldValue(
            'labProviderTestIds',
            props.order.labTests.map(line => line.labProviderTestId)
          )
          hydratingProvider.value = false
        }
      }
    } else {
      resetForm()
    }
  }
)

const onSubmit = handleSubmit(async formValues => {
  loading.value = true
  try {
    const isLab = formValues.category === MedicalOrderCategory.LABORATORIOS
    // Convert empty strings to null for API. For labs, send provider + tests and clear the
    // inventory item; for everything else, send the inventory item and clear lab fields.
    const data = {
      category: formValues.category,
      startDate: formValues.startDate,
      endDate: formValues.endDate || null,
      medication: formValues.medication || null,
      dosage: formValues.dosage || null,
      route: formValues.route || null,
      frequency: formValues.frequency || null,
      schedule: formValues.schedule || null,
      observations: formValues.observations || null,
      inventoryItemId: isLab ? null : formValues.inventoryItemId || null,
      labProviderId: isLab ? (formValues.labProviderId ?? null) : null,
      labProviderTestIds: isLab ? (formValues.labProviderTestIds ?? []) : null
    }

    if (isEditMode.value && props.order) {
      await medicalOrderStore.updateMedicalOrder(props.admissionId, props.order.id, data)
    } else {
      await medicalOrderStore.createMedicalOrder(props.admissionId, data)
    }
    emit('saved')
    closeDialog()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function closeDialog() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="
      isEditMode ? t('medicalRecord.medicalOrder.edit') : t('medicalRecord.medicalOrder.add')
    "
    :modal="true"
    :closable="!loading"
    :style="{ width: '600px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form @submit="onSubmit" class="medical-order-form">
      <!-- Category -->
      <div class="form-field">
        <label for="category">{{ t('medicalRecord.medicalOrder.fields.category') }} *</label>
        <Select
          id="category"
          v-model="category"
          :options="categoryOptions"
          optionLabel="label"
          optionValue="value"
          :disabled="isEditMode"
          :class="{ 'p-invalid': errors.category }"
          class="w-full"
        />
        <small v-if="isEditMode" class="field-hint">
          {{ t('medicalRecord.medicalOrder.categoryLockedOnEdit') }}
        </small>
        <Message v-if="errors.category" severity="error" :closable="false">
          {{ errors.category }}
        </Message>
      </div>

      <!-- Dates Row — hidden for LABORATORIOS (a lab requisition has no start/end window;
           its request date is the creation date and results arrive via document upload). -->
      <div v-if="!showLabFields" class="form-row">
        <div class="form-field">
          <label for="startDate">{{ t('medicalRecord.medicalOrder.fields.startDate') }} *</label>
          <DatePicker
            id="startDate"
            v-model="startDatePicker"
            :class="{ 'p-invalid': errors.startDate }"
            class="w-full"
          />
          <Message v-if="errors.startDate" severity="error" :closable="false">
            {{ errors.startDate }}
          </Message>
        </div>
        <div class="form-field">
          <label for="endDate">{{ t('medicalRecord.medicalOrder.fields.endDate') }}</label>
          <DatePicker
            id="endDate"
            v-model="endDatePicker"
            :minDate="startDatePicker || undefined"
            class="w-full"
          />
        </div>
      </div>

      <!-- Medication Fields (shown for MEDICAMENTOS category) -->
      <div v-if="showMedicationFields" class="medication-fields">
        <div class="form-field">
          <label for="medication">{{ t('medicalRecord.medicalOrder.fields.medication') }}</label>
          <InputText
            id="medication"
            v-model="medication"
            :placeholder="t('medicalRecord.medicalOrder.placeholders.medication')"
            :class="{ 'p-invalid': errors.medication }"
            class="w-full"
          />
          <Message v-if="errors.medication" severity="error" :closable="false">
            {{ errors.medication }}
          </Message>
        </div>

        <div class="form-row">
          <div class="form-field">
            <label for="dosage">{{ t('medicalRecord.medicalOrder.fields.dosage') }}</label>
            <InputText
              id="dosage"
              v-model="dosage"
              :placeholder="t('medicalRecord.medicalOrder.placeholders.dosage')"
              :class="{ 'p-invalid': errors.dosage }"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="route">{{ t('medicalRecord.medicalOrder.fields.route') }}</label>
            <Select
              id="route"
              v-model="route"
              :options="routeOptions"
              optionLabel="label"
              optionValue="value"
              class="w-full"
            />
          </div>
        </div>

        <div class="form-row">
          <div class="form-field">
            <label for="frequency">{{ t('medicalRecord.medicalOrder.fields.frequency') }}</label>
            <InputText
              id="frequency"
              v-model="frequency"
              :placeholder="t('medicalRecord.medicalOrder.placeholders.frequency')"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label for="schedule">{{ t('medicalRecord.medicalOrder.fields.schedule') }}</label>
            <InputText
              id="schedule"
              v-model="schedule"
              :placeholder="t('medicalRecord.medicalOrder.placeholders.schedule')"
              class="w-full"
            />
          </div>
        </div>
      </div>

      <!-- Schedule / Horario (for non-medication, non-lab orders) -->
      <div v-else-if="!showLabFields" class="form-field">
        <label for="schedule">{{ t('medicalRecord.medicalOrder.fields.schedule') }}</label>
        <InputText
          id="schedule"
          v-model="schedule"
          :placeholder="t('medicalRecord.medicalOrder.placeholders.schedule')"
          class="w-full"
        />
      </div>

      <!-- Inventory Item (for billable categories) -->
      <div v-if="showInventoryItemSelector" class="form-field">
        <label for="inventoryItemId">{{
          t('medicalRecord.medicalOrder.fields.inventoryItem')
        }}</label>
        <Select
          id="inventoryItemId"
          v-model="inventoryItemId"
          :options="inventoryItemOptions"
          optionLabel="label"
          optionValue="value"
          filter
          :placeholder="t('medicalRecord.medicalOrder.placeholders.inventoryItem')"
          class="w-full"
        />
        <Message v-if="errors.inventoryItemId" severity="error" :closable="false">
          {{ errors.inventoryItemId }}
        </Message>
      </div>

      <!-- Lab provider + tests (for LABORATORIOS) -->
      <div v-if="showLabFields" class="lab-fields">
        <Message v-if="!labFieldsEditable" severity="info" :closable="false">
          {{ t('medicalRecord.medicalOrder.lab.editLockedNotice') }}
        </Message>

        <div class="form-field">
          <label for="labProviderId"
            >{{ t('medicalRecord.medicalOrder.fields.labProvider') }} *</label
          >
          <Select
            id="labProviderId"
            v-model="labProviderId"
            :options="providerOptions"
            optionLabel="label"
            optionValue="value"
            filter
            :disabled="!labFieldsEditable"
            :placeholder="t('medicalRecord.medicalOrder.placeholders.labProvider')"
            :class="{ 'p-invalid': errors.labProviderId }"
            class="w-full"
          />
          <Message v-if="errors.labProviderId" severity="error" :closable="false">
            {{ errors.labProviderId }}
          </Message>
        </div>

        <div class="form-field">
          <label for="labProviderTestIds"
            >{{ t('medicalRecord.medicalOrder.fields.labTests') }} *</label
          >
          <MultiSelect
            id="labProviderTestIds"
            v-model="labProviderTestIds"
            :options="labTestOptions"
            optionLabel="label"
            optionValue="value"
            display="chip"
            filter
            :disabled="!labFieldsEditable || labProviderId == null"
            :placeholder="t('medicalRecord.medicalOrder.placeholders.labTests')"
            :class="{ 'p-invalid': errors.labProviderTestIds }"
            class="w-full"
          />
          <Message v-if="errors.labProviderTestIds" severity="error" :closable="false">
            {{ errors.labProviderTestIds }}
          </Message>
        </div>

        <!-- Apply panel -->
        <div v-if="labFieldsEditable" class="form-field">
          <label for="labPanel">{{ t('medicalRecord.medicalOrder.fields.labPanel') }}</label>
          <div class="panel-row">
            <Select
              id="labPanel"
              v-model="selectedPanelId"
              :options="panelOptions"
              optionLabel="label"
              optionValue="value"
              :disabled="labProviderId == null"
              :placeholder="t('medicalRecord.medicalOrder.placeholders.labPanel')"
              class="w-full"
            />
            <Button
              type="button"
              :label="t('medicalRecord.medicalOrder.lab.applyPanel')"
              severity="secondary"
              :disabled="labProviderId == null || selectedPanelId == null"
              @click="applyPanel"
            />
          </div>
        </div>

        <Message v-if="panelNotice" severity="warn" :closable="true">
          {{
            t('medicalRecord.medicalOrder.lab.unmatchedNotice', {
              provider: panelNotice.provider,
              tests: panelNotice.tests
            })
          }}
        </Message>
      </div>

      <!-- Observations -->
      <div class="form-field">
        <RichTextEditor
          v-model="observations"
          :label="t('medicalRecord.medicalOrder.fields.observations')"
          :placeholder="t('medicalRecord.medicalOrder.placeholders.observations')"
          :rows="3"
        />
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
.medical-order-form {
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
  color: var(--p-text-color);
}

.field-hint {
  color: var(--p-text-muted-color);
  font-size: 0.8125rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.medication-fields {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
}

.lab-fields {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
}

.panel-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
