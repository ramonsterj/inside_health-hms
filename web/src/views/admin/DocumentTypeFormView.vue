<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@/validation/zodI18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { useDocumentTypeStore } from '@/stores/documentType'
import { documentTypeSchema, type DocumentTypeFormData } from '@/validation/document'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError, showSuccess, setFieldErrorsFromResponse } = useErrorHandler()
const documentTypeStore = useDocumentTypeStore()

const loading = ref(false)
const isEditMode = computed(() => !!route.params.id)
const documentTypeId = computed(() => Number(route.params.id) || null)

const { defineField, handleSubmit, errors, setValues, setErrors } = useForm<DocumentTypeFormData>({
  validationSchema: toTypedSchema(documentTypeSchema),
  initialValues: {
    code: '',
    name: '',
    description: null,
    displayOrder: 0
  }
})

const [code] = defineField('code')
const [name] = defineField('name')
const [description] = defineField('description')
const [displayOrder] = defineField('displayOrder')

onMounted(async () => {
  if (isEditMode.value && documentTypeId.value) {
    await loadDocumentType()
  }
})

async function loadDocumentType() {
  loading.value = true
  try {
    const docType = await documentTypeStore.fetchDocumentType(documentTypeId.value!)
    setValues({
      code: docType.code,
      name: docType.name,
      description: docType.description,
      displayOrder: docType.displayOrder
    })
  } catch (error) {
    showError(error)
    router.push({ name: 'document-types' })
  } finally {
    loading.value = false
  }
}

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    if (isEditMode.value && documentTypeId.value) {
      await documentTypeStore.updateDocumentType(documentTypeId.value, values)
      showSuccess('documentType.updated')
    } else {
      await documentTypeStore.createDocumentType(values)
      showSuccess('documentType.created')
    }
    router.push({ name: 'document-types' })
  } catch (error) {
    if (!setFieldErrorsFromResponse(setErrors, error)) {
      showError(error)
    }
  } finally {
    loading.value = false
  }
})

function cancel() {
  router.push({ name: 'document-types' })
}
</script>

<template>
  <div class="document-type-form-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditMode ? t('documentType.edit') : t('documentType.new') }}
      </h1>
    </div>

    <Card>
      <template #content>
        <form @submit="onSubmit" class="form-grid">
          <div class="form-field">
            <label for="code">{{ t('documentType.code') }} *</label>
            <InputText
              id="code"
              v-model="code"
              :class="{ 'p-invalid': errors.code }"
              placeholder="CONSENT_EXAMPLE"
            />
            <small class="field-help">{{ t('documentType.codeHelp') }}</small>
            <Message v-if="errors.code" severity="error" :closable="false">
              {{ errors.code }}
            </Message>
          </div>

          <div class="form-field">
            <label for="name">{{ t('documentType.name') }} *</label>
            <InputText id="name" v-model="name" :class="{ 'p-invalid': errors.name }" />
            <Message v-if="errors.name" severity="error" :closable="false">
              {{ errors.name }}
            </Message>
          </div>

          <div class="form-field full-width">
            <label for="description">{{ t('documentType.description') }}</label>
            <Textarea
              id="description"
              v-model="description"
              :class="{ 'p-invalid': errors.description }"
              rows="3"
            />
            <Message v-if="errors.description" severity="error" :closable="false">
              {{ errors.description }}
            </Message>
          </div>

          <div class="form-field">
            <label for="displayOrder">{{ t('documentType.displayOrder') }}</label>
            <InputNumber
              id="displayOrder"
              v-model="displayOrder"
              :min="0"
              :class="{ 'p-invalid': errors.displayOrder }"
            />
            <Message v-if="errors.displayOrder" severity="error" :closable="false">
              {{ errors.displayOrder }}
            </Message>
          </div>

          <div class="form-actions full-width">
            <Button
              type="button"
              :label="t('common.cancel')"
              severity="secondary"
              outlined
              @click="cancel"
            />
            <Button type="submit" :label="t('common.save')" :loading="loading" />
          </div>
        </form>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.document-type-form-page {
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field.full-width {
  grid-column: span 2;
}

.form-field label {
  font-weight: 500;
}

.field-help {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}
</style>
