<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { MedicalOrderStatus, MedicalOrderCategory } from '@/types/medicalRecord'
import type { MedicalOrderResponse } from '@/types/medicalRecord'
import { useAuthStore } from '@/stores/auth'
import MedicationAdministrationDialog from './MedicationAdministrationDialog.vue'
import MedicationAdministrationHistory from './MedicationAdministrationHistory.vue'

const props = defineProps<{
  order: MedicalOrderResponse
  canEdit: boolean
}>()

const emit = defineEmits<{
  edit: []
  discontinue: []
}>()

const { t } = useI18n()
const confirm = useConfirm()
const authStore = useAuthStore()

const showAdministrationDialog = ref(false)
const showHistory = ref(false)
const historyRef = ref<InstanceType<typeof MedicationAdministrationHistory> | null>(null)

const isActive = computed(() => props.order.status === MedicalOrderStatus.ACTIVE)
const isMedicationCategory = computed(
  () => props.order.category === MedicalOrderCategory.MEDICAMENTOS
)
const canAdminister = computed(
  () =>
    authStore.hasPermission('medication-administration:create') &&
    isMedicationCategory.value &&
    isActive.value &&
    props.order.inventoryItemId !== null
)

const statusSeverity = computed(() => (isActive.value ? 'success' : 'secondary'))

const authorName = computed(() => {
  if (!props.order.createdBy) return '-'
  const staff = props.order.createdBy
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
})

const discontinuedByName = computed(() => {
  if (!props.order.discontinuedBy) return '-'
  const staff = props.order.discontinuedBy
  const salutationLabel = staff.salutation ? t(`user.salutations.${staff.salutation}`) : ''
  const fullName = `${staff.firstName || ''} ${staff.lastName || ''}`.trim()
  return `${salutationLabel} ${fullName}`.trim() || '-'
})

function formatDate(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString()
}

function formatDateTime(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

function confirmDiscontinue() {
  confirm.require({
    message: t('medicalRecord.medicalOrder.confirmDiscontinue'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-warning',
    accept: () => emit('discontinue')
  })
}

function openAdministrationDialog() {
  showAdministrationDialog.value = true
}

function onAdministrationSaved() {
  showAdministrationDialog.value = false
  if (showHistory.value) {
    historyRef.value?.refresh()
  }
}

function toggleHistory() {
  showHistory.value = !showHistory.value
}
</script>

<template>
  <Card class="medical-order-card" :class="{ discontinued: !isActive }">
    <template #content>
      <div class="order-content">
        <!-- Header Row -->
        <div class="order-header">
          <div class="order-main-info">
            <!-- Medication Name (prominent for medication orders) -->
            <span v-if="isMedicationCategory && order.medication" class="medication-name">
              {{ order.medication }}
            </span>
            <!-- Dosage & Route -->
            <div v-if="isMedicationCategory" class="medication-details">
              <span v-if="order.dosage" class="dosage">{{ order.dosage }}</span>
              <span v-if="order.route" class="route">
                {{ t(`medicalRecord.medicalOrder.routes.${order.route}`) }}
              </span>
              <span v-if="order.frequency" class="frequency">{{ order.frequency }}</span>
            </div>
            <!-- Schedule -->
            <span v-if="order.schedule" class="schedule">{{ order.schedule }}</span>
          </div>
          <Tag
            :value="t(`medicalRecord.medicalOrder.statuses.${order.status}`)"
            :severity="statusSeverity"
          />
        </div>

        <!-- Observations -->
        <div v-if="order.observations" class="observations">
          <label>{{ t('medicalRecord.medicalOrder.fields.observations') }}:</label>
          <span>{{ order.observations }}</span>
        </div>

        <!-- Dates Row -->
        <div class="dates-row">
          <div class="date-item">
            <label>{{ t('medicalRecord.medicalOrder.fields.startDate') }}:</label>
            <span>{{ formatDate(order.startDate) }}</span>
          </div>
          <div v-if="order.endDate" class="date-item">
            <label>{{ t('medicalRecord.medicalOrder.fields.endDate') }}:</label>
            <span>{{ formatDate(order.endDate) }}</span>
          </div>
        </div>

        <!-- Created By -->
        <div class="meta-row">
          <span class="created-info">
            {{ t('medicalRecord.medicalOrder.orderedBy') }}: {{ authorName }}
            <span class="date-small">({{ formatDateTime(order.createdAt) }})</span>
          </span>
        </div>

        <!-- Discontinued Info -->
        <div v-if="!isActive && order.discontinuedAt" class="discontinued-info">
          <i class="pi pi-ban"></i>
          {{ t('medicalRecord.medicalOrder.discontinuedBy') }}: {{ discontinuedByName }}
          <span class="date-small">({{ formatDateTime(order.discontinuedAt) }})</span>
        </div>

        <!-- Actions -->
        <div class="order-actions">
          <Button
            v-if="canEdit"
            icon="pi pi-pencil"
            :label="t('common.edit')"
            text
            size="small"
            @click="emit('edit')"
          />
          <Button
            v-if="canEdit && isActive"
            icon="pi pi-ban"
            :label="t('medicalRecord.medicalOrder.discontinue')"
            text
            size="small"
            severity="warning"
            @click="confirmDiscontinue"
          />
          <Button
            v-if="canAdminister"
            icon="pi pi-plus"
            :label="t('medicationAdministration.administer')"
            text
            size="small"
            severity="success"
            @click="openAdministrationDialog"
          />
          <Button
            v-if="isMedicationCategory && order.inventoryItemId"
            icon="pi pi-history"
            :label="showHistory ? t('common.collapse') : t('medicationAdministration.history')"
            text
            size="small"
            @click="toggleHistory"
          />
        </div>

        <!-- Administration History -->
        <MedicationAdministrationHistory
          v-if="showHistory && order.inventoryItemId"
          ref="historyRef"
          :admissionId="order.admissionId"
          :orderId="order.id"
        />
      </div>
    </template>
  </Card>

  <!-- Administration Dialog -->
  <MedicationAdministrationDialog
    v-model:visible="showAdministrationDialog"
    :admissionId="order.admissionId"
    :orderId="order.id"
    :medicationName="order.medication"
    @saved="onAdministrationSaved"
  />
</template>

<style scoped>
.medical-order-card {
  background: var(--p-surface-card);
  border-left: 4px solid var(--p-green-500);
}

.medical-order-card.discontinued {
  border-left-color: var(--p-gray-400);
  opacity: 0.8;
}

.order-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.order-main-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.medication-name {
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--p-text-color);
}

.medication-details {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  font-size: 0.875rem;
}

.dosage {
  font-weight: 600;
  color: var(--p-primary-color);
}

.route {
  color: var(--p-text-muted-color);
}

.frequency {
  color: var(--p-text-color);
}

.schedule {
  font-size: 0.875rem;
  color: var(--p-text-muted-color);
}

.observations {
  padding: 0.5rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
  font-size: 0.875rem;
}

.observations label {
  font-weight: 500;
  margin-right: 0.25rem;
}

.dates-row {
  display: flex;
  gap: 1.5rem;
  font-size: 0.875rem;
}

.date-item label {
  font-weight: 500;
  margin-right: 0.25rem;
  color: var(--p-text-muted-color);
}

.meta-row {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
}

.date-small {
  font-size: 0.7rem;
  margin-left: 0.25rem;
}

.discontinued-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--p-orange-600);
  padding: 0.5rem;
  background: var(--p-orange-50);
  border-radius: var(--p-border-radius);
}

.discontinued-info i {
  font-size: 0.875rem;
}

.order-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
