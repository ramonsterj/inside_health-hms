<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
import { useAdmissionStore } from '@/stores/admission'
import { useTriageCodeStore } from '@/stores/triageCode'
import { useRoomStore } from '@/stores/room'
import type { PatientSummary } from '@/types'
import { Sex } from '@/types/patient'
import { RoomGender } from '@/types/room'
import type { Doctor } from '@/types/admission'
import {
  AdmissionType,
  admissionTypeRequiresRoom,
  admissionTypeRequiresTriageCode
} from '@/types/admission'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError, showSuccess } = useErrorHandler()
const admissionStore = useAdmissionStore()
const triageCodeStore = useTriageCodeStore()
const roomStore = useRoomStore()

const loading = ref(false)
const initializing = ref(true)
const isEditMode = computed(() => !!route.params.id)
const admissionId = computed(() => Number(route.params.id) || null)
const patientIdFromQuery = computed(() => {
  const id = route.query.patientId
  return id ? Number(id) : null
})

// Patient
const selectedPatient = ref<PatientSummary | null>(null)

// Admission details
const selectedTriageCode = ref<number | null>(null)
const selectedRoom = ref<number | null>(null)
const selectedPhysician = ref<number | null>(null)
const selectedType = ref<AdmissionType>(AdmissionType.HOSPITALIZATION)
const admissionDate = ref<Date>(new Date())

const typeOptions = computed(() => [
  { label: t('admission.types.HOSPITALIZATION'), value: AdmissionType.HOSPITALIZATION },
  { label: t('admission.types.AMBULATORY'), value: AdmissionType.AMBULATORY },
  { label: t('admission.types.ELECTROSHOCK_THERAPY'), value: AdmissionType.ELECTROSHOCK_THERAPY },
  { label: t('admission.types.KETAMINE_INFUSION'), value: AdmissionType.KETAMINE_INFUSION },
  { label: t('admission.types.EMERGENCY'), value: AdmissionType.EMERGENCY }
])

const roomRequired = computed(() => admissionTypeRequiresRoom(selectedType.value))
const triageCodeRequired = computed(() => admissionTypeRequiresTriageCode(selectedType.value))
const showRoomField = computed(() => admissionTypeRequiresRoom(selectedType.value))
const showTriageCodeField = computed(() => admissionTypeRequiresTriageCode(selectedType.value))

const filteredAvailableRooms = computed(() => {
  const patientSex = selectedPatient.value?.sex
  if (!patientSex) return roomStore.availableRooms
  const roomGender = patientSex === Sex.FEMALE ? RoomGender.FEMALE : RoomGender.MALE
  return roomStore.availableRooms.filter(r => r.gender === roomGender)
})

// Watch for type changes and clear irrelevant values
watch(selectedType, newType => {
  if (!admissionTypeRequiresRoom(newType)) {
    selectedRoom.value = null
  }
  if (!admissionTypeRequiresTriageCode(newType)) {
    selectedTriageCode.value = null
  }
})

// Errors
const errors = ref<Record<string, string>>({})

onMounted(async () => {
  try {
    await Promise.all([
      triageCodeStore.fetchTriageCodes(),
      roomStore.fetchAvailableRooms(),
      admissionStore.fetchDoctors()
    ])

    if (isEditMode.value && admissionId.value) {
      await loadAdmission()
    } else if (patientIdFromQuery.value) {
      await loadPatient()
    } else {
      // No patient specified, redirect back
      showError(t('admission.errors.invalidPatient'))
      router.push({ name: 'patients' })
    }
  } finally {
    initializing.value = false
  }
})

async function loadAdmission() {
  try {
    const admission = await admissionStore.fetchAdmission(admissionId.value!)
    selectedPatient.value = admission.patient
    selectedTriageCode.value = admission.triageCode?.id ?? null
    selectedRoom.value = admission.room?.id ?? null
    selectedPhysician.value = admission.treatingPhysician.id
    selectedType.value = admission.type
    admissionDate.value = new Date(admission.admissionDate)
  } catch (error) {
    showError(error)
    router.push({ name: 'admissions' })
  }
}

async function loadPatient() {
  try {
    const patient = await admissionStore.fetchPatientSummary(patientIdFromQuery.value!)
    if (patient) {
      selectedPatient.value = patient
    } else {
      showError(t('admission.errors.invalidPatient'))
      router.push({ name: 'patients' })
    }
  } catch {
    showError(t('admission.errors.invalidPatient'))
    router.push({ name: 'patients' })
  }
}

function getDoctorLabel(doctor: Doctor): string {
  const salutationLabel = doctor.salutation ? t(`user.salutations.${doctor.salutation}`) : ''
  return `${salutationLabel} ${doctor.firstName || ''} ${doctor.lastName || ''}`.trim()
}

function getTriageCodeLabel(triageCode: { code: string; description: string | null }): string {
  return `${triageCode.code}${triageCode.description ? ` - ${triageCode.description}` : ''}`
}

function getRoomLabel(room: { number: string; availableBeds: number }): string {
  return `${room.number} (${room.availableBeds} ${t('room.bedsAvailable')})`
}

function validate(): boolean {
  errors.value = {}

  if (!selectedPatient.value) {
    errors.value.patient = t('validation.admission.patientId.required')
  }
  if (!selectedType.value) {
    errors.value.type = t('validation.admission.type.required')
  }
  if (triageCodeRequired.value && !selectedTriageCode.value) {
    errors.value.triageCode = t('validation.admission.triageCodeId.requiredForType')
  }
  if (roomRequired.value && !selectedRoom.value) {
    errors.value.room = t('validation.admission.roomId.requiredForType')
  }
  if (!selectedPhysician.value) {
    errors.value.physician = t('validation.admission.treatingPhysicianId.required')
  }
  if (!admissionDate.value) {
    errors.value.admissionDate = t('validation.admission.admissionDate.required')
  }

  return Object.keys(errors.value).length === 0
}

async function submitAdmission() {
  if (!validate()) return

  loading.value = true
  try {
    if (isEditMode.value && admissionId.value) {
      await admissionStore.updateAdmission(admissionId.value, {
        triageCodeId: selectedTriageCode.value,
        roomId: selectedRoom.value,
        treatingPhysicianId: selectedPhysician.value!,
        type: selectedType.value
      })
      showSuccess('admission.updated')
      router.push({ name: 'admission-detail', params: { id: admissionId.value } })
    } else {
      const admission = await admissionStore.createAdmission({
        patientId: selectedPatient.value!.id,
        triageCodeId: selectedTriageCode.value,
        roomId: selectedRoom.value,
        treatingPhysicianId: selectedPhysician.value!,
        admissionDate: admissionDate.value.toISOString(),
        type: selectedType.value
      })
      showSuccess('admission.created')
      router.push({ name: 'admission-detail', params: { id: admission.id } })
    }
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function cancel() {
  if (isEditMode.value && admissionId.value) {
    router.push({ name: 'admission-detail', params: { id: admissionId.value } })
  } else if (selectedPatient.value) {
    router.push({ name: 'patient-detail', params: { id: selectedPatient.value.id } })
  } else {
    router.push({ name: 'patients' })
  }
}
</script>

<template>
  <div class="admission-form-page">
    <div v-if="initializing" class="loading-container">
      <ProgressSpinner />
    </div>

    <template v-else-if="selectedPatient">
      <div class="page-header">
        <div class="header-left">
          <Button icon="pi pi-arrow-left" severity="secondary" text rounded @click="cancel" />
          <div>
            <h1 class="page-title">{{ isEditMode ? t('admission.edit') : t('admission.new') }}</h1>
            <h2 class="patient-name">
              {{ selectedPatient.firstName }} {{ selectedPatient.lastName }}
            </h2>
          </div>
        </div>
      </div>

      <Message
        v-if="selectedPatient.hasActiveAdmission && !isEditMode"
        severity="warn"
        :closable="false"
        class="active-admission-warning"
      >
        {{ t('admission.patientAlreadyAdmittedWarning') }}
      </Message>

      <Card>
        <template #content>
          <div class="form-grid">
            <div class="form-field">
              <label>{{ t('admission.type') }} *</label>
              <Select
                v-model="selectedType"
                :options="typeOptions"
                optionValue="value"
                optionLabel="label"
                :placeholder="t('admission.selectType')"
                :class="{ 'p-invalid': errors.type }"
              />
              <Message v-if="errors.type" severity="error" :closable="false">
                {{ errors.type }}
              </Message>
            </div>

            <div v-if="showTriageCodeField" class="form-field">
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

            <div v-if="showRoomField" class="form-field">
              <label>{{ t('admission.room') }} *</label>
              <Select
                v-model="selectedRoom"
                :options="filteredAvailableRooms"
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

          <div class="form-actions">
            <Button :label="t('common.cancel')" severity="secondary" outlined @click="cancel" />
            <Button
              :label="isEditMode ? t('common.save') : t('admission.submit')"
              icon="pi pi-check"
              :loading="loading"
              :disabled="selectedPatient.hasActiveAdmission && !isEditMode"
              @click="submitAdmission"
            />
          </div>
        </template>
      </Card>
    </template>
  </div>
</template>

<style scoped>
.admission-form-page {
  max-width: 700px;
  margin: 0 auto;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
}

.patient-name {
  margin: 0.25rem 0 0 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.active-admission-warning {
  margin-bottom: 1rem;
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
}

.form-field label {
  font-weight: 500;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--p-surface-border);
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
