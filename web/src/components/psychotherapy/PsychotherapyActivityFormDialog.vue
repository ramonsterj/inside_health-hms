<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { toTypedSchema } from '@/validation/zodI18n'
import {
  psychotherapyActivitySchema,
  type PsychotherapyActivityFormData
} from '@/validation/psychotherapy'
import { usePsychotherapyActivityStore } from '@/stores/psychotherapyActivity'
import { usePsychotherapyCategoryStore } from '@/stores/psychotherapyCategory'
import { useErrorHandler } from '@/composables/useErrorHandler'

const props = defineProps<{
  visible: boolean
  admissionId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()
const activityStore = usePsychotherapyActivityStore()
const categoryStore = usePsychotherapyCategoryStore()

const loading = ref(false)

const { defineField, handleSubmit, errors, resetForm } = useForm<PsychotherapyActivityFormData>({
  validationSchema: toTypedSchema(psychotherapyActivitySchema),
  initialValues: {
    description: ''
  }
})

const [categoryId] = defineField('categoryId')
const [description] = defineField('description')

onMounted(async () => {
  await categoryStore.fetchActiveCategories()
})

watch(
  () => props.visible,
  newValue => {
    if (newValue) {
      resetForm()
    }
  }
)

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    await activityStore.createActivity(props.admissionId, {
      categoryId: values.categoryId,
      description: values.description
    })
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
    :header="t('psychotherapy.activity.add')"
    :modal="true"
    :closable="!loading"
    :style="{ width: '600px' }"
    :breakpoints="{ '768px': '90vw' }"
  >
    <form @submit="onSubmit" class="activity-form">
      <!-- Category -->
      <div class="form-field">
        <label for="categoryId">{{ t('psychotherapy.activity.category') }} *</label>
        <Select
          id="categoryId"
          v-model="categoryId"
          :options="categoryStore.activeCategories"
          optionLabel="name"
          optionValue="id"
          :placeholder="t('psychotherapy.activity.selectCategory')"
          :class="{ 'p-invalid': errors.categoryId }"
          class="w-full"
        />
        <Message v-if="errors.categoryId" severity="error" :closable="false">
          {{ errors.categoryId }}
        </Message>
      </div>

      <!-- Description -->
      <div class="form-field">
        <label for="description">{{ t('psychotherapy.activity.description') }} *</label>
        <Textarea
          id="description"
          v-model="description"
          rows="6"
          :placeholder="t('psychotherapy.activity.descriptionPlaceholder')"
          :class="{ 'p-invalid': errors.description }"
          class="w-full"
        />
        <div class="char-count">{{ description?.length || 0 }} / 2000</div>
        <Message v-if="errors.description" severity="error" :closable="false">
          {{ errors.description }}
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
.activity-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 500;
}

.char-count {
  text-align: right;
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
