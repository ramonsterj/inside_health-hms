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
import ToggleSwitch from 'primevue/toggleswitch'
import Message from 'primevue/message'
import { useInventoryCategoryStore } from '@/stores/inventoryCategory'
import {
  inventoryCategorySchema,
  type InventoryCategoryFormData
} from '@/validation/inventory'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError, showSuccess } = useErrorHandler()
const categoryStore = useInventoryCategoryStore()

const loading = ref(false)
const isEditMode = computed(() => !!route.params.id)
const categoryId = computed(() => Number(route.params.id) || null)

const { defineField, handleSubmit, errors, setValues } = useForm<InventoryCategoryFormData>({
  validationSchema: toTypedSchema(inventoryCategorySchema),
  initialValues: {
    name: '',
    description: '',
    displayOrder: 0,
    active: true
  }
})

const [name] = defineField('name')
const [description] = defineField('description')
const [displayOrder] = defineField('displayOrder')
const [active] = defineField('active')

onMounted(async () => {
  if (isEditMode.value && categoryId.value) {
    await loadCategory()
  }
})

async function loadCategory() {
  loading.value = true
  try {
    const category = await categoryStore.fetchCategory(categoryId.value!)
    setValues({
      name: category.name,
      description: category.description || '',
      displayOrder: category.displayOrder,
      active: category.active
    })
  } catch (error) {
    showError(error)
    router.push({ name: 'inventory-categories' })
  } finally {
    loading.value = false
  }
}

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  try {
    const data = {
      ...values,
      description: values.description || null
    }

    if (isEditMode.value && categoryId.value) {
      await categoryStore.updateCategory(categoryId.value, data)
      showSuccess('inventory.category.updated')
    } else {
      await categoryStore.createCategory(data)
      showSuccess('inventory.category.created')
    }
    router.push({ name: 'inventory-categories' })
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function cancel() {
  router.push({ name: 'inventory-categories' })
}
</script>

<template>
  <div class="category-form-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditMode ? t('inventory.category.edit') : t('inventory.category.new') }}
      </h1>
    </div>

    <Card>
      <template #content>
        <form @submit="onSubmit" class="form-grid">
          <div class="form-field full-width">
            <label for="name">{{ t('inventory.category.name') }} *</label>
            <InputText id="name" v-model="name" :class="{ 'p-invalid': errors.name }" />
            <Message v-if="errors.name" severity="error" :closable="false">
              {{ errors.name }}
            </Message>
          </div>

          <div class="form-field full-width">
            <label for="description">{{ t('inventory.category.description') }}</label>
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
            <label for="displayOrder">{{ t('inventory.category.displayOrder') }}</label>
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

          <div class="form-field">
            <label for="active">{{ t('inventory.category.active') }}</label>
            <div class="toggle-wrapper">
              <ToggleSwitch id="active" v-model="active" />
              <span class="toggle-label">{{ active ? t('common.yes') : t('common.no') }}</span>
            </div>
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
.category-form-page {
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

.toggle-wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.toggle-label {
  color: var(--p-text-muted-color);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}
</style>
