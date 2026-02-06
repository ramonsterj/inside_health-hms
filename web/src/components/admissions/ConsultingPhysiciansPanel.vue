<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

interface ConsultingPhysician {
  id: number
  physician: {
    id: number
    salutation: string | null
    firstName: string | null
    lastName: string | null
  }
  reason: string | null
  requestedDate: string | null
  createdAt: string | null
  createdBy: { username: string } | null
}

defineProps<{
  consultingPhysicians: ConsultingPhysician[]
  canUpdate: boolean
}>()

const emit = defineEmits<{
  add: []
  remove: [id: number]
}>()

const { t } = useI18n()

function getFullName(firstName: string | null, lastName: string | null): string {
  return `${firstName || ''} ${lastName || ''}`.trim()
}

function formatDoctorName(doctor: {
  salutation: string | null
  firstName: string | null
  lastName: string | null
}): string {
  const salutationLabel = doctor.salutation ? t(`user.salutations.${doctor.salutation}`) : ''
  return `${salutationLabel} ${getFullName(doctor.firstName, doctor.lastName)}`.trim()
}

function formatDate(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleDateString()
}

function formatDateTime(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}
</script>

<template>
  <div class="consulting-physicians-panel">
    <div class="panel-header">
      <Button
        v-if="canUpdate"
        icon="pi pi-plus"
        :label="t('admission.consultingPhysicians.add')"
        @click="emit('add')"
      />
    </div>

    <div v-if="consultingPhysicians.length === 0" class="empty-section">
      <i class="pi pi-users"></i>
      <p>{{ t('admission.consultingPhysicians.empty') }}</p>
      <Button
        v-if="canUpdate"
        icon="pi pi-plus"
        :label="t('admission.consultingPhysicians.add')"
        @click="emit('add')"
      />
    </div>

    <DataTable v-else :value="consultingPhysicians" class="consulting-physicians-table">
      <Column :header="t('admission.consultingPhysicians.physician')">
        <template #body="{ data }">
          {{ formatDoctorName(data.physician) }}
        </template>
      </Column>
      <Column :header="t('admission.consultingPhysicians.reason')">
        <template #body="{ data }">
          {{ data.reason || '-' }}
        </template>
      </Column>
      <Column :header="t('admission.consultingPhysicians.requestedDate')">
        <template #body="{ data }">
          {{ formatDate(data.requestedDate) }}
        </template>
      </Column>
      <Column :header="t('admission.consultingPhysicians.addedBy')">
        <template #body="{ data }">
          <div class="added-info">
            <span>{{ data.createdBy?.username || '-' }}</span>
            <small>{{ formatDateTime(data.createdAt) }}</small>
          </div>
        </template>
      </Column>
      <Column v-if="canUpdate" :header="t('common.actions')">
        <template #body="{ data }">
          <Button
            icon="pi pi-trash"
            severity="danger"
            text
            rounded
            size="small"
            @click="emit('remove', data.id)"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.consulting-physicians-panel {
  padding: 1rem 0;
}

.panel-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 1rem;
}

.empty-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-section i {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-section p {
  margin-bottom: 1rem;
}

.consulting-physicians-table {
  margin-top: 0.5rem;
}

.added-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.added-info small {
  color: var(--p-text-muted-color);
  font-size: 0.75rem;
}
</style>
