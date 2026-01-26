<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Stepper from 'primevue/stepper'
import StepItem from 'primevue/stepitem'
import StepPanel from 'primevue/steppanel'
import Step from 'primevue/step'
import AutoComplete from 'primevue/autocomplete'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Textarea from 'primevue/textarea'
import FileUpload from 'primevue/fileupload'
import Message from 'primevue/message'
import { useAdmissionStore } from '@/stores/admission'
import { useTriageCodeStore } from '@/stores/triageCode'
import { useRoomStore } from '@/stores/room'
import type { PatientSummary } from '@/types'
import type { Doctor } from '@/types/admission'
import { MAX_CONSENT_FILE_SIZE, ACCEPTED_CONSENT_TYPES } from '@/validation/admission'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError, showSuccess } = useErrorHandler()
const admissionStore = useAdmissionStore()
const triageCodeStore = useTriageCodeStore()
const roomStore = useRoomStore()

const loading = ref(false)
const isEditMode = computed(() => !!route.params.id)
const admissionId = computed(() => Number(route.params.id) || null)
const patientIdFromQuery = computed(() => {
  const id = route.query.patientId
  return id ? Number(id) : null
})
const isPatientPreselected = ref(false)

// Step 1: Patient
const patientSearch = ref('')
const patientSuggestions = ref<PatientSummary[]>([])
const selectedPatient = ref<PatientSummary | null>(null)

// Step 2: Details
const selectedTriageCode = ref<number | null>(null)
const selectedRoom = ref<number | null>(null)
const selectedPhysician = ref<number | null>(null)
const admissionDate = ref<Date>(new Date())

// Step 3: Extras
const inventory = ref('')
const consentFile = ref<File | null>(null)

// Errors
const errors = ref<Record<string, string>>({})

onMounted(async () => {
  await Promise.all([
    triageCodeStore.fetchTriageCodes(),
    roomStore.fetchAvailableRooms(),
    admissionStore.fetchDoctors()
  ])

  if (isEditMode.value && admissionId.value) {
    await loadAdmission()
  } else if (patientIdFromQuery.value) {
    await loadPatientFromQuery()
  }
})

async function loadAdmission() {
  loading.value = true
  try {
    const admission = await admissionStore.fetchAdmission(admissionId.value!)
    selectedPatient.value = admission.patient
    selectedTriageCode.value = admission.triageCode.id
    selectedRoom.value = admission.room.id
    selectedPhysician.value = admission.treatingPhysician.id
    admissionDate.value = new Date(admission.admissionDate)
    inventory.value = admission.inventory || ''
  } catch (error) {
    showError(error)
    router.push({ name: 'admissions' })
  } finally {
    loading.value = false
  }
}

async function loadPatientFromQuery() {
  loading.value = true
  try {
    const patient = await admissionStore.fetchPatientSummary(patientIdFromQuery.value!)
    if (patient) {
      selectedPatient.value = patient
      isPatientPreselected.value = true
    } else {
      showError(t('admission.errors.invalidPatient'))
      router.push({ name: 'patients' })
    }
  } catch {
    showError(t('admission.errors.invalidPatient'))
    router.push({ name: 'patients' })
  } finally {
    loading.value = false
  }
}

async function searchPatients(event: { query: string }) {
  const results = await admissionStore.searchPatients(event.query)
  patientSuggestions.value = results
}

function onPatientSelect(event: { value: PatientSummary }) {
  selectedPatient.value = event.value
}

function getPatientLabel(patient: PatientSummary): string {
  return `${patient.firstName} ${patient.lastName} ${patient.idDocumentNumber ? `(${patient.idDocumentNumber})` : ''}`
}

function getDoctorLabel(doctor: Doctor): string {
  return `${doctor.salutation || ''} ${doctor.firstName || ''} ${doctor.lastName || ''}`.trim()
}

function getTriageCodeLabel(triageCode: { code: string; description: string | null }): string {
  return `${triageCode.code}${triageCode.description ? ` - ${triageCode.description}` : ''}`
}

function getRoomLabel(room: { number: string; availableBeds: number }): string {
  return `${room.number} (${room.availableBeds} ${t('room.bedsAvailable')})`
}

const canProceedStep1 = computed(() => {
  return selectedPatient.value && !selectedPatient.value.hasActiveAdmission
})

function validateStep1(): boolean {
  errors.value = {}
  if (!selectedPatient.value) {
    errors.value.patient = t('validation.admission.patientId.required')
    return false
  }
  return true
}

function validateStep2(): boolean {
  errors.value = {}
  if (!selectedTriageCode.value) {
    errors.value.triageCode = t('validation.admission.triageCodeId.required')
  }
  if (!selectedRoom.value) {
    errors.value.room = t('validation.admission.roomId.required')
  }
  if (!selectedPhysician.value) {
    errors.value.physician = t('validation.admission.treatingPhysicianId.required')
  }
  if (!admissionDate.value) {
    errors.value.admissionDate = t('validation.admission.admissionDate.required')
  }
  return Object.keys(errors.value).length === 0
}

function onConsentSelect(event: { files: File[] }) {
  consentFile.value = event.files[0] || null
}

async function submitAdmission() {
  if (!validateStep1() || !validateStep2()) return

  loading.value = true
  try {
    if (isEditMode.value && admissionId.value) {
      await admissionStore.updateAdmission(admissionId.value, {
        triageCodeId: selectedTriageCode.value!,
        roomId: selectedRoom.value!,
        treatingPhysicianId: selectedPhysician.value!,
        inventory: inventory.value || undefined
      })
      showSuccess('admission.updated')
    } else {
      const admission = await admissionStore.createAdmission({
        patientId: selectedPatient.value!.id,
        triageCodeId: selectedTriageCode.value!,
        roomId: selectedRoom.value!,
        treatingPhysicianId: selectedPhysician.value!,
        admissionDate: admissionDate.value.toISOString(),
        inventory: inventory.value || undefined
      })

      // Upload consent if provided
      if (consentFile.value) {
        await admissionStore.uploadConsentDocument(admission.id, consentFile.value)
      }

      showSuccess('admission.created')
    }
    router.push({ name: 'admissions' })
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function cancel() {
  router.push({ name: 'admissions' })
}
</script>

<template>
  <div class="admission-wizard-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditMode ? t('admission.edit') : t('admission.new') }}
      </h1>
    </div>

    <Card>
      <template #content>
        <Stepper value="1" linear>
          <!-- Step 1: Patient Selection -->
          <StepItem value="1">
            <Step>{{ t('admission.steps.patient') }}</Step>
            <StepPanel v-slot="{ activateCallback }">
              <div class="step-content">
                <div v-if="!isEditMode && !isPatientPreselected" class="patient-search-section">
                  <div class="form-field patient-search-field">
                    <label>{{ t('admission.searchPatient') }} *</label>
                    <AutoComplete
                      v-model="patientSearch"
                      :suggestions="patientSuggestions"
                      @complete="searchPatients"
                      @item-select="onPatientSelect"
                      :optionLabel="getPatientLabel"
                      :placeholder="t('admission.searchPatientPlaceholder')"
                      :class="{ 'p-invalid': errors.patient }"
                      fluid
                    />
                    <Message v-if="errors.patient" severity="error" :closable="false">
                      {{ errors.patient }}
                    </Message>
                  </div>
                  <div class="register-patient-action">
                    <span class="register-hint">{{ t('admission.patientNotFound') }}</span>
                    <Button
                      :label="t('admission.registerNewPatient')"
                      icon="pi pi-user-plus"
                      severity="secondary"
                      outlined
                      @click="router.push({ name: 'patient-create' })"
                    />
                  </div>
                </div>

                <div v-if="selectedPatient" class="selected-patient">
                  <h4>{{ t('admission.selectedPatient') }}</h4>
                  <p>
                    <strong>{{ selectedPatient.firstName }} {{ selectedPatient.lastName }}</strong>
                    <span v-if="selectedPatient.idDocumentNumber">
                      ({{ selectedPatient.idDocumentNumber }})
                    </span>
                  </p>
                  <Message v-if="selectedPatient.hasActiveAdmission" severity="warn" :closable="false" class="mt-3">
                    {{ t('admission.patientAlreadyAdmittedWarning') }}
                  </Message>
                </div>

                <div class="step-actions">
                  <Button
                    :label="t('common.next')"
                    icon="pi pi-arrow-right"
                    iconPos="right"
                    :disabled="!canProceedStep1"
                    @click="validateStep1() && activateCallback('2')"
                  />
                </div>
              </div>
            </StepPanel>
          </StepItem>

          <!-- Step 2: Admission Details -->
          <StepItem value="2">
            <Step>{{ t('admission.steps.details') }}</Step>
            <StepPanel v-slot="{ activateCallback }">
              <div class="step-content">
                <div class="form-grid">
                  <div class="form-field">
                    <label>{{ t('admission.triageCode') }} *</label>
                    <Select
                      v-model="selectedTriageCode"
                      :options="triageCodeStore.triageCodes"
                      optionValue="id"
                      :optionLabel="getTriageCodeLabel"
                      :placeholder="t('admission.selectTriageCode')"
                      :class="{ 'p-invalid': errors.triageCode }"
                    />
                    <Message v-if="errors.triageCode" severity="error" :closable="false">
                      {{ errors.triageCode }}
                    </Message>
                  </div>

                  <div class="form-field">
                    <label>{{ t('admission.room') }} *</label>
                    <Select
                      v-model="selectedRoom"
                      :options="roomStore.availableRooms"
                      optionValue="id"
                      :optionLabel="getRoomLabel"
                      :placeholder="t('admission.selectRoom')"
                      :class="{ 'p-invalid': errors.room }"
                    />
                    <Message v-if="errors.room" severity="error" :closable="false">
                      {{ errors.room }}
                    </Message>
                  </div>

                  <div class="form-field">
                    <label>{{ t('admission.treatingPhysician') }} *</label>
                    <Select
                      v-model="selectedPhysician"
                      :options="admissionStore.doctors"
                      optionValue="id"
                      :optionLabel="getDoctorLabel"
                      :placeholder="t('admission.selectPhysician')"
                      :class="{ 'p-invalid': errors.physician }"
                    />
                    <Message v-if="errors.physician" severity="error" :closable="false">
                      {{ errors.physician }}
                    </Message>
                  </div>

                  <div class="form-field">
                    <label>{{ t('admission.admissionDate') }} *</label>
                    <DatePicker
                      v-model="admissionDate"
                      showTime
                      hourFormat="24"
                      :class="{ 'p-invalid': errors.admissionDate }"
                      :disabled="isEditMode"
                    />
                    <Message v-if="errors.admissionDate" severity="error" :closable="false">
                      {{ errors.admissionDate }}
                    </Message>
                  </div>
                </div>

                <div class="step-actions">
                  <Button
                    :label="t('common.back')"
                    icon="pi pi-arrow-left"
                    severity="secondary"
                    @click="activateCallback('1')"
                  />
                  <Button
                    :label="t('common.next')"
                    icon="pi pi-arrow-right"
                    iconPos="right"
                    @click="validateStep2() && activateCallback('3')"
                  />
                </div>
              </div>
            </StepPanel>
          </StepItem>

          <!-- Step 3: Additional Information -->
          <StepItem value="3">
            <Step>{{ t('admission.steps.extras') }}</Step>
            <StepPanel v-slot="{ activateCallback }">
              <div class="step-content">
                <div class="form-field">
                  <label>{{ t('admission.inventory') }}</label>
                  <Textarea
                    v-model="inventory"
                    rows="4"
                    :placeholder="t('admission.inventoryPlaceholder')"
                  />
                </div>

                <div v-if="!isEditMode" class="form-field">
                  <label>{{ t('admission.consent') }}</label>
                  <FileUpload
                    mode="basic"
                    name="consent"
                    :accept="ACCEPTED_CONSENT_TYPES"
                    :maxFileSize="MAX_CONSENT_FILE_SIZE"
                    @select="onConsentSelect"
                    :chooseLabel="t('admission.selectConsent')"
                  />
                  <small class="hint">{{ t('admission.consentHint') }}</small>
                </div>

                <div class="step-actions">
                  <Button
                    :label="t('common.back')"
                    icon="pi pi-arrow-left"
                    severity="secondary"
                    @click="activateCallback('2')"
                  />
                  <Button
                    :label="t('common.cancel')"
                    severity="secondary"
                    outlined
                    @click="cancel"
                  />
                  <Button
                    :label="isEditMode ? t('common.save') : t('admission.submit')"
                    icon="pi pi-check"
                    :loading="loading"
                    @click="submitAdmission"
                  />
                </div>
              </div>
            </StepPanel>
          </StepItem>
        </Stepper>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.admission-wizard-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.step-content {
  padding: 1.5rem 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.patient-search-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.patient-search-field {
  width: 100%;
}

.register-patient-action {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: var(--p-surface-ground);
  border-radius: 8px;
}

.register-hint {
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.form-field label {
  font-weight: 500;
}

.selected-patient {
  background: var(--p-surface-ground);
  padding: 1rem;
  border-radius: 8px;
  margin: 1rem 0;
}

.selected-patient h4 {
  margin: 0 0 0.5rem 0;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.selected-patient p {
  margin: 0;
}

.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px solid var(--p-surface-border);
}

.hint {
  color: var(--p-text-muted-color);
}
</style>
