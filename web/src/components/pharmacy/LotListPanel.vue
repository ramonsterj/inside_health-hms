<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useInventoryLotStore } from '@/stores/inventoryLot'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useConfirm } from 'primevue/useconfirm'
import { formatDate } from '@/utils/format'
import LotFormDialog from './LotFormDialog.vue'
import ExpiryStatusChip from './ExpiryStatusChip.vue'
import { type InventoryLot } from '@/types/pharmacy'
import { lotExpiryStatusFromDate } from '@/utils/expiry'

const props = defineProps<{ itemId: number }>()
const { t } = useI18n()
const lotStore = useInventoryLotStore()
const authStore = useAuthStore()
const { showError, showSuccess } = useErrorHandler()
const confirm = useConfirm()

const dialogVisible = ref(false)
const editingLot = ref<InventoryLot | null>(null)

const canCreate = computed(() => authStore.hasPermission('inventory-lot:create'))
const canUpdate = computed(() => authStore.hasPermission('inventory-lot:update'))

watch(
  () => props.itemId,
  () => load(),
  { immediate: true }
)

async function load() {
  try {
    await lotStore.fetchByItem(props.itemId)
  } catch (e) {
    showError(e)
  }
}

function openNew() {
  editingLot.value = null
  dialogVisible.value = true
}

function openEdit(lot: InventoryLot) {
  editingLot.value = lot
  dialogVisible.value = true
}

function confirmDelete(lot: InventoryLot) {
  confirm.require({
    message: t('pharmacy.lot.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await lotStore.deleteLot(lot.id)
        showSuccess('pharmacy.lot.deleted')
        load()
      } catch (e) {
        showError(e)
      }
    }
  })
}

function statusFor(lot: InventoryLot) {
  return lotExpiryStatusFromDate(lot.expirationDate)
}
</script>

<template>
  <div class="lot-panel">
    <div class="panel-header">
      <h3>{{ t('pharmacy.lot.title') }}</h3>
      <Button
        v-if="canCreate"
        icon="pi pi-plus"
        :label="t('pharmacy.lot.new')"
        size="small"
        @click="openNew"
      />
    </div>

    <DataTable :value="lotStore.lots" :loading="lotStore.loading" dataKey="id" stripedRows>
      <template #empty>
        <div class="text-center p-4">{{ t('pharmacy.lot.empty') }}</div>
      </template>
      <Column field="lotNumber" :header="t('pharmacy.lot.lotNumber')" />
      <Column :header="t('pharmacy.lot.expirationDate')">
        <template #body="{ data }">{{ formatDate(data.expirationDate) }}</template>
      </Column>
      <Column field="quantityOnHand" :header="t('pharmacy.lot.quantityOnHand')" />
      <Column :header="t('pharmacy.lot.status')">
        <template #body="{ data }"><ExpiryStatusChip :status="statusFor(data)" /></template>
      </Column>
      <Column :header="t('pharmacy.lot.recalled')">
        <template #body="{ data }">
          <Tag v-if="data.recalled" severity="danger" :value="t('common.yes')" />
        </template>
      </Column>
      <Column field="supplier" :header="t('pharmacy.lot.supplier')" />
      <Column :header="t('common.actions')" style="width: 120px">
        <template #body="{ data }">
          <Button
            v-if="canUpdate"
            icon="pi pi-pencil"
            text
            rounded
            severity="secondary"
            @click="openEdit(data)"
          />
          <Button
            v-if="canUpdate"
            icon="pi pi-trash"
            text
            rounded
            severity="danger"
            @click="confirmDelete(data)"
          />
        </template>
      </Column>
    </DataTable>

    <LotFormDialog
      v-model:visible="dialogVisible"
      :item-id="itemId"
      :lot="editingLot"
      @saved="load"
    />
  </div>
</template>

<style scoped>
.lot-panel {
  margin-top: 1rem;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}
</style>
