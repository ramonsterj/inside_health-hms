<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { toTypedSchema } from '@/validation/zodI18n'
import { progressNoteSchema, type ProgressNoteFormData } from '@/validation/medicalRecord'
import { useProgressNoteStore } from '@/stores/progressNote'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { ProgressNoteResponse } from '@/types/medicalRecord'
import RichTextEditor from '@/components/common/RichTextEditor.vue'

const props = defineProps<{
  visible: boolean
  admissionId: number
  note?: ProgressNoteResponse | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()
const progressNoteStore = useProgressNoteStore()

const loading = ref(false)
const isEditMode = computed(() => !!props.note)

const { defineField, handleSubmit, setValues, resetForm } = useForm<ProgressNoteFormData>({
  validationSchema: toTypedSchema(progressNoteSchema),
  initialValues: {
    subjectiveData: '',
    objectiveData: '',
    analysis: '',
    actionPlans: ''
  }
})

const [subjectiveData] = defineField('subjectiveData')
const [objectiveData] = defineField('objectiveData')
const [analysis] = defineField('analysis')
const [actionPlans] = defineField('actionPlans')

watch(
  () => props.visible,
  newValue => {
    if (newValue) {
      if (props.note) {
        setValues({
          subjectiveData: props.note.subjectiveData || '',
          objectiveData: props.note.objectiveData || '',
          analysis: props.note.analysis || '',
          actionPlans: props.note.actionPlans || ''
        })
      } else {
        resetForm()
      }
    }
  }
)

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    // Convert empty strings to null for API
    const data = {
      subjectiveData: values.subjectiveData || null,
      objectiveData: values.objectiveData || null,
      analysis: values.analysis || null,
      actionPlans: values.actionPlans || null
    }

    if (isEditMode.value && props.note) {
      await progressNoteStore.updateProgressNote(props.admissionId, props.note.id, data)
    } else {
      await progressNoteStore.createProgressNote(props.admissionId, data)
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
      isEditMode ? t('medicalRecord.progressNote.edit') : t('medicalRecord.progressNote.add')
    "
    :modal="true"
    :closable="!loading"
    :style="{ width: '700px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form @submit="onSubmit" class="progress-note-form">
      <p class="form-description">
        {{ t('medicalRecord.progressNote.soapDescription') }}
      </p>

      <!-- Subjective -->
      <div class="form-field">
        <RichTextEditor
          v-model="subjectiveData"
          :label="t('medicalRecord.progressNote.fields.subjectiveData')"
          :placeholder="t('medicalRecord.progressNote.placeholders.subjectiveData')"
          :rows="3"
        />
      </div>

      <!-- Objective -->
      <div class="form-field">
        <RichTextEditor
          v-model="objectiveData"
          :label="t('medicalRecord.progressNote.fields.objectiveData')"
          :placeholder="t('medicalRecord.progressNote.placeholders.objectiveData')"
          :rows="3"
        />
      </div>

      <!-- Assessment/Analysis -->
      <div class="form-field">
        <RichTextEditor
          v-model="analysis"
          :label="t('medicalRecord.progressNote.fields.analysis')"
          :placeholder="t('medicalRecord.progressNote.placeholders.analysis')"
          :rows="3"
        />
      </div>

      <!-- Plan -->
      <div class="form-field">
        <RichTextEditor
          v-model="actionPlans"
          :label="t('medicalRecord.progressNote.fields.actionPlans')"
          :placeholder="t('medicalRecord.progressNote.placeholders.actionPlans')"
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
.progress-note-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-description {
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
  margin: 0;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
