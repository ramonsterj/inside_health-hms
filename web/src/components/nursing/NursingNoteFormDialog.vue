<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import RichTextEditor from '@/components/common/RichTextEditor.vue'
import { nursingNoteSchema, type NursingNoteFormData } from '@/validation/nursing'
import { useNursingNoteStore } from '@/stores/nursingNote'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { NursingNoteResponse } from '@/types/nursing'

const props = defineProps<{
  admissionId: number
  noteToEdit?: NursingNoteResponse | null
}>()

const visible = defineModel<boolean>('visible', { required: true })

const emit = defineEmits<{
  saved: []
}>()

const { t } = useI18n()
const nursingNoteStore = useNursingNoteStore()
const { showError } = useErrorHandler()

const isEdit = computed(() => !!props.noteToEdit)

const { handleSubmit, resetForm, errors, defineField, meta } = useForm<NursingNoteFormData>({
  validationSchema: toTypedSchema(nursingNoteSchema)
})

const [description] = defineField('description')

const saving = ref(false)

watch(visible, newValue => {
  if (newValue) {
    if (props.noteToEdit) {
      // Edit mode: populate form
      resetForm({
        values: {
          description: props.noteToEdit.description
        }
      })
    } else {
      // Create mode: clear form
      resetForm({
        values: {
          description: ''
        }
      })
    }
  }
})

const onSubmit = handleSubmit(async values => {
  saving.value = true
  try {
    if (isEdit.value && props.noteToEdit) {
      await nursingNoteStore.updateNursingNote(props.admissionId, props.noteToEdit.id, {
        description: values.description
      })
    } else {
      await nursingNoteStore.createNursingNote(props.admissionId, {
        description: values.description
      })
    }
    visible.value = false
    emit('saved')
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
})

function handleCancel() {
  visible.value = false
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    :header="isEdit ? t('nursing.notes.edit') : t('nursing.notes.add')"
    modal
    :style="{ width: '50vw' }"
    :breakpoints="{ '960px': '75vw', '640px': '90vw' }"
    :closable="!saving"
    :closeOnEscape="!saving"
  >
    <form @submit.prevent="onSubmit">
      <div class="form-field">
        <RichTextEditor
          v-model="description"
          :label="t('nursing.notes.description')"
          :placeholder="t('nursing.notes.descriptionPlaceholder')"
          :rows="8"
          :invalid="!!errors.description"
          :required="true"
        />
        <small v-if="errors.description" class="p-error">
          {{ t(errors.description) }}
        </small>
      </div>
    </form>

    <template #footer>
      <Button
        :label="t('common.cancel')"
        severity="secondary"
        text
        :disabled="saving"
        @click="handleCancel"
      />
      <Button
        :label="t('common.save')"
        icon="pi pi-check"
        :loading="saving"
        :disabled="!meta.valid"
        @click="onSubmit"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
</style>
