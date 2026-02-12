<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useBillingStore } from '@/stores/billing'
import { useAuthStore } from '@/stores/auth'
import { ChargeType, getChargeTypeSeverity } from '@/types/billing'
import { formatCurrency } from '@/utils/format'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Select from 'primevue/select'
import CreateChargeDialog from '@/components/billing/CreateChargeDialog.vue'
import CreateAdjustmentDialog from '@/components/billing/CreateAdjustmentDialog.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { showError } = useErrorHandler()
const billingStore = useBillingStore()
const authStore = useAuthStore()

const admissionId = computed(() => Number(route.params.id))
const showCreateChargeDialog = ref(false)
const showCreateAdjustmentDialog = ref(false)
const selectedChargeType = ref<ChargeType | null>(null)

const canCreate = computed(() => authStore.hasPermission('billing:create'))
const canAdjust = computed(() => authStore.hasPermission('billing:adjust'))

const chargeTypeOptions = computed(() => [
  { label: t('common.all'), value: null },
  ...Object.values(ChargeType).map((type) => ({
    label: t(`billing.chargeTypes.${type}`),
    value: type
  }))
])

const filteredCharges = computed(() => {
  if (!selectedChargeType.value) return billingStore.charges
  return billingStore.charges.filter((c) => c.chargeType === selectedChargeType.value)
})

onMounted(async () => {
  try {
    await billingStore.fetchCharges(admissionId.value)
  } catch (error) {
    showError(error)
  }
})

async function handleChargeCreated() {
  try {
    await billingStore.fetchCharges(admissionId.value)
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="charges-page">
    <div class="page-header">
      <div class="header-left">
        <Button
          icon="pi pi-arrow-left"
          text
          rounded
          @click="router.push({ name: 'admission-detail', params: { id: admissionId } })"
        />
        <h1 class="page-title">{{ t('billing.charges') }}</h1>
      </div>
      <div class="header-actions">
        <Button
          v-if="canAdjust"
          icon="pi pi-minus-circle"
          :label="t('billing.newAdjustment')"
          severity="warning"
          outlined
          @click="showCreateAdjustmentDialog = true"
        />
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('billing.newCharge')"
          @click="showCreateChargeDialog = true"
        />
      </div>
    </div>

    <div class="filter-bar">
      <Select
        v-model="selectedChargeType"
        :options="chargeTypeOptions"
        optionLabel="label"
        optionValue="value"
        :placeholder="t('billing.filterByType')"
        class="filter-select"
      />
    </div>

    <DataTable
      :value="filteredCharges"
      :loading="billingStore.loading"
      stripedRows
      responsiveLayout="scroll"
    >
      <template #empty>{{ t('billing.noCharges') }}</template>
      <Column field="chargeDate" :header="t('billing.date')" sortable />
      <Column field="chargeType" :header="t('billing.chargeType')">
        <template #body="{ data }">
          <Tag
            :value="t(`billing.chargeTypes.${data.chargeType}`)"
            :severity="getChargeTypeSeverity(data.chargeType)"
          />
        </template>
      </Column>
      <Column field="description" :header="t('billing.description')" />
      <Column field="quantity" :header="t('billing.quantity')" style="width: 80px" />
      <Column field="unitPrice" :header="t('billing.unitPrice')">
        <template #body="{ data }">{{ formatCurrency(data.unitPrice) }}</template>
      </Column>
      <Column field="totalAmount" :header="t('billing.totalAmount')">
        <template #body="{ data }">
          <span :class="{ 'text-red-500': data.totalAmount < 0 }">
            {{ formatCurrency(data.totalAmount) }}
          </span>
        </template>
      </Column>
      <Column field="invoiced" :header="t('billing.invoiced')">
        <template #body="{ data }">
          <Tag
            :value="data.invoiced ? t('common.yes') : t('common.no')"
            :severity="data.invoiced ? 'success' : 'secondary'"
          />
        </template>
      </Column>
    </DataTable>

    <CreateChargeDialog
      v-model:visible="showCreateChargeDialog"
      :admissionId="admissionId"
      @created="handleChargeCreated"
    />

    <CreateAdjustmentDialog
      v-model:visible="showCreateAdjustmentDialog"
      :admissionId="admissionId"
      @created="handleChargeCreated"
    />
  </div>
</template>

<style scoped>
.charges-page {
  max-width: 1400px;
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

.filter-bar {
  margin-bottom: 1rem;
}

.filter-select {
  min-width: 200px;
}
</style>
