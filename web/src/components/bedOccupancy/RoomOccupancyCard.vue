<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import BedSlot from './BedSlot.vue'
import { RoomGender, type RoomOccupancyItem } from '@/types/room'

const props = defineProps<{
  room: RoomOccupancyItem
  /**
   * Lowercased patient-name search query. Each occupant whose name contains this
   * substring receives a highlight ring on its slot. Empty string disables highlighting.
   */
  searchHighlight?: string
}>()

const { t } = useI18n()

const totalSlots = computed(() => props.room.capacity)

/**
 * One slot per unit of capacity. The first `occupiedBeds` slots map to occupants
 * in their server-provided order; the remainder are free.
 */
const slots = computed(() => {
  const result: { key: string; occupant: ReturnType<typeof getOccupant> }[] = []
  for (let i = 0; i < totalSlots.value; i++) {
    const occupant = getOccupant(i)
    result.push({
      key: occupant ? `occ-${occupant.admissionId}` : `free-${props.room.id}-${i}`,
      occupant
    })
  }
  return result
})

function getOccupant(index: number) {
  // eslint-disable-next-line security/detect-object-injection -- index is bounded by capacity loop above
  return props.room.occupants[index] ?? null
}

function isHighlighted(occupant: ReturnType<typeof getOccupant>): boolean {
  if (!occupant || !props.searchHighlight) return false
  return occupant.patientName.toLowerCase().includes(props.searchHighlight)
}

const genderLabel = computed(() => t(`room.genders.${props.room.gender}`))
const typeLabel = computed(() => t(`room.types.${props.room.type}`))
const genderSeverity = computed(() =>
  props.room.gender === RoomGender.FEMALE ? 'warn' : 'info'
)
</script>

<template>
  <Card class="room-card">
    <template #header>
      <div class="room-card-header">
        <div class="room-id">
          <h3 class="room-number">{{ t('room.numberPrefix') }}{{ room.number }}</h3>
          <div class="room-tags">
            <Tag :value="genderLabel" :severity="genderSeverity" />
            <Tag :value="typeLabel" severity="secondary" />
          </div>
        </div>
        <div class="capacity-summary">
          <span class="capacity-text">
            {{ room.availableBeds }} / {{ room.capacity }}
          </span>
          <span class="capacity-label">{{ t('bedOccupancy.bed.free') }}</span>
        </div>
      </div>
    </template>
    <template #content>
      <div class="bed-grid">
        <BedSlot
          v-for="slot in slots"
          :key="slot.key"
          :occupant="slot.occupant"
          :room-id="room.id"
          :highlighted="isHighlighted(slot.occupant)"
        />
      </div>
    </template>
  </Card>
</template>

<style scoped>
.room-card {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.room-card :deep(.p-card-body) {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.room-card :deep(.p-card-content) {
  flex: 1;
}

.room-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1rem 1rem 0.5rem 1rem;
  gap: 1rem;
}

.room-id {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.room-number {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
}

.room-tags {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.capacity-summary {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.capacity-text {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--p-text-color);
}

.capacity-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-text-muted-color);
}

.bed-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.5rem;
}
</style>
