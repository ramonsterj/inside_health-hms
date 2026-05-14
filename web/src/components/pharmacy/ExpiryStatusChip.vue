<script setup lang="ts">
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { useI18n } from 'vue-i18n'
import { LotExpiryStatus } from '@/types/pharmacy'

const props = defineProps<{ status: LotExpiryStatus }>()
const { t } = useI18n()

const severity = computed(() => {
  switch (props.status) {
    case LotExpiryStatus.EXPIRED:
    case LotExpiryStatus.RED:
      return 'danger'
    case LotExpiryStatus.YELLOW:
      return 'warn'
    case LotExpiryStatus.GREEN:
      return 'success'
    case LotExpiryStatus.NO_EXPIRY:
    default:
      return 'secondary'
  }
})

const label = computed(() => t(`pharmacy.expiry.status.${props.status}`))
</script>

<template>
  <Tag :value="label" :severity="severity" />
</template>
