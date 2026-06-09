<script setup lang="ts">
import Badge from 'primevue/badge'
import GenderIcon from '@/components/icons/GenderIcon.vue'
import { ADMISSION_TYPE_META } from '@/constants/admissionType'
import { getContrastColor } from '@/utils/format'
import type { CardGroupDescriptor } from '@/composables/useAdmissionsGrouping'

/**
 * Shared header for an admissions card group, used at both grouping levels
 * (primary outer panel and secondary inner panel). Renders the dimension's
 * icon/swatch/badge, the localized label, and a member-count badge.
 */
defineProps<{
  group: CardGroupDescriptor
}>()
</script>

<template>
  <div class="group-header">
    <span v-if="group.kind === 'gender'" class="group-icon">
      <GenderIcon :sex="group.sex" :size="18" />
    </span>
    <span
      v-else-if="group.kind === 'type' && group.type"
      class="group-swatch"
      :class="ADMISSION_TYPE_META[group.type].dotClass"
      aria-hidden="true"
    ></span>
    <span
      v-else-if="group.kind === 'triage' && group.triageColor && group.triageCode"
      class="group-triage-badge"
      :style="{
        backgroundColor: group.triageColor,
        color: getContrastColor(group.triageColor)
      }"
    >
      {{ group.triageCode }}
    </span>
    <span class="group-label">{{ group.label }}</span>
    <Badge :value="group.items.length" severity="secondary" />
  </div>
</template>

<style scoped>
.group-header {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.group-icon {
  display: inline-flex;
  color: var(--p-text-muted-color);
}

.group-swatch {
  display: inline-block;
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 999px;
  flex-shrink: 0;
}

.group-triage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  padding: 0.125rem 0.5rem;
  border-radius: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
}

.group-label {
  font-weight: 600;
  color: var(--p-text-color);
}
</style>
