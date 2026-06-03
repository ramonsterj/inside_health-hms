<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useLabCatalogStore } from '@/stores/labCatalog'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { LabPanel } from '@/types/lab'
import LabPanelFormDialog from './LabPanelFormDialog.vue'

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const store = useLabCatalogStore()
const authStore = useAuthStore()

const canManage = computed(() => authStore.hasPermission('lab-catalog:manage'))

const dialogVisible = ref(false)
const editingPanel = ref<LabPanel | null>(null)

onMounted(load)

async function load(): Promise<void> {
  try {
    await store.fetchPanels()
  } catch (error) {
    showError(error)
  }
}

function openCreate(): void {
  editingPanel.value = null
  dialogVisible.value = true
}

function openEdit(panel: LabPanel): void {
  editingPanel.value = panel
  dialogVisible.value = true
}

function confirmDelete(panel: LabPanel): void {
  confirm.require({
    message: t('lab.panel.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => remove(panel.id)
  })
}

async function remove(id: number): Promise<void> {
  try {
    await store.deletePanel(id)
    showSuccess('lab.panel.deleted')
    await load()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <Card>
    <template #content>
      <div class="list-header">
        <Button
          v-if="canManage"
          icon="pi pi-plus"
          :label="t('lab.panel.new')"
          @click="openCreate"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          :loading="store.loading"
          @click="load"
        />
      </div>

      <DataTable :value="store.panels" :loading="store.loading" dataKey="id" stripedRows>
        <template #empty>
          <div class="text-center p-4">{{ t('lab.panel.empty') }}</div>
        </template>
        <Column field="name" :header="t('lab.panel.name')" />
        <Column :header="t('lab.panel.testCount')" style="width: 120px">
          <template #body="{ data }">{{ data.items.length }}</template>
        </Column>
        <Column :header="t('lab.panel.active')" style="width: 100px">
          <template #body="{ data }">
            <Tag
              :value="data.active ? t('common.yes') : t('common.no')"
              :severity="data.active ? 'success' : 'secondary'"
            />
          </template>
        </Column>
        <Column v-if="canManage" :header="t('common.actions')" style="width: 120px">
          <template #body="{ data }">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              :aria-label="t('common.edit')"
              v-tooltip.top="t('common.edit')"
              @click="openEdit(data)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              :aria-label="t('common.delete')"
              v-tooltip.top="t('common.delete')"
              @click="confirmDelete(data)"
            />
          </template>
        </Column>
      </DataTable>
    </template>
  </Card>

  <LabPanelFormDialog v-model:visible="dialogVisible" :panel="editingPanel" @saved="load" />
</template>

<style scoped>
.list-header {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
</style>
