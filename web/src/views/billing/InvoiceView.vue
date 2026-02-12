<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useBillingStore } from '@/stores/billing'
import { useAdmissionStore } from '@/stores/admission'
import { useAuthStore } from '@/stores/auth'
import { AdmissionStatus } from '@/types/admission'
import Card from 'primevue/card'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Message from 'primevue/message'
import GenerateInvoiceDialog from '@/components/billing/GenerateInvoiceDialog.vue'
import { formatCurrency } from '@/utils/format'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { showError } = useErrorHandler()
const billingStore = useBillingStore()
const admissionStore = useAdmissionStore()
const authStore = useAuthStore()

const admissionId = computed(() => Number(route.params.id))
const showGenerateDialog = ref(false)
const invoiceNotFound = ref(false)

const canGenerate = computed(() => authStore.hasPermission('invoice:create'))
const isDischarged = computed(
  () => admissionStore.currentAdmission?.status === AdmissionStatus.DISCHARGED
)

onMounted(async () => {
  const admissionPromise =
    admissionStore.currentAdmission?.id !== admissionId.value
      ? admissionStore.fetchAdmission(admissionId.value).catch(() => {})
      : Promise.resolve()

  try {
    await billingStore.fetchInvoice(admissionId.value)
  } catch (error: unknown) {
    const axiosError = error as { response?: { status?: number } }
    if (axiosError.response?.status === 404) {
      invoiceNotFound.value = true
    } else {
      showError(error)
    }
  }

  await admissionPromise
})

async function handleInvoiceGenerated() {
  invoiceNotFound.value = false
  try {
    await billingStore.fetchInvoice(admissionId.value)
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="invoice-page">
    <div class="page-header">
      <div class="header-left">
        <Button
          icon="pi pi-arrow-left"
          text
          rounded
          @click="router.push({ name: 'admission-detail', params: { id: admissionId } })"
        />
        <h1 class="page-title">{{ t('billing.invoice') }}</h1>
      </div>
      <div class="header-actions" v-if="invoiceNotFound && canGenerate">
        <span v-tooltip.top="!isDischarged ? t('billing.invoiceRequiresDischarged') : undefined">
          <Button
            icon="pi pi-file"
            :label="t('billing.generateInvoice')"
            :disabled="!isDischarged"
            @click="showGenerateDialog = true"
          />
        </span>
      </div>
    </div>

    <div v-if="billingStore.loading" class="loading-container">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
    </div>

    <Message v-else-if="invoiceNotFound" severity="info" :closable="false">
      {{ t('billing.noInvoice') }}
    </Message>

    <template v-else-if="billingStore.invoice">
      <Card class="invoice-card">
        <template #title>
          <div class="invoice-header">
            <div>
              <h2>{{ billingStore.invoice.invoiceNumber }}</h2>
              <p class="patient-name">{{ billingStore.invoice.patientName }}</p>
            </div>
            <div class="invoice-total">
              <span class="total-label">{{ t('billing.totalAmount') }}</span>
              <span class="total-value">
                {{ formatCurrency(billingStore.invoice.totalAmount) }}
              </span>
            </div>
          </div>
        </template>
        <template #content>
          <div class="invoice-details">
            <div class="detail-row">
              <span class="detail-label">{{ t('billing.admissionDate') }}</span>
              <span>{{ billingStore.invoice.admissionDate }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">{{ t('billing.dischargeDate') }}</span>
              <span>{{ billingStore.invoice.dischargeDate || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">{{ t('billing.chargeCount') }}</span>
              <span>{{ billingStore.invoice.chargeCount }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">{{ t('billing.generatedAt') }}</span>
              <span>{{
                billingStore.invoice.generatedAt
                  ? new Date(billingStore.invoice.generatedAt).toLocaleString()
                  : '-'
              }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">{{ t('billing.generatedBy') }}</span>
              <span>{{ billingStore.invoice.generatedByName || '-' }}</span>
            </div>
          </div>
        </template>
      </Card>

      <Card class="summary-card">
        <template #title>{{ t('billing.chargeSummary') }}</template>
        <template #content>
          <DataTable :value="billingStore.invoice.chargeSummary" stripedRows>
            <Column field="chargeType" :header="t('billing.chargeType')">
              <template #body="{ data }">
                {{ t(`billing.chargeTypes.${data.chargeType}`) }}
              </template>
            </Column>
            <Column field="count" :header="t('billing.count')" />
            <Column field="subtotal" :header="t('billing.subtotal')">
              <template #body="{ data }">
                <span :class="{ 'text-red-500': data.subtotal < 0 }">
                  {{ formatCurrency(data.subtotal) }}
                </span>
              </template>
            </Column>
          </DataTable>
        </template>
      </Card>
    </template>

    <GenerateInvoiceDialog
      v-model:visible="showGenerateDialog"
      :admissionId="admissionId"
      @generated="handleInvoiceGenerated"
    />
  </div>
</template>

<style scoped>
.invoice-page {
  max-width: 900px;
  margin: 0 auto;
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
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 4rem;
}

.invoice-card {
  margin-bottom: 1rem;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.invoice-header h2 {
  margin: 0;
}

.patient-name {
  color: var(--p-text-muted-color);
  margin: 0.25rem 0 0;
}

.invoice-total {
  text-align: right;
}

.total-label {
  display: block;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.total-value {
  font-size: 1.5rem;
  font-weight: 700;
}

.invoice-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--p-surface-border);
}

.detail-label {
  font-weight: 500;
  color: var(--p-text-muted-color);
}

.summary-card {
  margin-bottom: 1rem;
}
</style>
