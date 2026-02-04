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
import { useDocumentTypeStore } from '@/stores/documentType'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const documentTypeStore = useDocumentTypeStore()
const authStore = useAuthStore()

const canCreate = computed(() => authStore.hasPermission('document-type:create'))
const canUpdate = computed(() => authStore.hasPermission('document-type:update'))
const canDelete = computed(() => authStore.hasPermission('document-type:delete'))

onMounted(() => {
  loadDocumentTypes()
})

async function loadDocumentTypes() {
  try {
    await documentTypeStore.fetchDocumentTypes()
  } catch (error) {
    showError(error)
  }
}

function createNewDocumentType() {
  router.push({ name: 'document-type-create' })
}

function editDocumentType(id: number) {
  router.push({ name: 'document-type-edit', params: { id } })
}

function confirmDelete(id: number) {
  confirm.require({
    message: t('documentType.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteDocumentType(id)
  })
}

async function deleteDocumentType(id: number) {
  try {
    await documentTypeStore.deleteDocumentType(id)
    showSuccess('documentType.deleted')
    loadDocumentTypes()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="document-types-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('documentType.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('documentType.new')"
          @click="createNewDocumentType"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadDocumentTypes"
          :loading="documentTypeStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="documentTypeStore.documentTypes"
          :loading="documentTypeStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('documentType.empty') }}
            </div>
          </template>

          <Column field="code" :header="t('documentType.code')" style="width: 180px" />

          <Column field="name" :header="t('documentType.name')" />

          <Column field="description" :header="t('documentType.description')" />

          <Column
            field="displayOrder"
            :header="t('documentType.displayOrder')"
            style="width: 100px"
          />

          <Column :header="t('common.actions')" style="width: 120px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  v-if="canUpdate"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  @click="editDocumentType(data.id)"
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
.document-types-page {
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
