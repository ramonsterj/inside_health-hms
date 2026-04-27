<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import { RoomGender, RoomType } from '@/types/room'

export type OccupancyStatusFilter = 'ALL' | 'FREE' | 'OCCUPIED'

const genderFilter = defineModel<RoomGender | 'ALL'>('gender', { required: true })
const typeFilter = defineModel<RoomType | 'ALL'>('type', { required: true })
const statusFilter = defineModel<OccupancyStatusFilter>('status', { required: true })
const search = defineModel<string>('search', { required: true })

const { t } = useI18n()

const genderOptions = computed(() => [
  { label: t('bedOccupancy.filter.gender.all'), value: 'ALL' as const },
  { label: t('bedOccupancy.filter.gender.male'), value: RoomGender.MALE },
  { label: t('bedOccupancy.filter.gender.female'), value: RoomGender.FEMALE }
])

const typeOptions = computed(() => [
  { label: t('bedOccupancy.filter.type.all'), value: 'ALL' as const },
  { label: t('bedOccupancy.filter.type.private'), value: RoomType.PRIVATE },
  { label: t('bedOccupancy.filter.type.shared'), value: RoomType.SHARED }
])

const statusOptions = computed<{ label: string; value: OccupancyStatusFilter }[]>(() => [
  { label: t('bedOccupancy.filter.status.all'), value: 'ALL' },
  { label: t('bedOccupancy.filter.status.free'), value: 'FREE' },
  { label: t('bedOccupancy.filter.status.occupied'), value: 'OCCUPIED' }
])
</script>

<template>
  <div class="filter-bar">
    <div class="filter-field">
      <label class="filter-label" for="bed-gender-filter">
        {{ t('room.gender') }}
      </label>
      <Select
        id="bed-gender-filter"
        v-model="genderFilter"
        :options="genderOptions"
        optionLabel="label"
        optionValue="value"
        style="min-width: 150px"
      />
    </div>
    <div class="filter-field">
      <label class="filter-label" for="bed-type-filter">
        {{ t('room.type') }}
      </label>
      <Select
        id="bed-type-filter"
        v-model="typeFilter"
        :options="typeOptions"
        optionLabel="label"
        optionValue="value"
        style="min-width: 150px"
      />
    </div>
    <div class="filter-field">
      <label class="filter-label" for="bed-status-filter">
        {{ t('admission.status') }}
      </label>
      <Select
        id="bed-status-filter"
        v-model="statusFilter"
        :options="statusOptions"
        optionLabel="label"
        optionValue="value"
        style="min-width: 150px"
      />
    </div>
    <div class="filter-field filter-field--grow">
      <label class="filter-label" for="bed-search">
        {{ t('common.search') }}
      </label>
      <InputText
        id="bed-search"
        v-model="search"
        :placeholder="t('bedOccupancy.search.placeholder')"
      />
    </div>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.filter-field--grow {
  flex: 1;
  min-width: 220px;
}

.filter-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
</style>
