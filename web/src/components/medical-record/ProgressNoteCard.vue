<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { ProgressNoteResponse } from '@/types/medicalRecord'
import { formatDateTime } from '@/utils/format'
import { sanitizeHtml } from '@/utils/sanitize'

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

const createdAtFormatted = computed(() => formatDateTime(props.note.createdAt))

const wasEdited = computed(() => {
  return props.note.updatedAt && props.note.updatedAt !== props.note.createdAt
})

function renderedHtml(html: string | null): string {
  if (!html) return '-'
  return sanitizeHtml(html)
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
      <!-- eslint-disable vue/no-v-html -- SOAP content is sanitized via DOMPurify before binding -->
      <div class="note-content" :class="{ expanded }">
        <!-- Subjective -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">S</span>
            {{ t('medicalRecord.progressNote.fields.subjectiveData') }}
          </h5>
          <div
            class="soap-content"
            :class="{ truncated: !expanded }"
            v-html="renderedHtml(note.subjectiveData)"
          ></div>
        </div>

        <!-- Objective -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">O</span>
            {{ t('medicalRecord.progressNote.fields.objectiveData') }}
          </h5>
          <div
            class="soap-content"
            :class="{ truncated: !expanded }"
            v-html="renderedHtml(note.objectiveData)"
          ></div>
        </div>

        <!-- Assessment/Analysis -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">A</span>
            {{ t('medicalRecord.progressNote.fields.analysis') }}
          </h5>
          <div
            class="soap-content"
            :class="{ truncated: !expanded }"
            v-html="renderedHtml(note.analysis)"
          ></div>
        </div>

        <!-- Plan -->
        <div class="soap-section">
          <h5 class="soap-label">
            <span class="soap-letter">P</span>
            {{ t('medicalRecord.progressNote.fields.actionPlans') }}
          </h5>
          <div
            class="soap-content"
            :class="{ truncated: !expanded }"
            v-html="renderedHtml(note.actionPlans)"
          ></div>
        </div>
      </div>
      <!-- eslint-enable vue/no-v-html -->
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
  word-wrap: break-word;
  font-size: 0.875rem;
}

.soap-content.truncated {
  max-height: 3.5em;
  overflow: hidden;
  position: relative;
}

.soap-content.truncated::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1.5em;
  background: linear-gradient(transparent, var(--p-surface-ground));
}

.soap-content:deep(p) {
  margin: 0 0 0.5rem 0;
}

.soap-content:deep(p:last-child) {
  margin-bottom: 0;
}

.soap-content:deep(ul),
.soap-content:deep(ol) {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}
</style>
