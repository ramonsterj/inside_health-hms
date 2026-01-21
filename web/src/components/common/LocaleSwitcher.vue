<script setup lang="ts">
import { computed } from 'vue'
import Select from 'primevue/select'
import { useLocale } from '@/composables/useLocale'
import type { SupportedLocale } from '@/i18n'

defineProps<{
  inputId?: string
}>()

const { currentLocale, localeOptions, setLocale, loading } = useLocale()

interface LocaleOption {
  code: SupportedLocale
  label: string
}

const selectedLocale = computed({
  get: () => localeOptions.value.find(opt => opt.code === currentLocale.value),
  set: async (option: LocaleOption | undefined) => {
    if (option) {
      await setLocale(option.code)
    }
  }
})
</script>

<template>
  <Select
    v-model="selectedLocale"
    :options="localeOptions"
    optionLabel="label"
    :loading="loading"
    :inputId="inputId"
    class="locale-switcher"
  >
    <template #value="slotProps">
      <div v-if="slotProps.value" class="locale-option">
        <i class="pi pi-globe" style="margin-right: 0.5rem"></i>
        {{ slotProps.value.label }}
      </div>
    </template>
    <template #option="slotProps">
      <div class="locale-option">
        {{ slotProps.option.label }}
      </div>
    </template>
  </Select>
</template>

<style scoped>
.locale-switcher {
  min-width: 120px;
}

.locale-option {
  display: flex;
  align-items: center;
}
</style>
