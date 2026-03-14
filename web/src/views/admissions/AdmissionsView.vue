<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useRelativeTime } from '@/composables/useRelativeTime'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import { getContrastColor, getFullName, formatShortDateTime } from '@/utils/format'
import { useAdmissionStore } from '@/stores/admission'
import { AdmissionStatus, AdmissionType } from '@/types/admission'
import AdmissionTypeBadge from '@/components/admissions/AdmissionTypeBadge.vue'

const { t, locale } = useI18n()
const router = useRouter()
const { showError } = useErrorHandler()
const { getRelativeTime } = useRelativeTime()
const admissionStore = useAdmissionStore()

const first = ref(0)
const rows = ref(20)
const statusFilter = ref<AdmissionStatus | null>(AdmissionStatus.ACTIVE)
const typeFilter = ref<AdmissionType | null>(null)

const statusOptions = computed(() => [
  { label: t('common.all'), value: null },
  { label: t('admission.statuses.ACTIVE'), value: AdmissionStatus.ACTIVE },
  { label: t('admission.statuses.DISCHARGED'), value: AdmissionStatus.DISCHARGED }
])

const typeOptions = computed(() => [
  { label: t('common.all'), value: null },
  { label: t('admission.types.HOSPITALIZATION'), value: AdmissionType.HOSPITALIZATION },
  { label: t('admission.types.AMBULATORY'), value: AdmissionType.AMBULATORY },
  { label: t('admission.types.ELECTROSHOCK_THERAPY'), value: AdmissionType.ELECTROSHOCK_THERAPY },
  { label: t('admission.types.KETAMINE_INFUSION'), value: AdmissionType.KETAMINE_INFUSION },
  { label: t('admission.types.EMERGENCY'), value: AdmissionType.EMERGENCY }
])

onMounted(() => {
  loadAdmissions()
})

async function loadAdmissions() {
  try {
    const page = Math.floor(first.value / rows.value)
    await admissionStore.fetchAdmissions(page, rows.value, statusFilter.value, typeFilter.value)
  } catch (error) {
    showError(error)
  }
}

function onFilterChange() {
  first.value = 0
  loadAdmissions()
}

function onPageChange() {
  loadAdmissions()
}

function viewAdmission(id: number) {
  router.push({ name: 'admission-detail', params: { id } })
}

function getStatusSeverity(status: AdmissionStatus): 'success' | 'secondary' {
  return status === AdmissionStatus.ACTIVE ? 'success' : 'secondary'
}
</script>

<template>
  <div class="admissions-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('admission.title') }}</h1>
      <div class="header-actions">
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="loadAdmissions"
          :loading="admissionStore.loading"
        />
      </div>
    </div>

    <Card class="filter-card">
      <template #content>
        <div class="filter-bar">
          <div class="filter-field">
            <label>{{ t('admission.status') }}</label>
            <Select
              v-model="statusFilter"
              :options="statusOptions"
              optionLabel="label"
              optionValue="value"
              @change="onFilterChange"
              style="width: 200px"
            />
          </div>
          <div class="filter-field">
            <label>{{ t('admission.type') }}</label>
            <Select
              v-model="typeFilter"
              :options="typeOptions"
              optionLabel="label"
              optionValue="value"
              @change="onFilterChange"
              style="width: 200px"
            />
          </div>
        </div>
      </template>
    </Card>

    <Card>
      <template #content>
        <DataTable
          :value="admissionStore.admissions"
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
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('admission.empty') }}
            </div>
          </template>

          <Column :header="t('admission.patient')">
            <template #body="{ data }">
              {{ getFullName(data.patient.firstName, data.patient.lastName) }}
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

          <Column :header="t('admission.status')" style="width: 120px">
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
    </Card>
  </div>
</template>

<style scoped>
@import '@/assets/admission-table.css';

.admissions-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}
</style>
