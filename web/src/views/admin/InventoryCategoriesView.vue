<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useInventoryCategoryStore } from '@/stores/inventoryCategory'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const categoryStore = useInventoryCategoryStore()
const authStore = useAuthStore()

const canCreate = computed(() => authStore.hasPermission('inventory-category:create'))
const canUpdate = computed(() => authStore.hasPermission('inventory-category:update'))
const canDelete = computed(() => authStore.hasPermission('inventory-category:delete'))

onMounted(() => {
  loadCategories()
})

async function loadCategories() {
  try {
    await categoryStore.fetchCategories()
  } catch (error) {
    showError(error)
  }
}

function createNewCategory() {
  router.push({ name: 'inventory-category-create' })
}

function editCategory(id: number) {
  router.push({ name: 'inventory-category-edit', params: { id } })
}

function confirmDelete(id: number) {
  confirm.require({
    message: t('inventory.category.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteCategory(id)
  })
}

async function deleteCategory(id: number) {
  try {
    await categoryStore.deleteCategory(id)
    showSuccess('inventory.category.deleted')
    loadCategories()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="inventory-categories-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('inventory.category.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('inventory.category.new')"
          @click="createNewCategory"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadCategories"
          :loading="categoryStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="categoryStore.categories"
          :loading="categoryStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('inventory.category.empty') }}
            </div>
          </template>

          <Column field="name" :header="t('inventory.category.name')" />

          <Column field="description" :header="t('inventory.category.description')">
            <template #body="{ data }">
              {{ data.description || '-' }}
            </template>
          </Column>

          <Column
            field="displayOrder"
            :header="t('inventory.category.displayOrder')"
            style="width: 120px"
          />

          <Column :header="t('inventory.category.active')" style="width: 100px">
            <template #body="{ data }">
              <Tag
                :value="data.active ? t('common.yes') : t('common.no')"
                :severity="data.active ? 'success' : 'secondary'"
              />
            </template>
          </Column>

          <Column :header="t('common.actions')" style="width: 120px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  v-if="canUpdate"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  @click="editCategory(data.id)"
                  v-tooltip.top="t('common.edit')"
                  :aria-label="t('common.edit')"
                />
                <Button
                  v-if="canDelete"
                  icon="pi pi-trash"
                  severity="danger"
                  text
                  rounded
                  @click="confirmDelete(data.id)"
                  v-tooltip.top="t('common.delete')"
                  :aria-label="t('common.delete')"
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
.inventory-categories-page {
  max-width: 900px;
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
