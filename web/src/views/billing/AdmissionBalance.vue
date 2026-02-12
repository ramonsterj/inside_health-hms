<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { useBillingStore } from '@/stores/billing'
import { getChargeTypeSeverity } from '@/types/billing'
import { formatCurrency } from '@/utils/format'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const { showError } = useErrorHandler()
const billingStore = useBillingStore()

const admissionId = computed(() => Number(route.params.id))

onMounted(async () => {
  try {
    await billingStore.fetchBalance(admissionId.value)
  } catch (error) {
    showError(error)
  }
})
</script>

<template>
  <div class="balance-page">
    <div class="page-header">
      <div class="header-left">
        <Button
          icon="pi pi-arrow-left"
          text
          rounded
          @click="router.push({ name: 'admission-detail', params: { id: admissionId } })"
        />
        <h1 class="page-title">{{ t('billing.balance') }}</h1>
      </div>
    </div>

    <div v-if="billingStore.loading" class="loading-container">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
    </div>

    <template v-else-if="billingStore.balance">
      <Card class="total-card">
        <template #content>
          <div class="total-header">
            <div>
              <h2>{{ billingStore.balance.patientName }}</h2>
              <p class="admission-date">
                {{ t('billing.admittedOn') }}: {{ billingStore.balance.admissionDate }}
              </p>
            </div>
            <div class="total-amount">
              <span class="total-label">{{ t('billing.totalBalance') }}</span>
              <span class="total-value">
                {{ formatCurrency(billingStore.balance.totalBalance) }}
              </span>
            </div>
          </div>
        </template>
      </Card>

      <div
        v-if="billingStore.balance.dailyBreakdown.length === 0"
        class="empty-message"
      >
        {{ t('billing.noCharges') }}
      </div>

      <div
        v-for="day in billingStore.balance.dailyBreakdown"
        :key="day.date"
        class="day-card"
      >
        <Card>
          <template #title>
            <div class="day-header">
              <span>{{ day.date }}</span>
              <div class="day-totals">
                <span class="day-total">
                  {{ t('billing.dailyTotal') }}: {{ formatCurrency(day.dailyTotal) }}
                </span>
                <span class="cumulative-total">
                  {{ t('billing.cumulative') }}: {{ formatCurrency(day.cumulativeTotal) }}
                </span>
              </div>
            </div>
          </template>
          <template #content>
            <div
              v-for="charge in day.charges"
              :key="charge.id"
              class="charge-row"
            >
              <div class="charge-info">
                <Tag
                  :value="t(`billing.chargeTypes.${charge.chargeType}`)"
                  :severity="getChargeTypeSeverity(charge.chargeType)"
                />
                <span class="charge-description">{{ charge.description }}</span>
              </div>
              <div class="charge-amount" :class="{ negative: charge.totalAmount < 0 }">
                <span v-if="charge.quantity > 1" class="charge-qty">
                  {{ charge.quantity }} x {{ formatCurrency(charge.unitPrice) }}
                </span>
                {{ formatCurrency(charge.totalAmount) }}
              </div>
            </div>
          </template>
        </Card>
      </div>
    </template>
  </div>
</template>

<style scoped>
.balance-page {
  max-width: 900px;
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

.loading-container {
  display: flex;
  justify-content: center;
  padding: 4rem;
}

.total-card {
  margin-bottom: 1.5rem;
}

.total-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-header h2 {
  margin: 0;
}

.admission-date {
  color: var(--p-text-muted-color);
  margin: 0.25rem 0 0;
}

.total-amount {
  text-align: right;
}

.total-label {
  display: block;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.total-value {
  font-size: 1.5rem;
  font-weight: 700;
}

.empty-message {
  text-align: center;
  color: var(--p-text-muted-color);
  padding: 2rem;
}

.day-card {
  margin-bottom: 1rem;
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.day-totals {
  display: flex;
  gap: 1.5rem;
  font-size: 0.875rem;
}

.day-total {
  font-weight: 600;
}

.cumulative-total {
  color: var(--p-text-muted-color);
}

.charge-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--p-surface-border);
}

.charge-row:last-child {
  border-bottom: none;
}

.charge-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.charge-amount {
  font-weight: 500;
  text-align: right;
}

.charge-amount.negative {
  color: var(--p-red-500);
}

.charge-qty {
  display: block;
  color: var(--p-text-muted-color);
  font-size: 0.8rem;
  font-weight: normal;
}
</style>
