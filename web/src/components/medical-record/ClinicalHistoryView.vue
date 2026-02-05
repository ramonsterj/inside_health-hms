<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import Button from 'primevue/button'
import { useClinicalHistoryStore } from '@/stores/clinicalHistory'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { ClinicalHistoryFieldName } from '@/types/medicalRecord'
import { sanitizeHtml } from '@/utils/sanitize'
import AuditInfo from '@/components/common/AuditInfo.vue'
import ClinicalHistoryForm from './ClinicalHistoryForm.vue'

const props = defineProps<{
  admissionId: number
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()
const clinicalHistoryStore = useClinicalHistoryStore()
const authStore = useAuthStore()

const showForm = ref(false)
const loaded = ref(false)

const clinicalHistory = computed(() => clinicalHistoryStore.getClinicalHistory(props.admissionId))
const loading = computed(() => clinicalHistoryStore.loading)
const hasHistory = computed(() => clinicalHistory.value !== undefined)

const canCreate = computed(() => authStore.hasPermission('clinical-history:create'))
const canUpdate = computed(() => authStore.hasPermission('clinical-history:update'))

// Group fields into sections for accordion display
const fieldSections = computed(() => {
  const sections = [
    {
      key: 'presentation',
      fields: ['reasonForAdmission', 'historyOfPresentIllness'] as ClinicalHistoryFieldName[]
    },
    {
      key: 'history',
      fields: [
        'psychiatricHistory',
        'medicalHistory',
        'familyHistory',
        'personalHistory',
        'substanceUseHistory'
      ] as ClinicalHistoryFieldName[]
    },
    {
      key: 'social',
      fields: [
        'legalHistory',
        'socialHistory',
        'developmentalHistory',
        'educationalOccupationalHistory',
        'sexualHistory',
        'religiousSpiritualHistory'
      ] as ClinicalHistoryFieldName[]
    },
    {
      key: 'exam',
      fields: ['mentalStatusExam', 'physicalExam'] as ClinicalHistoryFieldName[]
    },
    {
      key: 'assessment',
      fields: [
        'diagnosticImpression',
        'treatmentPlan',
        'riskAssessment',
        'prognosis'
      ] as ClinicalHistoryFieldName[]
    },
    {
      key: 'notes',
      fields: ['informedConsentNotes', 'additionalNotes'] as ClinicalHistoryFieldName[]
    }
  ]
  return sections
})

onMounted(async () => {
  await loadClinicalHistory()
})

async function loadClinicalHistory() {
  try {
    await clinicalHistoryStore.fetchClinicalHistory(props.admissionId)
  } catch (error) {
    showError(error)
  } finally {
    loaded.value = true
  }
}

function getFieldValue(field: ClinicalHistoryFieldName): string {
  if (!clinicalHistory.value) return ''
  const value = (clinicalHistory.value[field] as string) || ''
  return sanitizeHtml(value)
}

function hasFieldContent(field: ClinicalHistoryFieldName): boolean {
  const value = getFieldValue(field)
  return value !== null && value !== undefined && value.trim() !== ''
}

function sectionHasContent(fields: ClinicalHistoryFieldName[]): boolean {
  return fields.some(field => hasFieldContent(field))
}

function handleFormSaved() {
  showForm.value = false
  loadClinicalHistory()
}

function handleFormCancelled() {
  showForm.value = false
}
</script>

<template>
  <div class="clinical-history-view">
    <!-- Loading State -->
    <div v-if="loading && !loaded" class="loading-container">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
    </div>

    <!-- Form Mode -->
    <ClinicalHistoryForm
      v-else-if="showForm"
      :admissionId="admissionId"
      :clinicalHistory="clinicalHistory"
      @saved="handleFormSaved"
      @cancelled="handleFormCancelled"
    />

    <!-- View Mode - No History -->
    <div v-else-if="loaded && !hasHistory" class="empty-state">
      <i class="pi pi-file-edit empty-icon"></i>
      <p>{{ t('medicalRecord.clinicalHistory.empty') }}</p>
      <Button
        v-if="canCreate"
        icon="pi pi-plus"
        :label="t('medicalRecord.clinicalHistory.create')"
        @click="showForm = true"
      />
    </div>

    <!-- View Mode - Has History -->
    <div v-else-if="clinicalHistory">
      <div class="view-header">
        <h3>{{ t('medicalRecord.clinicalHistory.title') }}</h3>
        <Button
          v-if="canUpdate"
          icon="pi pi-pencil"
          :label="t('common.edit')"
          severity="secondary"
          @click="showForm = true"
        />
      </div>

      <Accordion multiple class="clinical-history-accordion">
        <AccordionPanel
          v-for="section in fieldSections"
          :key="section.key"
          :value="section.key"
          :disabled="!sectionHasContent(section.fields)"
        >
          <AccordionHeader>
            <span class="section-title">
              {{ t(`medicalRecord.clinicalHistory.sections.${section.key}`) }}
            </span>
            <span v-if="!sectionHasContent(section.fields)" class="empty-badge">
              {{ t('medicalRecord.clinicalHistory.noData') }}
            </span>
          </AccordionHeader>
          <AccordionContent>
            <div class="fields-container">
              <div
                v-for="field in section.fields"
                :key="field"
                class="field-item"
                :class="{ 'no-content': !hasFieldContent(field) }"
              >
                <label class="field-label">
                  {{ t(`medicalRecord.clinicalHistory.fields.${field}`) }}
                </label>
                <div class="field-value" v-html="getFieldValue(field) || '-'"></div>
              </div>
            </div>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>

      <div class="audit-section">
        <h4>{{ t('common.auditInfo') }}</h4>
        <AuditInfo
          :createdAt="clinicalHistory.createdAt"
          :createdBy="clinicalHistory.createdBy"
          :updatedAt="clinicalHistory.updatedAt"
          :updatedBy="clinicalHistory.updatedBy"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.clinical-history-view {
  padding: 1rem;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 3rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state p {
  margin-bottom: 1.5rem;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.view-header h3 {
  margin: 0;
}

.clinical-history-accordion {
  margin-bottom: 1.5rem;
}

.section-title {
  font-weight: 600;
}

.empty-badge {
  margin-left: 0.5rem;
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  font-style: italic;
}

.fields-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-item.no-content {
  opacity: 0.6;
}

.field-label {
  font-weight: 600;
  color: var(--p-text-color);
  font-size: 0.875rem;
}

.field-value {
  padding: 0.75rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
  white-space: pre-wrap;
  word-wrap: break-word;
}

.audit-section {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--p-surface-border);
}

.audit-section h4 {
  margin: 0 0 1rem 0;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-text-muted-color);
}
</style>
