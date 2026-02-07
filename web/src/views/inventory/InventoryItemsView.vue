<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { useInventoryCategoryStore } from '@/stores/inventoryCategory'
import { useAuthStore } from '@/stores/auth'
import { formatPrice } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const itemStore = useInventoryItemStore()
const categoryStore = useInventoryCategoryStore()
const authStore = useAuthStore()

const canCreate = computed(() => authStore.hasPermission('inventory-item:create'))
const canUpdate = computed(() => authStore.hasPermission('inventory-item:update'))
const canDelete = computed(() => authStore.hasPermission('inventory-item:delete'))

const first = ref(0)
const rows = ref(20)
const selectedCategoryId = ref<number | null>(null)
const searchTerm = ref('')

const categoryOptions = computed(() => [
  { label: t('inventory.item.allCategories'), value: null },
  ...categoryStore.activeCategories.map((c) => ({ label: c.name, value: c.id }))
])

onMounted(async () => {
  await categoryStore.fetchActiveCategories()
  await loadItems()
})

async function loadItems() {
  try {
    const page = Math.floor(first.value / rows.value)
    await itemStore.fetchItems(
      page,
      rows.value,
      selectedCategoryId.value || undefined,
      searchTerm.value || undefined
    )
  } catch (error) {
    showError(error)
  }
}

function onFilterChange() {
  first.value = 0
  loadItems()
}

function onPageChange() {
  loadItems()
}

function createNewItem() {
  router.push({ name: 'inventory-item-create' })
}

function viewItem(id: number) {
  router.push({ name: 'inventory-item-detail', params: { id } })
}

function editItem(id: number) {
  router.push({ name: 'inventory-item-edit', params: { id } })
}

function confirmDelete(id: number) {
  confirm.require({
    message: t('inventory.item.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteItem(id)
  })
}

async function deleteItem(id: number) {
  try {
    await itemStore.deleteItem(id)
    showSuccess('inventory.item.deleted')
    loadItems()
  } catch (error) {
    showError(error)
  }
}

</script>

<template>
  <div class="inventory-items-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('inventory.item.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('inventory.item.new')"
          @click="createNewItem"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadItems"
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
          <InputText
            v-model="searchTerm"
            :placeholder="t('inventory.item.searchPlaceholder')"
            @keyup.enter="onFilterChange"
            style="width: 250px"
          />
          <Button
            icon="pi pi-search"
            severity="secondary"
            outlined
            @click="onFilterChange"
          />
        </div>

        <DataTable
          :value="itemStore.items"
          :loading="itemStore.loading"
          :paginator="true"
          v-model:rows="rows"
          v-model:first="first"
          :totalRecords="itemStore.totalItems"
          :lazy="true"
          @page="onPageChange"
          :rowsPerPageOptions="[10, 20, 50]"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('inventory.item.empty') }}
            </div>
          </template>

          <Column field="name" :header="t('inventory.item.name')" />

          <Column :header="t('inventory.item.category')" style="width: 160px">
            <template #body="{ data }">
              {{ data.category.name }}
            </template>
          </Column>

          <Column :header="t('inventory.item.price')" style="width: 100px">
            <template #body="{ data }">
              {{ formatPrice(data.price) }}
            </template>
          </Column>

          <Column :header="t('inventory.item.cost')" style="width: 100px">
            <template #body="{ data }">
              {{ formatPrice(data.cost) }}
            </template>
          </Column>

          <Column field="quantity" :header="t('inventory.item.quantity')" style="width: 90px" />

          <Column
            field="restockLevel"
            :header="t('inventory.item.restockLevel')"
            style="width: 90px"
          />

          <Column :header="t('inventory.item.pricingType')" style="width: 120px">
            <template #body="{ data }">
              <Tag
                :value="t(`inventory.item.pricingTypes.${data.pricingType}`)"
                :severity="data.pricingType === 'FLAT' ? 'info' : 'warn'"
              />
            </template>
          </Column>

          <Column :header="t('inventory.item.active')" style="width: 80px">
            <template #body="{ data }">
              <Tag
                :value="data.active ? t('common.yes') : t('common.no')"
                :severity="data.active ? 'success' : 'secondary'"
              />
            </template>
          </Column>

          <Column :header="t('common.actions')" style="width: 150px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  icon="pi pi-eye"
                  severity="info"
                  text
                  rounded
                  @click="viewItem(data.id)"
                  v-tooltip.top="t('common.view')"
                />
                <Button
                  v-if="canUpdate"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  @click="editItem(data.id)"
                  v-tooltip.top="t('common.edit')"
                />
                <Button
                  v-if="canDelete"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  @click="confirmDelete(data.id)"
                  v-tooltip.top="t('common.delete')"
                />
              </div>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.inventory-items-page {
  max-width: 1200px;
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

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
