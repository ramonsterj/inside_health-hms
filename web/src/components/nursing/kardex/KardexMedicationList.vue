<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import MedicationAdministrationDialog from '@/components/medical-record/MedicationAdministrationDialog.vue'
import { formatShortDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import type { KardexMedicationSummary } from '@/types'

const props = defineProps<{
  medications: KardexMedicationSummary[]
  admissionId: number
}>()

const emit = defineEmits<{
  actionCompleted: []
}>()

const { t, locale } = useI18n()
const authStore = useAuthStore()

const adminDialogVisible = ref(false)
const selectedOrderId = ref(0)
const selectedMedicationName = ref<string | null>(null)

const canAdminister = authStore.hasPermission('medication-administration:create')

function openAdministerDialog(med: KardexMedicationSummary) {
  selectedOrderId.value = med.orderId
  selectedMedicationName.value = med.medication
  adminDialogVisible.value = true
}

function onAdministered() {
  emit('actionCompleted')
}

function getStatusSeverity(status: string): 'success' | 'danger' | 'warn' | 'info' {
  switch (status) {
    case 'GIVEN':
      return 'success'
    case 'MISSED':
      return 'danger'
    case 'REFUSED':
      return 'warn'
    case 'HELD':
      return 'info'
    default:
      return 'info'
  }
}
</script>

<template>
  <div class="kardex-medications">
    <h4>{{ t('kardex.medications.title') }}</h4>

    <div v-if="props.medications.length === 0" class="empty-state">
      {{ t('kardex.medications.empty') }}
    </div>

    <div v-else class="medication-list">
      <div v-for="med in props.medications" :key="med.orderId" class="medication-item">
        <div class="med-info">
          <div class="med-name">
            <strong>{{ med.medication || '-' }}</strong>
            <span v-if="med.dosage" class="med-dosage">{{ med.dosage }}</span>
            <Tag v-if="med.route" :value="med.route" severity="info" class="med-route" />
          </div>
          <div class="med-details">
            <span v-if="med.frequency"
              >{{ t('kardex.medications.frequency') }}: {{ med.frequency }}</span
            >
            <span v-if="med.schedule">
              &middot; {{ t('kardex.medications.schedule') }}: {{ med.schedule }}</span
            >
          </div>
          <div v-if="med.inventoryItemName" class="med-inventory">
            {{ med.inventoryItemName }}
          </div>
          <div class="med-last-admin">
            <template v-if="med.lastAdministration">
              <Tag
                :value="t(`medicationAdministration.statuses.${med.lastAdministration.status}`)"
                :severity="getStatusSeverity(med.lastAdministration.status)"
                class="admin-status"
              />
              <span class="admin-time">
                {{ formatShortDateTime(med.lastAdministration.administeredAt, locale) }}
              </span>
              <span v-if="med.lastAdministration.administeredByName" class="admin-by">
                &middot; {{ med.lastAdministration.administeredByName }}
              </span>
            </template>
            <span v-else class="never-administered">
              {{ t('kardex.medications.neverAdministered') }}
            </span>
          </div>
        </div>
        <div class="med-actions">
          <Button
            v-if="canAdminister"
            :label="t('kardex.medications.administer')"
            icon="pi pi-check"
            size="small"
            severity="success"
            outlined
            @click="openAdministerDialog(med)"
          />
        </div>
      </div>
    </div>

    <MedicationAdministrationDialog
      :visible="adminDialogVisible"
      :admissionId="props.admissionId"
      :orderId="selectedOrderId"
      :medicationName="selectedMedicationName"
      @update:visible="adminDialogVisible = $event"
      @saved="onAdministered"
    />
  </div>
</template>

<style scoped>
.kardex-medications h4 {
  margin: 0 0 0.75rem 0;
  font-size: 1rem;
}

.empty-state {
  color: var(--p-text-muted-color);
  font-style: italic;
  padding: 0.5rem 0;
}

.medication-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.medication-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 0.75rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: var(--p-border-radius);
  gap: 1rem;
}

.med-info {
  flex: 1;
  min-width: 0;
}

.med-name {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.med-dosage {
  color: var(--p-text-muted-color);
}

.med-details,
.med-inventory,
.med-last-admin {
  margin-top: 0.25rem;
  font-size: 0.875rem;
  color: var(--p-text-muted-color);
}

.med-inventory {
  font-style: italic;
}

.med-last-admin {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.never-administered {
  font-style: italic;
}

.med-actions {
  flex-shrink: 0;
}
</style>
