<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import Paginator from 'primevue/paginator'
import SelectButton from 'primevue/selectbutton'
import ProgressSpinner from 'primevue/progressspinner'
import { useNursingNoteStore } from '@/stores/nursingNote'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useErrorHandler } from '@/composables/useErrorHandler'
import NursingNoteCard from './NursingNoteCard.vue'
import NursingNoteFormDialog from './NursingNoteFormDialog.vue'
import type { NursingNoteResponse } from '@/types/nursing'
import type { AdmissionStatus } from '@/types/admission'

const props = defineProps<{
  admissionId: number
  admissionStatus: AdmissionStatus
}>()

const { t } = useI18n()
const nursingNoteStore = useNursingNoteStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const { showError } = useErrorHandler()

// Pagination state
const first = ref(0)
const rows = ref(20)
const sortDirection = ref<'ASC' | 'DESC'>('DESC')

// Dialog state
const dialogVisible = ref(false)
const noteToEdit = ref<NursingNoteResponse | null>(null)

// Permission checks
const canCreate = computed(
  () => authStore.hasPermission('nursing-note:create') && props.admissionStatus === 'ACTIVE'
)
const canUpdate = computed(
  () => authStore.hasPermission('nursing-note:update') && props.admissionStatus === 'ACTIVE'
)

// Sort options
const sortOptions = computed(() => [
  { label: t('nursing.notes.newestFirst'), value: 'DESC' },
  { label: t('nursing.notes.oldestFirst'), value: 'ASC' }
])

// Computed data
const notes = computed(() => nursingNoteStore.getNursingNotes(props.admissionId))
const totalNotes = computed(() => nursingNoteStore.getTotalNotes(props.admissionId))
const loading = computed(() => nursingNoteStore.loading)

// Load notes
async function loadNotes() {
  try {
    await nursingNoteStore.fetchNursingNotes(
      props.admissionId,
      first.value / rows.value,
      rows.value,
      sortDirection.value
    )
  } catch (error) {
    showError(error)
  }
}

// Pagination handler
function onPageChange(event: { first: number; rows: number }) {
  first.value = event.first
  rows.value = event.rows
  loadNotes()
}

// Sort change handler
function onSortChange() {
  first.value = 0
  loadNotes()
}

// Dialog handlers
function openCreateDialog() {
  noteToEdit.value = null
  dialogVisible.value = true
}

function openEditDialog(note: NursingNoteResponse) {
  noteToEdit.value = note
  dialogVisible.value = true
}

function onNoteSaved() {
  notificationStore.success(
    noteToEdit.value ? t('nursing.notes.updated') : t('nursing.notes.created')
  )
}

// Watch for admission changes
watch(
  () => props.admissionId,
  () => {
    first.value = 0
    loadNotes()
  }
)

// Watch for sort direction changes
watch(sortDirection, onSortChange)

// Initial load
onMounted(loadNotes)
</script>

<template>
  <div class="nursing-note-list">
    <!-- Header -->
    <div class="list-header">
      <div class="header-left">
        <Badge v-if="totalNotes > 0" :value="totalNotes" severity="secondary" />
      </div>
      <div class="header-right">
        <SelectButton
          v-model="sortDirection"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
        />
        <Button
          v-if="canCreate"
          :label="t('nursing.notes.add')"
          icon="pi pi-plus"
          @click="openCreateDialog"
        />
      </div>
    </div>

    <!-- Content -->
    <div class="list-content">
      <!-- Loading state -->
      <div v-if="loading" class="loading-state">
        <ProgressSpinner strokeWidth="3" />
      </div>

      <!-- Empty state -->
      <div v-else-if="notes.length === 0" class="empty-state">
        <i class="pi pi-file-edit empty-icon"></i>
        <p>{{ t('nursing.notes.empty') }}</p>
        <Button
          v-if="canCreate"
          :label="t('nursing.notes.addFirst')"
          icon="pi pi-plus"
          @click="openCreateDialog"
        />
      </div>

      <!-- Notes list -->
      <template v-else>
        <NursingNoteCard
          v-for="note in notes"
          :key="note.id"
          :note="note"
          :canEdit="canUpdate"
          @edit="openEditDialog"
        />

        <Paginator
          v-if="totalNotes > rows"
          :first="first"
          :rows="rows"
          :totalRecords="totalNotes"
          :rowsPerPageOptions="[10, 20, 50]"
          @page="onPageChange"
        />
      </template>
    </div>

    <!-- Form dialog -->
    <NursingNoteFormDialog
      v-model:visible="dialogVisible"
      :admissionId="admissionId"
      :noteToEdit="noteToEdit"
      @saved="onNoteSaved"
    />
  </div>
</template>

<style scoped>
.nursing-note-list {
  padding: 1rem 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.list-content {
  min-height: 200px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state p {
  margin-bottom: 1rem;
}
</style>
