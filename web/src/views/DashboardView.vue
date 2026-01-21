<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Card from 'primevue/card'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const authStore = useAuthStore()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return t('dashboard.greeting.morning')
  if (hour < 18) return t('dashboard.greeting.afternoon')
  return t('dashboard.greeting.evening')
})

const displayName = computed(() => {
  if (authStore.user?.firstName) {
    return authStore.user.firstName
  }
  return authStore.user?.email?.split('@')[0] || ''
})
</script>

<template>
  <div class="dashboard">
    <h1 class="dashboard-title">{{ greeting }}, {{ displayName }}!</h1>

    <div class="dashboard-grid">
      <Card class="dashboard-card">
        <template #title>
          <i class="pi pi-user" style="margin-right: 0.5rem"></i>
          {{ t('dashboard.cards.profile.title') }}
        </template>
        <template #content>
          <p>{{ t('dashboard.cards.profile.description') }}</p>
        </template>
        <template #footer>
          <router-link :to="{ name: 'profile' }" class="card-link">
            {{ t('dashboard.cards.profile.link') }} <i class="pi pi-arrow-right"></i>
          </router-link>
        </template>
      </Card>

      <Card v-if="authStore.isAdmin" class="dashboard-card">
        <template #title>
          <i class="pi pi-users" style="margin-right: 0.5rem"></i>
          {{ t('dashboard.cards.users.title') }}
        </template>
        <template #content>
          <p>{{ t('dashboard.cards.users.description') }}</p>
        </template>
        <template #footer>
          <router-link :to="{ name: 'users' }" class="card-link">
            {{ t('dashboard.cards.users.link') }} <i class="pi pi-arrow-right"></i>
          </router-link>
        </template>
      </Card>

      <Card class="dashboard-card info-card">
        <template #title>
          <i class="pi pi-info-circle" style="margin-right: 0.5rem"></i>
          {{ t('dashboard.cards.accountInfo.title') }}
        </template>
        <template #content>
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">{{ t('dashboard.cards.accountInfo.email') }}</span>
              <span class="info-value">{{ authStore.user?.email }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ t('dashboard.cards.accountInfo.roles') }}</span>
              <span class="info-value">{{ authStore.user?.roles?.join(', ') || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ t('dashboard.cards.accountInfo.status') }}</span>
              <span class="info-value">{{ authStore.user?.status }}</span>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-title {
  margin-bottom: 2rem;
  font-size: 1.75rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.dashboard-card {
  height: 100%;
}

.card-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
}

.card-link:hover {
  text-decoration: none;
}

.card-link:hover i {
  transform: translateX(4px);
  transition: transform 0.2s;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  color: var(--text-color-secondary);
}

.info-value {
  font-weight: 500;
}
</style>
