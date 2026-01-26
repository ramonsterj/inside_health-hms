<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import Card from 'primevue/card'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import FileUpload from 'primevue/fileupload'
import { usePatientStore, DuplicatePatientError } from '@/stores/patient'
import { useAuthStore } from '@/stores/auth'
import {
  patientSchema,
  type PatientFormData,
  MAX_ID_DOCUMENT_SIZE,
  ACCEPTED_ID_DOCUMENT_TYPES
} from '@/validation/patient'
import type { EmergencyContact, PatientSummary } from '@/types'
import { Sex, MaritalStatus, EducationLevel } from '@/types'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { showError, showSuccess } = useErrorHandler()
const patientStore = usePatientStore()
const authStore = useAuthStore()

const isEditMode = computed(() => !!route.params.id)
const patientId = computed(() => Number(route.params.id) || null)

const loading = ref(false)
const showDuplicateDialog = ref(false)
const duplicates = ref<PatientSummary[]>([])

// ID Document state
const uploadLoading = ref(false)
const showIdDocumentDialog = ref(false)
const idDocumentUrl = ref<string | null>(null)
const hasIdDocument = ref(false)

// Permissions
const canUploadId = computed(() => authStore.hasPermission('patient:upload-id'))
const canViewId = computed(() => authStore.hasPermission('patient:view-id'))

// Emergency contacts
const emergencyContacts = ref<EmergencyContact[]>([{ name: '', relationship: '', phone: '' }])

// Form setup
const { defineField, handleSubmit, errors, setValues } = useForm<PatientFormData>({
  validationSchema: toTypedSchema(patientSchema),
  initialValues: {
    firstName: '',
    lastName: '',
    age: 0,
    sex: 'MALE',
    gender: '',
    maritalStatus: 'SINGLE',
    religion: '',
    educationLevel: 'NONE',
    occupation: '',
    address: '',
    email: '',
    idDocumentNumber: '',
    notes: '',
    emergencyContacts: [{ name: '', relationship: '', phone: '' }]
  }
})

const [firstName] = defineField('firstName')
const [lastName] = defineField('lastName')
const [age] = defineField('age')
const [sex] = defineField('sex')
const [gender] = defineField('gender')
const [maritalStatus] = defineField('maritalStatus')
const [religion] = defineField('religion')
const [educationLevel] = defineField('educationLevel')
const [occupation] = defineField('occupation')
const [address] = defineField('address')
const [email] = defineField('email')
const [idDocumentNumber] = defineField('idDocumentNumber')
const [notes] = defineField('notes')

// Options for dropdowns
const sexOptions = computed(() => [
  { label: t('patient.sexOptions.MALE'), value: Sex.MALE },
  { label: t('patient.sexOptions.FEMALE'), value: Sex.FEMALE }
])

const maritalStatusOptions = computed(() => [
  { label: t('patient.maritalStatusOptions.SINGLE'), value: MaritalStatus.SINGLE },
  { label: t('patient.maritalStatusOptions.MARRIED'), value: MaritalStatus.MARRIED },
  { label: t('patient.maritalStatusOptions.DIVORCED'), value: MaritalStatus.DIVORCED },
  { label: t('patient.maritalStatusOptions.WIDOWED'), value: MaritalStatus.WIDOWED },
  { label: t('patient.maritalStatusOptions.SEPARATED'), value: MaritalStatus.SEPARATED },
  { label: t('patient.maritalStatusOptions.OTHER'), value: MaritalStatus.OTHER }
])

const educationLevelOptions = computed(() => [
  { label: t('patient.educationLevelOptions.NONE'), value: EducationLevel.NONE },
  { label: t('patient.educationLevelOptions.PRIMARY'), value: EducationLevel.PRIMARY },
  { label: t('patient.educationLevelOptions.SECONDARY'), value: EducationLevel.SECONDARY },
  { label: t('patient.educationLevelOptions.TECHNICAL'), value: EducationLevel.TECHNICAL },
  { label: t('patient.educationLevelOptions.UNIVERSITY'), value: EducationLevel.UNIVERSITY },
  { label: t('patient.educationLevelOptions.POSTGRADUATE'), value: EducationLevel.POSTGRADUATE }
])

// Sync emergency contacts with form validation
watch(
  emergencyContacts,
  newContacts => {
    setValues({ emergencyContacts: newContacts }, false)
  },
  { deep: true }
)

onMounted(async () => {
  if (isEditMode.value && patientId.value) {
    await loadPatient()
  }
})

async function loadPatient() {
  if (!patientId.value) return

  loading.value = true
  try {
    const patient = await patientStore.fetchPatient(patientId.value)
    setValues({
      firstName: patient.firstName,
      lastName: patient.lastName,
      age: patient.age,
      sex: patient.sex,
      gender: patient.gender,
      maritalStatus: patient.maritalStatus,
      religion: patient.religion,
      educationLevel: patient.educationLevel,
      occupation: patient.occupation,
      address: patient.address,
      email: patient.email,
      idDocumentNumber: patient.idDocumentNumber || '',
      notes: patient.notes || '',
      emergencyContacts: patient.emergencyContacts
    })
    emergencyContacts.value = patient.emergencyContacts.map(c => ({ ...c }))
    hasIdDocument.value = patient.hasIdDocument
  } catch (error) {
    showError(error)
    router.push({ name: 'patients' })
  } finally {
    loading.value = false
  }
}

function addContact() {
  emergencyContacts.value.push({ name: '', relationship: '', phone: '' })
}

function removeContact(index: number) {
  if (emergencyContacts.value.length > 1) {
    emergencyContacts.value.splice(index, 1)
  }
}

const onSubmit = handleSubmit(async values => {
  loading.value = true
  try {
    const data = {
      firstName: values.firstName,
      lastName: values.lastName,
      age: values.age,
      sex: values.sex as Sex,
      gender: values.gender,
      maritalStatus: values.maritalStatus as MaritalStatus,
      religion: values.religion,
      educationLevel: values.educationLevel as EducationLevel,
      occupation: values.occupation,
      address: values.address,
      email: values.email,
      idDocumentNumber: values.idDocumentNumber || undefined,
      notes: values.notes || undefined,
      emergencyContacts: emergencyContacts.value
    }

    if (isEditMode.value && patientId.value) {
      await patientStore.updatePatient(patientId.value, data)
      showSuccess('patient.updateSuccess')
    } else {
      await patientStore.createPatient(data)
      showSuccess('patient.createSuccess')
    }
    router.push({ name: 'patients' })
  } catch (error) {
    if (error instanceof DuplicatePatientError) {
      duplicates.value = error.potentialDuplicates
      showDuplicateDialog.value = true
    } else {
      showError(error)
    }
  } finally {
    loading.value = false
  }
})

function cancel() {
  router.push({ name: 'patients' })
}

function viewDuplicatePatient(id: number) {
  router.push({ name: 'patient-detail', params: { id } })
}

// ID Document functions
async function onFileUpload(event: { files: File[] }) {
  const file = event.files[0]
  if (!file || !patientId.value) return

  uploadLoading.value = true
  try {
    await patientStore.uploadIdDocument(patientId.value, file)
    hasIdDocument.value = true
    showSuccess('patient.idDocumentUploaded')
  } catch (error) {
    showError(error)
  } finally {
    uploadLoading.value = false
  }
}

async function viewIdDocument() {
  if (!patientId.value) return

  try {
    const blob = await patientStore.downloadIdDocument(patientId.value)
    idDocumentUrl.value = URL.createObjectURL(blob)
    showIdDocumentDialog.value = true
  } catch (error) {
    showError(error)
  }
}

async function downloadIdDocument() {
  if (!patientId.value) return

  try {
    const blob = await patientStore.downloadIdDocument(patientId.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `patient-${patientId.value}-id-document`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (error) {
    showError(error)
  }
}

async function deleteIdDocument() {
  if (!patientId.value) return

  try {
    await patientStore.deleteIdDocument(patientId.value)
    hasIdDocument.value = false
    showSuccess('patient.idDocumentDeleted')
  } catch (error) {
    showError(error)
  }
}

function closeIdDocumentDialog() {
  showIdDocumentDialog.value = false
  if (idDocumentUrl.value) {
    URL.revokeObjectURL(idDocumentUrl.value)
    idDocumentUrl.value = null
  }
}
</script>

<template>
  <div class="patient-form-page">
    <div class="page-header">
      <h1 class="page-title">
        {{ isEditMode ? t('patient.editPatient') : t('patient.newPatient') }}
      </h1>
    </div>

    <form @submit="onSubmit">
      <Card class="form-card">
        <template #title>{{ t('patient.generalInfo') }}</template>
        <template #content>
          <div class="form-grid">
            <div class="form-row">
              <div class="form-field">
                <label for="firstName">{{ t('patient.firstName') }} *</label>
                <InputText
                  id="firstName"
                  v-model="firstName"
                  :class="{ 'p-invalid': errors.firstName }"
                  class="w-full"
                />
                <small v-if="errors.firstName" class="p-error">{{ errors.firstName }}</small>
              </div>
              <div class="form-field">
                <label for="lastName">{{ t('patient.lastName') }} *</label>
                <InputText
                  id="lastName"
                  v-model="lastName"
                  :class="{ 'p-invalid': errors.lastName }"
                  class="w-full"
                />
                <small v-if="errors.lastName" class="p-error">{{ errors.lastName }}</small>
              </div>
            </div>

            <div class="form-row">
              <div class="form-field">
                <label for="age">{{ t('patient.age') }} *</label>
                <InputNumber
                  id="age"
                  v-model="age"
                  :min="0"
                  :max="150"
                  :class="{ 'p-invalid': errors.age }"
                  class="w-full"
                />
                <small v-if="errors.age" class="p-error">{{ errors.age }}</small>
              </div>
              <div class="form-field">
                <label for="sex">{{ t('patient.sex') }} *</label>
                <Select
                  id="sex"
                  v-model="sex"
                  :options="sexOptions"
                  optionLabel="label"
                  optionValue="value"
                  :class="{ 'p-invalid': errors.sex }"
                  class="w-full"
                />
                <small v-if="errors.sex" class="p-error">{{ errors.sex }}</small>
              </div>
            </div>

            <div class="form-row">
              <div class="form-field">
                <label for="gender">{{ t('patient.gender') }} *</label>
                <InputText
                  id="gender"
                  v-model="gender"
                  :class="{ 'p-invalid': errors.gender }"
                  class="w-full"
                />
                <small v-if="errors.gender" class="p-error">{{ errors.gender }}</small>
              </div>
              <div class="form-field">
                <label for="maritalStatus">{{ t('patient.maritalStatus') }} *</label>
                <Select
                  id="maritalStatus"
                  v-model="maritalStatus"
                  :options="maritalStatusOptions"
                  optionLabel="label"
                  optionValue="value"
                  :class="{ 'p-invalid': errors.maritalStatus }"
                  class="w-full"
                />
                <small v-if="errors.maritalStatus" class="p-error">{{
                  errors.maritalStatus
                }}</small>
              </div>
            </div>

            <div class="form-row">
              <div class="form-field">
                <label for="religion">{{ t('patient.religion') }} *</label>
                <InputText
                  id="religion"
                  v-model="religion"
                  :class="{ 'p-invalid': errors.religion }"
                  class="w-full"
                />
                <small v-if="errors.religion" class="p-error">{{ errors.religion }}</small>
              </div>
              <div class="form-field">
                <label for="educationLevel">{{ t('patient.educationLevel') }} *</label>
                <Select
                  id="educationLevel"
                  v-model="educationLevel"
                  :options="educationLevelOptions"
                  optionLabel="label"
                  optionValue="value"
                  :class="{ 'p-invalid': errors.educationLevel }"
                  class="w-full"
                />
                <small v-if="errors.educationLevel" class="p-error">{{
                  errors.educationLevel
                }}</small>
              </div>
            </div>

            <div class="form-field">
              <label for="occupation">{{ t('patient.occupation') }} *</label>
              <InputText
                id="occupation"
                v-model="occupation"
                :class="{ 'p-invalid': errors.occupation }"
                class="w-full"
              />
              <small v-if="errors.occupation" class="p-error">{{ errors.occupation }}</small>
            </div>

            <div class="form-field">
              <label for="address">{{ t('patient.address') }} *</label>
              <Textarea
                id="address"
                v-model="address"
                rows="2"
                :class="{ 'p-invalid': errors.address }"
                class="w-full"
              />
              <small v-if="errors.address" class="p-error">{{ errors.address }}</small>
            </div>

            <div class="form-row">
              <div class="form-field">
                <label for="email">{{ t('patient.email') }} *</label>
                <InputText
                  id="email"
                  v-model="email"
                  type="email"
                  :class="{ 'p-invalid': errors.email }"
                  class="w-full"
                />
                <small v-if="errors.email" class="p-error">{{ errors.email }}</small>
              </div>
              <div class="form-field">
                <label for="idDocumentNumber">{{ t('patient.idDocumentNumber') }}</label>
                <InputText
                  id="idDocumentNumber"
                  v-model="idDocumentNumber"
                  :class="{ 'p-invalid': errors.idDocumentNumber }"
                  class="w-full"
                />
                <small v-if="errors.idDocumentNumber" class="p-error">{{
                  errors.idDocumentNumber
                }}</small>
              </div>
            </div>

            <div class="form-field">
              <label for="notes">{{ t('patient.notes') }}</label>
              <Textarea id="notes" v-model="notes" rows="3" class="w-full" />
            </div>
          </div>
        </template>
      </Card>

      <Card class="form-card">
        <template #title>
          <div class="card-title-with-action">
            <span>{{ t('patient.emergencyContacts') }} *</span>
            <Button
              icon="pi pi-plus"
              :label="t('contact.addContact')"
              severity="secondary"
              size="small"
              @click="addContact"
              type="button"
            />
          </div>
        </template>
        <template #content>
          <Message v-if="errors.emergencyContacts" severity="error" :closable="false">
            {{ errors.emergencyContacts }}
          </Message>

          <div v-for="(contact, index) in emergencyContacts" :key="index" class="contact-row">
            <div class="contact-fields">
              <div class="form-field">
                <label>{{ t('contact.name') }} *</label>
                <InputText
                  v-model="contact.name"
                  :class="{ 'p-invalid': errors[`emergencyContacts[${index}].name`] }"
                  class="w-full"
                />
                <small v-if="errors[`emergencyContacts[${index}].name`]" class="p-error">
                  {{ errors[`emergencyContacts[${index}].name`] }}
                </small>
              </div>
              <div class="form-field">
                <label>{{ t('contact.relationship') }} *</label>
                <InputText
                  v-model="contact.relationship"
                  :class="{ 'p-invalid': errors[`emergencyContacts[${index}].relationship`] }"
                  class="w-full"
                />
                <small v-if="errors[`emergencyContacts[${index}].relationship`]" class="p-error">
                  {{ errors[`emergencyContacts[${index}].relationship`] }}
                </small>
              </div>
              <div class="form-field">
                <label>{{ t('contact.phone') }} *</label>
                <InputText
                  v-model="contact.phone"
                  :class="{ 'p-invalid': errors[`emergencyContacts[${index}].phone`] }"
                  class="w-full"
                />
                <small v-if="errors[`emergencyContacts[${index}].phone`]" class="p-error">
                  {{ errors[`emergencyContacts[${index}].phone`] }}
                </small>
              </div>
            </div>
            <Button
              v-if="emergencyContacts.length > 1"
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              @click="removeContact(index)"
              type="button"
              class="remove-contact-btn"
            />
          </div>
        </template>
      </Card>

      <!-- ID Document Section (Edit mode only) -->
      <Card v-if="isEditMode && canUploadId" class="form-card">
        <template #title>{{ t('patient.idDocument') }}</template>
        <template #content>
          <div v-if="hasIdDocument" class="id-document-section">
            <div class="id-document-status">
              <i class="pi pi-check-circle" style="color: var(--p-green-500)"></i>
              <span>{{ t('patient.idDocumentUploaded') }}</span>
            </div>
            <div class="id-document-actions">
              <Button
                v-if="canViewId"
                icon="pi pi-eye"
                :label="t('patient.viewIdDocument')"
                severity="secondary"
                size="small"
                @click="viewIdDocument"
                type="button"
              />
              <Button
                v-if="canViewId"
                icon="pi pi-download"
                :label="t('common.download')"
                severity="secondary"
                size="small"
                @click="downloadIdDocument"
                type="button"
              />
              <Button
                icon="pi pi-trash"
                :label="t('common.delete')"
                severity="danger"
                size="small"
                outlined
                @click="deleteIdDocument"
                type="button"
              />
            </div>
            <div class="id-document-replace">
              <p class="replace-label">{{ t('patient.replaceIdDocument') }}</p>
              <FileUpload
                mode="basic"
                :auto="true"
                :accept="ACCEPTED_ID_DOCUMENT_TYPES"
                :maxFileSize="MAX_ID_DOCUMENT_SIZE"
                :chooseLabel="t('patient.uploadNewDocument')"
                @select="onFileUpload"
                :disabled="uploadLoading"
              />
            </div>
          </div>
          <div v-else class="id-document-upload">
            <div class="upload-prompt">
              <i class="pi pi-id-card upload-icon"></i>
              <p>{{ t('patient.noIdDocument') }}</p>
            </div>
            <FileUpload
              mode="basic"
              :auto="true"
              :accept="ACCEPTED_ID_DOCUMENT_TYPES"
              :maxFileSize="MAX_ID_DOCUMENT_SIZE"
              :chooseLabel="t('patient.uploadIdDocument')"
              @select="onFileUpload"
              :disabled="uploadLoading"
            />
          </div>
        </template>
      </Card>

      <div class="form-actions">
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          outlined
          @click="cancel"
          type="button"
        />
        <Button :label="t('common.save')" icon="pi pi-check" type="submit" :loading="loading" />
      </div>
    </form>

    <!-- Duplicate Patient Dialog -->
    <Dialog
      v-model:visible="showDuplicateDialog"
      :header="t('patient.duplicateFound')"
      :modal="true"
      :style="{ width: '500px' }"
      :breakpoints="{ '640px': '90vw' }"
    >
      <Message severity="warn" :closable="false">
        {{ t('patient.duplicateMessage') }}
      </Message>

      <div class="duplicate-list">
        <div v-for="dup in duplicates" :key="dup.id" class="duplicate-item">
          <div class="duplicate-info">
            <strong>{{ dup.firstName }} {{ dup.lastName }}</strong>
            <span>{{ t('patient.age') }}: {{ dup.age }}</span>
            <span v-if="dup.idDocumentNumber">
              {{ t('patient.idDocumentNumber') }}: {{ dup.idDocumentNumber }}
            </span>
          </div>
          <Button
            icon="pi pi-eye"
            :label="t('common.view')"
            size="small"
            severity="secondary"
            @click="viewDuplicatePatient(dup.id)"
          />
        </div>
      </div>

      <template #footer>
        <Button :label="t('common.close')" @click="showDuplicateDialog = false" />
      </template>
    </Dialog>

    <!-- ID Document Preview Dialog -->
    <Dialog
      v-model:visible="showIdDocumentDialog"
      :header="t('patient.idDocument')"
      :modal="true"
      :style="{ width: '80vw', maxWidth: '800px' }"
      :breakpoints="{ '640px': '95vw' }"
      @hide="closeIdDocumentDialog"
    >
      <div class="document-preview">
        <img v-if="idDocumentUrl" :src="idDocumentUrl" alt="ID Document" class="document-image" />
      </div>
      <template #footer>
        <Button :label="t('common.close')" @click="closeIdDocumentDialog" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.patient-form-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
}

.form-card {
  margin-bottom: 1rem;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field label {
  font-weight: 500;
  font-size: 0.875rem;
}

.w-full {
  width: 100%;
}

.card-title-with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.contact-row {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--surface-border);
  border-radius: 6px;
  margin-bottom: 1rem;
}

.contact-fields {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.remove-contact-btn {
  margin-top: 1.75rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}

.duplicate-list {
  margin-top: 1rem;
}

.duplicate-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  border: 1px solid var(--surface-border);
  border-radius: 6px;
  margin-bottom: 0.5rem;
}

.duplicate-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.duplicate-info span {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

@media (max-width: 768px) {
  .form-row,
  .contact-fields {
    grid-template-columns: 1fr;
  }

  .contact-row {
    flex-direction: column;
  }

  .remove-contact-btn {
    margin-top: 0;
    align-self: flex-end;
  }
}

/* ID Document Section Styles */
.id-document-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.id-document-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
}

.id-document-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.id-document-replace {
  margin-top: 0.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--surface-border);
}

.replace-label {
  margin: 0 0 0.5rem;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.id-document-upload {
  text-align: center;
  padding: 1rem;
}

.upload-prompt {
  margin-bottom: 1rem;
}

.upload-icon {
  font-size: 2rem;
  color: var(--text-color-secondary);
  margin-bottom: 0.5rem;
}

.upload-prompt p {
  margin: 0;
  color: var(--text-color-secondary);
}

.document-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.document-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}
</style>
