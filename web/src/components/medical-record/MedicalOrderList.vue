<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import { useMedicalOrderStore } from '@/stores/medicalOrder'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import {
  MedicalOrderCategory,
  MedicalOrderStatus,
  type MedicalOrderResponse
} from '@/types/medicalRecord'
import MedicalOrderCard from './MedicalOrderCard.vue'
import MedicalOrderFormDialog from './MedicalOrderFormDialog.vue'

const props = defineProps<{
  admissionId: number
}>()

const { t } = useI18n()
const { showError, showSuccess } = useErrorHandler()
const medicalOrderStore = useMedicalOrderStore()
const authStore = useAuthStore()

const statusFilter = ref<MedicalOrderStatus | 'ALL'>('ALL')
const showFormDialog = ref(false)
const editingOrder = ref<MedicalOrderResponse | null>(null)
const loaded = ref(false)

const orders = computed(() => medicalOrderStore.getMedicalOrders(props.admissionId))
const loading = computed(() => medicalOrderStore.loading)

const canCreate = computed(() => authStore.hasPermission('medical-order:create'))
const canUpdate = computed(() => authStore.hasPermission('medical-order:update'))

const statusOptions = computed(() => [
  { label: t('common.all'), value: 'ALL' },
  { label: t('medicalRecord.medicalOrder.statuses.ACTIVE'), value: MedicalOrderStatus.ACTIVE },
  {
    label: t('medicalRecord.medicalOrder.statuses.DISCONTINUED'),
    value: MedicalOrderStatus.DISCONTINUED
  }
])

// Category display order
const categoryOrder = [
  MedicalOrderCategory.ORDENES_MEDICAS,
  MedicalOrderCategory.MEDICAMENTOS,
  MedicalOrderCategory.LABORATORIOS,
  MedicalOrderCategory.REFERENCIAS_MEDICAS,
  MedicalOrderCategory.PRUEBAS_PSICOMETRICAS,
  MedicalOrderCategory.ACTIVIDAD_FISICA,
  MedicalOrderCategory.CUIDADOS_ESPECIALES,
  MedicalOrderCategory.DIETA,
  MedicalOrderCategory.RESTRICCIONES_MOVILIDAD,
  MedicalOrderCategory.PERMISOS_VISITA,
  MedicalOrderCategory.OTRAS
]

// Filter orders by status
const filteredOrders = computed(() => {
  if (!orders.value) return null

  const result: Record<MedicalOrderCategory, MedicalOrderResponse[]> = {} as Record<
    MedicalOrderCategory,
    MedicalOrderResponse[]
  >

  for (const category of categoryOrder) {
    const categoryOrders = orders.value[category] || []
    if (statusFilter.value === 'ALL') {
      result[category] = categoryOrders
    } else {
      result[category] = categoryOrders.filter(o => o.status === statusFilter.value)
    }
  }

  return result
})

// Count orders per category for display
function getCategoryCount(category: MedicalOrderCategory): number {
  if (!filteredOrders.value) return 0
  return filteredOrders.value[category]?.length || 0
}

// Total orders count
const totalOrders = computed(() => {
  if (!filteredOrders.value) return 0
  return Object.values(filteredOrders.value).reduce((sum, arr) => sum + arr.length, 0)
})

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  try {
    await medicalOrderStore.fetchMedicalOrders(props.admissionId)
  } catch (error) {
    showError(error)
  } finally {
    loaded.value = true
  }
}

function openCreateDialog() {
  editingOrder.value = null
  showFormDialog.value = true
}

function openEditDialog(order: MedicalOrderResponse) {
  editingOrder.value = order
  showFormDialog.value = true
}

async function handleDiscontinue(order: MedicalOrderResponse) {
  try {
    await medicalOrderStore.discontinueMedicalOrder(props.admissionId, order.id)
    showSuccess('medicalRecord.medicalOrder.discontinued')
  } catch (error) {
    showError(error)
  }
}

function handleOrderSaved() {
  const wasEditing = !!editingOrder.value
  showFormDialog.value = false
  editingOrder.value = null
  showSuccess(
    wasEditing
      ? 'medicalRecord.medicalOrder.updated'
      : 'medicalRecord.medicalOrder.created'
  )
}
</script>

<template>
  <div class="medical-order-list">
    <!-- Header -->
    <div class="list-header">
      <div class="header-left">
        <h3>{{ t('medicalRecord.medicalOrder.title') }}</h3>
        <span class="order-count" v-if="totalOrders > 0">
          ({{ totalOrders }} {{ t('medicalRecord.medicalOrder.orders') }})
        </span>
      </div>
      <div class="header-actions">
        <SelectButton
          v-model="statusFilter"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
        />
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('medicalRecord.medicalOrder.add')"
          @click="openCreateDialog"
        />
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !loaded" class="loading-container">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
    </div>

    <!-- Empty State -->
    <div v-else-if="loaded && totalOrders === 0" class="empty-state">
      <i class="pi pi-clipboard empty-icon"></i>
      <p>{{ t('medicalRecord.medicalOrder.empty') }}</p>
      <Button
        v-if="canCreate"
        icon="pi pi-plus"
        :label="t('medicalRecord.medicalOrder.addFirst')"
        @click="openCreateDialog"
      />
    </div>

    <!-- Orders by Category -->
    <Accordion v-else multiple class="orders-accordion">
      <AccordionPanel
        v-for="category in categoryOrder"
        :key="category"
        :value="category"
        :disabled="getCategoryCount(category) === 0"
      >
        <AccordionHeader>
          <span class="category-title">
            {{ t(`medicalRecord.medicalOrder.categories.${category}`) }}
          </span>
          <Tag
            :value="String(getCategoryCount(category))"
            :severity="getCategoryCount(category) > 0 ? 'primary' : 'secondary'"
            class="count-badge"
          />
        </AccordionHeader>
        <AccordionContent>
          <div class="category-orders">
            <MedicalOrderCard
              v-for="order in filteredOrders?.[category]"
              :key="order.id"
              :order="order"
              :canEdit="canUpdate"
              @edit="openEditDialog(order)"
              @discontinue="handleDiscontinue(order)"
            />
          </div>
        </AccordionContent>
      </AccordionPanel>
    </Accordion>

    <!-- Form Dialog -->
    <MedicalOrderFormDialog
      v-model:visible="showFormDialog"
      :admissionId="admissionId"
      :order="editingOrder"
      @saved="handleOrderSaved"
    />
  </div>
</template>

<style scoped>
.medical-order-list {
  padding: 1rem;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.header-left h3 {
  margin: 0;
}

.order-count {
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 3rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state p {
  margin-bottom: 1.5rem;
}

.orders-accordion {
  margin-bottom: 1rem;
}

.category-title {
  font-weight: 600;
}

.count-badge {
  margin-left: 0.5rem;
}

.category-orders {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
</style>
