<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import { usePharmacyStore } from '@/stores/pharmacy'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import MedicationFormDialog from '@/components/pharmacy/MedicationFormDialog.vue'
import { MedicationSection, MedicationReviewStatus } from '@/types/pharmacy'

const { t } = useI18n()
const router = useRouter()
const pharmacyStore = usePharmacyStore()
const authStore = useAuthStore()
const { showError } = useErrorHandler()

const canCreate = computed(() => authStore.hasPermission('medication:create'))
const canExpiryReport = computed(() => authStore.hasPermission('medication:expiry-report'))

const sectionTab = ref<MedicationSection | 'ALL'>('ALL')
const controlled = ref<boolean | null>(null)
const search = ref('')
const first = ref(0)
const rows = ref(20)
const dialogVisible = ref(false)

const controlledOptions = [
  { label: t('common.all'), value: null },
  { label: t('common.yes'), value: true },
  { label: t('common.no'), value: false }
]

onMounted(() => load())

async function load() {
  try {
    const page = Math.floor(first.value / rows.value)
    await pharmacyStore.fetchMedications(page, rows.value, {
      section: sectionTab.value === 'ALL' ? undefined : sectionTab.value,
      controlled: controlled.value ?? undefined,
      search: search.value || undefined
    })
  } catch (e) {
    showError(e)
  }
}

function onFilterChange() {
  first.value = 0
  load()
}

function viewMedication(id: number) {
  router.push({ name: 'pharmacy-medication-detail', params: { id } })
}
</script>

<template>
  <div class="pharmacy-list-page">
    <div class="page-header">
      <h1 class="page-title">{{ t('pharmacy.title') }}</h1>
      <div class="header-actions">
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('pharmacy.medication.new')"
          @click="dialogVisible = true"
        />
        <Button
          v-if="canExpiryReport"
          icon="pi pi-calendar-clock"
          :label="t('pharmacy.expiry.dashboard')"
          severity="secondary"
          outlined
          @click="router.push({ name: 'pharmacy-expiry-report' })"
        />
      </div>
    </div>

    <Card>
      <template #content>
        <Tabs v-model:value="sectionTab" @update:value="onFilterChange">
          <TabList>
            <Tab value="ALL">{{ t('common.all') }}</Tab>
            <Tab v-for="s in Object.values(MedicationSection)" :key="s" :value="s">
              {{ t(`pharmacy.section.${s}`) }}
            </Tab>
          </TabList>
        </Tabs>

        <div class="filters">
          <Select
            v-model="controlled"
            :options="controlledOptions"
            optionLabel="label"
            optionValue="value"
            :placeholder="t('pharmacy.medication.controlled')"
            @change="onFilterChange"
          />
          <InputText
            v-model="search"
            :placeholder="t('pharmacy.medication.searchPlaceholder')"
            @keyup.enter="onFilterChange"
          />
          <Button icon="pi pi-search" severity="secondary" outlined @click="onFilterChange" />
        </div>

        <DataTable
          :value="pharmacyStore.items"
          :loading="pharmacyStore.loading"
          :paginator="true"
          v-model:rows="rows"
          v-model:first="first"
          :totalRecords="pharmacyStore.totalItems"
          :lazy="true"
          @page="load"
          :rowsPerPageOptions="[10, 20, 50]"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">{{ t('pharmacy.medication.empty') }}</div>
          </template>
          <Column field="sku" :header="t('pharmacy.medication.sku')" style="width: 80px" />
          <Column field="genericName" :header="t('pharmacy.medication.genericName')" />
          <Column field="commercialName" :header="t('pharmacy.medication.commercialName')" />
          <Column field="strength" :header="t('pharmacy.medication.strength')" />
          <Column :header="t('pharmacy.medication.dosageForm')">
            <template #body="{ data }">
              {{ t(`pharmacy.dosageForm.${data.dosageForm}`) }}
            </template>
          </Column>
          <Column
            field="quantity"
            :header="t('pharmacy.medication.quantity')"
            style="width: 80px"
          />
          <Column :header="t('pharmacy.medication.controlled')" style="width: 100px">
            <template #body="{ data }">
              <Tag v-if="data.controlled" severity="warn" :value="t('common.yes')" />
            </template>
          </Column>
          <Column :header="t('pharmacy.medication.reviewStatus')" style="width: 140px">
            <template #body="{ data }">
              <Tag
                v-if="data.reviewStatus === MedicationReviewStatus.NEEDS_REVIEW"
                severity="warn"
                :value="t('pharmacy.medication.needsReview')"
              />
            </template>
          </Column>
          <Column :header="t('common.actions')" style="width: 100px">
            <template #body="{ data }">
              <Button
                icon="pi pi-eye"
                text
                rounded
                severity="info"
                @click="viewMedication(data.itemId)"
              />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <MedicationFormDialog v-model:visible="dialogVisible" @saved="load" />
  </div>
</template>

<style scoped>
.pharmacy-list-page {
  max-width: 1400px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.page-title {
  margin: 0;
}
.header-actions {
  display: flex;
  gap: 0.5rem;
}
.filters {
  display: flex;
  gap: 0.75rem;
  margin: 1rem 0;
  align-items: center;
}
</style>
