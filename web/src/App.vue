<script setup lang="ts">
import { RouterView } from 'vue-router'
import ConfirmDialog from 'primevue/confirmdialog'
import Toast from 'primevue/toast'
import SessionExpiredModal from '@/components/auth/SessionExpiredModal.vue'
</script>

<template>
  <ConfirmDialog />
  <Toast />
  <SessionExpiredModal />
  <RouterView />
</template>
