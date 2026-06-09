<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Panel from 'primevue/panel'
import AdmissionCardList from './AdmissionCardList.vue'
import AdmissionGroupHeader from './AdmissionGroupHeader.vue'
import AdmissionCardSubGroup from './AdmissionCardSubGroup.vue'
import { ADMISSION_TYPE_META } from '@/constants/admissionType'
import { type AdmissionListItem } from '@/types/admission'
import { Sex } from '@/types/patient'
import {
  bucketAdmissions,
  formatTriageGroupLabel,
  sortByTriage,
  type BucketLabelers,
  type CardGroupDescriptor
} from '@/composables/useAdmissionsGrouping'
import { useCodeLabels } from '@/composables/useCodeLabels'
import type { AdmissionsListGroupBy } from '@/stores/admissionsListPreferences'

const props = defineProps<{
  admissions: AdmissionListItem[]
  primaryGroupBy: AdmissionsListGroupBy
  secondaryGroupBy: AdmissionsListGroupBy
  /** Pass through to each card; controls whether the Status row is shown. */
  showStatus?: boolean
  emptyLabel?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  view: [id: number]
}>()

const { t } = useI18n()
const { triageCodeLabel } = useCodeLabels()

// i18n-aware label resolvers passed into the (vue-i18n-free) bucketing helper.
const labelers: BucketLabelers = {
  genderLabel: sex => {
    if (sex === Sex.FEMALE) return t('admission.listView.groups.female')
    if (sex === Sex.MALE) return t('admission.listView.groups.male')
    return t('admission.listView.groups.other')
  },
  // eslint-disable-next-line security/detect-object-injection -- Safe: type is a typed enum value.
  typeLabel: type => t(ADMISSION_TYPE_META[type].labelKey),
  triageLabel: tc => {
    if (!tc) return t('admission.listView.groups.untriaged')
    const localized = triageCodeLabel(tc.code, tc.description ?? '')
    return formatTriageGroupLabel(tc, localized) ?? tc.code
  }
}

/** Outer (primary) groups; empty unless a primary dimension is selected. */
const primaryGroups = computed<CardGroupDescriptor[]>(() => {
  if (props.primaryGroupBy === 'none') return []
  return bucketAdmissions(props.admissions, props.primaryGroupBy, labelers)
})

/** Flat, triage-sorted list used when no grouping is active. */
const flatItems = computed(() => sortByTriage(props.admissions))

/** Narrowed secondary dimension (null when no second level is selected). */
const secondaryDimension = computed<Exclude<AdmissionsListGroupBy, 'none'> | null>(() =>
  props.secondaryGroupBy === 'none' ? null : props.secondaryGroupBy
)

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

    <!-- Ungrouped: flat grid. -->
    <AdmissionCardList
      v-else-if="primaryGroupBy === 'none'"
      :items="flatItems"
      :show-status="showStatus"
      @view="onView"
    />

    <!-- Grouped: collapsible Panel per primary group. -->
    <template v-else>
      <Panel v-for="group in primaryGroups" :key="group.key" toggleable class="group-panel">
        <template #header>
          <AdmissionGroupHeader :group="group" />
        </template>

        <!-- Second level: nested subgroups by the secondary dimension. -->
        <AdmissionCardSubGroup
          v-if="secondaryDimension"
          :items="group.items"
          :dimension="secondaryDimension"
          :labelers="labelers"
          :show-status="showStatus"
          @view="onView"
        />

        <!-- Single level: leaf cards grid. -->
        <AdmissionCardList v-else :items="group.items" :show-status="showStatus" @view="onView" />
      </Panel>
    </template>
  </div>
</template>

<style scoped>
.admission-card-grid {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.empty-state {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--p-text-muted-color);
}

.group-panel {
  background: transparent;
}
</style>
