<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useWarehouseStore } from '@/stores/warehouse'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import WarehouseFormDialog from '@/components/warehouse/WarehouseFormDialog.vue'
import type { Warehouse } from '@/types/warehouse'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const warehouseStore = useWarehouseStore()
const authStore = useAuthStore()

const canCreate = computed(() => authStore.hasPermission('warehouse:create'))
const canUpdate = computed(() => authStore.hasPermission('warehouse:update'))
const canDelete = computed(() => authStore.hasPermission('warehouse:delete'))

const dialogVisible = ref(false)
const editingWarehouse = ref<Warehouse | null>(null)

onMounted(loadWarehouses)

async function loadWarehouses() {
  try {
    await warehouseStore.fetchWarehouses()
  } catch (error) {
    showError(error)
  }
}

function openCreate() {
  editingWarehouse.value = null
  dialogVisible.value = true
}

function openEdit(warehouse: Warehouse) {
  editingWarehouse.value = warehouse
  dialogVisible.value = true
}

function viewStock(warehouse: Warehouse) {
  router.push({ name: 'warehouse-stock', params: { code: warehouse.code } })
}

function confirmDelete(warehouse: Warehouse) {
  confirm.require({
    message: t('warehouse.confirmDelete', { name: warehouse.name }),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteWarehouse(warehouse.id)
  })
}

async function deleteWarehouse(id: number) {
  try {
    await warehouseStore.deleteWarehouse(id)
    showSuccess('warehouse.deleted')
    loadWarehouses()
  } catch (error) {
    showError(error)
  }
}

function onSaved() {
  loadWarehouses()
}
</script>

<template>
  <div class="warehouse-list-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('warehouse.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('warehouse.new')"
          @click="openCreate"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadWarehouses"
          :loading="warehouseStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="warehouseStore.warehouses"
          :loading="warehouseStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('warehouse.empty') }}</div>
          </template>

          <Column field="code" :header="t('warehouse.code')" style="width: 140px" />
          <Column field="name" :header="t('warehouse.name')" />
          <Column :header="t('warehouse.description')">
            <template #body="{ data }">{{ data.description || '-' }}</template>
          </Column>
          <Column :header="t('warehouse.active')" style="width: 100px">
            <template #body="{ data }">
              <Tag
                :value="data.active ? t('common.yes') : t('common.no')"
                :severity="data.active ? 'success' : 'secondary'"
              />
            </template>
          </Column>
          <Column :header="t('common.actions')" style="width: 160px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  icon="pi pi-box"
                  severity="info"
                  text
                  rounded
                  @click="viewStock(data)"
                  v-tooltip.top="t('warehouse.viewStock')"
                  :aria-label="t('warehouse.viewStock')"
                />
                <Button
                  v-if="canUpdate"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  @click="openEdit(data)"
                  v-tooltip.top="t('common.edit')"
                  :aria-label="t('common.edit')"
                />
                <Button
                  v-if="canDelete"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  @click="confirmDelete(data)"
                  v-tooltip.top="t('common.delete')"
                  :aria-label="t('common.delete')"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <WarehouseFormDialog
      v-model:visible="dialogVisible"
      :warehouse="editingWarehouse"
      @saved="onSaved"
    />
  </div>
</template>

<style scoped>
.warehouse-list-page {
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
.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
