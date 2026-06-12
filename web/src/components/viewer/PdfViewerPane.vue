<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import { loadPdfjs } from './pdfjsLoader'
import type { PDFDocumentLoadingTask, PDFDocumentProxy, RenderTask } from 'pdfjs-dist'

const props = defineProps<{
  blob: Blob
}>()

const { t } = useI18n()

const ZOOM_STEP = 1.25
const MIN_ZOOM = 0.25
const MAX_ZOOM = 8
// Chromebook memory guard: never rasterize beyond 2x CSS pixels.
const MAX_OUTPUT_SCALE = 2
const PANE_PADDING_PX = 32

const loading = ref(false)
const renderFailed = ref(false)
const currentPage = ref(1)
const pageCount = ref(0)
const fitMode = ref<'width' | 'page'>('page')
// User zoom multiplier applied on top of the fit-mode base scale.
const userZoom = ref(1)

const canvasRef = ref<HTMLCanvasElement | null>(null)
const scrollRef = ref<HTMLElement | null>(null)

// pdf.js handles are deliberately non-reactive: only the lifecycle below may
// touch them, and each document/render must be destroyed/cancelled before the
// next one starts (two renders on one canvas is a pdf.js error).
let pdfDoc: PDFDocumentProxy | null = null
let loadingTask: PDFDocumentLoadingTask | null = null
let renderTask: RenderTask | null = null
let loadSeq = 0
let renderSeq = 0

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  loadDocument()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  destroyDocument()
})

watch(() => props.blob, loadDocument)
watch([currentPage, fitMode, userZoom], () => {
  renderPage()
})

async function loadDocument() {
  const seq = ++loadSeq
  destroyDocument()
  loading.value = true
  renderFailed.value = false
  currentPage.value = 1
  try {
    const pdfjs = await loadPdfjs()
    const data = await props.blob.arrayBuffer()
    if (seq !== loadSeq) return
    loadingTask = pdfjs.getDocument({ data })
    const doc = await loadingTask.promise
    if (seq !== loadSeq) return
    pdfDoc = doc
    pageCount.value = doc.numPages
    loading.value = false
    await nextTick()
    await renderPage()
  } catch (error) {
    if (seq !== loadSeq) return
    reportUnexpected(error)
  } finally {
    if (seq === loadSeq) {
      loading.value = false
    }
  }
}

async function renderPage() {
  if (!pdfDoc || !canvasRef.value) return
  const seq = ++renderSeq
  const canvas = canvasRef.value
  try {
    const page = await pdfDoc.getPage(currentPage.value)
    if (seq !== renderSeq) return

    const baseViewport = page.getViewport({ scale: 1 })
    const availableWidth = Math.max((scrollRef.value?.clientWidth ?? 800) - PANE_PADDING_PX, 100)
    const availableHeight = Math.max((scrollRef.value?.clientHeight ?? 600) - PANE_PADDING_PX, 100)
    const widthScale = availableWidth / baseViewport.width
    const fitScale =
      fitMode.value === 'width'
        ? widthScale
        : Math.min(widthScale, availableHeight / baseViewport.height)
    const viewport = page.getViewport({ scale: fitScale * userZoom.value })

    const outputScale = Math.min(window.devicePixelRatio || 1, MAX_OUTPUT_SCALE)
    canvas.width = Math.floor(viewport.width * outputScale)
    canvas.height = Math.floor(viewport.height * outputScale)
    canvas.style.width = `${Math.floor(viewport.width)}px`
    canvas.style.height = `${Math.floor(viewport.height)}px`

    renderTask?.cancel()
    renderTask = page.render({
      canvas,
      viewport,
      transform: outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : undefined
    })
    await renderTask.promise
  } catch (error) {
    if ((error as Error | null)?.name === 'RenderingCancelledException') return
    if (seq !== renderSeq) return
    reportUnexpected(error)
  }
}

function reportUnexpected(error: unknown) {
  // Destroying the loading task mid-load rejects its promise; that is teardown,
  // not a broken document.
  if (error instanceof Error && /destroyed/i.test(error.message)) return
  renderFailed.value = true
}

function destroyDocument() {
  renderTask?.cancel()
  renderTask = null
  loadingTask?.destroy().catch(() => undefined)
  loadingTask = null
  pdfDoc = null
  pageCount.value = 0
}

function goToPage(page: number) {
  if (!pdfDoc) return
  currentPage.value = Math.min(Math.max(page, 1), pageCount.value)
}

function zoomIn() {
  userZoom.value = Math.min(userZoom.value * ZOOM_STEP, MAX_ZOOM)
}

function zoomOut() {
  userZoom.value = Math.max(userZoom.value / ZOOM_STEP, MIN_ZOOM)
}

function setFitMode(mode: 'width' | 'page') {
  if (fitMode.value === mode && userZoom.value === 1) return
  fitMode.value = mode
  userZoom.value = 1
  // The watcher only fires on change; force a render when resetting zoom only.
  renderPage()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowRight') {
    goToPage(currentPage.value + 1)
  } else if (event.key === 'ArrowLeft') {
    goToPage(currentPage.value - 1)
  }
}
</script>

<template>
  <div class="pdf-pane">
    <div v-if="renderFailed" class="render-error">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ t('document.viewer.renderError') }}</p>
    </div>
    <template v-else>
      <div class="pane-toolbar">
        <Button
          icon="pi pi-chevron-left"
          text
          rounded
          :disabled="currentPage <= 1"
          :aria-label="t('document.viewer.previousPage')"
          @click="goToPage(currentPage - 1)"
        />
        <span class="page-indicator" data-testid="pdf-page-indicator">
          {{ t('document.viewer.pageIndicator', { current: currentPage, total: pageCount }) }}
        </span>
        <Button
          icon="pi pi-chevron-right"
          text
          rounded
          :disabled="currentPage >= pageCount"
          :aria-label="t('document.viewer.nextPage')"
          @click="goToPage(currentPage + 1)"
        />
        <span class="toolbar-separator"></span>
        <Button
          icon="pi pi-search-minus"
          text
          rounded
          :aria-label="t('document.viewer.zoomOut')"
          @click="zoomOut"
        />
        <span class="zoom-level">{{ Math.round(userZoom * 100) }}%</span>
        <Button
          icon="pi pi-search-plus"
          text
          rounded
          :aria-label="t('document.viewer.zoomIn')"
          @click="zoomIn"
        />
        <Button
          icon="pi pi-arrows-h"
          text
          rounded
          :aria-label="t('document.viewer.fitWidth')"
          @click="setFitMode('width')"
        />
        <Button
          icon="pi pi-expand"
          text
          rounded
          :aria-label="t('document.viewer.fitPage')"
          @click="setFitMode('page')"
        />
      </div>
      <div ref="scrollRef" class="pane-scroll">
        <div v-if="loading" class="loading-container">
          <ProgressSpinner />
        </div>
        <canvas v-show="!loading" ref="canvasRef" :aria-label="t('document.viewer.title')"></canvas>
      </div>
    </template>
  </div>
</template>

<style scoped>
.pdf-pane {
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
  flex-wrap: wrap;
}

.toolbar-separator {
  width: 1px;
  height: 1.5rem;
  background: var(--surface-border);
  margin: 0 0.5rem;
}

.page-indicator,
.zoom-level {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  text-align: center;
}

.zoom-level {
  min-width: 3.5rem;
}

.pane-scroll {
  flex: 1;
  overflow: auto;
  display: flex;
  justify-content: safe center;
  align-items: flex-start;
  padding: 1rem;
}

.pane-scroll canvas {
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
  background: white;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.render-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
  color: var(--text-color-secondary);
  text-align: center;
  padding: 2rem;
}

.render-error i {
  font-size: 3rem;
  margin-bottom: 1rem;
}
</style>
