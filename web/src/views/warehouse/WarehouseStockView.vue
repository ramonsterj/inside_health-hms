<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import InputText from 'primevue/inputtext'
import ToggleSwitch from 'primevue/toggleswitch'
import { useWarehouseStore } from '@/stores/warehouse'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useCodeLabels } from '@/composables/useCodeLabels'
import { formatPrice } from '@/utils/format'
import type { Warehouse } from '@/types/warehouse'

const { t } = useI18n()
const { warehouseName } = useCodeLabels()
const route = useRoute()
const { showError } = useErrorHandler()
const warehouseStore = useWarehouseStore()

const first = ref(0)
const rows = ref(20)
const searchTerm = ref('')
const lowStockOnly = ref(false)

// The :code param is optional. The /warehouses/stock entry (used by the
// "My warehouse stock" nav item) arrives without one, so we fall back to the
// first warehouse the caller can see and let them switch via the picker.
const code = computed(() => (route.params.code ? String(route.params.code) : null))
const warehouse = ref<Warehouse | null>(null)

onMounted(async () => {
  try {
    if (warehouseStore.warehouses.length === 0) {
      await warehouseStore.fetchWarehouses()
    }
    warehouse.value =
      (code.value ? warehouseStore.warehouses.find(w => w.code === code.value) : null) ??
      warehouseStore.warehouses[0] ??
      null
    if (warehouse.value) {
      await loadStock()
    }
  } catch (error) {
    showError(error)
  }
})

function onWarehouseChange() {
  first.value = 0
  loadStock()
}

async function loadStock() {
  if (!warehouse.value) return
  try {
    const page = Math.floor(first.value / rows.value)
    await warehouseStore.fetchStock(warehouse.value.id, {
      search: searchTerm.value || undefined,
      lowStockOnly: lowStockOnly.value || undefined,
      page,
      size: rows.value
    })
  } catch (error) {
    showError(error)
  }
}

function onFilterChange() {
  first.value = 0
  loadStock()
}

function onPageChange() {
  loadStock()
}
</script>

<template>
  <div class="warehouse-stock-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ t('warehouse.stock.title') }}
        <span v-if="warehouse" class="warehouse-name"
          >— {{ warehouseName(warehouse.code, warehouse.name) }}</span
        >
      </h1>
      <div class="header-actions">
        <Select
          v-if="warehouseStore.warehouses.length > 1"
          v-model="warehouse"
          :options="warehouseStore.warehouses"
          optionLabel="name"
          dataKey="id"
          :placeholder="t('warehouse.stock.title')"
          @change="onWarehouseChange"
          style="min-width: 220px"
        >
          <template #value="{ value, placeholder }">
            <span v-if="value">{{ warehouseName(value.code, value.name) }}</span>
            <span v-else>{{ placeholder }}</span>
          </template>
          <template #option="{ option }">
            {{ warehouseName(option.code, option.name) }}
          </template>
        </Select>
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadStock"
          :loading="warehouseStore.loading"
        />
      </div>
    </div>

    <Card v-if="!warehouse && !warehouseStore.loading">
      <template #content>
        <div class="text-center p-4">{{ t('warehouse.stock.notFound') }}</div>
      </template>
    </Card>

    <Card v-else>
      <template #content>
        <div class="filters">
          <InputText
            v-model="searchTerm"
            :placeholder="t('warehouse.stock.searchPlaceholder')"
            @keyup.enter="onFilterChange"
            style="width: 250px"
          />
          <div class="toggle-item">
            <label>{{ t('warehouse.stock.lowStockOnly') }}</label>
            <ToggleSwitch v-model="lowStockOnly" @change="onFilterChange" />
          </div>
          <Button icon="pi pi-search" severity="secondary" outlined @click="onFilterChange" />
        </div>

        <DataTable
          :value="warehouseStore.stock"
          :loading="warehouseStore.loading"
          :paginator="true"
          v-model:rows="rows"
          v-model:first="first"
          :totalRecords="warehouseStore.totalStock"
          :lazy="true"
          @page="onPageChange"
          :rowsPerPageOptions="[10, 20, 50]"
          dataKey="itemId"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('warehouse.stock.empty') }}</div>
          </template>

          <Column field="name" :header="t('warehouse.stock.item')" />
          <Column field="sku" :header="t('warehouse.stock.sku')" style="width: 120px">
            <template #body="{ data }">{{ data.sku || '-' }}</template>
          </Column>
          <Column :header="t('warehouse.stock.kind')" style="width: 120px">
            <template #body="{ data }">
              <Tag
                :value="t(`inventory.item.kinds.${data.kind}`)"
                :severity="data.kind === 'DRUG' ? 'warn' : 'secondary'"
              />
            </template>
          </Column>
          <Column :header="t('warehouse.stock.price')" style="width: 110px">
            <template #body="{ data }">{{ formatPrice(data.price) }}</template>
          </Column>
          <Column field="quantity" :header="t('warehouse.stock.quantity')" style="width: 100px" />
          <Column
            field="restockLevel"
            :header="t('warehouse.stock.restockLevel')"
            style="width: 110px"
          />
          <Column :header="t('warehouse.stock.lowStock')" style="width: 110px">
            <template #body="{ data }">
              <Tag v-if="data.lowStock" :value="t('warehouse.stock.low')" severity="danger" />
              <span v-else>-</span>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.warehouse-stock-page {
  max-width: 1100px;
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
  gap: 0.75rem;
  align-items: center;
}
.warehouse-name {
  color: var(--text-color-secondary);
  font-weight: 400;
}
.filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  align-items: center;
}
.toggle-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
</style>
