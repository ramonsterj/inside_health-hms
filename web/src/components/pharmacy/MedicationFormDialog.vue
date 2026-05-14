<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { createMedicationSchema } from '@/validation/pharmacy'
import { usePharmacyStore } from '@/stores/pharmacy'
import { useErrorHandler } from '@/composables/useErrorHandler'
import {
  DosageForm,
  AdministrationRoute,
  MedicationSection,
  type Medication
} from '@/types/pharmacy'

const props = defineProps<{
  visible: boolean
  medication?: Medication | null
}>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved', medication: Medication): void
}>()

const { t } = useI18n()
const pharmacyStore = usePharmacyStore()
const { showError, showSuccess } = useErrorHandler()

const submitting = ref(false)

const { defineField, handleSubmit, errors, resetForm, setValues } = useForm({
  validationSchema: toTypedSchema(createMedicationSchema),
  initialValues: {
    name: '',
    description: '',
    price: 0,
    cost: 0,
    sku: '',
    restockLevel: 0,
    genericName: '',
    commercialName: '',
    strength: '',
    dosageForm: DosageForm.TABLET,
    route: null,
    controlled: false,
    atcCode: '',
    section: MedicationSection.PSIQUIATRICO,
    active: true
  }
})

const [name] = defineField('name')
const [description] = defineField('description')
const [price] = defineField('price')
const [cost] = defineField('cost')
const [sku] = defineField('sku')
const [restockLevel] = defineField('restockLevel')
const [genericName] = defineField('genericName')
const [commercialName] = defineField('commercialName')
const [strength] = defineField('strength')
const [dosageForm] = defineField('dosageForm')
const [route] = defineField('route')
const [controlled] = defineField('controlled')
const [atcCode] = defineField('atcCode')
const [section] = defineField('section')
const [active] = defineField('active')

watch(
  () => props.visible,
  v => {
    if (v) {
      if (props.medication) {
        setValues({
          name: props.medication.name,
          description: props.medication.description ?? '',
          price: props.medication.price,
          cost: props.medication.cost,
          sku: props.medication.sku ?? '',
          restockLevel: props.medication.restockLevel ?? 0,
          genericName: props.medication.genericName,
          commercialName: props.medication.commercialName ?? '',
          strength: props.medication.strength ?? '',
          dosageForm: props.medication.dosageForm,
          route: props.medication.route ?? null,
          controlled: props.medication.controlled,
          atcCode: props.medication.atcCode ?? '',
          section: props.medication.section,
          active: props.medication.active
        })
      } else {
        resetForm()
      }
    }
  }
)

const dosageOptions = computed(() =>
  Object.values(DosageForm).map(v => ({
    label: t(`pharmacy.dosageForm.${v}`),
    value: v
  }))
)
const routeOptions = computed(() => [
  { label: t('pharmacy.medication.routeNone'), value: null },
  ...Object.values(AdministrationRoute).map(v => ({
    label: t(`pharmacy.route.${v}`),
    value: v
  }))
])
const sectionOptions = computed(() =>
  Object.values(MedicationSection).map(v => ({
    label: t(`pharmacy.section.${v}`),
    value: v
  }))
)

const onSubmit = handleSubmit(async values => {
  submitting.value = true
  try {
    const payload = {
      name: values.name,
      description: values.description || null,
      price: Number(values.price),
      cost: Number(values.cost),
      sku: values.sku || null,
      restockLevel: Number(values.restockLevel ?? 0),
      genericName: values.genericName,
      commercialName: values.commercialName || null,
      strength: values.strength || null,
      dosageForm: values.dosageForm,
      route: values.route ?? null,
      controlled: values.controlled,
      atcCode: values.atcCode || null,
      section: values.section,
      active: values.active
    }
    const saved = props.medication
      ? await pharmacyStore.updateMedication(props.medication.itemId, payload)
      : await pharmacyStore.createMedication(payload)
    showSuccess(props.medication ? 'pharmacy.medication.updated' : 'pharmacy.medication.created')
    emit('saved', saved)
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
    :header="medication ? t('pharmacy.medication.edit') : t('pharmacy.medication.new')"
    modal
    :style="{ width: '720px' }"
  >
    <form @submit.prevent="onSubmit" class="form-grid">
      <div class="field col-span-2">
        <label>{{ t('pharmacy.medication.name') }}</label>
        <InputText v-model="name" />
        <Message v-if="errors.name" severity="error" :closable="false">
          {{ errors.name }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.sku') }}</label>
        <InputText v-model="sku" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.genericName') }}</label>
        <InputText v-model="genericName" />
        <Message v-if="errors.genericName" severity="error" :closable="false">
          {{ errors.genericName }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.commercialName') }}</label>
        <InputText v-model="commercialName" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.strength') }}</label>
        <InputText v-model="strength" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.dosageForm') }}</label>
        <Select
          v-model="dosageForm"
          :options="dosageOptions"
          optionLabel="label"
          optionValue="value"
        />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.route') }}</label>
        <Select v-model="route" :options="routeOptions" optionLabel="label" optionValue="value" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.section') }}</label>
        <Select
          v-model="section"
          :options="sectionOptions"
          optionLabel="label"
          optionValue="value"
        />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.price') }}</label>
        <InputNumber v-model="price" mode="decimal" :minFractionDigits="2" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.cost') }}</label>
        <InputNumber v-model="cost" mode="decimal" :minFractionDigits="2" />
      </div>
      <div class="field">
        <label>{{ t('inventory.item.restockLevel') }}</label>
        <InputNumber v-model="restockLevel" :min="0" showButtons />
        <Message v-if="errors.restockLevel" severity="error" :closable="false">
          {{ errors.restockLevel }}
        </Message>
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.atcCode') }}</label>
        <InputText v-model="atcCode" />
      </div>
      <div class="field col-span-2">
        <label>{{ t('pharmacy.medication.description') }}</label>
        <Textarea v-model="description" rows="2" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.controlled') }}</label>
        <ToggleSwitch v-model="controlled" />
      </div>
      <div class="field">
        <label>{{ t('pharmacy.medication.active') }}</label>
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
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.col-span-2 {
  grid-column: span 2;
}
</style>
