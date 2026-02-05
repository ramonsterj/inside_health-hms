<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import { usePsychotherapyActivityStore } from '@/stores/psychotherapyActivity'
import { useAuthStore } from '@/stores/auth'
import { useErrorHandler } from '@/composables/useErrorHandler'
import PsychotherapyActivityCard from './PsychotherapyActivityCard.vue'
import PsychotherapyActivityFormDialog from './PsychotherapyActivityFormDialog.vue'

const props = defineProps<{
  admissionId: number
}>()

const { t } = useI18n()
const confirm = useConfirm()
const { showError, showSuccess } = useErrorHandler()
const activityStore = usePsychotherapyActivityStore()
const authStore = useAuthStore()

const sortDirection = ref<'desc' | 'asc'>('desc')
const showFormDialog = ref(false)
const loaded = ref(false)

const activities = computed(() => activityStore.getActivities(props.admissionId))
const loading = computed(() => activityStore.loading)

const canCreate = computed(() => authStore.hasPermission('psychotherapy-activity:create'))
const canDelete = computed(() => authStore.hasPermission('psychotherapy-activity:delete'))

const sortOptions = computed(() => [
  { label: t('psychotherapy.activity.newestFirst'), value: 'desc' },
  { label: t('psychotherapy.activity.oldestFirst'), value: 'asc' }
])

onMounted(async () => {
  await loadActivities()
})

watch(sortDirection, async () => {
  await loadActivities()
})

async function loadActivities() {
  try {
    await activityStore.fetchActivities(props.admissionId, sortDirection.value)
  } catch (error) {
    showError(error)
  } finally {
    loaded.value = true
  }
}

function openCreateDialog() {
  showFormDialog.value = true
}

function handleActivitySaved() {
  showFormDialog.value = false
  showSuccess('psychotherapy.activity.created')
}

function confirmDelete(activityId: number) {
  confirm.require({
    message: t('psychotherapy.activity.confirmDelete'),
    header: t('common.confirm'),
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => deleteActivity(activityId)
  })
}

async function deleteActivity(activityId: number) {
  try {
    await activityStore.deleteActivity(props.admissionId, activityId)
    showSuccess('psychotherapy.activity.deleted')
  } catch (error) {
    showError(error)
  }
}
</script>

<template>
  <div class="activity-list">
    <!-- Header -->
    <div class="list-header">
      <div class="header-left">
        <h3>{{ t('psychotherapy.activity.title') }}</h3>
        <span class="activity-count" v-if="activities.length > 0">
          ({{ activities.length }}
          {{
            activities.length === 1
              ? t('psychotherapy.activity.activity')
              : t('psychotherapy.activity.activities')
          }})
        </span>
      </div>
      <div class="header-actions">
        <SelectButton
          v-model="sortDirection"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          :allowEmpty="false"
        />
        <Button
          v-if="canCreate"
          icon="pi pi-plus"
          :label="t('psychotherapy.activity.add')"
          @click="openCreateDialog"
        />
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !loaded" class="loading-container">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
    </div>

    <!-- Empty State -->
    <div v-else-if="loaded && activities.length === 0" class="empty-state">
      <i class="pi pi-heart empty-icon"></i>
      <p>{{ t('psychotherapy.activity.empty') }}</p>
      <Button
        v-if="canCreate"
        icon="pi pi-plus"
        :label="t('psychotherapy.activity.addFirst')"
        @click="openCreateDialog"
      />
    </div>

    <!-- Activities List -->
    <div v-else class="activities-list">
      <PsychotherapyActivityCard
        v-for="activity in activities"
        :key="activity.id"
        :activity="activity"
        :canDelete="canDelete"
        @delete="confirmDelete(activity.id)"
      />
    </div>

    <!-- Form Dialog -->
    <PsychotherapyActivityFormDialog
      v-model:visible="showFormDialog"
      :admissionId="admissionId"
      @saved="handleActivitySaved"
    />
  </div>
</template>

<style scoped>
.activity-list {
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

.activity-count {
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
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

.activities-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
</style>
