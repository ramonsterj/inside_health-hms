<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import { useBedOccupancyStore } from '@/stores/bedOccupancy'
import BedOccupancySummary from '@/components/bedOccupancy/BedOccupancySummary.vue'
import BedOccupancyFilters, {
  type OccupancyStatusFilter
} from '@/components/bedOccupancy/BedOccupancyFilters.vue'
import RoomOccupancyCard from '@/components/bedOccupancy/RoomOccupancyCard.vue'
import { RoomGender, RoomType, type RoomOccupancyItem } from '@/types/room'

const REFRESH_INTERVAL_MS = 30_000

const { t, locale } = useI18n()
const store = useBedOccupancyStore()

const genderFilter = ref<RoomGender | 'ALL'>('ALL')
const typeFilter = ref<RoomType | 'ALL'>('ALL')
const statusFilter = ref<OccupancyStatusFilter>('ALL')
const search = ref('')

const refreshTimer = ref<number | null>(null)

const trimmedSearch = computed(() => search.value.trim().toLowerCase())

const filteredRooms = computed<RoomOccupancyItem[]>(() => {
  return store.rooms.filter(room => {
    if (genderFilter.value !== 'ALL' && room.gender !== genderFilter.value) return false
    if (typeFilter.value !== 'ALL' && room.type !== typeFilter.value) return false
    if (statusFilter.value === 'FREE' && room.availableBeds === 0) return false
    if (statusFilter.value === 'OCCUPIED' && room.occupiedBeds === 0) return false

    if (trimmedSearch.value) {
      const matchesNumber = room.number.toLowerCase().includes(trimmedSearch.value)
      const matchesOccupant = room.occupants.some(o =>
        o.patientName.toLowerCase().includes(trimmedSearch.value)
      )
      if (!matchesNumber && !matchesOccupant) return false
    }

    return true
  })
})

const groupedByGender = computed(() => {
  const groups = new Map<RoomGender, RoomOccupancyItem[]>()
  for (const room of filteredRooms.value) {
    const list = groups.get(room.gender) ?? []
    list.push(room)
    groups.set(room.gender, list)
  }
  return Array.from(groups.entries()).sort((a, b) => a[0].localeCompare(b[0]))
})

const showInitialLoading = computed(() => store.loading && !store.hasLoadedOnce)
const showInitialError = computed(() => !!store.error && !store.hasLoadedOnce)
const showStaleBanner = computed(() => !!store.error && store.hasLoadedOnce)

const lastFetchedAbsolute = computed(() => {
  if (!store.lastFetchedAt) return ''
  return store.lastFetchedAt.toLocaleTimeString(locale.value, {
    hour: '2-digit',
    minute: '2-digit'
  })
})

async function refresh() {
  try {
    await store.fetchOccupancy()
  } catch {
    // store has already populated `error`; banner will surface it
  }
}

function handleVisibilityChange() {
  if (document.hidden) {
    stopAutoRefresh()
  } else {
    refresh()
    startAutoRefresh()
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer.value = window.setInterval(refresh, REFRESH_INTERVAL_MS)
}

function stopAutoRefresh() {
  if (refreshTimer.value !== null) {
    window.clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

onMounted(async () => {
  await refresh()
  startAutoRefresh()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  store.reset()
})

function genderGroupLabel(genderKey: RoomGender): string {
  return t(`room.genders.${genderKey}`)
}
</script>

<template>
  <div class="bed-occupancy-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ t('bedOccupancy.title') }}</h1>
        <p v-if="store.lastFetchedAt" class="last-updated">
          {{ t('bedOccupancy.lastUpdated', { time: lastFetchedAbsolute }) }}
        </p>
      </div>
      <Button
        icon="pi pi-refresh"
        :label="t('bedOccupancy.refresh')"
        severity="secondary"
        outlined
        :loading="store.loading"
        @click="refresh"
      />
    </div>

    <div v-if="showInitialLoading" class="centered-state">
      <ProgressSpinner />
    </div>

    <div v-else-if="showInitialError" class="centered-state">
      <Message severity="error" :closable="false">
        {{ t('bedOccupancy.error.loadFailed') }}
      </Message>
      <Button
        :label="t('bedOccupancy.error.retry')"
        icon="pi pi-refresh"
        @click="refresh"
        class="retry-button"
      />
    </div>

    <template v-else>
      <Message
        v-if="showStaleBanner"
        severity="warn"
        :closable="false"
        class="stale-banner"
      >
        <div class="stale-content">
          <span>
            {{ t('bedOccupancy.error.stale', { time: lastFetchedAbsolute }) }}
          </span>
          <Button
            :label="t('bedOccupancy.error.retry')"
            icon="pi pi-refresh"
            severity="warn"
            size="small"
            @click="refresh"
          />
        </div>
      </Message>

      <BedOccupancySummary :summary="store.summary" />

      <Card class="filter-card">
        <template #content>
          <BedOccupancyFilters
            v-model:gender="genderFilter"
            v-model:type="typeFilter"
            v-model:status="statusFilter"
            v-model:search="search"
          />
        </template>
      </Card>

      <div v-if="store.rooms.length === 0" class="empty-state">
        <i class="pi pi-building empty-icon" aria-hidden="true" />
        <p>{{ t('bedOccupancy.empty') }}</p>
      </div>

      <div v-else-if="filteredRooms.length === 0" class="empty-state">
        <i class="pi pi-search empty-icon" aria-hidden="true" />
        <p>{{ t('bedOccupancy.emptyFiltered') }}</p>
      </div>

      <div v-else class="gender-groups">
        <section
          v-for="[genderKey, roomsInGroup] in groupedByGender"
          :key="genderKey"
          class="gender-group"
        >
          <h2 class="group-title">{{ genderGroupLabel(genderKey) }}</h2>
          <div class="room-grid">
            <RoomOccupancyCard
              v-for="room in roomsInGroup"
              :key="room.id"
              :room="room"
              :search-highlight="trimmedSearch"
            />
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.bed-occupancy-page {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.page-title {
  margin: 0;
}

.last-updated {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
}

.centered-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  min-height: 250px;
}

.retry-button {
  align-self: center;
}

.stale-banner :deep(.p-message-text) {
  width: 100%;
}

.stale-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  width: 100%;
  flex-wrap: wrap;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: var(--p-text-muted-color);
  text-align: center;
  gap: 0.75rem;
}

.empty-icon {
  font-size: 2.5rem;
}

.gender-groups {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.gender-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.group-title {
  margin: 0;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-text-muted-color);
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}
</style>
