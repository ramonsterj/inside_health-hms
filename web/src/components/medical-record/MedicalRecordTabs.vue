<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import SelectButton from 'primevue/selectbutton'
import { useAuthStore } from '@/stores/auth'
import { AdmissionType, AdmissionStatus } from '@/types/admission'
import ClinicalHistoryView from './ClinicalHistoryView.vue'
import ProgressNoteList from './ProgressNoteList.vue'
import MedicalOrderList from './MedicalOrderList.vue'
import PsychotherapyActivityList from '@/components/psychotherapy/PsychotherapyActivityList.vue'
import NursingNoteList from '@/components/nursing/NursingNoteList.vue'
import VitalSignTable from '@/components/nursing/VitalSignTable.vue'
import VitalSignCharts from '@/components/nursing/VitalSignCharts.vue'
import DocumentList from '@/components/documents/DocumentList.vue'
import ConsultingPhysiciansPanel from '@/components/admissions/ConsultingPhysiciansPanel.vue'

interface TabDefinition {
  key: string
  label: string
  icon: string
  permission: boolean
  priority: number // lower = higher priority
}

const props = defineProps<{
  admissionId: number
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
  treatingPhysicianId?: number
}>()

const emit = defineEmits<{
  uploadDocument: []
  addConsultingPhysician: []
  removeConsultingPhysician: [id: number]
}>()

const { t } = useI18n()
const authStore = useAuthStore()

const activeTab = ref('')

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

// Define all tabs with role-based priority
// Priority: 1 = highest priority for role
// For nursing roles: nursing notes, vital signs have priority
// For medical roles: clinical history, progress notes, medical orders have priority
const isNursingRole = computed(() => {
  return (
    (authStore.hasPermission('nursing-note:create') ||
      authStore.hasPermission('vital-sign:create')) &&
    !authStore.hasPermission('clinical-history:create')
  )
})

const allTabs = computed<TabDefinition[]>(() => {
  const tabs: TabDefinition[] = []

  if (canViewClinicalHistory.value) {
    tabs.push({
      key: 'clinicalHistory',
      label: t('medicalRecord.tabs.clinicalHistory'),
      icon: 'pi pi-book',
      permission: true,
      priority: isNursingRole.value ? 4 : 1
    })
  }

  if (canViewProgressNotes.value) {
    tabs.push({
      key: 'progressNotes',
      label: t('medicalRecord.tabs.progressNotes'),
      icon: 'pi pi-list',
      permission: true,
      priority: isNursingRole.value ? 5 : 2
    })
  }

  if (canViewMedicalOrders.value) {
    tabs.push({
      key: 'medicalOrders',
      label: t('medicalRecord.tabs.medicalOrders'),
      icon: 'pi pi-clipboard',
      permission: true,
      priority: isNursingRole.value ? 6 : 3
    })
  }

  if (canViewNursingNotes.value) {
    tabs.push({
      key: 'nursingNotes',
      label: t('nursing.tabs.notes'),
      icon: 'pi pi-file-edit',
      permission: true,
      priority: isNursingRole.value ? 1 : 4
    })
  }

  if (canViewVitalSigns.value) {
    tabs.push({
      key: 'vitalSigns',
      label: t('nursing.tabs.vitalSigns'),
      icon: 'pi pi-heart',
      permission: true,
      priority: isNursingRole.value ? 2 : 5
    })
  }

  if (canViewDocuments.value) {
    tabs.push({
      key: 'documents',
      label: t('document.title'),
      icon: 'pi pi-folder',
      permission: true,
      priority: 7
    })
  }

  if (canViewConsulting.value) {
    tabs.push({
      key: 'consulting',
      label: t('admission.consultingPhysicians.title'),
      icon: 'pi pi-users',
      permission: true,
      priority: 8
    })
  }

  if (canViewPsychotherapyActivities.value) {
    tabs.push({
      key: 'psychotherapyActivities',
      label: t('medicalRecord.tabs.psychotherapyActivities'),
      icon: 'pi pi-heart-fill',
      permission: true,
      priority: isNursingRole.value ? 3 : 6
    })
  }

  return tabs.sort((a, b) => a.priority - b.priority)
})

const hasAnyPermission = computed(() => allTabs.value.length > 0)

// Get the label of the active tab for the dynamic title
const activeTabLabel = computed(() => {
  const tab = allTabs.value.find(t => t.key === activeTab.value)
  return tab?.label || ''
})

// Set initial active tab
watch(
  allTabs,
  tabs => {
    if (tabs.length > 0 && !activeTab.value && tabs[0]) {
      activeTab.value = tabs[0].key
    }
  },
  { immediate: true }
)
</script>

<template>
  <Card v-if="hasAnyPermission" class="medical-record-tabs">
    <template #title>
      <h2 class="card-title">
        {{ t('medicalRecord.title')
        }}<span v-if="activeTabLabel" class="active-tab-label">: {{ activeTabLabel }}</span>
      </h2>
    </template>
    <template #content>
      <Tabs v-model:value="activeTab" class="tab-navigation">
        <TabList>
          <Tab v-for="tab in allTabs" :key="tab.key" :value="tab.key">
            <i :class="tab.icon" class="tab-icon"></i>
            <span>{{ tab.label }}</span>
          </Tab>
        </TabList>
      </Tabs>

      <div class="tab-content">
        <!-- Clinical History -->
        <div v-if="activeTab === 'clinicalHistory' && canViewClinicalHistory" class="tab-panel">
          <ClinicalHistoryView :admissionId="admissionId" />
        </div>

        <!-- Progress Notes -->
        <div v-if="activeTab === 'progressNotes' && canViewProgressNotes" class="tab-panel">
          <ProgressNoteList :admissionId="admissionId" />
        </div>

        <!-- Medical Orders -->
        <div v-if="activeTab === 'medicalOrders' && canViewMedicalOrders" class="tab-panel">
          <MedicalOrderList :admissionId="admissionId" />
        </div>

        <!-- Nursing Notes -->
        <div v-if="activeTab === 'nursingNotes' && canViewNursingNotes" class="tab-panel">
          <NursingNoteList
            :admissionId="admissionId"
            :admissionStatus="admissionStatus || AdmissionStatus.DISCHARGED"
          />
        </div>

        <!-- Vital Signs -->
        <div v-if="activeTab === 'vitalSigns' && canViewVitalSigns" class="tab-panel">
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
        <div v-if="activeTab === 'documents' && canViewDocuments" class="tab-panel">
          <DocumentList :admissionId="admissionId" @upload="emit('uploadDocument')" />
        </div>

        <!-- Consulting Physicians -->
        <div v-if="activeTab === 'consulting' && canViewConsulting" class="tab-panel">
          <ConsultingPhysiciansPanel
            :consultingPhysicians="consultingPhysicians || []"
            :canUpdate="canUpdateAdmission"
            @add="emit('addConsultingPhysician')"
            @remove="emit('removeConsultingPhysician', $event)"
          />
        </div>

        <!-- Psychotherapy Activities -->
        <div
          v-if="activeTab === 'psychotherapyActivities' && canViewPsychotherapyActivities"
          class="tab-panel"
        >
          <PsychotherapyActivityList :admissionId="admissionId" />
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.medical-record-tabs {
  margin-top: 1rem;
}

.card-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.active-tab-label {
  color: var(--p-primary-color);
}

.tab-navigation {
  margin-bottom: 1rem;
}

.tab-icon {
  margin-right: 0.5rem;
}

.tab-content {
  min-height: 200px;
}

.tab-panel {
  padding: 0;
}
</style>
