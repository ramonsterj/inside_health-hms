<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { usePharmacyStore } from '@/stores/pharmacy'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { formatPrice } from '@/utils/format'
import MedicationFormDialog from '@/components/pharmacy/MedicationFormDialog.vue'
import LotListPanel from '@/components/pharmacy/LotListPanel.vue'
import { MedicationReviewStatus } from '@/types/pharmacy'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const pharmacyStore = usePharmacyStore()
const authStore = useAuthStore()
const { showError } = useErrorHandler()

const itemId = computed(() => Number(route.params.id))
const editVisible = ref(false)

const canUpdate = computed(() => authStore.hasPermission('medication:update'))

onMounted(() => load())

async function load() {
  try {
    await pharmacyStore.fetchMedication(itemId.value)
  } catch (e) {
    showError(e)
  }
}
</script>

<template>
  <div class="medication-detail-page">
    <div class="page-header">
      <Button icon="pi pi-arrow-left" text @click="router.back()" />
      <h1 class="page-title">{{ pharmacyStore.currentMedication?.genericName }}</h1>
      <Button
        v-if="canUpdate"
        icon="pi pi-pencil"
        :label="t('common.edit')"
        @click="editVisible = true"
      />
    </div>

    <Card v-if="pharmacyStore.currentMedication">
      <template #content>
        <div class="info-grid">
          <div>
            <strong>{{ t('pharmacy.medication.sku') }}:</strong>
            {{ pharmacyStore.currentMedication.sku || '-' }}
          </div>
          <div>
            <strong>{{ t('pharmacy.medication.commercialName') }}:</strong>
            {{ pharmacyStore.currentMedication.commercialName || '-' }}
          </div>
          <div>
            <strong>{{ t('pharmacy.medication.strength') }}:</strong>
            {{ pharmacyStore.currentMedication.strength || '-' }}
          </div>
          <div>
            <strong>{{ t('pharmacy.medication.dosageForm') }}:</strong>
            {{ t(`pharmacy.dosageForm.${pharmacyStore.currentMedication.dosageForm}`) }}
          </div>
          <div>
            <strong>{{ t('pharmacy.medication.section') }}:</strong>
            {{ t(`pharmacy.section.${pharmacyStore.currentMedication.section}`) }}
          </div>
          <div>
            <strong>{{ t('pharmacy.medication.price') }}:</strong>
            {{ formatPrice(pharmacyStore.currentMedication.price) }}
          </div>
          <div>
            <strong>{{ t('pharmacy.medication.quantityTotal') }}:</strong>
            {{ pharmacyStore.currentMedication.quantity }}
            <i
              class="pi pi-info-circle stock-hint-icon"
              v-tooltip.top="t('pharmacy.medication.quantityTotalHint')"
            />
          </div>
          <div>
            <strong>{{ t('inventory.item.restockLevel') }}:</strong>
            {{ pharmacyStore.currentMedication.restockLevel }}
          </div>
          <div v-if="pharmacyStore.currentMedication.controlled">
            <Tag severity="warn" :value="t('pharmacy.medication.controlled')" />
          </div>
          <div
            v-if="
              pharmacyStore.currentMedication.reviewStatus === MedicationReviewStatus.NEEDS_REVIEW
            "
          >
            <Tag severity="warn" :value="t('pharmacy.medication.needsReview')" />
          </div>
        </div>
      </template>
    </Card>

    <Card
      v-if="pharmacyStore.currentMedication?.warehouseStock?.length"
      class="mt-4"
    >
      <template #title>
        <span class="stock-title">{{ t('pharmacy.medication.stockByWarehouse') }}</span>
      </template>
      <template #content>
        <p class="stock-subtitle">{{ t('pharmacy.medication.stockByWarehouseHint') }}</p>
        <div class="warehouse-stock-grid">
          <div
            v-for="ws in pharmacyStore.currentMedication.warehouseStock"
            :key="ws.warehouseId"
            class="warehouse-stock-row"
            :class="{ 'is-empty': ws.quantity <= 0 }"
          >
            <span class="warehouse-name">{{ ws.warehouseName }}</span>
            <span class="warehouse-qty">{{ ws.quantity }}</span>
          </div>
        </div>
      </template>
    </Card>

    <Card v-if="pharmacyStore.currentMedication" class="mt-4">
      <template #content>
        <LotListPanel :item-id="itemId" />
      </template>
    </Card>

    <MedicationFormDialog
      v-model:visible="editVisible"
      :medication="pharmacyStore.currentMedication"
      @saved="load"
    />
  </div>
</template>

<style scoped>
.medication-detail-page {
  max-width: 1200px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}
.page-title {
  flex: 1;
  margin: 0;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}
.mt-4 {
  margin-top: 1rem;
}
.stock-hint-icon {
  margin-left: 0.35rem;
  font-size: 0.8rem;
  color: var(--text-color-secondary);
  cursor: help;
}
.stock-title {
  font-size: 1rem;
  font-weight: 600;
}
.stock-subtitle {
  margin: 0 0 0.75rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}
.warehouse-stock-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.5rem;
}
.warehouse-stock-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: var(--p-border-radius);
}
.warehouse-stock-row.is-empty .warehouse-qty {
  color: var(--p-red-500);
}
.warehouse-name {
  font-weight: 500;
}
.warehouse-qty {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}
</style>
