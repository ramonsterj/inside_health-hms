<script setup lang="ts">
import { ref, computed, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import { useAuthStore } from '@/stores/auth'
import { AdmissionType, AdmissionStatus, type AdmissionDetail } from '@/types/admission'
import {
  useMedicalRecordSummary,
  type VisibleSections,
  type SummarySectionKey
} from '@/composables/useMedicalRecordSummary'
import ClinicalHistoryView from './ClinicalHistoryView.vue'
import ProgressNoteList from './ProgressNoteList.vue'
import MedicalOrderList from './MedicalOrderList.vue'
import MedicalRecordSectionCard from './MedicalRecordSectionCard.vue'
import PsychotherapyActivityList from '@/components/psychotherapy/PsychotherapyActivityList.vue'
import NursingNoteList from '@/components/nursing/NursingNoteList.vue'
import VitalSignTable from '@/components/nursing/VitalSignTable.vue'
import VitalSignCharts from '@/components/nursing/VitalSignCharts.vue'
import DocumentList from '@/components/documents/DocumentList.vue'
import ConsultingPhysiciansPanel from '@/components/admissions/ConsultingPhysiciansPanel.vue'

interface SectionDefinition {
  key: SummarySectionKey
  label: string
  icon: string
  priority: number // lower = higher priority
}

const props = defineProps<{
  admissionId: number
  admission?: AdmissionDetail | null
  admissionType?: AdmissionType
  admissionStatus?: AdmissionStatus
  consultingPhysicians?: Array<{
    id: number
    physician: {
      id: number
      salutation: string | null
      firstName: string | null
      lastName: string | null
    }
    reason: string | null
    requestedDate: string | null
    createdAt: string | null
    createdBy: { username: string } | null
  }>
}>()

const emit = defineEmits<{
  uploadDocument: []
  addConsultingPhysician: []
  removeConsultingPhysician: [id: number]
}>()

const { t } = useI18n()
const authStore = useAuthStore()

// No default-open section — the hub opens on the card grid (level 1).
const activeSection = ref<SummarySectionKey | null>(null)

// Vital signs view toggle (table vs charts)
const vitalSignsView = ref<'table' | 'charts'>('table')
const viewOptions = computed(() => [
  { label: t('nursing.vitalSigns.table'), value: 'table', icon: 'pi pi-table' },
  { label: t('nursing.vitalSigns.charts'), value: 'charts', icon: 'pi pi-chart-line' }
])

// Permission checks
const canViewClinicalHistory = computed(
  () =>
    authStore.hasPermission('clinical-history:read') ||
    authStore.hasPermission('clinical-history:create')
)
const canViewProgressNotes = computed(
  () =>
    authStore.hasPermission('progress-note:read') || authStore.hasPermission('progress-note:create')
)
const canViewMedicalOrders = computed(
  () =>
    authStore.hasPermission('medical-order:read') || authStore.hasPermission('medical-order:create')
)
const canViewPsychotherapyActivities = computed(
  () =>
    props.admissionType === AdmissionType.HOSPITALIZATION &&
    (authStore.hasPermission('psychotherapy-activity:read') ||
      authStore.hasPermission('psychotherapy-activity:create'))
)
const canViewNursingNotes = computed(
  () =>
    authStore.hasPermission('nursing-note:read') || authStore.hasPermission('nursing-note:create')
)
const canViewVitalSigns = computed(
  () => authStore.hasPermission('vital-sign:read') || authStore.hasPermission('vital-sign:create')
)
const canViewDocuments = computed(
  () =>
    authStore.hasPermission('admission:read') ||
    authStore.hasPermission('admission:upload-documents')
)
const canViewConsulting = computed(
  () => authStore.hasPermission('admission:read') || authStore.hasPermission('admission:update')
)
const canUpdateAdmission = computed(
  () =>
    authStore.hasPermission('admission:update') && props.admissionStatus === AdmissionStatus.ACTIVE
)

// Discharge protection: when the admission is discharged the whole record is read-only.
// Each section gates its own write affordances; this banner explains why they are gone.
const isDischarged = computed(() => props.admissionStatus === AdmissionStatus.DISCHARGED)

// Priority drives the order of the section cards in the grid.
// For nursing roles: nursing notes / vital signs lead.
// For medical roles: clinical history / progress notes / medical orders lead.
const isNursingRole = computed(() => {
  return (
    (authStore.hasPermission('nursing-note:create') ||
      authStore.hasPermission('vital-sign:create')) &&
    !authStore.hasPermission('clinical-history:create')
  )
})

const sections = computed<SectionDefinition[]>(() => {
  const list: SectionDefinition[] = []

  if (canViewClinicalHistory.value) {
    list.push({
      key: 'clinicalHistory',
      label: t('medicalRecord.tabs.clinicalHistory'),
      icon: 'pi pi-book',
      priority: isNursingRole.value ? 4 : 1
    })
  }
  if (canViewProgressNotes.value) {
    list.push({
      key: 'progressNotes',
      label: t('medicalRecord.tabs.progressNotes'),
      icon: 'pi pi-list',
      priority: isNursingRole.value ? 5 : 2
    })
  }
  if (canViewMedicalOrders.value) {
    list.push({
      key: 'medicalOrders',
      label: t('medicalRecord.tabs.medicalOrders'),
      icon: 'pi pi-clipboard',
      priority: isNursingRole.value ? 6 : 3
    })
  }
  if (canViewNursingNotes.value) {
    list.push({
      key: 'nursingNotes',
      label: t('nursing.tabs.notes'),
      icon: 'pi pi-file-edit',
      priority: isNursingRole.value ? 1 : 4
    })
  }
  if (canViewVitalSigns.value) {
    list.push({
      key: 'vitalSigns',
      label: t('nursing.tabs.vitalSigns'),
      icon: 'pi pi-heart',
      priority: isNursingRole.value ? 2 : 5
    })
  }
  if (canViewDocuments.value) {
    list.push({
      key: 'documents',
      label: t('document.title'),
      icon: 'pi pi-folder',
      priority: 7
    })
  }
  if (canViewConsulting.value) {
    list.push({
      key: 'consulting',
      label: t('admission.consultingPhysicians.title'),
      icon: 'pi pi-users',
      priority: 8
    })
  }
  if (canViewPsychotherapyActivities.value) {
    list.push({
      key: 'psychotherapyActivities',
      label: t('medicalRecord.tabs.psychotherapyActivities'),
      icon: 'pi pi-heart-fill',
      priority: isNursingRole.value ? 3 : 6
    })
  }

  return list.sort((a, b) => a.priority - b.priority)
})

const hasAnyPermission = computed(() => sections.value.length > 0)

const activeSectionDef = computed(() => sections.value.find(s => s.key === activeSection.value))

// Live metrics for the hub cards — prefetched for visible sections only.
const visibleSections = computed<VisibleSections>(() => ({
  clinicalHistory: canViewClinicalHistory.value,
  progressNotes: canViewProgressNotes.value,
  medicalOrders: canViewMedicalOrders.value,
  nursingNotes: canViewNursingNotes.value,
  vitalSigns: canViewVitalSigns.value,
  documents: canViewDocuments.value,
  consulting: canViewConsulting.value,
  psychotherapyActivities: canViewPsychotherapyActivities.value
}))

const { summary } = useMedicalRecordSummary(
  toRef(props, 'admissionId'),
  visibleSections,
  toRef(props, 'admission')
)

function openSection(key: SummarySectionKey) {
  activeSection.value = key
}
function backToHub() {
  activeSection.value = null
}
</script>

<template>
  <Card v-if="hasAnyPermission" class="medical-record-hub">
    <template #title>
      <h2 class="card-title">{{ t('medicalRecord.title') }}</h2>
    </template>
    <template #content>
      <div v-if="isDischarged" class="discharged-banner">
        <i class="pi pi-lock"></i>
        <span>{{ t('medicalRecord.dischargedReadOnly') }}</span>
      </div>

      <!-- ============ LEVEL 1: SECTION HUB ============ -->
      <div v-if="!activeSection" class="section-grid">
        <MedicalRecordSectionCard
          v-for="section in sections"
          :key="section.key"
          :section-key="section.key"
          :title="section.label"
          :icon="section.icon"
          :metric="summary[section.key]?.metric"
          :metric-severity="summary[section.key]?.severity"
          :updated="summary[section.key]?.updated"
          @open="openSection(section.key)"
        />
      </div>

      <!-- ============ LEVEL 2: DRILL-IN ============ -->
      <div v-else class="section-detail">
        <div class="drill-header">
          <Button
            data-testid="section-back"
            icon="pi pi-arrow-left"
            :label="t('medicalRecord.backToRecord')"
            severity="secondary"
            text
            @click="backToHub"
          />
          <h2 class="drill-title">
            <i :class="activeSectionDef?.icon" /> {{ activeSectionDef?.label }}
          </h2>
        </div>

        <!-- Clinical History -->
        <div
          v-if="activeSection === 'clinicalHistory' && canViewClinicalHistory"
          class="section-panel"
        >
          <ClinicalHistoryView
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
          />
        </div>

        <!-- Progress Notes -->
        <div v-if="activeSection === 'progressNotes' && canViewProgressNotes" class="section-panel">
          <ProgressNoteList
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
          />
        </div>

        <!-- Medical Orders -->
        <div v-if="activeSection === 'medicalOrders' && canViewMedicalOrders" class="section-panel">
          <MedicalOrderList
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
          />
        </div>

        <!-- Nursing Notes -->
        <div v-if="activeSection === 'nursingNotes' && canViewNursingNotes" class="section-panel">
          <NursingNoteList
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
          />
        </div>

        <!-- Vital Signs -->
        <div v-if="activeSection === 'vitalSigns' && canViewVitalSigns" class="section-panel">
          <VitalSignTable
            v-if="vitalSignsView === 'table'"
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
          >
            <template #header-right>
              <SelectButton
                v-model="vitalSignsView"
                :options="viewOptions"
                optionLabel="label"
                optionValue="value"
                :allowEmpty="false"
              >
                <template #option="{ option }">
                  <i :class="option.icon" style="margin-right: 0.5rem"></i>
                  {{ option.label }}
                </template>
              </SelectButton>
            </template>
          </VitalSignTable>
          <VitalSignCharts v-else :admissionId="admissionId">
            <template #header-right>
              <SelectButton
                v-model="vitalSignsView"
                :options="viewOptions"
                optionLabel="label"
                optionValue="value"
                :allowEmpty="false"
              >
                <template #option="{ option }">
                  <i :class="option.icon" style="margin-right: 0.5rem"></i>
                  {{ option.label }}
                </template>
              </SelectButton>
            </template>
          </VitalSignCharts>
        </div>

        <!-- Documents -->
        <div v-if="activeSection === 'documents' && canViewDocuments" class="section-panel">
          <DocumentList
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
            @upload="emit('uploadDocument')"
          />
        </div>

        <!-- Consulting Physicians -->
        <div v-if="activeSection === 'consulting' && canViewConsulting" class="section-panel">
          <ConsultingPhysiciansPanel
            :consultingPhysicians="consultingPhysicians || []"
            :canUpdate="canUpdateAdmission"
            @add="emit('addConsultingPhysician')"
            @remove="emit('removeConsultingPhysician', $event)"
          />
        </div>

        <!-- Psychotherapy Activities -->
        <div
          v-if="activeSection === 'psychotherapyActivities' && canViewPsychotherapyActivities"
          class="section-panel"
        >
          <PsychotherapyActivityList
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
          />
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.medical-record-hub {
  margin-top: 1rem;
}

.card-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.discharged-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  background: var(--p-surface-100);
  border: 1px solid var(--p-surface-border);
  border-radius: var(--p-border-radius);
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

/* LEVEL 1 — section hub grid (same pattern as the bed-occupancy room grid) */
.section-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

/* LEVEL 2 — drill-in */
.drill-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}
.drill-title {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  font-size: 1.25rem;
}

.section-panel {
  min-height: 200px;
}
</style>
