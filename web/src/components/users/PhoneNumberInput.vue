<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Checkbox from 'primevue/checkbox'
import Button from 'primevue/button'
import { PhoneType } from '@/types'
import type { PhoneNumberRequest } from '@/types'

const { t } = useI18n()

const props = defineProps<{
  modelValue: PhoneNumberRequest
  showRemove: boolean
  error?: string
  index: number
}>()

const checkboxId = computed(() => `phone-primary-${props.index}`)

const emit = defineEmits<{
  'update:modelValue': [value: PhoneNumberRequest]
  remove: []
}>()

const phoneTypeOptions = computed(() => [
  { label: t('user.phoneTypes.MOBILE'), value: PhoneType.MOBILE },
  { label: t('user.phoneTypes.PRACTICE'), value: PhoneType.PRACTICE },
  { label: t('user.phoneTypes.HOME'), value: PhoneType.HOME },
  { label: t('user.phoneTypes.WORK'), value: PhoneType.WORK },
  { label: t('user.phoneTypes.OTHER'), value: PhoneType.OTHER }
])

function updateField<K extends keyof PhoneNumberRequest>(field: K, value: PhoneNumberRequest[K]) {
  emit('update:modelValue', { ...props.modelValue, [field]: value })
}
</script>

<template>
  <div class="flex flex-col gap-2 p-3 border border-surface-200 rounded-lg">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div class="flex flex-col gap-1">
        <label class="text-sm font-medium">{{ t('user.phoneNumber') }}</label>
        <InputText
          :model-value="modelValue.phoneNumber"
          @update:model-value="updateField('phoneNumber', $event as string)"
          :placeholder="t('user.phoneNumber')"
          :class="{ 'p-invalid': error }"
        />
      </div>

      <div class="flex flex-col gap-1">
        <label class="text-sm font-medium">{{ t('user.phoneType') }}</label>
        <Select
          :model-value="modelValue.phoneType"
          @update:model-value="updateField('phoneType', $event)"
          :options="phoneTypeOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="t('user.phoneType')"
        />
      </div>

      <div class="flex items-end gap-3">
        <div class="flex items-center gap-2">
          <Checkbox
            :model-value="modelValue.isPrimary"
            @update:model-value="updateField('isPrimary', $event as boolean)"
            :binary="true"
            :false-value="false"
            :inputId="checkboxId"
          />
          <label :for="checkboxId" class="text-sm">{{ t('user.primaryPhone') }}</label>
        </div>

        <Button
          v-if="showRemove"
          icon="pi pi-trash"
          severity="danger"
          text
          rounded
          @click="emit('remove')"
          :title="t('user.removePhoneNumber')"
        />
      </div>
    </div>
    <small v-if="error" class="p-error">{{ error }}</small>
  </div>
</template>
