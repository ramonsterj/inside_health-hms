<script setup lang="ts">
import AdmissionCard from './AdmissionCard.vue'
import type { AdmissionListItem } from '@/types/admission'

/**
 * Responsive grid of admission cards — the single leaf renderer shared by the
 * flat (ungrouped) view and both grouping levels, so the grid breakpoints live
 * in one place.
 */
defineProps<{
  items: AdmissionListItem[]
  /** Pass through to each card; controls whether the Status row is shown. */
  showStatus?: boolean
}>()

const emit = defineEmits<{
  view: [id: number]
}>()
</script>

<template>
  <div class="cards-grid">
    <AdmissionCard
      v-for="item in items"
      :key="item.id"
      :admission="item"
      :show-status="showStatus"
      @view="emit('view', $event)"
    />
  </div>
</template>

<style scoped>
.cards-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

@media (min-width: 640px) {
  .cards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1024px) {
  .cards-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
