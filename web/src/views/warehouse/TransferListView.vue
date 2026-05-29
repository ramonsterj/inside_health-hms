<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import { useWarehouseTransferStore } from '@/stores/warehouseTransfer'
import { useWarehouseStore } from '@/stores/warehouse'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useRelativeTime } from '@/composables/useRelativeTime'
import { formatDateTime } from '@/utils/format'
import TransferFormDialog from '@/components/warehouse/TransferFormDialog.vue'

const { t } = useI18n()
const { showError } = useErrorHandler()
const { getRelativeTime } = useRelativeTime()
const transferStore = useWarehouseTransferStore()
const warehouseStore = useWarehouseStore()
const authStore = useAuthStore()

const canCreate = computed(() => authStore.hasPermission('warehouse-transfer:create'))

const first = ref(0)
const rows = ref(20)
const warehouseFilter = ref<number | null>(null)
const dialogVisible = ref(false)

const warehouseOptions = computed(() => [
  { label: t('warehouse.transfer.allWarehouses'), value: null },
  ...warehouseStore.warehouses.map(w => ({ label: `${w.code} - ${w.name}`, value: w.id }))
])

function statusSeverity(status: string): 'success' | 'warn' | 'danger' | 'info' {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'PENDING':
      return 'warn'
    case 'CANCELLED':
      return 'danger'
    default:
      return 'info'
  }
}

onMounted(async () => {
  if (warehouseStore.warehouses.length === 0) {
    await warehouseStore.fetchWarehouses().catch(() => undefined)
  }
  await loadTransfers()
})

async function loadTransfers() {
  try {
    const page = Math.floor(first.value / rows.value)
    await transferStore.fetchTransfers(page, rows.value, warehouseFilter.value || undefined)
  } catch (error) {
    showError(error)
  }
}

function onFilterChange() {
  first.value = 0
  loadTransfers()
}

function onPageChange() {
  loadTransfers()
}

function onSaved() {
  first.value = 0
  loadTransfers()
}
</script>

<template>
  <div class="transfer-list-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('warehouse.transfer.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('warehouse.transfer.new')"
          @click="dialogVisible = true"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadTransfers"
          :loading="transferStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <div class="filters">
          <Select
            v-model="warehouseFilter"
            :options="warehouseOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('warehouse.transfer.allWarehouses')"
            @change="onFilterChange"
            style="width: 250px"
          />
        </div>

        <DataTable
          :value="transferStore.transfers"
          :loading="transferStore.loading"
          :paginator="true"
          v-model:rows="rows"
          v-model:first="first"
          :totalRecords="transferStore.totalTransfers"
          :lazy="true"
          @page="onPageChange"
          :rowsPerPageOptions="[10, 20, 50]"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('warehouse.transfer.empty') }}</div>
          </template>

          <Column :header="t('warehouse.transfer.item')">
            <template #body="{ data }">
              {{ data.item.name }}
              <span v-if="data.lot" class="muted">· {{ data.lot.lotNumber || '—' }}</span>
            </template>
          </Column>
          <Column :header="t('warehouse.transfer.source')" style="width: 140px">
            <template #body="{ data }">{{ data.sourceWarehouse.code }}</template>
          </Column>
          <Column :header="t('warehouse.transfer.destination')" style="width: 140px">
            <template #body="{ data }">{{ data.destinationWarehouse.code }}</template>
          </Column>
          <Column field="quantity" :header="t('warehouse.transfer.quantity')" style="width: 90px" />
          <Column :header="t('warehouse.transfer.status')" style="width: 120px">
            <template #body="{ data }">
              <Tag
                :value="t(`warehouse.transfer.statuses.${data.status}`)"
                :severity="statusSeverity(data.status)"
              />
            </template>
          </Column>
          <Column :header="t('warehouse.transfer.issuedAt')" style="width: 180px">
            <template #body="{ data }">
              <span v-if="data.issuedAt" v-tooltip.top="formatDateTime(data.issuedAt)">
                {{ getRelativeTime(data.issuedAt) }}
              </span>
              <span v-else>-</span>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <TransferFormDialog v-model:visible="dialogVisible" @saved="onSaved" />
  </div>
</template>

<style scoped>
.transfer-list-page {
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
  gap: 0.5rem;
}
.filters {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  align-items: center;
}
.muted {
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}
</style>
