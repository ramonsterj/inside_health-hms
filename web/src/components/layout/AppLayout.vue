<script setup lang="ts">
import AppNavbar from './AppNavbar.vue'
</script>

<template>
  <div class="app-layout">
    <AppNavbar />
    <main class="app-content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-content {
  flex: 1;
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}
</style>
