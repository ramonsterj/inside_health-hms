<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import ProgressSpinner from 'primevue/progressspinner'
import { useVitalSignStore } from '@/stores/vitalSign'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useErrorHandler } from '@/composables/useErrorHandler'
import VitalSignDateFilter from './VitalSignDateFilter.vue'
import VitalSignFormDialog from './VitalSignFormDialog.vue'
import type { VitalSignResponse, VitalSignDateRange } from '@/types/nursing'
import type { AdmissionStatus } from '@/types/admission'

const props = defineProps<{
  admissionId: number
  admissionStatus: AdmissionStatus
}>()

const { t, d } = useI18n()
const vitalSignStore = useVitalSignStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const { showError } = useErrorHandler()

// Pagination state
const first = ref(0)
const rows = ref(20)

// Dialog state
const dialogVisible = ref(false)
const vitalSignToEdit = ref<VitalSignResponse | null>(null)

// Permission checks
const canCreate = computed(
  () => authStore.hasPermission('vital-sign:create') && props.admissionStatus === 'ACTIVE'
)
const canUpdate = computed(
  () => authStore.hasPermission('vital-sign:update') && props.admissionStatus === 'ACTIVE'
)

// Date range filter
const dateRange = computed({
  get: () => vitalSignStore.dateRange,
  set: (value: VitalSignDateRange) => vitalSignStore.setDateRange(value)
})

// Computed data
const vitalSigns = computed(() => vitalSignStore.getVitalSigns(props.admissionId))
const totalVitalSigns = computed(() => vitalSignStore.getTotalVitalSigns(props.admissionId))
const loading = computed(() => vitalSignStore.loading)

// Load vital signs
async function loadVitalSigns() {
  try {
    await vitalSignStore.fetchVitalSigns(props.admissionId, first.value / rows.value, rows.value)
  } catch (error) {
    showError(error)
  }
}

// Pagination handler
function onPage(event: { first: number; rows: number }) {
  first.value = event.first
  rows.value = event.rows
  loadVitalSigns()
}

// Filter change handler
function onFilterChange() {
  first.value = 0
  loadVitalSigns()
}

// Format blood pressure as "systolic/diastolic"
function formatBloodPressure(vs: VitalSignResponse): string {
  return `${vs.systolicBp}/${vs.diastolicBp}`
}

// Format author name
function formatAuthor(vs: VitalSignResponse): string {
  const author = vs.createdBy
  if (!author) return '-'
  const salutation = author.salutation || ''
  const firstName = author.firstName || ''
  const lastName = author.lastName || ''
  return `${salutation} ${firstName} ${lastName}`.trim() || '-'
}

// Check if can edit specific record
function canEditRecord(vs: VitalSignResponse): boolean {
  return canUpdate.value && vs.canEdit
}

// Dialog handlers
function openCreateDialog() {
  vitalSignToEdit.value = null
  dialogVisible.value = true
}

function openEditDialog(vs: VitalSignResponse) {
  vitalSignToEdit.value = vs
  dialogVisible.value = true
}

function onVitalSignSaved() {
  notificationStore.success(
    vitalSignToEdit.value ? t('nursing.vitalSigns.updated') : t('nursing.vitalSigns.created')
  )
}

// Watch for admission changes
watch(
  () => props.admissionId,
  () => {
    first.value = 0
    loadVitalSigns()
  }
)

// Initial load
onMounted(loadVitalSigns)
</script>

<template>
  <div class="vital-sign-table">
    <!-- Header -->
    <div class="table-header">
      <div class="header-left">
        <Badge v-if="totalVitalSigns > 0" :value="totalVitalSigns" severity="secondary" />
      </div>
      <div class="header-right">
        <slot name="header-right" />
        <Button
          v-if="canCreate"
          :label="t('nursing.vitalSigns.add')"
          icon="pi pi-plus"
          @click="openCreateDialog"
        />
      </div>
    </div>

    <!-- Date filter -->
    <VitalSignDateFilter v-model="dateRange" @filter-change="onFilterChange" />

    <!-- Content -->
    <div class="table-content">
      <!-- Loading state -->
      <div v-if="loading" class="loading-state">
        <ProgressSpinner strokeWidth="3" />
      </div>

      <!-- Empty state -->
      <div v-else-if="vitalSigns.length === 0" class="empty-state">
        <i class="pi pi-heart empty-icon"></i>
        <p>{{ t('nursing.vitalSigns.empty') }}</p>
        <Button
          v-if="canCreate"
          :label="t('nursing.vitalSigns.addFirst')"
          icon="pi pi-plus"
          @click="openCreateDialog"
        />
      </div>

      <!-- Data table -->
      <DataTable
        v-else
        :value="vitalSigns"
        :loading="loading"
        :paginator="totalVitalSigns > rows"
        :rows="rows"
        :totalRecords="totalVitalSigns"
        :rowsPerPageOptions="[10, 20, 50]"
        :first="first"
        lazy
        @page="onPage"
        stripedRows
        responsiveLayout="scroll"
      >
        <Column field="recordedAt" :header="t('nursing.vitalSigns.fields.recordedAt')">
          <template #body="{ data }">
            {{ d(new Date(data.recordedAt), 'long') }}
          </template>
        </Column>
        <Column :header="t('nursing.vitalSigns.fields.bloodPressure')">
          <template #body="{ data }">
            <span class="bp-value">{{ formatBloodPressure(data) }}</span>
            <span class="unit">{{ t('nursing.vitalSigns.units.mmHg') }}</span>
          </template>
        </Column>
        <Column field="heartRate" :header="t('nursing.vitalSigns.fields.heartRate')">
          <template #body="{ data }">
            {{ data.heartRate }}
            <span class="unit">{{ t('nursing.vitalSigns.units.bpm') }}</span>
          </template>
        </Column>
        <Column field="respiratoryRate" :header="t('nursing.vitalSigns.fields.respiratoryRate')">
          <template #body="{ data }">
            {{ data.respiratoryRate }}
            <span class="unit">{{ t('nursing.vitalSigns.units.breathsPerMin') }}</span>
          </template>
        </Column>
        <Column field="temperature" :header="t('nursing.vitalSigns.fields.temperature')">
          <template #body="{ data }">
            {{ data.temperature.toFixed(1) }}
            <span class="unit">{{ t('nursing.vitalSigns.units.celsius') }}</span>
          </template>
        </Column>
        <Column field="oxygenSaturation" :header="t('nursing.vitalSigns.fields.oxygenSaturation')">
          <template #body="{ data }">
            {{ data.oxygenSaturation }}
            <span class="unit">{{ t('nursing.vitalSigns.units.percent') }}</span>
          </template>
        </Column>
        <Column :header="t('common.createdBy')">
          <template #body="{ data }">
            {{ formatAuthor(data) }}
          </template>
        </Column>
        <Column :header="t('common.actions')" :exportable="false" style="min-width: 80px">
          <template #body="{ data }">
            <Button
              v-if="canEditRecord(data)"
              icon="pi pi-pencil"
              text
              rounded
              @click="openEditDialog(data)"
              v-tooltip.top="t('common.edit')"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Form dialog -->
    <VitalSignFormDialog
      v-model:visible="dialogVisible"
      :admissionId="admissionId"
      :vitalSignToEdit="vitalSignToEdit"
      @saved="onVitalSignSaved"
    />
  </div>
</template>

<style scoped>
.vital-sign-table {
  padding: 1rem 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.table-content {
  min-height: 200px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state p {
  margin-bottom: 1rem;
}

.bp-value {
  font-weight: 600;
  margin-right: 0.25rem;
}

.unit {
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
}
</style>
