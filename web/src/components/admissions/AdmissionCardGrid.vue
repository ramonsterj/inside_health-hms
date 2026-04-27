<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Panel from 'primevue/panel'
import Badge from 'primevue/badge'
import AdmissionCard from './AdmissionCard.vue'
import GenderIcon from '@/components/icons/GenderIcon.vue'
import { ADMISSION_TYPE_META, ADMISSION_TYPE_ORDER } from '@/constants/admissionType'
import { type AdmissionListItem, AdmissionType } from '@/types/admission'
import { Sex } from '@/types/patient'
import { getContrastColor } from '@/utils/format'
import {
  compareTriageCode,
  formatTriageGroupLabel,
  sortByTriage
} from '@/composables/useAdmissionsTableGrouping'
import type { AdmissionsListGroupBy } from '@/stores/admissionsListPreferences'

interface CardGroup {
  /** Stable identifier (used as Panel key). */
  key: string
  /** Translated label for the group header. Omitted when `kind === 'none'`. */
  label?: string
  /** Drives the icon/dot rendering on the group header. */
  kind: 'gender' | 'type' | 'triage' | 'none'
  /** For gender groups: which sex to render in the icon. */
  sex?: Sex | null
  /** For type groups: which type to render in the dot. */
  type?: AdmissionType
  /** For triage groups: badge color (null for untriaged). */
  triageColor?: string | null
  /** For triage groups: the triage code letter (null for untriaged). */
  triageCode?: string | null
  items: AdmissionListItem[]
}

const props = defineProps<{
  admissions: AdmissionListItem[]
  groupBy: AdmissionsListGroupBy
  /** Pass through to each card; controls whether the Status row is shown. */
  showStatus?: boolean
  emptyLabel?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  view: [id: number]
}>()

const { t } = useI18n()

const groups = computed<CardGroup[]>(() => {
  if (props.groupBy === 'none') {
    return [
      {
        key: 'all',
        kind: 'none',
        items: sortByTriage(props.admissions)
      }
    ]
  }

  if (props.groupBy === 'gender') {
    const female: AdmissionListItem[] = []
    const male: AdmissionListItem[] = []
    const other: AdmissionListItem[] = []
    for (const item of props.admissions) {
      if (item.patient.sex === Sex.FEMALE) female.push(item)
      else if (item.patient.sex === Sex.MALE) male.push(item)
      else other.push(item)
    }
    const result: CardGroup[] = []
    if (female.length > 0) {
      result.push({
        key: 'female',
        label: t('admission.listView.groups.female'),
        kind: 'gender',
        sex: Sex.FEMALE,
        items: sortByTriage(female)
      })
    }
    if (male.length > 0) {
      result.push({
        key: 'male',
        label: t('admission.listView.groups.male'),
        kind: 'gender',
        sex: Sex.MALE,
        items: sortByTriage(male)
      })
    }
    if (other.length > 0) {
      result.push({
        key: 'other',
        label: t('admission.listView.groups.other'),
        kind: 'gender',
        sex: null,
        items: sortByTriage(other)
      })
    }
    return result
  }

  if (props.groupBy === 'triage') {
    // Map keyed by triage id; key `null` collects untriaged admissions.
    const buckets = new Map<number | null, AdmissionListItem[]>()
    for (const item of props.admissions) {
      const id = item.triageCode?.id ?? null
      const list = buckets.get(id) ?? []
      list.push(item)
      buckets.set(id, list)
    }
    // Sort triage groups by code (untriaged last).
    const entries = [...buckets.entries()].sort(([, listA], [, listB]) =>
      compareTriageCode(listA[0]?.triageCode?.code, listB[0]?.triageCode?.code)
    )
    return entries.map(([id, list]): CardGroup => {
      const tc = list[0]?.triageCode ?? null
      if (id === null || !tc) {
        return {
          key: 'triage-none',
          label: t('admission.listView.groups.untriaged'),
          kind: 'triage',
          triageColor: null,
          triageCode: null,
          items: list
        }
      }
      return {
        key: `triage-${id}`,
        label: formatTriageGroupLabel(tc) ?? tc.code,
        kind: 'triage',
        triageColor: tc.color,
        triageCode: tc.code,
        items: list
      }
    })
  }

  // groupBy === 'type' — preserve enum order, drop empty buckets.
  const buckets = new Map<AdmissionType, AdmissionListItem[]>()
  for (const item of props.admissions) {
    const list = buckets.get(item.type) ?? []
    list.push(item)
    buckets.set(item.type, list)
  }
  return ADMISSION_TYPE_ORDER.flatMap((type): CardGroup[] => {
    const items = buckets.get(type)
    if (!items || items.length === 0) return []
    return [
      {
        key: type,
        // eslint-disable-next-line security/detect-object-injection -- Safe: type is iterated from a typed enum constant.
        label: t(ADMISSION_TYPE_META[type].labelKey),
        kind: 'type',
        type,
        items: sortByTriage(items)
      }
    ]
  })
})

const isEmpty = computed(() => !props.loading && props.admissions.length === 0)

function onView(id: number) {
  emit('view', id)
}
</script>

<template>
  <div class="admission-card-grid">
    <div v-if="isEmpty" class="empty-state">
      {{ emptyLabel ?? t('admission.empty') }}
    </div>

    <template v-else>
      <template v-for="group in groups" :key="group.key">
        <!-- Ungrouped: render flat grid directly. -->
        <div v-if="group.kind === 'none'" class="cards-grid">
          <AdmissionCard
            v-for="item in group.items"
            :key="item.id"
            :admission="item"
            :show-status="showStatus"
            @view="onView"
          />
        </div>

        <!-- Grouped: collapsible Panel per group. -->
        <Panel v-else toggleable class="group-panel">
          <template #header>
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
          <div class="cards-grid">
            <AdmissionCard
              v-for="item in group.items"
              :key="item.id"
              :admission="item"
              :show-status="showStatus"
              @view="onView"
            />
          </div>
        </Panel>
      </template>
    </template>
  </div>
</template>

<style scoped>
.admission-card-grid {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

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

.empty-state {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--p-text-muted-color);
}

.group-panel {
  background: transparent;
}

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
