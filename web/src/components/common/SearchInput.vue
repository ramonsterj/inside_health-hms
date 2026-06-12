<script setup lang="ts">
import { computed, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import { useDebouncedSearch } from '@/composables/useDebouncedSearch'

/**
 * The single sanctioned search field for the platform. Encapsulates the search
 * icon, debounce, and the "fire after N characters" rule so every search box —
 * present and future — behaves identically. Do NOT build search boxes from a
 * raw <InputText>; use this component so the as-you-type behavior is guaranteed.
 *
 * Usage:
 *   <SearchInput :placeholder="t('patient.search')" @search="onSearch" />
 *   function onSearch(term: string) { ... }  // '' means cleared (show all)
 *
 * `v-model` is optional and exposes the live raw text (e.g. to clear the box
 * from a parent "Clear filters" button).
 */
const props = withDefaults(
  defineProps<{
    placeholder?: string
    /** Minimum characters before searching. Default 3. */
    minLength?: number
    /** Debounce delay in ms. Default 300. */
    debounceMs?: number
    disabled?: boolean
    /** Optional fixed width (CSS value). */
    width?: string
    /** Passed to the inner <input> so a <label for="…"> can be associated. */
    inputId?: string
  }>(),
  { minLength: 3, debounceMs: 300, disabled: false }
)

const emit = defineEmits<{
  /** Committed (debounced) search term. Empty string means "cleared". */
  search: [term: string]
}>()

const { t } = useI18n()

// Live raw text — two-way so parents can clear/seed it via v-model.
const model = defineModel<string>({ default: '' })

const { reset, cancel } = useDebouncedSearch(term => emit('search', term), {
  source: model,
  minLength: props.minLength,
  debounceMs: props.debounceMs
})

onUnmounted(cancel)

// Let parents clear the box (e.g. a "Clear filters" button) without emitting.
defineExpose({ reset })

// Hint shown while the user has typed a partial term below the threshold.
const showMinCharsHint = computed(() => {
  const len = model.value.trim().length
  return len > 0 && len < props.minLength
})
</script>

<template>
  <div class="search-input-wrapper" :style="width ? { width } : undefined">
    <IconField>
      <InputIcon class="pi pi-search" />
      <InputText
        v-model="model"
        :input-id="inputId"
        :placeholder="placeholder ?? t('common.search')"
        :disabled="disabled"
        class="search-input-field"
      />
    </IconField>
    <small v-if="showMinCharsHint" class="search-input-hint">
      {{ t('common.searchMinChars', { min: minLength }) }}
    </small>
  </div>
</template>

<style scoped>
.search-input-wrapper {
  display: inline-flex;
  flex-direction: column;
  gap: 0.25rem;
}

.search-input-field {
  width: 100%;
}

.search-input-hint {
  color: var(--p-text-muted-color);
  font-size: 0.75rem;
}
</style>
