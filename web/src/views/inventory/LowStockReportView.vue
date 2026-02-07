<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Select from 'primevue/select'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { useInventoryCategoryStore } from '@/stores/inventoryCategory'

const { t } = useI18n()
const { showError } = useErrorHandler()
const itemStore = useInventoryItemStore()
const categoryStore = useInventoryCategoryStore()

const selectedCategoryId = ref<number | null>(null)

const categoryOptions = computed(() => [
  { label: t('inventory.item.allCategories'), value: null },
  ...categoryStore.activeCategories.map((c) => ({ label: c.name, value: c.id }))
])

onMounted(async () => {
  await categoryStore.fetchActiveCategories()
  await loadReport()
})

async function loadReport() {
  try {
    await itemStore.fetchLowStock(selectedCategoryId.value || undefined)
  } catch (error) {
    showError(error)
  }
}

function onFilterChange() {
  loadReport()
}
</script>

<template>
  <div class="low-stock-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('inventory.lowStock.title') }}</h1>
      <div class="header-actions">
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadReport"
          :loading="itemStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <div class="filters">
          <Select
            v-model="selectedCategoryId"
            :options="categoryOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('inventory.item.allCategories')"
            @change="onFilterChange"
            style="width: 250px"
          />
        </div>

        <DataTable
          :value="itemStore.lowStockItems"
          :loading="itemStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('inventory.lowStock.empty') }}
            </div>
          </template>

          <Column field="name" :header="t('inventory.item.name')" />

          <Column :header="t('inventory.item.category')" style="width: 180px">
            <template #body="{ data }">
              {{ data.category.name }}
            </template>
          </Column>

          <Column field="quantity" :header="t('inventory.item.quantity')" style="width: 120px" />

          <Column
            field="restockLevel"
            :header="t('inventory.item.restockLevel')"
            style="width: 120px"
          />

          <Column :header="t('inventory.lowStock.belowThreshold')" style="width: 120px">
            <template #body="{ data }">
              <span class="deficit">-{{ data.restockLevel - data.quantity }}</span>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.low-stock-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.filters {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.deficit {
  color: var(--p-red-500);
  font-weight: 600;
}
</style>
