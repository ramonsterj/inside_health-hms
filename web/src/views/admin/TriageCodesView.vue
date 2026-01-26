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
import { useTriageCodeStore } from '@/stores/triageCode'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const triageCodeStore = useTriageCodeStore()
const authStore = useAuthStore()

const canCreate = computed(() => authStore.hasPermission('triage-code:create'))
const canUpdate = computed(() => authStore.hasPermission('triage-code:update'))
const canDelete = computed(() => authStore.hasPermission('triage-code:delete'))

onMounted(() => {
  loadTriageCodes()
})

async function loadTriageCodes() {
  try {
    await triageCodeStore.fetchTriageCodes()
  } catch (error) {
    showError(error)
  }
}

function createNewTriageCode() {
  router.push({ name: 'triage-code-create' })
}

function editTriageCode(id: number) {
  router.push({ name: 'triage-code-edit', params: { id } })
}

function confirmDelete(id: number) {
  confirm.require({
    message: t('triageCode.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteTriageCode(id)
  })
}

async function deleteTriageCode(id: number) {
  try {
    await triageCodeStore.deleteTriageCode(id)
    showSuccess('triageCode.deleted')
    loadTriageCodes()
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="triage-codes-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('triageCode.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('triageCode.new')"
          @click="createNewTriageCode"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadTriageCodes"
          :loading="triageCodeStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="triageCodeStore.triageCodes"
          :loading="triageCodeStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('triageCode.empty') }}
            </div>
          </template>

          <Column :header="t('triageCode.code')" style="width: 100px">
            <template #body="{ data }">
              <span
                class="triage-badge"
                :style="{ backgroundColor: data.color, color: getContrastColor(data.color) }"
              >
                {{ data.code }}
              </span>
            </template>
          </Column>

          <Column field="description" :header="t('triageCode.description')" />

          <Column
            field="displayOrder"
            :header="t('triageCode.displayOrder')"
            style="width: 120px"
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
                  @click="editTriageCode(data.id)"
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

<script lang="ts">
function getContrastColor(hexColor: string): string {
  const r = parseInt(hexColor.slice(1, 3), 16)
  const g = parseInt(hexColor.slice(3, 5), 16)
  const b = parseInt(hexColor.slice(5, 7), 16)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return luminance > 0.5 ? '#000000' : '#FFFFFF'
}
</script>

<style scoped>
.triage-codes-page {
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

.triage-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
