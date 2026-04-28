<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useErrorHandler } from '@/composables/useErrorHandler'

const props = defineProps<{
  visible: boolean
  admissionId: number
  orderId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  rejected: []
}>()

const { t } = useI18n()
const orderStore = useMedicalOrderStore()
const { showError, showSuccess } = useErrorHandler()

const reason = ref('')

watch(
  () => props.visible,
  (open) => {
    if (open) reason.value = ''
  }
)

async function submit() {
  try {
    await orderStore.rejectMedicalOrder(props.admissionId, props.orderId, {
      reason: reason.value || null
    })
    showSuccess('medicalRecord.medicalOrder.transitions.rejected')
    emit('update:visible', false)
    emit('rejected')
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
    :header="t('medicalRecord.medicalOrder.confirmReject')"
    :style="{ width: '32rem' }"
  >
    <div class="reject-dialog-body">
      <label for="medical-order-reject-reason">
        {{ t('medicalRecord.medicalOrder.rejectReason') }}
      </label>
      <Textarea
        id="medical-order-reject-reason"
        v-model="reason"
        rows="3"
        :placeholder="t('medicalRecord.medicalOrder.rejectReasonPlaceholder')"
        maxlength="500"
        autoResize
      />
    </div>
    <template #footer>
      <Button text :label="t('common.cancel')" @click="emit('update:visible', false)" />
      <Button
        severity="danger"
        :label="t('medicalRecord.medicalOrder.actions.reject')"
        @click="submit"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.reject-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.reject-dialog-body label {
  font-weight: 500;
}
</style>
