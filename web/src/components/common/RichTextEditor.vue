<script setup lang="ts">
import { computed } from 'vue'
import Editor from 'primevue/editor'
import { sanitizeHtml } from '@/utils/sanitize'

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
    <div v-if="readonly" class="readonly-content" v-html="sanitizedContent"></div>
    <Editor
      v-else
      v-model="internalValue"
      :placeholder="placeholder"
      :class="{ 'p-invalid': invalid }"
      :style="editorStyle"
    >
      <template #toolbar>
        <span class="ql-formats">
          <button class="ql-bold" v-tooltip.bottom="'Bold'"></button>
          <button class="ql-italic" v-tooltip.bottom="'Italic'"></button>
          <button class="ql-underline" v-tooltip.bottom="'Underline'"></button>
        </span>
        <span class="ql-formats">
          <button class="ql-list" value="ordered" v-tooltip.bottom="'Numbered List'"></button>
          <button class="ql-list" value="bullet" v-tooltip.bottom="'Bullet List'"></button>
        </span>
        <span class="ql-formats">
          <button class="ql-clean" v-tooltip.bottom="'Clear Formatting'"></button>
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
