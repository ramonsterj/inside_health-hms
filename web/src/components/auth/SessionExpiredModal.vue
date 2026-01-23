<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { useSessionExpiration } from '@/composables/useSessionExpiration'

const { t } = useI18n()
const { sessionExpired, intendedRoute, resetSessionExpired } = useSessionExpiration()

// Local ref for v-model binding (PrimeVue Dialog requires writable ref)
const isVisible = ref(false)

// Sync local state with composable state
watch(
  sessionExpired,
  newValue => {
    isVisible.value = newValue
  },
  { immediate: true }
)

function handleLogin(): void {
  // Capture the route before resetting
  const redirectPath = intendedRoute.value

  // Close dialog and reset state
  isVisible.value = false
  resetSessionExpired()

  // Build the login URL with redirect parameter
  const loginUrl = redirectPath ? `/login?redirect=${encodeURIComponent(redirectPath)}` : '/login'

  // Use window.location for guaranteed navigation
  window.location.href = loginUrl
}
</script>

<template>
  <Dialog
    v-model:visible="isVisible"
    :header="t('auth.sessionExpired.title')"
    :modal="true"
    :closable="false"
    :closeOnEscape="false"
    :dismissableMask="false"
    :blockScroll="true"
    :draggable="false"
    :style="{ width: '400px' }"
    position="center"
  >
    <div class="session-expired-content">
      <div class="icon-container">
        <i class="pi pi-clock" style="font-size: 3rem; color: var(--orange-500)"></i>
      </div>
      <p class="message">{{ t('auth.sessionExpired.message') }}</p>
    </div>

    <template #footer>
      <div class="footer-actions">
        <Button
          :label="t('auth.sessionExpired.loginButton')"
          icon="pi pi-sign-in"
          @click="handleLogin"
          autofocus
        />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.session-expired-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 1rem 0;
}

.icon-container {
  margin-bottom: 1.5rem;
}

.message {
  color: var(--text-color);
  font-size: 1rem;
  line-height: 1.5;
  margin: 0;
}

.footer-actions {
  display: flex;
  justify-content: center;
  width: 100%;
}
</style>
