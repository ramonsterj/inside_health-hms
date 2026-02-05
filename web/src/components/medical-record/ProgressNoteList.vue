<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import Paginator from 'primevue/paginator'
import SelectButton from 'primevue/selectbutton'
import { useProgressNoteStore } from '@/stores/progressNote'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { ProgressNoteResponse } from '@/types/medicalRecord'
import ProgressNoteCard from './ProgressNoteCard.vue'
import ProgressNoteFormDialog from './ProgressNoteFormDialog.vue'

const props = defineProps<{
  admissionId: number
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const progressNoteStore = useProgressNoteStore()
const authStore = useAuthStore()

const page = ref(0)
const pageSize = ref(10)
const sortDirection = ref<'DESC' | 'ASC'>('DESC')
const showFormDialog = ref(false)
const editingNote = ref<ProgressNoteResponse | null>(null)
const loaded = ref(false)

const notes = computed(() => progressNoteStore.getProgressNotes(props.admissionId))
const totalNotes = computed(() => progressNoteStore.getTotalNotes(props.admissionId))
const loading = computed(() => progressNoteStore.loading)

const canCreate = computed(() => authStore.hasPermission('progress-note:create'))
const canUpdate = computed(() => authStore.hasPermission('progress-note:update'))

const sortOptions = computed(() => [
  { label: t('medicalRecord.progressNote.newestFirst'), value: 'DESC' },
  { label: t('medicalRecord.progressNote.oldestFirst'), value: 'ASC' }
])

onMounted(async () => {
  await loadNotes()
})

watch([page, sortDirection], async () => {
  await loadNotes()
})

async function loadNotes() {
  try {
    await progressNoteStore.fetchProgressNotes(
      props.admissionId,
      page.value,
      pageSize.value,
      sortDirection.value
    )
  } catch (error) {
    showError(error)
  } finally {
    loaded.value = true
  }
}

function handlePageChange(event: { page: number; rows: number }) {
  page.value = event.page
  pageSize.value = event.rows
}

function openCreateDialog() {
  editingNote.value = null
  showFormDialog.value = true
}

function openEditDialog(note: ProgressNoteResponse) {
  editingNote.value = note
  showFormDialog.value = true
}

function handleNoteSaved() {
  const wasEditing = !!editingNote.value
  showFormDialog.value = false
  editingNote.value = null
  showSuccess(
    wasEditing ? 'medicalRecord.progressNote.updated' : 'medicalRecord.progressNote.created'
  )
}
</script>

<template>
  <div class="progress-note-list">
    <!-- Header -->
    <div class="list-header">
      <div class="header-left">
        <h3>{{ t('medicalRecord.progressNote.title') }}</h3>
        <span class="note-count" v-if="totalNotes > 0">
          ({{ totalNotes }} {{ t('medicalRecord.progressNote.notes') }})
        </span>
      </div>
      <div class="header-actions">
        <SelectButton
          v-model="sortDirection"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
        />
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('medicalRecord.progressNote.add')"
          @click="openCreateDialog"
        />
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !loaded" class="loading-container">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
    </div>

    <!-- Empty State -->
    <div v-else-if="loaded && notes.length === 0" class="empty-state">
      <i class="pi pi-file-edit empty-icon"></i>
      <p>{{ t('medicalRecord.progressNote.empty') }}</p>
      <Button
        v-if="canCreate"
        icon="pi pi-plus"
        :label="t('medicalRecord.progressNote.addFirst')"
        @click="openCreateDialog"
      />
    </div>

    <!-- Notes List -->
    <div v-else class="notes-list">
      <ProgressNoteCard
        v-for="note in notes"
        :key="note.id"
        :note="note"
        :canEdit="canUpdate"
        @edit="openEditDialog(note)"
      />

      <!-- Pagination -->
      <Paginator
        v-if="totalNotes > pageSize"
        :rows="pageSize"
        :totalRecords="totalNotes"
        :first="page * pageSize"
        :rowsPerPageOptions="[5, 10, 20]"
        @page="handlePageChange"
      />
    </div>

    <!-- Form Dialog -->
    <ProgressNoteFormDialog
      v-model:visible="showFormDialog"
      :admissionId="admissionId"
      :note="editingNote"
      @saved="handleNoteSaved"
    />
  </div>
</template>

<style scoped>
.progress-note-list {
  padding: 1rem;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.header-left h3 {
  margin: 0;
}

.note-count {
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 3rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state p {
  margin-bottom: 1.5rem;
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
</style>
