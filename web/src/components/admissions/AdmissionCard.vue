<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Tag from 'primevue/tag'
import { useRelativeTime } from '@/composables/useRelativeTime'
import { shortLabelFromDescription } from '@/composables/useAdmissionsTableGrouping'
import { getContrastColor, getFullName } from '@/utils/format'
import { ADMISSION_TYPE_META } from '@/constants/admissionType'
import { type AdmissionListItem, AdmissionStatus } from '@/types/admission'
import GenderIcon from '@/components/icons/GenderIcon.vue'

const props = defineProps<{
  admission: AdmissionListItem
  /** When true, includes a Status row in the card body (Admissions screen). */
  showStatus?: boolean
}>()

const emit = defineEmits<{
  view: [id: number]
}>()

const { t } = useI18n()
const { getRelativeTime } = useRelativeTime()

const typeMeta = computed(() => ADMISSION_TYPE_META[props.admission.type])
const typeLabel = computed(() => t(typeMeta.value.labelKey))

const triageShortLabel = computed(() => {
  const tc = props.admission.triageCode
  if (!tc) return null
  // Resolve the locale-aware description (falls back to the raw value for
  // admin-created codes that aren't in the i18n bundle), then extract the
  // short label before " - ".
  const localizedDescription = t(`triageCode.codes.${tc.code}`, tc.description ?? '')
  return shortLabelFromDescription(localizedDescription, tc.code)
})

const patientName = computed(() =>
  getFullName(props.admission.patient.firstName, props.admission.patient.lastName)
)

const doctorName = computed(() => {
  const doc = props.admission.treatingPhysician
  const salutation = doc.salutation ? t(`user.salutations.${doc.salutation}`) : ''
  const fullName = getFullName(doc.firstName, doc.lastName)
  const combined = `${salutation} ${fullName}`.trim()
  return combined || null
})

const statusSeverity = computed((): 'success' | 'secondary' =>
  props.admission.status === AdmissionStatus.ACTIVE ? 'success' : 'secondary'
)

const ariaLabel = computed(() => t('admission.listView.viewActionFor', { name: patientName.value }))

function onActivate() {
  emit('view', props.admission.id)
}
</script>

<template>
  <article
    class="admission-card"
    role="button"
    tabindex="0"
    :aria-label="ariaLabel"
    @click="onActivate"
    @keydown.enter.prevent="onActivate"
    @keydown.space.prevent="onActivate"
  >
    <header class="card-header">
      <div class="avatar" aria-hidden="true">
        <GenderIcon :sex="admission.patient.sex" :size="28" />
      </div>
      <div class="header-text">
        <h3 class="patient-name">{{ patientName }}</h3>
        <p class="type-subtitle">
          <span class="type-pill" :class="typeMeta.dotClass">{{ typeLabel }}</span>
        </p>
      </div>
    </header>

    <dl class="card-body">
      <template v-if="doctorName">
        <dt>{{ t('admission.listView.labels.doctor') }}</dt>
        <dd>{{ doctorName }}</dd>
      </template>

      <template v-if="admission.room">
        <dt>{{ t('admission.listView.labels.room') }}</dt>
        <dd>{{ admission.room.number }}</dd>
      </template>

      <template v-if="admission.triageCode">
        <dt>{{ t('admission.listView.labels.triage') }}</dt>
        <dd class="triage-cell">
          <span
            class="triage-pill"
            :style="{
              backgroundColor: admission.triageCode.color,
              color: getContrastColor(admission.triageCode.color)
            }"
          >
            {{ triageShortLabel }}
          </span>
        </dd>
      </template>

      <dt>{{ t('admission.listView.labels.admitted') }}</dt>
      <dd>{{ getRelativeTime(admission.admissionDate) }}</dd>

      <template v-if="showStatus">
        <dt>{{ t('admission.listView.labels.status') }}</dt>
        <dd>
          <Tag :value="t(`admission.statuses.${admission.status}`)" :severity="statusSeverity" />
        </dd>
      </template>
    </dl>
  </article>
</template>

<style scoped>
.admission-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background: var(--p-content-background, #ffffff);
  border-radius: 0.75rem;
  box-shadow:
    0 1px 2px 0 rgb(0 0 0 / 0.05),
    0 1px 3px 0 rgb(0 0 0 / 0.05);
  padding: 1.25rem;
  border: 1px solid var(--p-content-border-color, transparent);
  cursor: pointer;
  text-align: left;
  transition:
    box-shadow 120ms ease,
    transform 120ms ease;
}

.admission-card:hover {
  box-shadow:
    0 4px 6px -1px rgb(0 0 0 / 0.08),
    0 2px 4px -2px rgb(0 0 0 / 0.06);
  transform: translateY(-1px);
}

.admission-card:focus-visible {
  outline: 2px solid var(--p-primary-color, #3b82f6);
  outline-offset: 2px;
}

.card-header {
  display: flex;
  gap: 0.875rem;
  align-items: center;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 999px;
  background-color: var(--p-surface-100, #f1f5f9);
  color: var(--p-text-muted-color, #64748b);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.patient-name {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--p-text-color);
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-subtitle {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
}

.type-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.125rem 0.625rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: #ffffff;
  line-height: 1.4;
}

.card-body {
  display: grid;
  grid-template-columns: minmax(5rem, auto) 1fr;
  column-gap: 1rem;
  row-gap: 0.5rem;
  margin: 0;
  font-size: 0.875rem;
}

.card-body dt {
  color: var(--p-text-muted-color);
  font-weight: 500;
}

.card-body dd {
  margin: 0;
  text-align: right;
  color: var(--p-text-color);
  font-weight: 500;
  word-break: break-word;
}

.triage-cell {
  display: flex;
  justify-content: flex-end;
}

.triage-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.125rem 0.625rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1.4;
}
</style>
