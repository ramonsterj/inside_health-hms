<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'

defineProps<{
  src: string
  alt: string
}>()

const { t } = useI18n()

const ZOOM_STEP = 1.25
const MIN_ZOOM = 0.25
const MAX_ZOOM = 8

// null = fit inside the pane; a number = image width relative to the pane (1 = 100%)
const zoom = ref<number | null>(null)

function zoomIn() {
  zoom.value = Math.min((zoom.value ?? 1) * ZOOM_STEP, MAX_ZOOM)
}

function zoomOut() {
  zoom.value = Math.max((zoom.value ?? 1) / ZOOM_STEP, MIN_ZOOM)
}

function fit() {
  zoom.value = null
}
</script>

<template>
  <div class="image-pane">
    <div class="pane-toolbar">
      <Button
        icon="pi pi-search-minus"
        text
        rounded
        :aria-label="t('document.viewer.zoomOut')"
        @click="zoomOut"
      />
      <span class="zoom-level">{{ Math.round((zoom ?? 1) * 100) }}%</span>
      <Button
        icon="pi pi-search-plus"
        text
        rounded
        :aria-label="t('document.viewer.zoomIn')"
        @click="zoomIn"
      />
      <Button
        icon="pi pi-expand"
        text
        rounded
        :aria-label="t('document.viewer.fitPage')"
        @click="fit"
      />
    </div>
    <div class="pane-scroll" :class="{ fit: zoom === null }">
      <img
        :src="src"
        :alt="alt"
        :style="
          zoom !== null
            ? { width: `${zoom * 100}%`, maxWidth: 'none', maxHeight: 'none' }
            : undefined
        "
      />
    </div>
  </div>
</template>

<style scoped>
.image-pane {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}

.pane-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.25rem;
  border-bottom: 1px solid var(--surface-border);
  background: var(--surface-card);
}

.zoom-level {
  min-width: 3.5rem;
  text-align: center;
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

.pane-scroll {
  flex: 1;
  overflow: auto;
  padding: 1rem;
}

.pane-scroll.fit {
  display: flex;
  align-items: center;
  justify-content: center;
}

.pane-scroll.fit img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>
