<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import MultiSelect from 'primevue/multiselect'
import { useWarehouseStore } from '@/stores/warehouse'

const props = defineProps<{
  modelValue: number[]
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: number[]): void
}>()

const { t } = useI18n()
const warehouseStore = useWarehouseStore()

const options = computed(() =>
  warehouseStore.warehouses.map(w => ({ label: `${w.code} - ${w.name}`, value: w.id }))
)

const selected = computed({
  get: () => props.modelValue,
  set: (value: number[]) => emit('update:modelValue', value)
})

onMounted(() => {
  if (warehouseStore.warehouses.length === 0) {
    warehouseStore.fetchWarehouses().catch(() => undefined)
  }
})
</script>

<template>
  <div class="form-field">
    <label for="assignedWarehouses">{{ t('warehouse.assignment.label') }}</label>
    <MultiSelect
      inputId="assignedWarehouses"
      v-model="selected"
      :options="options"
      optionLabel="label"
      optionValue="value"
      :placeholder="t('warehouse.assignment.placeholder')"
      filter
      class="w-full"
    />
  </div>
</template>

<style scoped>
.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-field label {
  font-weight: 500;
  font-size: 0.875rem;
}
.w-full {
  width: 100%;
}
</style>
