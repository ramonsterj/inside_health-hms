<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { useBillingStore } from '@/stores/billing'

const props = defineProps<{
  visible: boolean
  admissionId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  generated: []
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const billingStore = useBillingStore()

const loading = ref(false)

async function onConfirm() {
  loading.value = true
  try {
    await billingStore.generateInvoice(props.admissionId)
    showSuccess('billing.invoiceGenerated')
    emit('generated')
    emit('update:visible', false)
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function onHide() {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog
    :visible="visible"
    @update:visible="onHide"
    :header="t('billing.generateInvoice')"
    modal
    :style="{ width: '450px' }"
  >
    <p>{{ t('billing.generateInvoiceConfirmation') }}</p>

    <template #footer>
      <Button
        :label="t('common.cancel')"
        severity="secondary"
        outlined
        @click="onHide"
      />
      <Button
        :label="t('billing.generate')"
        :loading="loading"
        @click="onConfirm"
      />
    </template>
  </Dialog>
</template>
