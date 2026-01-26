<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import { useAdmissionStore } from '@/stores/admission'
import { useAuthStore } from '@/stores/auth'
import { AdmissionStatus } from '@/types/admission'

const { t } = useI18n()
const router = useRouter()
const { showError } = useErrorHandler()
const admissionStore = useAdmissionStore()
const authStore = useAuthStore()

const first = ref(0)
const rows = ref(20)
const statusFilter = ref<AdmissionStatus | null>(null)

const canCreate = computed(() => authStore.hasPermission('admission:create'))

const statusOptions = computed(() => [
  { label: t('common.all'), value: null },
  { label: t('admission.statuses.ACTIVE'), value: AdmissionStatus.ACTIVE },
  { label: t('admission.statuses.DISCHARGED'), value: AdmissionStatus.DISCHARGED }
])

onMounted(() => {
  loadAdmissions()
})

async function loadAdmissions() {
  try {
    const page = Math.floor(first.value / rows.value)
    await admissionStore.fetchAdmissions(page, rows.value, statusFilter.value)
  } catch (error) {
    showError(error)
  }
}

function onStatusChange() {
  first.value = 0
  loadAdmissions()
}

function onPageChange() {
  loadAdmissions()
}

function viewAdmission(id: number) {
  router.push({ name: 'admission-detail', params: { id } })
}

function createNewAdmission() {
  router.push({ name: 'admission-create' })
}

function getFullName(firstName: string, lastName: string): string {
  return `${firstName} ${lastName}`.trim()
}

function getStatusSeverity(status: AdmissionStatus): 'success' | 'secondary' {
  return status === AdmissionStatus.ACTIVE ? 'success' : 'secondary'
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString()
}

function getContrastColor(hexColor: string): string {
  const r = parseInt(hexColor.slice(1, 3), 16)
  const g = parseInt(hexColor.slice(3, 5), 16)
  const b = parseInt(hexColor.slice(5, 7), 16)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return luminance > 0.5 ? '#000000' : '#FFFFFF'
}
</script>

<template>
  <div class="admissions-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('admission.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('admission.new')"
          @click="createNewAdmission"
        />
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
              @change="onStatusChange"
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
                class="triage-badge"
                :style="{
                  backgroundColor: data.triageCode.color,
                  color: getContrastColor(data.triageCode.color)
                }"
              >
                {{ data.triageCode.code }}
              </span>
            </template>
          </Column>

          <Column :header="t('admission.room')" style="width: 100px">
            <template #body="{ data }">
              {{ data.room.number }}
            </template>
          </Column>

          <Column :header="t('admission.admissionDate')" style="width: 120px">
            <template #body="{ data }">
              {{ formatDate(data.admissionDate) }}
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

.filter-card {
  margin-bottom: 1rem;
}

.filter-bar {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-field label {
  font-weight: 500;
  font-size: 0.875rem;
}

.triage-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
  font-size: 0.875rem;
}
</style>
