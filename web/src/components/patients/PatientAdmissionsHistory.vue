<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Paginator from 'primevue/paginator'
import ProgressSpinner from 'primevue/progressspinner'
import AdmissionTypeBadge from '@/components/admissions/AdmissionTypeBadge.vue'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useAdmissionStore } from '@/stores/admission'
import { useAuthStore } from '@/stores/auth'
import type { AdmissionListItem } from '@/types/admission'
import { formatDateTime, getFullName } from '@/utils/format'

const props = defineProps<{
  patientId: number
}>()

const { t } = useI18n()
const router = useRouter()
const { showError } = useErrorHandler()
const admissionStore = useAdmissionStore()
const authStore = useAuthStore()

const rowsPerPageOptions = [10, 20, 50]
const first = ref(0)
const rows = ref(20)
const loading = ref(false)

const admissions = computed(() => admissionStore.patientAdmissions)
const total = computed(() => admissionStore.totalPatientAdmissions)

// The admission detail page requires `admission:read`, which not every `patient:read`
// role holds. Without it the rows render but are non-navigable (no link into a 403).
const canViewAdmission = computed(() => authStore.hasPermission('admission:read'))

async function loadAdmissions() {
  loading.value = true
  try {
    await admissionStore.fetchPatientAdmissions(
      props.patientId,
      first.value / rows.value,
      rows.value
    )
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function onPageChange(event: { first: number; rows: number }) {
  first.value = event.first
  rows.value = event.rows
  loadAdmissions()
}

function viewAdmission(admission: AdmissionListItem) {
  if (!canViewAdmission.value) return
  router.push({ name: 'admission-detail', params: { id: admission.id } })
}

watch(
  () => props.patientId,
  () => {
    first.value = 0
    loadAdmissions()
  }
)

onMounted(loadAdmissions)
</script>

<template>
  <Card class="admissions-history-card">
    <template #title>{{ t('patient.admissionsHistory.title') }}</template>
    <template #content>
      <div v-if="loading" class="history-loading">
        <ProgressSpinner strokeWidth="3" />
      </div>

      <div v-else-if="admissions.length === 0" class="history-empty">
        <i class="pi pi-inbox empty-icon"></i>
        <p>{{ t('patient.admissionsHistory.empty') }}</p>
      </div>

      <template v-else>
        <DataTable
          :value="admissions"
          dataKey="id"
          stripedRows
          :rowHover="canViewAdmission"
          :class="{ 'rows-clickable': canViewAdmission }"
          @row-click="viewAdmission($event.data as AdmissionListItem)"
        >
          <Column :header="t('patient.admissionsHistory.admissionDate')">
            <template #body="{ data }">
              {{ formatDateTime(data.admissionDate) }}
            </template>
          </Column>

          <Column :header="t('patient.admissionsHistory.dischargeDate')">
            <template #body="{ data }">
              <span v-if="data.dischargeDate">{{ formatDateTime(data.dischargeDate) }}</span>
              <Tag v-else :value="t('patient.admissionsHistory.active')" severity="success" />
            </template>
          </Column>

          <Column :header="t('patient.admissionsHistory.type')">
            <template #body="{ data }">
              <AdmissionTypeBadge :type="data.type" />
            </template>
          </Column>

          <Column :header="t('patient.admissionsHistory.room')">
            <template #body="{ data }">
              {{ data.room?.number || '-' }}
            </template>
          </Column>

          <Column :header="t('patient.admissionsHistory.treatingPhysician')">
            <template #body="{ data }">
              {{ getFullName(data.treatingPhysician.firstName, data.treatingPhysician.lastName) }}
            </template>
          </Column>
        </DataTable>

        <!-- Show whenever more than the smallest page size exists, so the rows-per-page
             selector stays reachable even after the user picks a larger page size. -->
        <Paginator
          v-if="total > rowsPerPageOptions[0]"
          :first="first"
          :rows="rows"
          :totalRecords="total"
          :rowsPerPageOptions="rowsPerPageOptions"
          @page="onPageChange"
        />
      </template>
    </template>
  </Card>
</template>

<style scoped>
.history-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 160px;
}

.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: 0.75rem;
  opacity: 0.5;
}

.rows-clickable :deep(tbody tr) {
  cursor: pointer;
}
</style>
