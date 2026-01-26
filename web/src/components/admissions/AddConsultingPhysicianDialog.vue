<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Message from 'primevue/message'
import { useAdmissionStore } from '@/stores/admission'
import { useErrorHandler } from '@/composables/useErrorHandler'
import type { Doctor } from '@/types/admission'

const props = defineProps<{
  visible: boolean
  admissionId: number
  treatingPhysicianId: number
  existingPhysicianIds: number[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  added: []
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()
const admissionStore = useAdmissionStore()

const loading = ref(false)
const selectedPhysicianId = ref<number | null>(null)
const reason = ref('')
const requestedDate = ref<Date | null>(new Date())
const errors = ref<Record<string, string>>({})

// Filter available doctors: exclude treating physician and already assigned physicians
const availableDoctors = computed(() => {
  const excludeIds = new Set([props.treatingPhysicianId, ...props.existingPhysicianIds])
  return admissionStore.doctors.filter(doctor => !excludeIds.has(doctor.id))
})

// Watch for dialog visibility to reset form
watch(
  () => props.visible,
  newValue => {
    if (newValue) {
      resetForm()
    }
  }
)

function resetForm() {
  selectedPhysicianId.value = null
  reason.value = ''
  requestedDate.value = new Date()
  errors.value = {}
}

function getDoctorLabel(doctor: Doctor): string {
  return `${doctor.salutation || ''} ${doctor.firstName || ''} ${doctor.lastName || ''}`.trim()
}

function validate(): boolean {
  errors.value = {}

  if (!selectedPhysicianId.value) {
    errors.value.physician = t('validation.admission.consultingPhysicians.physicianId.required')
  }

  if (reason.value.length > 500) {
    errors.value.reason = t('validation.admission.consultingPhysicians.reason.max')
  }

  return Object.keys(errors.value).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  try {
    await admissionStore.addConsultingPhysician(props.admissionId, {
      physicianId: selectedPhysicianId.value!,
      reason: reason.value || null,
      requestedDate: requestedDate.value ? requestedDate.value.toISOString().split('T')[0] : null
    })
    emit('added')
    closeDialog()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function closeDialog() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    :header="t('admission.consultingPhysicians.add')"
    :modal="true"
    :closable="!loading"
    :style="{ width: '500px' }"
    :breakpoints="{ '640px': '90vw' }"
  >
    <div class="dialog-content">
      <!-- Physician Select -->
      <div class="field">
        <label for="physician">{{ t('admission.consultingPhysicians.physician') }} *</label>
        <Select
          id="physician"
          v-model="selectedPhysicianId"
          :options="availableDoctors"
          optionLabel="id"
          optionValue="id"
          :placeholder="t('admission.consultingPhysicians.selectPhysician')"
          :disabled="loading"
          class="w-full"
          :invalid="!!errors.physician"
        >
          <template #value="{ value }">
            <span v-if="value">
              {{ getDoctorLabel(availableDoctors.find(d => d.id === value)!) }}
            </span>
            <span v-else class="placeholder">
              {{ t('admission.consultingPhysicians.selectPhysician') }}
            </span>
          </template>
          <template #option="{ option }">
            {{ getDoctorLabel(option) }}
          </template>
        </Select>
        <Message v-if="errors.physician" severity="error" :closable="false" class="field-error">
          {{ errors.physician }}
        </Message>
      </div>

      <!-- Reason Textarea -->
      <div class="field">
        <label for="reason">{{ t('admission.consultingPhysicians.reason') }}</label>
        <Textarea
          id="reason"
          v-model="reason"
          :placeholder="t('admission.consultingPhysicians.reasonPlaceholder')"
          :disabled="loading"
          rows="3"
          class="w-full"
          :maxlength="500"
          :invalid="!!errors.reason"
        />
        <small class="char-count">{{ reason.length }}/500</small>
        <Message v-if="errors.reason" severity="error" :closable="false" class="field-error">
          {{ errors.reason }}
        </Message>
      </div>

      <!-- Requested Date -->
      <div class="field">
        <label for="requestedDate">{{ t('admission.consultingPhysicians.requestedDate') }}</label>
        <DatePicker
          id="requestedDate"
          v-model="requestedDate"
          :disabled="loading"
          dateFormat="yy-mm-dd"
          showIcon
          class="w-full"
        />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <Button
          :label="t('common.cancel')"
          severity="secondary"
          :disabled="loading"
          @click="closeDialog"
        />
        <Button
          :label="t('common.save')"
          :loading="loading"
          @click="handleSubmit"
        />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field label {
  font-weight: 500;
}

.w-full {
  width: 100%;
}

.placeholder {
  color: var(--p-text-muted-color);
}

.char-count {
  text-align: right;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.field-error {
  margin-top: 0.25rem;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
