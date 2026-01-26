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
import ColorPicker from 'primevue/colorpicker'
import Message from 'primevue/message'
import { useTriageCodeStore } from '@/stores/triageCode'
import { triageCodeSchema, type TriageCodeFormData } from '@/validation/triageCode'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError, showSuccess } = useErrorHandler()
const triageCodeStore = useTriageCodeStore()

const loading = ref(false)
const isEditMode = computed(() => !!route.params.id)
const triageCodeId = computed(() => Number(route.params.id) || null)

const { defineField, handleSubmit, errors, setValues } = useForm<TriageCodeFormData>({
  validationSchema: toTypedSchema(triageCodeSchema),
  initialValues: {
    code: '',
    color: '#FF0000',
    description: '',
    displayOrder: 0
  }
})

const [code] = defineField('code')
const [color] = defineField('color')
const [description] = defineField('description')
const [displayOrder] = defineField('displayOrder')

onMounted(async () => {
  if (isEditMode.value && triageCodeId.value) {
    await loadTriageCode()
  }
})

async function loadTriageCode() {
  loading.value = true
  try {
    const triageCode = await triageCodeStore.fetchTriageCode(triageCodeId.value!)
    setValues({
      code: triageCode.code,
      color: triageCode.color,
      description: triageCode.description || '',
      displayOrder: triageCode.displayOrder
    })
  } catch (error) {
    showError(error)
    router.push({ name: 'triage-codes' })
  } finally {
    loading.value = false
  }
}

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    const data = {
      ...values,
      color: values.color.startsWith('#') ? values.color : `#${values.color}`
    }

    if (isEditMode.value && triageCodeId.value) {
      await triageCodeStore.updateTriageCode(triageCodeId.value, data)
      showSuccess('triageCode.updated')
    } else {
      await triageCodeStore.createTriageCode(data)
      showSuccess('triageCode.created')
    }
    router.push({ name: 'triage-codes' })
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function cancel() {
  router.push({ name: 'triage-codes' })
}
</script>

<template>
  <div class="triage-code-form-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditMode ? t('triageCode.edit') : t('triageCode.new') }}
      </h1>
    </div>

    <Card>
      <template #content>
        <form @submit="onSubmit" class="form-grid">
          <div class="form-field">
            <label for="code">{{ t('triageCode.code') }} *</label>
            <InputText id="code" v-model="code" :class="{ 'p-invalid': errors.code }" />
            <Message v-if="errors.code" severity="error" :closable="false">
              {{ errors.code }}
            </Message>
          </div>

          <div class="form-field">
            <label for="color">{{ t('triageCode.color') }} *</label>
            <div class="color-picker-wrapper">
              <ColorPicker v-model="color" inputId="color" format="hex" />
              <InputText
                v-model="color"
                class="color-input"
                :class="{ 'p-invalid': errors.color }"
              />
            </div>
            <Message v-if="errors.color" severity="error" :closable="false">
              {{ errors.color }}
            </Message>
          </div>

          <div class="form-field full-width">
            <label for="description">{{ t('triageCode.description') }}</label>
            <Textarea
              id="description"
              v-model="description"
              rows="3"
              :class="{ 'p-invalid': errors.description }"
            />
            <Message v-if="errors.description" severity="error" :closable="false">
              {{ errors.description }}
            </Message>
          </div>

          <div class="form-field">
            <label for="displayOrder">{{ t('triageCode.displayOrder') }}</label>
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
.triage-code-form-page {
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

.color-picker-wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.color-input {
  flex: 1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}
</style>
