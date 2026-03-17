<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Paginator from 'primevue/paginator'
import KardexPatientCard from '@/components/nursing/kardex/KardexPatientCard.vue'
import { useKardexStore } from '@/stores/kardex'
import { AdmissionType } from '@/types/admission'

const { t } = useI18n()
const { showError } = useErrorHandler()
const kardexStore = useKardexStore()

const first = ref(0)
const rows = ref(20)
const typeFilter = ref<AdmissionType | null>(null)
const searchQuery = ref('')
let searchTimeout: ReturnType<typeof setTimeout> | null = null

const typeOptions = computed(() => [
  { label: t('kardex.allTypes'), value: null },
  { label: t('admission.types.HOSPITALIZATION'), value: AdmissionType.HOSPITALIZATION },
  { label: t('admission.types.AMBULATORY'), value: AdmissionType.AMBULATORY },
  { label: t('admission.types.ELECTROSHOCK_THERAPY'), value: AdmissionType.ELECTROSHOCK_THERAPY },
  { label: t('admission.types.KETAMINE_INFUSION'), value: AdmissionType.KETAMINE_INFUSION },
  { label: t('admission.types.EMERGENCY'), value: AdmissionType.EMERGENCY }
])

async function loadKardex() {
  try {
    const page = Math.floor(first.value / rows.value)
    await kardexStore.fetchSummaries(page, rows.value, typeFilter.value, searchQuery.value || null)
  } catch (error) {
    showError(error)
  }
}

function onFilterChange() {
  first.value = 0
  loadKardex()
}

function onSearchInput() {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    first.value = 0
    loadKardex()
  }, 300)
}

function onPageChange(event: { first: number; rows: number }) {
  first.value = event.first
  rows.value = event.rows
  loadKardex()
}

async function onActionCompleted(admissionId: number) {
  try {
    await kardexStore.refreshSingleAdmission(admissionId)
  } catch (error) {
    showError(error)
  }
}

watch(typeFilter, onFilterChange)

onMounted(() => {
  loadKardex()
  kardexStore.startAutoRefresh()
})

onUnmounted(() => {
  kardexStore.stopAutoRefresh()
  if (searchTimeout) clearTimeout(searchTimeout)
})
</script>

<template>
  <div class="nursing-kardex">
    <div class="page-header">
      <div>
        <h1>{{ t('kardex.title') }}</h1>
        <p class="subtitle">{{ t('kardex.subtitle') }}</p>
      </div>
      <div class="header-actions">
        <Button
          icon="pi pi-refresh"
          :label="t('kardex.refresh')"
          severity="secondary"
          outlined
          :loading="kardexStore.loading"
          @click="loadKardex"
        />
      </div>
    </div>

    <div class="filter-bar">
      <InputText
        v-model="searchQuery"
        :placeholder="t('kardex.searchPlaceholder')"
        class="search-input"
        @input="onSearchInput"
      />
      <Select
        v-model="typeFilter"
        :options="typeOptions"
        optionLabel="label"
        optionValue="value"
        :placeholder="t('kardex.filterByType')"
        class="type-filter"
      />
    </div>

    <div v-if="kardexStore.summaries.length === 0 && !kardexStore.loading" class="empty-state">
      {{ t('kardex.noAdmissions') }}
    </div>

    <div v-else class="kardex-cards">
      <KardexPatientCard
        v-for="summary in kardexStore.summaries"
        :key="summary.admissionId"
        :summary="summary"
        @action-completed="onActionCompleted"
      />
    </div>

    <Paginator
      v-if="kardexStore.totalElements > rows"
      :first="first"
      :rows="rows"
      :totalRecords="kardexStore.totalElements"
      :rowsPerPageOptions="[10, 20, 50]"
      @page="onPageChange"
    />
  </div>
</template>

<style scoped>
.nursing-kardex {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.page-header h1 {
  margin: 0;
  font-size: 1.75rem;
}

.subtitle {
  margin: 0.25rem 0 0 0;
  color: var(--p-text-muted-color);
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.filter-bar {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.type-filter {
  width: 220px;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--p-text-muted-color);
  font-size: 1.1rem;
}

.kardex-cards {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}
</style>
