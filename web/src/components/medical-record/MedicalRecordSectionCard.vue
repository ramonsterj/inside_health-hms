<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import Tag from 'primevue/tag'
import type { MetricSeverity } from '@/composables/useMedicalRecordSummary'

defineProps<{
  sectionKey: string
  title: string
  icon: string
  /** Small live metric shown as a Tag. Renders nothing when undefined. */
  metric?: string
  metricSeverity?: MetricSeverity
  /** Pre-formatted "last updated" datetime (dd/MM/yyyy - HH:mm). */
  updated?: string
}>()

const emit = defineEmits<{
  open: []
}>()

const { t } = useI18n()
</script>

<template>
  <Card
    class="section-card"
    :data-testid="`section-card-${sectionKey}`"
    role="button"
    tabindex="0"
    @click="emit('open')"
    @keydown.enter="emit('open')"
    @keydown.space.prevent="emit('open')"
  >
    <template #content>
      <div class="section-card-body">
        <div class="section-card-head">
          <div class="section-icon"><i :class="icon" /></div>
          <Tag v-if="metric" :value="metric" :severity="metricSeverity || 'secondary'" />
        </div>
        <h3 class="section-title">{{ title }}</h3>
        <div class="section-footer">
          <span v-if="updated" class="section-updated">
            <i class="pi pi-clock" /> {{ updated }}
          </span>
          <span class="section-open">
            {{ t('medicalRecord.openSection') }} <i class="pi pi-arrow-right" />
          </span>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
.section-card {
  cursor: pointer;
  transition:
    transform 0.12s ease,
    box-shadow 0.12s ease;
  height: 100%;
}
.section-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
}
.section-card:focus-visible {
  outline: 2px solid var(--p-primary-color);
  outline-offset: 2px;
}
.section-card-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  height: 100%;
}
.section-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.section-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--p-border-radius);
  background: var(--p-primary-50);
  color: var(--p-primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
}
.section-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
}
.section-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 0.5rem;
  font-size: 0.75rem;
}
.section-updated {
  color: var(--p-text-muted-color);
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}
.section-open {
  color: var(--p-primary-color);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  margin-left: auto;
}
</style>
