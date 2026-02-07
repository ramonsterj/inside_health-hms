<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
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
import Select from 'primevue/select'
import ToggleSwitch from 'primevue/toggleswitch'
import Message from 'primevue/message'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { useInventoryCategoryStore } from '@/stores/inventoryCategory'
import { inventoryItemSchema, type InventoryItemFormData } from '@/validation/inventory'
import { PricingType, TimeUnit } from '@/types/inventoryItem'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError, showSuccess } = useErrorHandler()
const itemStore = useInventoryItemStore()
const categoryStore = useInventoryCategoryStore()

const loading = ref(false)
const isEditMode = computed(() => !!route.params.id)
const itemId = computed(() => Number(route.params.id) || null)

const categoryOptions = computed(() =>
  categoryStore.activeCategories.map((c) => ({ label: c.name, value: c.id }))
)

const pricingTypeOptions = computed(() => [
  { label: t('inventory.item.pricingTypes.FLAT'), value: PricingType.FLAT },
  { label: t('inventory.item.pricingTypes.TIME_BASED'), value: PricingType.TIME_BASED }
])

const timeUnitOptions = computed(() => [
  { label: t('inventory.item.timeUnits.MINUTES'), value: TimeUnit.MINUTES },
  { label: t('inventory.item.timeUnits.HOURS'), value: TimeUnit.HOURS }
])

const { defineField, handleSubmit, errors, setValues } = useForm<InventoryItemFormData>({
  validationSchema: toTypedSchema(inventoryItemSchema),
  initialValues: {
    name: '',
    description: '',
    categoryId: undefined as unknown as number,
    price: 0,
    cost: 0,
    restockLevel: 0,
    pricingType: PricingType.FLAT,
    timeUnit: null,
    timeInterval: null,
    active: true
  }
})

const [name] = defineField('name')
const [description] = defineField('description')
const [categoryId] = defineField('categoryId')
const [price] = defineField('price')
const [cost] = defineField('cost')
const [restockLevel] = defineField('restockLevel')
const [pricingType] = defineField('pricingType')
const [timeUnit] = defineField('timeUnit')
const [timeInterval] = defineField('timeInterval')
const [active] = defineField('active')

const isTimeBased = computed(() => pricingType.value === PricingType.TIME_BASED)

watch(pricingType, (newVal) => {
  if (newVal === PricingType.FLAT) {
    timeUnit.value = null
    timeInterval.value = null
  }
})

onMounted(async () => {
  await categoryStore.fetchActiveCategories()
  if (isEditMode.value && itemId.value) {
    await loadItem()
  }
})

async function loadItem() {
  loading.value = true
  try {
    const item = await itemStore.fetchItem(itemId.value!)
    setValues({
      name: item.name,
      description: item.description || '',
      categoryId: item.category.id,
      price: item.price,
      cost: item.cost,
      restockLevel: item.restockLevel,
      pricingType: item.pricingType,
      timeUnit: item.timeUnit,
      timeInterval: item.timeInterval,
      active: item.active
    })
  } catch (error) {
    showError(error)
    router.push({ name: 'inventory-items' })
  } finally {
    loading.value = false
  }
}

const onSubmit = handleSubmit(async (values) => {
  loading.value = true
  try {
    const data = {
      ...values,
      description: values.description || null,
      timeUnit: values.pricingType === PricingType.TIME_BASED ? values.timeUnit : null,
      timeInterval: values.pricingType === PricingType.TIME_BASED ? values.timeInterval : null
    }

    if (isEditMode.value && itemId.value) {
      await itemStore.updateItem(itemId.value, data)
      showSuccess('inventory.item.updated')
    } else {
      await itemStore.createItem(data)
      showSuccess('inventory.item.created')
    }
    router.push({ name: 'inventory-items' })
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function cancel() {
  router.push({ name: 'inventory-items' })
}
</script>

<template>
  <div class="item-form-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditMode ? t('inventory.item.edit') : t('inventory.item.new') }}
      </h1>
    </div>

    <Card>
      <template #content>
        <form @submit="onSubmit" class="form-grid">
          <div class="form-field full-width">
            <label for="name">{{ t('inventory.item.name') }} *</label>
            <InputText id="name" v-model="name" :class="{ 'p-invalid': errors.name }" />
            <Message v-if="errors.name" severity="error" :closable="false">
              {{ errors.name }}
            </Message>
          </div>

          <div class="form-field full-width">
            <label for="description">{{ t('inventory.item.description') }}</label>
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
            <label for="categoryId">{{ t('inventory.item.category') }} *</label>
            <Select
              id="categoryId"
              v-model="categoryId"
              :options="categoryOptions"
              optionLabel="label"
              optionValue="value"
              :class="{ 'p-invalid': errors.categoryId }"
            />
            <Message v-if="errors.categoryId" severity="error" :closable="false">
              {{ errors.categoryId }}
            </Message>
          </div>

          <div class="form-field">
            <label for="pricingType">{{ t('inventory.item.pricingType') }} *</label>
            <Select
              id="pricingType"
              v-model="pricingType"
              :options="pricingTypeOptions"
              optionLabel="label"
              optionValue="value"
            />
          </div>

          <div class="form-field">
            <label for="price">{{ t('inventory.item.price') }} *</label>
            <InputNumber
              id="price"
              v-model="price"
              :min="0"
              :minFractionDigits="2"
              :maxFractionDigits="2"
              :class="{ 'p-invalid': errors.price }"
            />
            <Message v-if="errors.price" severity="error" :closable="false">
              {{ errors.price }}
            </Message>
          </div>

          <div class="form-field">
            <label for="cost">{{ t('inventory.item.cost') }} *</label>
            <InputNumber
              id="cost"
              v-model="cost"
              :min="0"
              :minFractionDigits="2"
              :maxFractionDigits="2"
              :class="{ 'p-invalid': errors.cost }"
            />
            <Message v-if="errors.cost" severity="error" :closable="false">
              {{ errors.cost }}
            </Message>
          </div>

          <div class="form-field">
            <label for="restockLevel">{{ t('inventory.item.restockLevel') }}</label>
            <InputNumber
              id="restockLevel"
              v-model="restockLevel"
              :min="0"
              :class="{ 'p-invalid': errors.restockLevel }"
            />
          </div>

          <div class="form-field">
            <label for="active">{{ t('inventory.item.active') }}</label>
            <div class="toggle-wrapper">
              <ToggleSwitch id="active" v-model="active" />
              <span class="toggle-label">{{ active ? t('common.yes') : t('common.no') }}</span>
            </div>
          </div>

          <template v-if="isTimeBased">
            <div class="form-field">
              <label for="timeUnit">{{ t('inventory.item.timeUnit') }} *</label>
              <Select
                id="timeUnit"
                v-model="timeUnit"
                :options="timeUnitOptions"
                optionLabel="label"
                optionValue="value"
                :class="{ 'p-invalid': errors.timeUnit }"
              />
              <Message v-if="errors.timeUnit" severity="error" :closable="false">
                {{ errors.timeUnit }}
              </Message>
            </div>

            <div class="form-field">
              <label for="timeInterval">{{ t('inventory.item.timeInterval') }} *</label>
              <InputNumber
                id="timeInterval"
                v-model="timeInterval"
                :min="1"
                :class="{ 'p-invalid': errors.timeInterval }"
              />
              <Message v-if="errors.timeInterval" severity="error" :closable="false">
                {{ errors.timeInterval }}
              </Message>
            </div>
          </template>

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
.item-form-page {
  max-width: 700px;
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
