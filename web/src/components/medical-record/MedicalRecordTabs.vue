<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import { useAuthStore } from '@/stores/auth'
import ClinicalHistoryView from './ClinicalHistoryView.vue'
import ProgressNoteList from './ProgressNoteList.vue'
import MedicalOrderList from './MedicalOrderList.vue'

defineProps<{
  admissionId: number
}>()

const { t } = useI18n()
const authStore = useAuthStore()

const activeTab = ref('clinicalHistory')

// Permission checks
const canViewClinicalHistory = computed(
  () =>
    authStore.hasPermission('clinical-history:read') ||
    authStore.hasPermission('clinical-history:create')
)
const canViewProgressNotes = computed(
  () =>
    authStore.hasPermission('progress-note:read') ||
    authStore.hasPermission('progress-note:create')
)
const canViewMedicalOrders = computed(
  () =>
    authStore.hasPermission('medical-order:read') ||
    authStore.hasPermission('medical-order:create')
)

const hasAnyPermission = computed(
  () => canViewClinicalHistory.value || canViewProgressNotes.value || canViewMedicalOrders.value
)
</script>

<template>
  <Card v-if="hasAnyPermission" class="medical-record-tabs">
    <template #title>
      <div class="card-title">
        <i class="pi pi-file-edit"></i>
        {{ t('medicalRecord.title') }}
      </div>
    </template>
    <template #content>
      <Tabs v-model:value="activeTab">
        <TabList>
          <Tab v-if="canViewClinicalHistory" value="clinicalHistory">
            <i class="pi pi-book tab-icon"></i>
            {{ t('medicalRecord.tabs.clinicalHistory') }}
          </Tab>
          <Tab v-if="canViewProgressNotes" value="progressNotes">
            <i class="pi pi-list tab-icon"></i>
            {{ t('medicalRecord.tabs.progressNotes') }}
          </Tab>
          <Tab v-if="canViewMedicalOrders" value="medicalOrders">
            <i class="pi pi-clipboard tab-icon"></i>
            {{ t('medicalRecord.tabs.medicalOrders') }}
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel v-if="canViewClinicalHistory" value="clinicalHistory">
            <ClinicalHistoryView :admissionId="admissionId" />
          </TabPanel>
          <TabPanel v-if="canViewProgressNotes" value="progressNotes">
            <ProgressNoteList :admissionId="admissionId" />
          </TabPanel>
          <TabPanel v-if="canViewMedicalOrders" value="medicalOrders">
            <MedicalOrderList :admissionId="admissionId" />
          </TabPanel>
        </TabPanels>
      </Tabs>
    </template>
  </Card>
</template>

<style scoped>
.medical-record-tabs {
  margin-top: 1rem;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-title i {
  color: var(--p-primary-color);
}

.tab-icon {
  margin-right: 0.5rem;
}

:deep(.p-tabpanels) {
  padding: 0;
}

:deep(.p-tabpanel) {
  padding: 0;
}
</style>
