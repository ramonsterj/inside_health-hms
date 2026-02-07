<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { useInventoryItemStore } from '@/stores/inventoryItem'
import { useAuthStore } from '@/stores/auth'
import InventoryMovementForm from '@/components/inventory/InventoryMovementForm.vue'
import { formatPrice } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const { showError } = useErrorHandler()
const itemStore = useInventoryItemStore()
const authStore = useAuthStore()

const itemId = Number(route.params.id)
const showMovementDialog = ref(false)

const canCreateMovement = authStore.hasPermission('inventory-movement:create')

onMounted(async () => {
  await loadItem()
  await loadMovements()
})

async function loadItem() {
  try {
    await itemStore.fetchItem(itemId)
  } catch (error) {
    showError(error)
    router.push({ name: 'inventory-items' })
  }
}

async function loadMovements() {
  try {
    await itemStore.fetchMovements(itemId)
  } catch (error) {
    showError(error)
  }
}

async function onMovementCreated() {
  await loadItem()
  await loadMovements()
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

function formatUser(user: { firstName?: string; lastName?: string } | null): string {
  if (!user) return '-'
  return [user.firstName, user.lastName].filter(Boolean).join(' ')
}

function goBack() {
  router.push({ name: 'inventory-items' })
}
</script>

<template>
  <div class="item-detail-page" v-if="itemStore.currentItem">
    <div class="page-header">
      <div>
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          rounded
          @click="goBack"
        />
        <h1 class="page-title">{{ t('inventory.item.detail') }}</h1>
      </div>
    </div>

    <Card class="detail-card">
      <template #content>
        <div class="detail-grid">
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.name') }}</span>
            <span>{{ itemStore.currentItem.name }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.category') }}</span>
            <span>{{ itemStore.currentItem.category.name }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.description') }}</span>
            <span>{{ itemStore.currentItem.description || '-' }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.pricingType') }}</span>
            <Tag
              :value="t(`inventory.item.pricingTypes.${itemStore.currentItem.pricingType}`)"
              :severity="itemStore.currentItem.pricingType === 'FLAT' ? 'info' : 'warn'"
            />
          </div>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.price') }}</span>
            <span>{{ formatPrice(itemStore.currentItem.price) }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.cost') }}</span>
            <span>{{ formatPrice(itemStore.currentItem.cost) }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.quantity') }}</span>
            <span class="quantity-value">{{ itemStore.currentItem.quantity }}</span>
          </div>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.restockLevel') }}</span>
            <span>{{ itemStore.currentItem.restockLevel }}</span>
          </div>
          <template v-if="itemStore.currentItem.pricingType === 'TIME_BASED'">
            <div class="detail-field">
              <span class="detail-label">{{ t('inventory.item.timeUnit') }}</span>
              <span>{{
                itemStore.currentItem.timeUnit
                  ? t(`inventory.item.timeUnits.${itemStore.currentItem.timeUnit}`)
                  : '-'
              }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-label">{{ t('inventory.item.timeInterval') }}</span>
              <span>{{ itemStore.currentItem.timeInterval || '-' }}</span>
            </div>
          </template>
          <div class="detail-field">
            <span class="detail-label">{{ t('inventory.item.active') }}</span>
            <Tag
              :value="itemStore.currentItem.active ? t('common.yes') : t('common.no')"
              :severity="itemStore.currentItem.active ? 'success' : 'secondary'"
            />
          </div>
        </div>
      </template>
    </Card>

    <div class="movements-header">
      <h2>{{ t('inventory.movement.title') }}</h2>
      <Button
        v-if="canCreateMovement"
        icon="pi pi-plus"
        :label="t('inventory.movement.new')"
        @click="showMovementDialog = true"
      />
    </div>

    <Card>
      <template #content>
        <DataTable
          :value="itemStore.movements"
          :loading="itemStore.loading"
          dataKey="id"
          stripedRows
        >
          <template #empty>
            <div class="text-center p-4">
              {{ t('inventory.movement.empty') }}
            </div>
          </template>

          <Column :header="t('inventory.movement.type')" style="width: 100px">
            <template #body="{ data }">
              <Tag
                :value="t(`inventory.movement.types.${data.type}`)"
                :severity="data.type === 'ENTRY' ? 'success' : 'danger'"
              />
            </template>
          </Column>

          <Column
            field="quantity"
            :header="t('inventory.movement.quantity')"
            style="width: 100px"
          />

          <Column
            field="previousQuantity"
            :header="t('inventory.movement.previousQuantity')"
            style="width: 100px"
          />

          <Column
            field="newQuantity"
            :header="t('inventory.movement.newQuantity')"
            style="width: 100px"
          />

          <Column field="notes" :header="t('inventory.movement.notes')">
            <template #body="{ data }">
              {{ data.notes || '-' }}
            </template>
          </Column>

          <Column :header="t('inventory.movement.registeredBy')" style="width: 150px">
            <template #body="{ data }">
              {{ formatUser(data.createdBy) }}
            </template>
          </Column>

          <Column :header="t('inventory.movement.registeredAt')" style="width: 180px">
            <template #body="{ data }">
              {{ formatDate(data.createdAt) }}
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <InventoryMovementForm
      :visible="showMovementDialog"
      @update:visible="showMovementDialog = $event"
      :item-id="itemId"
      @created="onMovementCreated"
    />
  </div>
</template>

<style scoped>
.item-detail-page {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-header > div {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-title {
  margin: 0;
}

.detail-card {
  margin-bottom: 2rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-label {
  font-weight: 600;
  color: var(--p-text-muted-color);
  font-size: 0.85rem;
}

.quantity-value {
  font-size: 1.25rem;
  font-weight: 700;
}

.movements-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.movements-header h2 {
  margin: 0;
}
</style>
