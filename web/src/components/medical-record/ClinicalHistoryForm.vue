<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useForm } from 'vee-validate'
import type { Ref } from 'vue'
import Button from 'primevue/button'
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import { toTypedSchema } from '@/validation/zodI18n'
import { clinicalHistorySchema, type ClinicalHistoryFormData } from '@/validation/medicalRecord'
import { useClinicalHistoryStore } from '@/stores/clinicalHistory'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { ClinicalHistoryResponse, ClinicalHistoryFieldName } from '@/types/medicalRecord'
import RichTextEditor from '@/components/common/RichTextEditor.vue'

const props = defineProps<{
  admissionId: number
  clinicalHistory?: ClinicalHistoryResponse
}>()

const emit = defineEmits<{
  saved: []
  cancelled: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const clinicalHistoryStore = useClinicalHistoryStore()

const loading = ref(false)
const isEditMode = computed(() => !!props.clinicalHistory)

// Field sections for organized display
const fieldSections = [
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

// VeeValidate form setup
const { defineField, handleSubmit, setValues } = useForm<ClinicalHistoryFormData>({
  validationSchema: toTypedSchema(clinicalHistorySchema),
  initialValues: {
    reasonForAdmission: '',
    historyOfPresentIllness: '',
    psychiatricHistory: '',
    medicalHistory: '',
    familyHistory: '',
    personalHistory: '',
    substanceUseHistory: '',
    legalHistory: '',
    socialHistory: '',
    developmentalHistory: '',
    educationalOccupationalHistory: '',
    sexualHistory: '',
    religiousSpiritualHistory: '',
    mentalStatusExam: '',
    physicalExam: '',
    diagnosticImpression: '',
    treatmentPlan: '',
    riskAssessment: '',
    prognosis: '',
    informedConsentNotes: '',
    additionalNotes: ''
  }
})

// Define all fields
const [reasonForAdmission] = defineField('reasonForAdmission')
const [historyOfPresentIllness] = defineField('historyOfPresentIllness')
const [psychiatricHistory] = defineField('psychiatricHistory')
const [medicalHistory] = defineField('medicalHistory')
const [familyHistory] = defineField('familyHistory')
const [personalHistory] = defineField('personalHistory')
const [substanceUseHistory] = defineField('substanceUseHistory')
const [legalHistory] = defineField('legalHistory')
const [socialHistory] = defineField('socialHistory')
const [developmentalHistory] = defineField('developmentalHistory')
const [educationalOccupationalHistory] = defineField('educationalOccupationalHistory')
const [sexualHistory] = defineField('sexualHistory')
const [religiousSpiritualHistory] = defineField('religiousSpiritualHistory')
const [mentalStatusExam] = defineField('mentalStatusExam')
const [physicalExam] = defineField('physicalExam')
const [diagnosticImpression] = defineField('diagnosticImpression')
const [treatmentPlan] = defineField('treatmentPlan')
const [riskAssessment] = defineField('riskAssessment')
const [prognosis] = defineField('prognosis')
const [informedConsentNotes] = defineField('informedConsentNotes')
const [additionalNotes] = defineField('additionalNotes')

// Map field names to refs for dynamic access
const fieldRefs: Record<ClinicalHistoryFieldName, Ref<string | undefined>> = {
  reasonForAdmission,
  historyOfPresentIllness,
  psychiatricHistory,
  medicalHistory,
  familyHistory,
  personalHistory,
  substanceUseHistory,
  legalHistory,
  socialHistory,
  developmentalHistory,
  educationalOccupationalHistory,
  sexualHistory,
  religiousSpiritualHistory,
  mentalStatusExam,
  physicalExam,
  diagnosticImpression,
  treatmentPlan,
  riskAssessment,
  prognosis,
  informedConsentNotes,
  additionalNotes
}

onMounted(() => {
  if (props.clinicalHistory) {
    setValues({
      reasonForAdmission: props.clinicalHistory.reasonForAdmission || '',
      historyOfPresentIllness: props.clinicalHistory.historyOfPresentIllness || '',
      psychiatricHistory: props.clinicalHistory.psychiatricHistory || '',
      medicalHistory: props.clinicalHistory.medicalHistory || '',
      familyHistory: props.clinicalHistory.familyHistory || '',
      personalHistory: props.clinicalHistory.personalHistory || '',
      substanceUseHistory: props.clinicalHistory.substanceUseHistory || '',
      legalHistory: props.clinicalHistory.legalHistory || '',
      socialHistory: props.clinicalHistory.socialHistory || '',
      developmentalHistory: props.clinicalHistory.developmentalHistory || '',
      educationalOccupationalHistory: props.clinicalHistory.educationalOccupationalHistory || '',
      sexualHistory: props.clinicalHistory.sexualHistory || '',
      religiousSpiritualHistory: props.clinicalHistory.religiousSpiritualHistory || '',
      mentalStatusExam: props.clinicalHistory.mentalStatusExam || '',
      physicalExam: props.clinicalHistory.physicalExam || '',
      diagnosticImpression: props.clinicalHistory.diagnosticImpression || '',
      treatmentPlan: props.clinicalHistory.treatmentPlan || '',
      riskAssessment: props.clinicalHistory.riskAssessment || '',
      prognosis: props.clinicalHistory.prognosis || '',
      informedConsentNotes: props.clinicalHistory.informedConsentNotes || '',
      additionalNotes: props.clinicalHistory.additionalNotes || ''
    })
  }
})

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    // Convert empty strings to null for API
    const data = Object.fromEntries(
      Object.entries(values).map(([key, value]) => [key, value || null])
    )

    if (isEditMode.value) {
      await clinicalHistoryStore.updateClinicalHistory(props.admissionId, data)
      showSuccess('medicalRecord.clinicalHistory.updated')
    } else {
      await clinicalHistoryStore.createClinicalHistory(props.admissionId, data)
      showSuccess('medicalRecord.clinicalHistory.created')
    }
    emit('saved')
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
})

function cancel() {
  emit('cancelled')
}
</script>

<template>
  <div class="clinical-history-form">
    <div class="form-header">
      <h3>
        {{
          isEditMode
            ? t('medicalRecord.clinicalHistory.edit')
            : t('medicalRecord.clinicalHistory.create')
        }}
      </h3>
    </div>

    <form @submit="onSubmit">
      <Accordion multiple :value="[]">
        <AccordionPanel v-for="section in fieldSections" :key="section.key" :value="section.key">
          <AccordionHeader>
            {{ t(`medicalRecord.clinicalHistory.sections.${section.key}`) }}
          </AccordionHeader>
          <AccordionContent>
            <div class="section-fields">
              <div v-for="field in section.fields" :key="field" class="form-field">
                <RichTextEditor
                  :modelValue="fieldRefs[field].value || ''"
                  @update:model-value="fieldRefs[field].value = $event"
                  :label="t(`medicalRecord.clinicalHistory.fields.${field}`)"
                  :rows="4"
                />
              </div>
            </div>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>

      <div class="form-actions">
        <Button
          type="button"
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          :disabled="loading"
          @click="cancel"
        />
        <Button type="submit" :label="t('common.save')" :loading="loading" />
      </div>
    </form>
  </div>
</template>

<style scoped>
.clinical-history-form {
  padding: 0;
}

.form-header {
  margin-bottom: 1.5rem;
}

.form-header h3 {
  margin: 0;
}

.section-fields {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 0.5rem 0;
}

.form-field {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--p-surface-border);
}
</style>
