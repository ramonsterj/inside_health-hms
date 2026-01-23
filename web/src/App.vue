<script setup lang="ts">
import { RouterView } from 'vue-router'
import ConfirmDialog from 'primevue/confirmdialog'
import SessionExpiredModal from '@/components/auth/SessionExpiredModal.vue'
</script>

<template>
  <ConfirmDialog />
  <SessionExpiredModal />
  <RouterView />
</template>
