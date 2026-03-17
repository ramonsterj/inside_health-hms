<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Panel from 'primevue/panel'
import KardexMedicationList from './KardexMedicationList.vue'
import KardexCareInstructions from './KardexCareInstructions.vue'
import KardexVitalsSummary from './KardexVitalsSummary.vue'
import VitalSignFormDialog from '@/components/nursing/VitalSignFormDialog.vue'
import NursingNoteFormDialog from '@/components/nursing/NursingNoteFormDialog.vue'
import { getContrastColor } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { useVitalsFreshness } from '@/composables/useVitalsFreshness'
import type { KardexAdmissionSummary } from '@/types'

const props = defineProps<{
  summary: KardexAdmissionSummary
}>()

const emit = defineEmits<{
  actionCompleted: [admissionId: number]
}>()

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const collapsed = ref(true)
const vitalsDialogVisible = ref(false)
const noteDialogVisible = ref(false)

const canRecordVitals = computed(() => authStore.hasPermission('vital-sign:create'))
const canAddNote = computed(() => authStore.hasPermission('nursing-note:create'))

const { freshnessClass: vitalsFreshnessClass, freshnessLabel: vitalsFreshnessLabel } =
  useVitalsFreshness(() => props.summary.latestVitalSigns?.recordedAt ?? null)

function togglePanel() {
  collapsed.value = !collapsed.value
}

function onActionCompleted() {
  emit('actionCompleted', props.summary.admissionId)
}

function viewDetail() {
  router.push({ name: 'admission-detail', params: { id: props.summary.admissionId } })
}
</script>

<template>
  <Panel :collapsed="collapsed" :toggleable="true" @update:collapsed="collapsed = $event">
    <template #header>
      <div class="card-header" @click="togglePanel">
        <div class="patient-info">
          <span class="patient-name">{{ summary.patientName }}</span>
          <span class="room-number">{{ summary.roomNumber || t('kardex.noRoom') }}</span>
          <span
            v-if="summary.triageCode"
            class="triage-badge"
            :style="{
              backgroundColor: summary.triageColorCode || '#ccc',
              color: getContrastColor(summary.triageColorCode || '#ccc')
            }"
          >
            {{ summary.triageCode }}
          </span>
        </div>
        <div class="metric">
          <span class="metric-label">{{ t('kardex.labels.stay') }}</span>
          <span class="metric-value">
            {{ t('kardex.daysAdmitted', { count: summary.daysAdmitted }, summary.daysAdmitted) }}
          </span>
        </div>
        <div class="metric">
          <span class="metric-label">{{ t('kardex.treatingPhysician') }}</span>
          <span class="metric-value">{{ summary.treatingPhysicianName }}</span>
        </div>
        <div class="metric metric-numeric">
          <span class="metric-label">{{ t('kardex.labels.medications') }}</span>
          <span class="metric-value metric-value-highlight">
            {{ summary.activeMedicationCount }}
          </span>
        </div>
        <div class="metric metric-numeric">
          <span class="metric-label">{{ t('kardex.labels.vitals') }}</span>
          <span :class="['metric-value', vitalsFreshnessClass]">
            {{ vitalsFreshnessLabel }}
          </span>
        </div>
        <div class="metric metric-numeric">
          <span class="metric-label">{{ t('kardex.labels.instructions') }}</span>
          <span class="metric-value metric-value-highlight">
            {{ summary.activeCareInstructionCount }}
          </span>
        </div>
      </div>
    </template>

    <div class="card-expanded">
      <div class="expanded-sections">
        <KardexMedicationList
          :medications="summary.medications"
          :admissionId="summary.admissionId"
          @action-completed="onActionCompleted"
        />

        <KardexCareInstructions :careInstructions="summary.careInstructions" />

        <KardexVitalsSummary :vitalSigns="summary.latestVitalSigns" />
      </div>

      <div class="quick-actions">
        <Button
          v-if="canRecordVitals"
          :label="t('kardex.vitals.recordVitals')"
          icon="pi pi-heart"
          severity="info"
          outlined
          size="small"
          @click="vitalsDialogVisible = true"
        />
        <Button
          v-if="canAddNote"
          :label="t('kardex.notes.addNote')"
          icon="pi pi-file-edit"
          severity="secondary"
          outlined
          size="small"
          @click="noteDialogVisible = true"
        />
        <Button
          :label="t('kardex.actions.viewDetail')"
          icon="pi pi-external-link"
          severity="secondary"
          text
          size="small"
          @click="viewDetail"
        />
      </div>
    </div>

    <VitalSignFormDialog
      v-model:visible="vitalsDialogVisible"
      :admissionId="summary.admissionId"
      @saved="onActionCompleted"
    />

    <NursingNoteFormDialog
      v-model:visible="noteDialogVisible"
      :admissionId="summary.admissionId"
      @saved="onActionCompleted"
    />
  </Panel>
</template>

<style scoped>
.card-header {
  display: grid;
  grid-template-columns: minmax(12rem, 1fr) 5rem 10rem auto auto auto;
  align-items: center;
  width: 100%;
  cursor: pointer;
  gap: 0.75rem;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  overflow: hidden;
}

.patient-name {
  font-weight: 700;
  font-size: 1rem;
}

.room-number {
  font-size: 0.9rem;
  color: var(--p-text-muted-color);
  font-weight: 500;
}

.triage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.15rem 0.5rem;
  border-radius: var(--p-border-radius);
  font-weight: 600;
  font-size: 0.8rem;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  overflow: hidden;
}

.metric-numeric {
  align-items: flex-end;
}

.metric-label {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--p-text-muted-color);
  white-space: nowrap;
}

.metric-value {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--p-text-color);
}

.metric-value-highlight {
  font-weight: 700;
  font-size: 0.95rem;
  text-align: right;
}

.freshness-ok {
  color: var(--p-green-500);
  font-weight: 600;
}

.freshness-warning {
  color: var(--p-yellow-500);
  font-weight: 600;
}

.freshness-critical {
  color: var(--p-red-500);
  font-weight: 600;
}

.patient-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expanded-sections {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.quick-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--p-content-border-color);
  flex-wrap: wrap;
}
</style>
