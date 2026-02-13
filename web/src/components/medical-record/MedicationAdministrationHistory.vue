<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { useMedicationAdministrationStore } from '@/stores/medicationAdministration'
import { useErrorHandler } from '@/composables/useErrorHandler'
import MedicationAdministrationBadge from './MedicationAdministrationBadge.vue'

const props = defineProps<{
  admissionId: number
  orderId: number
}>()

const { t } = useI18n()
const { showError } = useErrorHandler()
const administrationStore = useMedicationAdministrationStore()

const page = ref(0)
const pageSize = ref(20)

const paginatorRows = computed(() => pageSize.value)
const first = computed(() => page.value * pageSize.value)

onMounted(() => {
  loadAdministrations()
})

watch([() => props.orderId, () => props.admissionId], () => {
  page.value = 0
  loadAdministrations()
})

async function loadAdministrations() {
  try {
    await administrationStore.fetchAdministrations(
      props.admissionId,
      props.orderId,
      page.value,
      pageSize.value
    )
  } catch (error) {
    showError(error)
  }
}

function onPageChange(event: { page: number; rows: number }) {
  page.value = event.page
  pageSize.value = event.rows
  loadAdministrations()
}

function formatDateTime(dateString: string | null): string {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString()
}

defineExpose({ refresh: loadAdministrations })
</script>

<template>
  <div class="administration-history">
    <h4 class="history-title">{{ t('medicationAdministration.history') }}</h4>

    <DataTable
      :value="administrationStore.administrations"
      :loading="administrationStore.loading"
      lazy
      :paginator="administrationStore.totalElements > pageSize"
      :rows="paginatorRows"
      :first="first"
      :totalRecords="administrationStore.totalElements"
      @page="onPageChange"
      dataKey="id"
      stripedRows
    >
      <template #empty>
        <div class="text-center p-4">{{ t('medicationAdministration.empty') }}</div>
      </template>

      <Column :header="t('medicationAdministration.administeredAt')" style="width: 180px">
        <template #body="{ data }">
          {{ formatDateTime(data.administeredAt) }}
        </template>
      </Column>

      <Column :header="t('medicationAdministration.status')" style="width: 120px">
        <template #body="{ data }">
          <MedicationAdministrationBadge :status="data.status" />
        </template>
      </Column>

      <Column :header="t('medicationAdministration.notes')">
        <template #body="{ data }">
          {{ data.notes || '-' }}
        </template>
      </Column>

      <Column :header="t('medicationAdministration.administeredBy')" style="width: 180px">
        <template #body="{ data }">
          {{ data.administeredByName || '-' }}
        </template>
      </Column>

      <Column :header="t('medicationAdministration.billable')" style="width: 100px">
        <template #body="{ data }">
          <Tag
            :value="data.billable ? t('common.yes') : t('common.no')"
            :severity="data.billable ? 'success' : 'secondary'"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.administration-history {
  margin-top: 1rem;
  padding: 1rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
}

.history-title {
  margin: 0 0 0.75rem 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--p-text-color);
}
</style>
