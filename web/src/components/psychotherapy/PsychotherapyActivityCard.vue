<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { PsychotherapyActivity } from '@/types/psychotherapy'

const props = defineProps<{
  activity: PsychotherapyActivity
  canDelete: boolean
}>()

const emit = defineEmits<{
  delete: []
}>()

const { t } = useI18n()

const authorName = computed(() => {
  if (!props.activity.createdBy) return '-'
  const staff = props.activity.createdBy
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
})

const authorRoles = computed(() => {
  if (!props.activity.createdBy) return []
  return props.activity.createdBy.roles || []
})

const createdAtFormatted = computed(() => {
  if (!props.activity.createdAt) return '-'
  return new Date(props.activity.createdAt).toLocaleString()
})
</script>

<template>
  <Card class="activity-card">
    <template #header>
      <div class="activity-header">
        <div class="header-left">
          <Tag :value="activity.category.name" severity="info" class="category-tag" />
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
        </div>
        <div class="header-right">
          <span class="activity-date">{{ createdAtFormatted }}</span>
        </div>
      </div>
    </template>

    <template #content>
      <div class="activity-content">
        <p class="description">{{ activity.description }}</p>
      </div>
    </template>

    <template #footer v-if="canDelete">
      <div class="activity-footer">
        <Button
          icon="pi pi-trash"
          :label="t('common.delete')"
          severity="danger"
          text
          size="small"
          @click="emit('delete')"
        />
      </div>
    </template>
  </Card>
</template>

<style scoped>
.activity-card {
  background: var(--p-surface-card);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1rem 1.25rem 0;
  gap: 1rem;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.category-tag {
  align-self: flex-start;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.author-name {
  font-weight: 600;
  color: var(--p-text-color);
  font-size: 0.875rem;
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

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.activity-date {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.activity-content {
  padding: 0;
}

.description {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.5;
}

.activity-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
