<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { ProgressNoteResponse } from '@/types/medicalRecord'

const props = defineProps<{
  note: ProgressNoteResponse
  canEdit: boolean
}>()

const emit = defineEmits<{
  edit: []
}>()

const { t } = useI18n()

const expanded = ref(false)

const authorName = computed(() => {
  if (!props.note.createdBy) return '-'
  const staff = props.note.createdBy
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
})

const authorRoles = computed(() => {
  if (!props.note.createdBy) return []
  return props.note.createdBy.roles || []
})

const createdAtFormatted = computed(() => {
  if (!props.note.createdAt) return '-'
  return new Date(props.note.createdAt).toLocaleString()
})

const wasEdited = computed(() => {
  return props.note.updatedAt && props.note.updatedAt !== props.note.createdAt
})

function truncateText(text: string | null, maxLength = 150): string {
  if (!text) return '-'
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

function displayText(text: string | null): string {
  return text || '-'
}
</script>

<template>
  <Card class="progress-note-card">
    <template #header>
      <div class="note-header">
        <div class="author-info">
          <span class="author-name">{{ authorName }}</span>
          <div class="author-roles">
            <Tag
              v-for="role in authorRoles"
              :key="role"
              :value="role"
              severity="secondary"
              class="role-tag"
            />
          </div>
        </div>
        <div class="note-meta">
          <span class="note-date">{{ createdAtFormatted }}</span>
          <span v-if="wasEdited" class="edited-badge">
            ({{ t('medicalRecord.progressNote.edited') }})
          </span>
        </div>
      </div>
    </template>

    <template #content>
      <div class="note-content" :class="{ expanded }">
        <!-- Subjective -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">S</span>
            {{ t('medicalRecord.progressNote.fields.subjectiveData') }}
          </h5>
          <div class="soap-content">
            {{ expanded ? displayText(note.subjectiveData) : truncateText(note.subjectiveData) }}
          </div>
        </div>

        <!-- Objective -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">O</span>
            {{ t('medicalRecord.progressNote.fields.objectiveData') }}
          </h5>
          <div class="soap-content">
            {{ expanded ? displayText(note.objectiveData) : truncateText(note.objectiveData) }}
          </div>
        </div>

        <!-- Assessment/Analysis -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">A</span>
            {{ t('medicalRecord.progressNote.fields.analysis') }}
          </h5>
          <div class="soap-content">
            {{ expanded ? displayText(note.analysis) : truncateText(note.analysis) }}
          </div>
        </div>

        <!-- Plan -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">P</span>
            {{ t('medicalRecord.progressNote.fields.actionPlans') }}
          </h5>
          <div class="soap-content">
            {{ expanded ? displayText(note.actionPlans) : truncateText(note.actionPlans) }}
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="note-footer">
        <Button
          :icon="expanded ? 'pi pi-chevron-up' : 'pi pi-chevron-down'"
          :label="expanded ? t('common.collapse') : t('common.expand')"
          text
          size="small"
          @click="expanded = !expanded"
        />
        <Button
          v-if="canEdit"
          icon="pi pi-pencil"
          :label="t('common.edit')"
          text
          size="small"
          @click="emit('edit')"
        />
      </div>
    </template>
  </Card>
</template>

<style scoped>
.progress-note-card {
  background: var(--p-surface-card);
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1rem 1.25rem 0;
  gap: 1rem;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.author-name {
  font-weight: 600;
  color: var(--p-text-color);
}

.author-roles {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.role-tag {
  font-size: 0.7rem;
  padding: 0.1rem 0.4rem;
}

.note-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.note-date {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.edited-badge {
  font-size: 0.7rem;
  color: var(--p-text-muted-color);
  font-style: italic;
}

.note-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.soap-section {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.soap-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-text-muted-color);
}

.soap-letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  background: var(--p-primary-color);
  color: var(--p-primary-contrast-color);
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.7rem;
}

.soap-content {
  padding: 0.5rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 0.875rem;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}
</style>
