<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import RadioButton from 'primevue/radiobutton'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { EmergencyAuthorizationReason } from '@/types/medicalRecord'

const props = defineProps<{
  visible: boolean
  admissionId: number
  orderId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  authorized: []
}>()

const { t } = useI18n()
const orderStore = useMedicalOrderStore()
const { showError, showSuccess } = useErrorHandler()

const REASON_OPTIONS = [
  EmergencyAuthorizationReason.PATIENT_IN_CRISIS,
  EmergencyAuthorizationReason.AFTER_HOURS_NO_ADMIN,
  EmergencyAuthorizationReason.FAMILY_UNREACHABLE,
  EmergencyAuthorizationReason.OTHER
] as const

const reason = ref<EmergencyAuthorizationReason | null>(null)
const reasonNote = ref('')

const noteRequired = computed(() => reason.value === EmergencyAuthorizationReason.OTHER)
const submitDisabled = computed(() => {
  if (reason.value === null) return true
  if (noteRequired.value && reasonNote.value.trim() === '') return true
  return false
})

watch(
  () => props.visible,
  (open) => {
    if (open) {
      reason.value = null
      reasonNote.value = ''
    }
  }
)

async function submit() {
  if (reason.value === null) return
  try {
    await orderStore.emergencyAuthorize(props.admissionId, props.orderId, {
      reason: reason.value,
      reasonNote: reasonNote.value.trim() || null
    })
    showSuccess('medicalRecord.medicalOrder.transitions.authorized')
    emit('update:visible', false)
    emit('authorized')
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="emit('update:visible', $event)"
    modal
    :header="t('medicalRecord.medicalOrder.emergencyAuthorize.title')"
    :style="{ width: '36rem' }"
  >
    <div class="emergency-dialog-body">
      <p class="warning">
        <i class="pi pi-exclamation-triangle"></i>
        {{ t('medicalRecord.medicalOrder.emergencyAuthorize.warning') }}
      </p>

      <div class="reason-group">
        <label class="group-label">
          {{ t('medicalRecord.medicalOrder.emergencyAuthorize.reasonLabel') }}
        </label>
        <div v-for="opt in REASON_OPTIONS" :key="opt" class="reason-option">
          <RadioButton :inputId="`reason-${opt}`" :value="opt" v-model="reason" name="reason" />
          <label :for="`reason-${opt}`">
            {{ t(`medicalRecord.medicalOrder.emergencyAuthorize.reasons.${opt}`) }}
          </label>
        </div>
      </div>

      <div class="note-group">
        <label for="emergency-reason-note">
          {{ t('medicalRecord.medicalOrder.emergencyAuthorize.noteLabel') }}
          <span v-if="noteRequired" class="required-marker">*</span>
        </label>
        <Textarea
          id="emergency-reason-note"
          v-model="reasonNote"
          rows="3"
          :placeholder="t('medicalRecord.medicalOrder.emergencyAuthorize.notePlaceholder')"
          maxlength="500"
          autoResize
        />
      </div>
    </div>
    <template #footer>
      <Button text :label="t('common.cancel')" @click="emit('update:visible', false)" />
      <Button
        severity="warn"
        icon="pi pi-bolt"
        :label="t('medicalRecord.medicalOrder.actions.emergencyAuthorize')"
        :disabled="submitDisabled"
        @click="submit"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.emergency-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.warning {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--p-orange-50);
  color: var(--p-orange-800);
  border-radius: var(--p-border-radius);
  font-size: 0.875rem;
  margin: 0;
}

.warning i {
  font-size: 1rem;
  margin-top: 0.125rem;
}

.reason-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.group-label {
  font-weight: 500;
}

.reason-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.note-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.note-group label {
  font-weight: 500;
}

.required-marker {
  color: var(--p-red-600);
  margin-left: 0.125rem;
}
</style>
