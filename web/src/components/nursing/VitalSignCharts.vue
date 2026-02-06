<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import Chart from 'primevue/chart'
import ProgressSpinner from 'primevue/progressspinner'
import { useVitalSignStore } from '@/stores/vitalSign'
import { useErrorHandler } from '@/composables/useErrorHandler'
import VitalSignDateFilter from './VitalSignDateFilter.vue'
import type { VitalSignDateRange } from '@/types/nursing'

const props = defineProps<{
  admissionId: number
}>()

const { t, d } = useI18n()
const vitalSignStore = useVitalSignStore()
const { showError } = useErrorHandler()

// Date range filter
const dateRange = computed({
  get: () => vitalSignStore.dateRange,
  set: (value: VitalSignDateRange) => vitalSignStore.setDateRange(value)
})

// Chart data
const chartData = computed(() => vitalSignStore.getChartData(props.admissionId))
const loading = computed(() => vitalSignStore.chartLoading)
const hasData = computed(() => chartData.value.length > 0)

// Load chart data
async function loadChartData() {
  try {
    await vitalSignStore.fetchChartData(props.admissionId)
  } catch (error) {
    showError(error)
  }
}

// Filter change handler
function onFilterChange() {
  loadChartData()
}

// Format date for chart labels
function formatChartDate(dateStr: string): string {
  const date = new Date(dateStr)
  return d(date, 'short')
}

// Sort data chronologically (oldest first for chart)
const sortedData = computed(() => {
  return [...chartData.value].sort(
    (a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime()
  )
})

// Chart labels (timestamps)
const labels = computed(() => sortedData.value.map(vs => formatChartDate(vs.recordedAt)))

// Chart options (shared base)
const baseChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top' as const
    }
  },
  scales: {
    x: {
      ticks: {
        maxTicksLimit: 10
      }
    }
  }
}

// Blood Pressure chart (two lines)
const bpChartData = computed(() => ({
  labels: labels.value,
  datasets: [
    {
      label: t('nursing.vitalSigns.chartLabels.systolic'),
      data: sortedData.value.map(vs => vs.systolicBp),
      borderColor: '#ef4444',
      backgroundColor: 'rgba(239, 68, 68, 0.1)',
      tension: 0.4,
      fill: false
    },
    {
      label: t('nursing.vitalSigns.chartLabels.diastolic'),
      data: sortedData.value.map(vs => vs.diastolicBp),
      borderColor: '#3b82f6',
      backgroundColor: 'rgba(59, 130, 246, 0.1)',
      tension: 0.4,
      fill: false
    }
  ]
}))

const bpChartOptions = computed(() => ({
  ...baseChartOptions,
  scales: {
    ...baseChartOptions.scales,
    y: {
      title: {
        display: true,
        text: t('nursing.vitalSigns.units.mmHg')
      }
    }
  }
}))

// Heart Rate chart
const hrChartData = computed(() => ({
  labels: labels.value,
  datasets: [
    {
      label: t('nursing.vitalSigns.chartLabels.heartRate'),
      data: sortedData.value.map(vs => vs.heartRate),
      borderColor: '#22c55e',
      backgroundColor: 'rgba(34, 197, 94, 0.1)',
      tension: 0.4,
      fill: true
    }
  ]
}))

const hrChartOptions = computed(() => ({
  ...baseChartOptions,
  scales: {
    ...baseChartOptions.scales,
    y: {
      title: {
        display: true,
        text: t('nursing.vitalSigns.units.bpm')
      }
    }
  }
}))

// Respiratory Rate chart
const rrChartData = computed(() => ({
  labels: labels.value,
  datasets: [
    {
      label: t('nursing.vitalSigns.chartLabels.respiratoryRate'),
      data: sortedData.value.map(vs => vs.respiratoryRate),
      borderColor: '#f97316',
      backgroundColor: 'rgba(249, 115, 22, 0.1)',
      tension: 0.4,
      fill: true
    }
  ]
}))

const rrChartOptions = computed(() => ({
  ...baseChartOptions,
  scales: {
    ...baseChartOptions.scales,
    y: {
      title: {
        display: true,
        text: t('nursing.vitalSigns.units.breathsPerMin')
      }
    }
  }
}))

// Temperature chart
const tempChartData = computed(() => ({
  labels: labels.value,
  datasets: [
    {
      label: t('nursing.vitalSigns.chartLabels.temperature'),
      data: sortedData.value.map(vs => vs.temperature),
      borderColor: '#a855f7',
      backgroundColor: 'rgba(168, 85, 247, 0.1)',
      tension: 0.4,
      fill: true
    }
  ]
}))

const tempChartOptions = computed(() => ({
  ...baseChartOptions,
  scales: {
    ...baseChartOptions.scales,
    y: {
      title: {
        display: true,
        text: t('nursing.vitalSigns.units.celsius')
      }
    }
  }
}))

// Oxygen Saturation chart
const spo2ChartData = computed(() => ({
  labels: labels.value,
  datasets: [
    {
      label: t('nursing.vitalSigns.chartLabels.oxygenSaturation'),
      data: sortedData.value.map(vs => vs.oxygenSaturation),
      borderColor: '#06b6d4',
      backgroundColor: 'rgba(6, 182, 212, 0.1)',
      tension: 0.4,
      fill: true
    }
  ]
}))

const spo2ChartOptions = computed(() => ({
  ...baseChartOptions,
  scales: {
    ...baseChartOptions.scales,
    y: {
      title: {
        display: true,
        text: t('nursing.vitalSigns.units.percent')
      },
      min: 80,
      max: 100
    }
  }
}))

// Watch for admission changes
watch(
  () => props.admissionId,
  () => {
    loadChartData()
  }
)

// Initial load
onMounted(loadChartData)
</script>

<template>
  <div class="vital-sign-charts">
    <!-- Header -->
    <div class="charts-header">
      <div class="header-left"></div>
      <div class="header-right">
        <slot name="header-right" />
      </div>
    </div>

    <!-- Date filter -->
    <VitalSignDateFilter v-model="dateRange" @filter-change="onFilterChange" />

    <!-- Content -->
    <div class="charts-content">
      <!-- Loading state -->
      <div v-if="loading" class="loading-state">
        <ProgressSpinner strokeWidth="3" />
      </div>

      <!-- Empty state -->
      <div v-else-if="!hasData" class="empty-state">
        <i class="pi pi-chart-line empty-icon"></i>
        <p>{{ t('nursing.vitalSigns.noChartData') }}</p>
      </div>

      <!-- Charts grid -->
      <div v-else class="charts-grid">
        <!-- Blood Pressure -->
        <div class="chart-panel">
          <h4 class="chart-title">{{ t('nursing.vitalSigns.chartLabels.bloodPressure') }}</h4>
          <div class="chart-container">
            <Chart type="line" :data="bpChartData" :options="bpChartOptions" />
          </div>
        </div>

        <!-- Heart Rate -->
        <div class="chart-panel">
          <h4 class="chart-title">{{ t('nursing.vitalSigns.chartLabels.heartRate') }}</h4>
          <div class="chart-container">
            <Chart type="line" :data="hrChartData" :options="hrChartOptions" />
          </div>
        </div>

        <!-- Respiratory Rate -->
        <div class="chart-panel">
          <h4 class="chart-title">{{ t('nursing.vitalSigns.chartLabels.respiratoryRate') }}</h4>
          <div class="chart-container">
            <Chart type="line" :data="rrChartData" :options="rrChartOptions" />
          </div>
        </div>

        <!-- Temperature -->
        <div class="chart-panel">
          <h4 class="chart-title">{{ t('nursing.vitalSigns.chartLabels.temperature') }}</h4>
          <div class="chart-container">
            <Chart type="line" :data="tempChartData" :options="tempChartOptions" />
          </div>
        </div>

        <!-- Oxygen Saturation -->
        <div class="chart-panel">
          <h4 class="chart-title">{{ t('nursing.vitalSigns.chartLabels.oxygenSaturation') }}</h4>
          <div class="chart-container">
            <Chart type="line" :data="spo2ChartData" :options="spo2ChartOptions" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vital-sign-charts {
  padding: 1rem 0;
}

.charts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.charts-content {
  min-height: 200px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  text-align: center;
  color: var(--p-text-muted-color);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 1.5rem;
}

.chart-panel {
  background: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  border-radius: 0.5rem;
  padding: 1rem;
}

.chart-title {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.chart-container {
  height: 250px;
  position: relative;
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
