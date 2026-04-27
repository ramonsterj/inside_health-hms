<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ADMISSION_TYPE_META } from '@/constants/admissionType'
import { type AdmissionListItem } from '@/types/admission'
import type { AdmissionsListGroupBy } from '@/stores/admissionsListPreferences'
import { Sex } from '@/types/patient'
import { getContrastColor } from '@/utils/format'
import { formatTriageGroupLabel } from '@/composables/useAdmissionsTableGrouping'
import GenderIcon from '@/components/icons/GenderIcon.vue'

defineProps<{
  data: AdmissionListItem
  groupBy: Extract<AdmissionsListGroupBy, 'gender' | 'type' | 'triage'>
}>()

const { t } = useI18n()

function genderGroupLabel(sex: Sex | null | undefined): string {
  if (sex === Sex.FEMALE) return t('admission.listView.groups.female')
  if (sex === Sex.MALE) return t('admission.listView.groups.male')
  return t('admission.listView.groups.other')
}

function triageGroupLabel(item: AdmissionListItem): string {
  return formatTriageGroupLabel(item.triageCode) ?? t('admission.listView.groups.untriaged')
}
</script>

<template>
  <span class="group-row-header">
    <template v-if="groupBy === 'gender'">
      <GenderIcon :sex="data.patient.sex" :size="16" />
      <span class="group-row-label">{{ genderGroupLabel(data.patient.sex) }}</span>
    </template>
    <template v-else-if="groupBy === 'type'">
      <span
        class="group-row-swatch"
        :class="ADMISSION_TYPE_META[data.type].dotClass"
        aria-hidden="true"
      ></span>
      <span class="group-row-label">{{ t(ADMISSION_TYPE_META[data.type].labelKey) }}</span>
    </template>
    <template v-else>
      <span
        v-if="data.triageCode"
        class="group-row-triage-badge"
        :style="{
          backgroundColor: data.triageCode.color,
          color: getContrastColor(data.triageCode.color)
        }"
      >
        {{ data.triageCode.code }}
      </span>
      <span class="group-row-label">{{ triageGroupLabel(data) }}</span>
    </template>
  </span>
</template>

<style scoped>
.group-row-header {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
}

.group-row-swatch {
  display: inline-block;
  width: 0.625rem;
  height: 0.625rem;
  border-radius: 999px;
}

.group-row-triage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  padding: 0.125rem 0.5rem;
  border-radius: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
}
</style>
