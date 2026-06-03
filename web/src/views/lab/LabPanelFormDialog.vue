<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import MultiSelect from 'primevue/multiselect'
import Checkbox from 'primevue/checkbox'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import { labPanelSchema, type LabPanelFormData } from '@/validation/lab'
import { useLabCatalogStore } from '@/stores/labCatalog'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { LabPanel } from '@/types/lab'

const props = defineProps<{
  visible: boolean
  panel?: LabPanel | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const store = useLabCatalogStore()

const isEdit = computed(() => !!props.panel)
const saving = ref(false)

const testOptions = computed(() =>
  store.tests.filter(test => test.active).map(test => ({ label: test.name, value: test.id }))
)

const { defineField, handleSubmit, errors, resetForm } = useForm<LabPanelFormData>({
  validationSchema: toTypedSchema(labPanelSchema),
  initialValues: { name: '', active: true, labTestIds: [] }
})
const [name] = defineField('name')
const [active] = defineField('active')
const [labTestIds] = defineField('labTestIds')

watch(
  () => props.visible,
  async newValue => {
    if (!newValue) return
    try {
      await store.fetchTests(false)
    } catch {
      // Non-blocking — empty options just show no choices.
    }
    if (props.panel) {
      resetForm({
        values: {
          name: props.panel.name,
          active: props.panel.active,
          labTestIds: props.panel.items.map(item => item.labTestId)
        }
      })
    } else {
      resetForm({ values: { name: '', active: true, labTestIds: [] } })
    }
  }
)

const onSubmit = handleSubmit(async values => {
  saving.value = true
  try {
    if (props.panel) {
      await store.updatePanel(props.panel.id, values)
      showSuccess('lab.panel.updated')
    } else {
      await store.createPanel(values)
      showSuccess('lab.panel.created')
    }
    emit('saved')
    emit('update:visible', false)
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
})
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    :header="isEdit ? t('lab.panel.edit') : t('lab.panel.new')"
    modal
    :style="{ width: '520px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form class="lab-form" @submit.prevent="onSubmit">
      <div class="form-field">
        <label for="panel-name">{{ t('lab.panel.name') }} *</label>
        <InputText id="panel-name" v-model="name" :class="{ 'p-invalid': errors.name }" />
        <Message v-if="errors.name" severity="error" :closable="false">{{ errors.name }}</Message>
      </div>
      <div class="form-field">
        <label for="panel-tests">{{ t('lab.panel.tests') }} *</label>
        <MultiSelect
          id="panel-tests"
          v-model="labTestIds"
          :options="testOptions"
          optionLabel="label"
          optionValue="value"
          display="chip"
          filter
          :placeholder="t('lab.panel.testsPlaceholder')"
          :class="{ 'p-invalid': errors.labTestIds }"
        />
        <Message v-if="errors.labTestIds" severity="error" :closable="false">
          {{ errors.labTestIds }}
        </Message>
      </div>
      <div class="form-field-inline">
        <Checkbox v-model="active" inputId="panel-active" binary />
        <label for="panel-active">{{ t('lab.panel.active') }}</label>
      </div>
    </form>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        severity="secondary"
        :disabled="saving"
        @click="emit('update:visible', false)"
      />
      <Button :label="t('common.save')" :loading="saving" @click="onSubmit" />
    </template>
  </Dialog>
</template>

<style scoped>
.lab-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-field-inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
