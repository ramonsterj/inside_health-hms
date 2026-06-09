<script setup lang="ts">
import { computed } from 'vue'
import Panel from 'primevue/panel'
import AdmissionCardList from './AdmissionCardList.vue'
import AdmissionGroupHeader from './AdmissionGroupHeader.vue'
import { bucketAdmissions, type BucketLabelers } from '@/composables/useAdmissionsGrouping'
import type { AdmissionListItem } from '@/types/admission'
import type { AdmissionsListGroupBy } from '@/stores/admissionsListPreferences'

/**
 * Renders one primary group's members bucketed by the secondary dimension into
 * inner collapsible panels. Fixed depth 2 — no recursion. The leaf grid renders
 * each inner bucket's (already triage-sorted) cards.
 */
const props = defineProps<{
  items: AdmissionListItem[]
  dimension: Exclude<AdmissionsListGroupBy, 'none'>
  labelers: BucketLabelers
  showStatus?: boolean
}>()

const emit = defineEmits<{
  view: [id: number]
}>()

const subGroups = computed(() => bucketAdmissions(props.items, props.dimension, props.labelers))

function onView(id: number) {
  emit('view', id)
}
</script>

<template>
  <div class="sub-groups">
    <Panel v-for="group in subGroups" :key="group.key" toggleable class="sub-group-panel">
      <template #header>
        <AdmissionGroupHeader :group="group" />
      </template>
      <AdmissionCardList :items="group.items" :show-status="showStatus" @view="onView" />
    </Panel>
  </div>
</template>

<style scoped>
.sub-groups {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  align-items: start;
}

@media (min-width: 1024px) {
  .sub-groups {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  /* Each subgroup is now half-width, so cap its leaf card grid at 2 per row
     (overrides the shared 3-column rule) to keep cards wide enough. */
  .sub-groups :deep(.cards-grid) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.sub-group-panel {
  background: transparent;
}
</style>
