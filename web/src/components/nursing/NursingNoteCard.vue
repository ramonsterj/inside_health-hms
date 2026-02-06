<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import { sanitizeHtml } from '@/utils/sanitize'
import type { NursingNoteResponse } from '@/types/nursing'

const props = defineProps<{
  note: NursingNoteResponse
  canEdit: boolean
}>()

const emit = defineEmits<{
  edit: [note: NursingNoteResponse]
}>()

const { t, d } = useI18n()

const expanded = ref(false)

const sanitizedDescription = computed(() => sanitizeHtml(props.note.description))

const authorName = computed(() => {
  const author = props.note.createdBy
  if (!author) return t('common.unknown')
  const salutation = author.salutation || ''
  const firstName = author.firstName || ''
  const lastName = author.lastName || ''
  return `${salutation} ${firstName} ${lastName}`.trim() || t('common.unknown')
})

const authorRoles = computed(() => {
  return props.note.createdBy?.roles || []
})

const wasEdited = computed(() => {
  return props.note.createdAt !== props.note.updatedAt
})

const showEditButton = computed(() => {
  return props.canEdit && props.note.canEdit
})

function toggleExpand() {
  expanded.value = !expanded.value
}

function handleEdit() {
  emit('edit', props.note)
}
</script>

<template>
  <Card class="nursing-note-card">
    <template #header>
      <div class="card-header">
        <div class="author-info">
          <span class="author-name">{{ authorName }}</span>
          <div class="author-roles">
            <Badge
              v-for="role in authorRoles"
              :key="role"
              :value="role"
              severity="secondary"
              class="role-badge"
            />
          </div>
        </div>
        <div class="header-right">
          <span class="timestamp">
            {{ d(new Date(note.createdAt), 'long') }}
          </span>
          <Badge
            v-if="wasEdited"
            :value="t('nursing.notes.edited')"
            severity="info"
            class="edited-badge"
          />
        </div>
      </div>
    </template>
    <template #content>
      <div class="note-content">
        <div class="description" :class="{ expanded }" v-html="sanitizedDescription"></div>
        <Button
          :label="expanded ? t('common.collapse') : t('common.expand')"
          text
          size="small"
          @click="toggleExpand"
        />
      </div>
    </template>
    <template #footer>
      <div class="card-footer">
        <div class="footer-info">
          <span v-if="wasEdited" class="updated-info">
            {{ t('common.updatedAt') }}: {{ d(new Date(note.updatedAt), 'long') }}
          </span>
        </div>
        <Button
          v-if="showEditButton"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          text
          size="small"
          @click="handleEdit"
        />
      </div>
    </template>
  </Card>
</template>

<style scoped>
.nursing-note-card {
  margin-bottom: 1rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1rem;
  border-bottom: 1px solid var(--p-surface-200);
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

.role-badge {
  font-size: 0.7rem;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.timestamp {
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
}

.edited-badge {
  font-size: 0.7rem;
}

.note-content {
  padding: 0.5rem 0;
}

.description {
  word-break: break-word;
  line-height: 1.6;
  max-height: 150px;
  overflow: hidden;
  position: relative;
}

.description.expanded {
  max-height: none;
  overflow: visible;
}

.description:not(.expanded)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(transparent, var(--p-surface-ground));
}

.description:deep(p) {
  margin: 0 0 0.5rem 0;
}

.description:deep(ul),
.description:deep(ol) {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.5rem;
  border-top: 1px solid var(--p-surface-200);
}

.footer-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.updated-info {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
}
</style>
