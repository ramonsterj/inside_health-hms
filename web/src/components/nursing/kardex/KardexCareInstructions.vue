<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Tag from 'primevue/tag'
import type { KardexCareInstruction } from '@/types'

const props = defineProps<{
  careInstructions: KardexCareInstruction[]
}>()

const { t, d } = useI18n()

const CATEGORY_ORDER = ['DIETA', 'CUIDADOS_ESPECIALES', 'RESTRICCIONES_MOVILIDAD', 'PERMISOS_VISITA']

const CATEGORY_LABELS = new Map<string, string>([
  ['DIETA', 'medicalRecord.medicalOrder.categories.DIETA'],
  ['CUIDADOS_ESPECIALES', 'medicalRecord.medicalOrder.categories.CUIDADOS_ESPECIALES'],
  ['RESTRICCIONES_MOVILIDAD', 'medicalRecord.medicalOrder.categories.RESTRICCIONES_MOVILIDAD'],
  ['PERMISOS_VISITA', 'medicalRecord.medicalOrder.categories.PERMISOS_VISITA'],
])

const groupedInstructions = computed(() => {
  const groups: { category: string; label: string; items: KardexCareInstruction[] }[] = []
  for (const cat of CATEGORY_ORDER) {
    const items = props.careInstructions.filter(ci => ci.category === cat)
    if (items.length > 0) {
      const labelKey = CATEGORY_LABELS.get(cat)
      groups.push({
        category: cat,
        label: labelKey ? t(labelKey) : cat,
        items: items.sort((a, b) => a.startDate.localeCompare(b.startDate)),
      })
    }
  }
  return groups
})
</script>

<template>
  <div class="kardex-care-instructions">
    <h4>{{ t('kardex.careInstructions.title') }}</h4>

    <div v-if="props.careInstructions.length === 0" class="empty-state">
      {{ t('kardex.careInstructions.empty') }}
    </div>

    <div v-else class="care-groups">
      <div v-for="group in groupedInstructions" :key="group.category" class="care-group">
        <Tag :value="group.label" severity="warn" class="group-tag" />
        <div
          v-for="item in group.items"
          :key="item.orderId"
          class="care-item"
        >
          <span class="care-observations">{{ item.observations || '-' }}</span>
          <span class="care-since">
            {{ t('kardex.careInstructions.since') }} {{ d(new Date(item.startDate + 'T00:00:00'), 'short') }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kardex-care-instructions h4 {
  margin: 0 0 0.75rem 0;
  font-size: 1rem;
}

.empty-state {
  color: var(--p-text-muted-color);
  font-style: italic;
  padding: 0.5rem 0;
}

.care-groups {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.care-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.care-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: var(--p-border-radius);
  gap: 1rem;
}

.care-observations {
  flex: 1;
}

.care-since {
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
  white-space: nowrap;
}
</style>
