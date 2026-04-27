<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { BedOccupant } from '@/types/room'

const props = defineProps<{
  occupant?: BedOccupant | null
  roomId: number
  /** When true, this occupied slot matches the active patient-name search query. */
  highlighted?: boolean
}>()

const { t, locale } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const isOccupied = computed(() => !!props.occupant)
const canCreateAdmission = computed(() => authStore.hasPermission('admission:create'))

function viewAdmission() {
  if (!props.occupant) return
  router.push({ name: 'admission-detail', params: { id: props.occupant.admissionId } })
}

function admitHere() {
  router.push({ name: 'patients', query: { admitToRoom: String(props.roomId) } })
}

const formattedDate = computed(() => {
  if (!props.occupant) return ''
  const [year, month, day] = props.occupant.admissionDate.split('-').map(Number)
  const date =
    year && month && day ? new Date(year, month - 1, day) : new Date(props.occupant.admissionDate)
  return date.toLocaleDateString(locale.value, {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
})
</script>

<template>
  <div
    class="bed-slot"
    :class="{
      'bed-slot--free': !isOccupied,
      'bed-slot--occupied': isOccupied,
      'bed-slot--highlighted': isOccupied && highlighted
    }"
    :aria-label="isOccupied && highlighted ? t('bedOccupancy.search.matchHighlight') : undefined"
  >
    <div v-if="isOccupied" class="bed-slot-content">
      <div class="bed-icon" aria-hidden="true">
        <i class="pi pi-user" />
      </div>
      <div class="bed-info">
        <div class="bed-status">{{ t('bedOccupancy.bed.occupied') }}</div>
        <button
          type="button"
          class="patient-link"
          @click="viewAdmission"
          :aria-label="t('bedOccupancy.bed.viewAdmissionFor', { name: occupant!.patientName })"
        >
          {{ occupant!.patientName }}
        </button>
        <div class="admission-date">{{ formattedDate }}</div>
      </div>
    </div>

    <div v-else class="bed-slot-content">
      <div class="bed-icon" aria-hidden="true">
        <i class="pi pi-check-circle" />
      </div>
      <div class="bed-info">
        <div class="bed-status">{{ t('bedOccupancy.bed.free') }}</div>
        <Button
          v-if="canCreateAdmission"
          :label="t('bedOccupancy.bed.admitHere')"
          icon="pi pi-plus"
          size="small"
          severity="success"
          outlined
          class="admit-button"
          @click="admitHere"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.bed-slot {
  display: flex;
  border: 1px solid var(--p-surface-border);
  border-radius: 0.5rem;
  padding: 0.75rem;
  background: var(--p-surface-50, #f8fafc);
  transition: box-shadow 0.15s ease;
}

.bed-slot--free {
  border-color: var(--p-green-200, #bbf7d0);
  background: var(--p-green-50, #f0fdf4);
}

.bed-slot--occupied {
  border-color: var(--p-blue-200, #bfdbfe);
  background: var(--p-blue-50, #eff6ff);
}

.bed-slot--highlighted {
  outline: 2px solid var(--p-yellow-400, #facc15);
  outline-offset: 2px;
}

.bed-slot-content {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  width: 100%;
}

.bed-icon {
  font-size: 1.25rem;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bed-slot--free .bed-icon {
  color: var(--p-green-600, #16a34a);
  background: var(--p-green-100, #dcfce7);
}

.bed-slot--occupied .bed-icon {
  color: var(--p-blue-600, #2563eb);
  background: var(--p-blue-100, #dbeafe);
}

.bed-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
  flex: 1;
}

.bed-status {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-text-muted-color);
}

.patient-link {
  background: none;
  border: none;
  padding: 0;
  text-align: left;
  cursor: pointer;
  font-weight: 600;
  color: var(--p-primary-color);
  text-decoration: none;
  font-size: 0.95rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.patient-link:hover,
.patient-link:focus-visible {
  text-decoration: underline;
}

.admission-date {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
}

.admit-button {
  align-self: flex-start;
  margin-top: 0.25rem;
}
</style>
