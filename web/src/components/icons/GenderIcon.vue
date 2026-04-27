<script setup lang="ts">
import { computed } from 'vue'
import { Sex } from '@/types/patient'

const props = withDefaults(
  defineProps<{
    sex?: Sex | null
    size?: number
  }>(),
  {
    sex: null,
    size: 24
  }
)

type Variant = 'female' | 'male' | 'other'

const variant = computed<Variant>(() => {
  if (props.sex === Sex.FEMALE) return 'female'
  if (props.sex === Sex.MALE) return 'male'
  return 'other'
})
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
    class="gender-icon"
  >
    <template v-if="variant === 'female'">
      <circle cx="12" cy="9" r="5" />
      <line x1="12" y1="14" x2="12" y2="22" />
      <line x1="9" y1="19" x2="15" y2="19" />
    </template>
    <template v-else-if="variant === 'male'">
      <circle cx="10" cy="14" r="5" />
      <line x1="13.5" y1="10.5" x2="20" y2="4" />
      <polyline points="14,4 20,4 20,10" />
    </template>
    <template v-else>
      <circle cx="12" cy="12" r="6" />
    </template>
  </svg>
</template>

<style scoped>
.gender-icon {
  display: block;
}
</style>
