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
import { useRoomStore } from '@/stores/room'
import { useAuthStore } from '@/stores/auth'
import { RoomType } from '@/types/room'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const roomStore = useRoomStore()
const authStore = useAuthStore()

const canCreate = computed(() => authStore.hasPermission('room:create'))
const canUpdate = computed(() => authStore.hasPermission('room:update'))
const canDelete = computed(() => authStore.hasPermission('room:delete'))

onMounted(() => {
  loadRooms()
})

async function loadRooms() {
  try {
    await roomStore.fetchRooms()
  } catch (error) {
    showError(error)
  }
}

function createNewRoom() {
  router.push({ name: 'room-create' })
}

function editRoom(id: number) {
  router.push({ name: 'room-edit', params: { id } })
}

function confirmDelete(id: number) {
  confirm.require({
    message: t('room.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteRoom(id)
  })
}

async function deleteRoom(id: number) {
  try {
    await roomStore.deleteRoom(id)
    showSuccess('room.deleted')
    loadRooms()
  } catch (error) {
    showError(error)
  }
}

function getRoomTypeSeverity(type: RoomType): 'info' | 'success' {
  return type === RoomType.PRIVATE ? 'info' : 'success'
}
</script>

<template>
  <div class="rooms-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('room.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('room.new')"
          @click="createNewRoom"
        />
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadRooms"
          :loading="roomStore.loading"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="roomStore.rooms"
          :loading="roomStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('room.empty') }}
            </div>
          </template>

          <Column field="number" :header="t('room.number')" style="width: 120px" />

          <Column :header="t('room.type')" style="width: 120px">
            <template #body="{ data }">
              <Tag :value="t(`room.types.${data.type}`)" :severity="getRoomTypeSeverity(data.type)" />
            </template>
          </Column>

          <Column field="capacity" :header="t('room.capacity')" />

          <Column :header="t('common.actions')" style="width: 120px">
            <template #body="{ data }">
              <div class="action-buttons">
                <Button
                  v-if="canUpdate"
                  icon="pi pi-pencil"
                  severity="secondary"
                  text
                  rounded
                  @click="editRoom(data.id)"
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
.rooms-page {
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
