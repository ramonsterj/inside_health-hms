<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useWarehouseStore } from '@/stores/warehouse'
import { useWarehouseTransferStore } from '@/stores/warehouseTransfer'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useRelativeTime } from '@/composables/useRelativeTime'
import { formatDateTime } from '@/utils/format'
import WarehouseChargeDialog from '@/components/warehouse/WarehouseChargeDialog.vue'
import type { Warehouse } from '@/types/warehouse'

const { t } = useI18n()
const router = useRouter()
const { showError } = useErrorHandler()
const { getRelativeTime } = useRelativeTime()
const warehouseStore = useWarehouseStore()
const transferStore = useWarehouseTransferStore()
const authStore = useAuthStore()

const canCharge = computed(() => authStore.hasPermission('warehouse-charge:create'))

const chargeDialogVisible = ref(false)
const chargeWarehouseId = ref<number | null>(null)

// The warehouses returned by the backend are already scoped to the caller's
// assignment, so the full list is what the maintenance user may operate on.
const assignedWarehouses = computed(() => warehouseStore.warehouses)

onMounted(async () => {
  try {
    await warehouseStore.fetchWarehouses()
    await transferStore.fetchTransfers(0, 10)
  } catch (error) {
    showError(error)
  }
})

function openCharge(warehouse?: Warehouse) {
  chargeWarehouseId.value = warehouse ? warehouse.id : null
  chargeDialogVisible.value = true
}

function viewStock(warehouse: Warehouse) {
  router.push({ name: 'warehouse-stock', params: { code: warehouse.code } })
}

function onChargeSaved() {
  transferStore.fetchTransfers(0, 10).catch(() => undefined)
}
</script>

<template>
  <div class="maintenance-dashboard-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('warehouse.maintenance.title') }}</h1>
      <Button
        v-if="canCharge"
        icon="pi pi-shopping-cart"
        :label="t('warehouse.maintenance.chargeConsumable')"
        @click="openCharge()"
      />
    </div>

    <Card class="section-card">
      <template #title>{{ t('warehouse.maintenance.assignedWarehouses') }}</template>
      <template #content>
        <div v-if="assignedWarehouses.length === 0" class="empty-state">
          {{ t('warehouse.maintenance.noWarehouses') }}
        </div>
        <div v-else class="warehouse-grid">
          <Card v-for="w in assignedWarehouses" :key="w.id" class="warehouse-card">
            <template #content>
              <div class="warehouse-card-head">
                <strong>{{ w.code }}</strong>
                <Tag
                  :value="w.active ? t('common.yes') : t('common.no')"
                  :severity="w.active ? 'success' : 'secondary'"
                />
              </div>
              <div class="warehouse-card-name">{{ w.name }}</div>
              <div class="warehouse-card-actions">
                <Button
                  icon="pi pi-box"
                  :label="t('warehouse.viewStock')"
                  size="small"
                  text
                  @click="viewStock(w)"
                />
                <Button
                  v-if="canCharge"
                  icon="pi pi-shopping-cart"
                  :label="t('warehouse.maintenance.charge')"
                  size="small"
                  text
                  @click="openCharge(w)"
                />
              </div>
            </template>
          </Card>
        </div>
      </template>
    </Card>

    <Card class="section-card">
      <template #title>{{ t('warehouse.maintenance.recentTransfers') }}</template>
      <template #content>
        <DataTable
          :value="transferStore.transfers"
          :loading="transferStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('warehouse.transfer.empty') }}</div>
          </template>
          <Column :header="t('warehouse.transfer.item')">
            <template #body="{ data }">{{ data.item.name }}</template>
          </Column>
          <Column :header="t('warehouse.transfer.source')" style="width: 120px">
            <template #body="{ data }">{{ data.sourceWarehouse.code }}</template>
          </Column>
          <Column :header="t('warehouse.transfer.destination')" style="width: 120px">
            <template #body="{ data }">{{ data.destinationWarehouse.code }}</template>
          </Column>
          <Column field="quantity" :header="t('warehouse.transfer.quantity')" style="width: 90px" />
          <Column :header="t('warehouse.transfer.issuedAt')" style="width: 160px">
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

    <WarehouseChargeDialog
      v-model:visible="chargeDialogVisible"
      :warehouseId="chargeWarehouseId"
      @saved="onChargeSaved"
    />
  </div>
</template>

<style scoped>
.maintenance-dashboard-page {
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
.section-card {
  margin-bottom: 1.5rem;
}
.warehouse-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
}
.warehouse-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.warehouse-card-name {
  margin: 0.5rem 0;
  color: var(--text-color-secondary);
}
.warehouse-card-actions {
  display: flex;
  gap: 0.5rem;
}
.empty-state {
  color: var(--text-color-secondary);
  font-style: italic;
}
</style>
