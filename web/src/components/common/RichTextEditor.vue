<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Editor from 'primevue/editor'
import { sanitizeHtml, sanitizeRichText } from '@/utils/sanitize'

const { t } = useI18n()

// Quill instance type — PrimeVue's `EditorLoadEvent.instance` is typed as `any`.
// We treat it as a minimal interface (the only methods we call).
interface QuillLike {
  root: HTMLElement
  clipboard: { convert(input: { html: string } | string): unknown }
  getSelection(focus?: boolean): { index: number; length: number } | null
  updateContents(delta: unknown, source?: string): unknown
  deleteText(index: number, length: number, source?: string): unknown
  setSelection(index: number, length?: number, source?: string): void
  insertText(index: number, text: string, source?: string): unknown
}

/**
 * Intercept paste so the editor only ever ingests sanitized HTML:
 *  - strips inline `style` / `class` attributes (kills the
 *    `style="background-color: transparent; color: rgb(0,0,0);"` noise
 *    customers reported pasting in from Word / Google Docs / Chrome).
 *  - unwraps any tag outside the supported toolbar set (`<span>`, `<div>`,
 *    MS-Office `<o:p>` / `<w:*>`, headings, links, images, etc.).
 *  - falls back to the plain-text payload when no HTML is on the clipboard.
 *
 * Done at paste time so the editor shows the cleaned content immediately —
 * what the user pastes equals what gets stored.
 */
function onEditorLoad(event: { instance: QuillLike }) {
  const quill = event.instance
  // Capture phase + stopImmediatePropagation so Quill's built-in clipboard
  // listener (registered on the same node) never runs — otherwise it
  // converts the unsanitized HTML first, then our handler appends a second
  // (cleaned) copy below it.
  quill.root.addEventListener(
    'paste',
    e => {
      const clipboard = (e as ClipboardEvent).clipboardData
      if (!clipboard) return
      const html = clipboard.getData('text/html')
      const text = clipboard.getData('text/plain')
      if (!html && !text) return

      e.preventDefault()
      e.stopImmediatePropagation()

      const range = quill.getSelection(true) ?? { index: 0, length: 0 }

      if (html) {
        const cleaned = sanitizeRichText(html)
        // Quill 2.x accepts an object form; 1.x accepts a string. Both work.
        const delta = quill.clipboard.convert({ html: cleaned })
        if (range.length > 0) quill.deleteText(range.index, range.length, 'user')
        quill.updateContents(
          { ops: [{ retain: range.index }, ...(delta as { ops: unknown[] }).ops] },
          'user'
        )
        const insertedLen = (delta as { length?: () => number }).length?.() ?? 0
        quill.setSelection(range.index + insertedLen, 0, 'silent')
      } else {
        if (range.length > 0) quill.deleteText(range.index, range.length, 'user')
        quill.insertText(range.index, text, 'user')
        quill.setSelection(range.index + text.length, 0, 'silent')
      }
    },
    { capture: true }
  )
}

const props = withDefaults(
  defineProps<{
    modelValue: string | null | undefined
    label?: string
    placeholder?: string
    readonly?: boolean
    rows?: number
    invalid?: boolean
    required?: boolean
  }>(),
  {
    label: '',
    placeholder: '',
    readonly: false,
    rows: 4,
    invalid: false,
    required: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const internalValue = computed({
  get: () => props.modelValue || '',
  set: (value: string) => emit('update:modelValue', value)
})

// Calculate editor height based on rows (approximately 24px per row)
const editorStyle = computed(() => ({
  height: `${props.rows * 24 + 42}px` // 42px accounts for toolbar
}))

// Sanitize content for readonly display to prevent XSS
const sanitizedContent = computed(() => sanitizeHtml(internalValue.value))
</script>

<template>
  <div class="rich-text-editor">
    <label v-if="label" class="field-label">
      {{ label }}
      <span v-if="required" class="required-mark">*</span>
    </label>
    <!-- eslint-disable-next-line vue/no-v-html -- content is sanitized via DOMPurify before binding -->
    <div v-if="readonly" class="readonly-content" v-html="sanitizedContent"></div>
    <Editor
      v-else
      v-model="internalValue"
      :placeholder="placeholder"
      :class="{ 'p-invalid': invalid }"
      :style="editorStyle"
      @load="onEditorLoad"
    >
      <template #toolbar>
        <span class="ql-formats">
          <button class="ql-bold" v-tooltip.bottom="t('editor.bold')"></button>
          <button class="ql-italic" v-tooltip.bottom="t('editor.italic')"></button>
          <button class="ql-underline" v-tooltip.bottom="t('editor.underline')"></button>
        </span>
        <span class="ql-formats">
          <button
            class="ql-list"
            value="ordered"
            v-tooltip.bottom="t('editor.numberedList')"
          ></button>
          <button class="ql-list" value="bullet" v-tooltip.bottom="t('editor.bulletList')"></button>
        </span>
        <span class="ql-formats">
          <button class="ql-clean" v-tooltip.bottom="t('editor.clearFormatting')"></button>
        </span>
      </template>
    </Editor>
  </div>
</template>

<style scoped>
.rich-text-editor {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  font-weight: 500;
  color: var(--p-text-color);
}

.required-mark {
  color: var(--p-red-500);
  margin-left: 0.25rem;
}

.readonly-content {
  padding: 0.75rem;
  background: var(--p-surface-ground);
  border-radius: var(--p-border-radius);
  min-height: 3rem;
  word-wrap: break-word;
}

.readonly-content:deep(p) {
  margin: 0 0 0.5rem 0;
}

.readonly-content:deep(ul),
.readonly-content:deep(ol) {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}

:deep(.p-editor) {
  width: 100%;
}

:deep(.p-editor .p-editor-content) {
  height: calc(100% - 42px);
}

:deep(.p-editor .p-editor-content .ql-editor) {
  min-height: 100%;
}

:deep(.p-invalid .p-editor-container) {
  border-color: var(--p-red-500);
}
</style>
