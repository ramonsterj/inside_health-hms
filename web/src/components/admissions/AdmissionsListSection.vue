<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Paginator from 'primevue/paginator'
import AdmissionCardGrid from '@/components/admissions/AdmissionCardGrid.vue'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useAdmissionStore } from '@/stores/admission'
import { useAdmissionsListPreferencesStore } from '@/stores/admissionsListPreferences'
import { AdmissionStatus, AdmissionType } from '@/types/admission'

const props = withDefaults(
  defineProps<{
    statusFilter?: AdmissionStatus | null
    typeFilter?: AdmissionType | null
    emptyLabel: string
    showStatus?: boolean
  }>(),
  {
    statusFilter: null,
    typeFilter: null,
    showStatus: false
  }
)

const router = useRouter()
const { showError } = useErrorHandler()
const admissionStore = useAdmissionStore()
const preferences = useAdmissionsListPreferencesStore()

const first = ref(0)
const rows = ref(20)

watch(
  () => [props.statusFilter, props.typeFilter] as const,
  () => {
    first.value = 0
    loadAdmissions()
  },
  { immediate: true }
)

async function loadAdmissions() {
  try {
    const page = Math.floor(first.value / rows.value)
    await admissionStore.fetchAdmissions(page, rows.value, props.statusFilter, props.typeFilter)
  } catch (error) {
    showError(error)
  }
}

function onPageChange(event?: { first: number; rows: number }) {
  if (event) {
    first.value = event.first
    rows.value = event.rows
  }
  loadAdmissions()
}

function viewAdmission(id: number) {
  router.push({ name: 'admission-detail', params: { id } })
}

defineExpose({
  refresh: loadAdmissions
})
</script>

<template>
  <AdmissionCardGrid
    :admissions="admissionStore.admissions"
    :primary-group-by="preferences.primaryGroupBy"
    :secondary-group-by="preferences.secondaryGroupBy"
    :loading="admissionStore.loading"
    :show-status="showStatus"
    :empty-label="emptyLabel"
    @view="viewAdmission"
  />
  <Paginator
    v-if="admissionStore.totalAdmissions > rows"
    class="cards-paginator"
    :first="first"
    :rows="rows"
    :totalRecords="admissionStore.totalAdmissions"
    :rowsPerPageOptions="[10, 20, 50]"
    @page="onPageChange"
  />
</template>

<style scoped>
.cards-paginator {
  margin-top: 1rem;
}
</style>
