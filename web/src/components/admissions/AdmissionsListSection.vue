<script setup lang="ts">
import { computed, ref, toRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Paginator from 'primevue/paginator'
import AdmissionCardGrid from '@/components/admissions/AdmissionCardGrid.vue'
import AdmissionsGroupRowHeader from '@/components/admissions/AdmissionsGroupRowHeader.vue'
import AdmissionTypeBadge from '@/components/admissions/AdmissionTypeBadge.vue'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useRelativeTime } from '@/composables/useRelativeTime'
import { useAdmissionsTableGrouping } from '@/composables/useAdmissionsTableGrouping'
import { useAdmissionStore } from '@/stores/admission'
import { useAdmissionsListPreferencesStore } from '@/stores/admissionsListPreferences'
import { AdmissionStatus, AdmissionType, type AdmissionListItem } from '@/types/admission'
import { formatShortDateTime, getContrastColor, getFullName } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    statusFilter?: AdmissionStatus | null
    typeFilter?: AdmissionType | null
    emptyLabel: string
    showStatus?: boolean
    showTreatingPhysician?: boolean
  }>(),
  {
    statusFilter: null,
    typeFilter: null,
    showStatus: false,
    showTreatingPhysician: false
  }
)

const { t, locale } = useI18n()
const router = useRouter()
const { showError } = useErrorHandler()
const { getRelativeTime } = useRelativeTime()
const admissionStore = useAdmissionStore()
const preferences = useAdmissionsListPreferencesStore()

const first = ref(0)
const rows = ref(20)

const { tableData, groupRowsBy } = useAdmissionsTableGrouping(
  computed(() => admissionStore.admissions),
  toRef(preferences, 'groupBy')
)

watch(
  () => [props.statusFilter, props.typeFilter] as const,
  () => {
    first.value = 0
    loadAdmissions()
  },
  { immediate: true }
)

async function loadAdmissions() {
  try {
    const page = Math.floor(first.value / rows.value)
    await admissionStore.fetchAdmissions(page, rows.value, props.statusFilter, props.typeFilter)
  } catch (error) {
    showError(error)
  }
}

function onPageChange(event?: { first: number; rows: number }) {
  if (event) {
    first.value = event.first
    rows.value = event.rows
  }
  loadAdmissions()
}

function viewAdmission(id: number) {
  router.push({ name: 'admission-detail', params: { id } })
}

function formatDoctorName(doctor: {
  salutation: string | null
  firstName: string | null
  lastName: string | null
}): string {
  const salutationLabel = doctor.salutation ? t(`user.salutations.${doctor.salutation}`) : ''
  return `${salutationLabel} ${getFullName(doctor.firstName, doctor.lastName)}`.trim()
}

function getStatusSeverity(status: AdmissionStatus): 'success' | 'secondary' {
  return status === AdmissionStatus.ACTIVE ? 'success' : 'secondary'
}

defineExpose({
  refresh: loadAdmissions
})
</script>

<template>
  <template v-if="preferences.viewMode === 'cards'">
    <AdmissionCardGrid
      :admissions="admissionStore.admissions"
      :group-by="preferences.groupBy"
      :loading="admissionStore.loading"
      :show-status="showStatus"
      :empty-label="emptyLabel"
      @view="viewAdmission"
    />
    <Paginator
      v-if="admissionStore.totalAdmissions > rows"
      class="cards-paginator"
      :first="first"
      :rows="rows"
      :totalRecords="admissionStore.totalAdmissions"
      :rowsPerPageOptions="[10, 20, 50]"
      @page="onPageChange"
    />
  </template>

  <DataTable
    v-else
    :value="tableData"
    :loading="admissionStore.loading"
    :paginator="true"
    v-model:rows="rows"
    v-model:first="first"
    :totalRecords="admissionStore.totalAdmissions"
    :lazy="true"
    @page="onPageChange"
    :rowsPerPageOptions="[10, 20, 50]"
    dataKey="id"
    stripedRows
    scrollable
    :groupRowsBy="groupRowsBy"
    :rowGroupMode="groupRowsBy ? 'subheader' : undefined"
  >
    <template #empty>
      <div class="text-center p-4">
        {{ emptyLabel }}
      </div>
    </template>

    <template
      v-if="
        preferences.groupBy === 'gender' ||
        preferences.groupBy === 'type' ||
        preferences.groupBy === 'triage'
      "
      #groupheader="{ data }: { data: AdmissionListItem }"
    >
      <AdmissionsGroupRowHeader :data="data" :group-by="preferences.groupBy" />
    </template>

    <Column :header="t('admission.patient')">
      <template #body="{ data }">
        {{ getFullName(data.patient.firstName, data.patient.lastName) }}
      </template>
    </Column>

    <Column v-if="showTreatingPhysician" :header="t('admission.treatingPhysician')">
      <template #body="{ data }">
        {{ formatDoctorName(data.treatingPhysician) }}
      </template>
    </Column>

    <Column :header="t('admission.triageCode')" style="width: 100px">
      <template #body="{ data }">
        <span
          v-if="data.triageCode"
          class="triage-badge"
          :style="{
            backgroundColor: data.triageCode.color,
            color: getContrastColor(data.triageCode.color)
          }"
        >
          {{ data.triageCode.code }}
        </span>
        <span v-else>-</span>
      </template>
    </Column>

    <Column :header="t('admission.room')" style="width: 100px">
      <template #body="{ data }">
        {{ data.room?.number || '-' }}
      </template>
    </Column>

    <Column :header="t('admission.type')" style="width: 150px">
      <template #body="{ data }">
        <AdmissionTypeBadge :type="data.type" />
      </template>
    </Column>

    <Column :header="t('admission.admissionDate')" style="width: 180px">
      <template #body="{ data }">
        <div class="admission-date">
          <span class="date-time">{{ formatShortDateTime(data.admissionDate, locale) }}</span>
          <span class="relative-time">{{ getRelativeTime(data.admissionDate) }}</span>
        </div>
      </template>
    </Column>

    <Column v-if="showStatus" :header="t('admission.status')" style="width: 120px">
      <template #body="{ data }">
        <Tag
          :value="t(`admission.statuses.${data.status}`)"
          :severity="getStatusSeverity(data.status)"
        />
      </template>
    </Column>

    <Column :header="t('common.actions')" style="width: 80px">
      <template #body="{ data }">
        <Button
          icon="pi pi-eye"
          severity="info"
          text
          rounded
          @click="viewAdmission(data.id)"
          v-tooltip.top="t('common.view')"
        />
      </template>
    </Column>
  </DataTable>
</template>

<style scoped>
@import '@/assets/admission-table.css';

.cards-paginator {
  margin-top: 1rem;
}
</style>
