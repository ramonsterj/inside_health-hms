<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Select from 'primevue/select'
import { useAdmissionFilterOptions } from '@/composables/useAdmissionFilterOptions'
import { useAuthStore } from '@/stores/auth'
import { useAdmissionStore } from '@/stores/admission'
import { AdmissionStatus, AdmissionType } from '@/types/admission'
import AdmissionsListToolbar from '@/components/admissions/AdmissionsListToolbar.vue'
import AdmissionsListSection from '@/components/admissions/AdmissionsListSection.vue'

const { t } = useI18n()
const authStore = useAuthStore()
const admissionStore = useAdmissionStore()
const { typeOptions } = useAdmissionFilterOptions()

const typeFilter = ref<AdmissionType | null>(null)
const listSection = ref<InstanceType<typeof AdmissionsListSection> | null>(null)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return t('dashboard.greeting.morning')
  if (hour < 18) return t('dashboard.greeting.afternoon')
  return t('dashboard.greeting.evening')
})

const displayName = computed(() => {
  if (authStore.user?.firstName) {
    return authStore.user.firstName
  }
  return authStore.user?.email?.split('@')[0] || ''
})

function refreshAdmissions() {
  listSection.value?.refresh()
}
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <h1 class="dashboard-title">{{ greeting }}, {{ displayName }}!</h1>
      <div class="header-actions">
        <Button
          icon="pi pi-refresh"
          :label="t('common.refresh')"
          severity="secondary"
          outlined
          @click="refreshAdmissions"
          :loading="admissionStore.loading"
        />
      </div>
    </div>

    <Card class="filter-card">
      <template #content>
        <div class="filter-bar">
          <div class="filter-field">
            <label class="filter-label">{{ t('admission.type') }}</label>
            <Select
              v-model="typeFilter"
              :options="typeOptions"
              optionLabel="label"
              optionValue="value"
              style="width: 200px"
            />
          </div>
          <AdmissionsListToolbar class="filter-toolbar" />
        </div>
      </template>
    </Card>

    <Card>
      <template #content>
        <AdmissionsListSection
          ref="listSection"
          :status-filter="AdmissionStatus.ACTIVE"
          :type-filter="typeFilter"
          :empty-label="t('dashboard.noPatients')"
          show-treating-physician
        />
      </template>
    </Card>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.dashboard-title {
  margin: 0;
  font-size: 1.75rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  align-items: flex-end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.filter-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--p-text-muted-color);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.filter-toolbar {
  margin-left: auto;
}
</style>
