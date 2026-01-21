<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Menubar from 'primevue/menubar'
import Button from 'primevue/button'
import Menu from 'primevue/menu'
import LocaleSwitcher from '@/components/common/LocaleSwitcher.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const userMenu = ref()
const { t } = useI18n()

const menuItems = computed(() => {
  const items = [
    {
      label: t('nav.dashboard'),
      icon: 'pi pi-home',
      command: () => router.push({ name: 'dashboard' })
    }
  ]

  if (authStore.isAdmin) {
    items.push({
      label: t('nav.users'),
      icon: 'pi pi-users',
      command: () => router.push({ name: 'users' })
    })
    items.push({
      label: t('nav.roles'),
      icon: 'pi pi-shield',
      command: () => router.push({ name: 'roles' })
    })
    items.push({
      label: t('nav.auditLogs'),
      icon: 'pi pi-history',
      command: () => router.push({ name: 'audit-logs' })
    })
  }

  return items
})

const userMenuItems = computed(() => [
  {
    label: authStore.user?.email || 'User',
    items: [
      {
        label: t('nav.profile'),
        icon: 'pi pi-user',
        command: () => router.push({ name: 'profile' })
      },
      {
        separator: true
      },
      {
        label: t('nav.logout'),
        icon: 'pi pi-sign-out',
        command: handleLogout
      }
    ]
  }
])

function toggleUserMenu(event: Event) {
  userMenu.value.toggle(event)
}

async function handleLogout() {
  await authStore.logout()
  router.push({ name: 'login' })
}

const displayName = computed(() => {
  if (authStore.user?.firstName || authStore.user?.lastName) {
    return `${authStore.user.firstName || ''} ${authStore.user.lastName || ''}`.trim()
  }
  return authStore.user?.email || 'User'
})
</script>

<template>
  <Menubar :model="menuItems" class="app-navbar">
    <template #start>
      <span class="app-title">{{ t('common.appName') }}</span>
    </template>
    <template #end>
      <div class="navbar-end">
        <LocaleSwitcher />
        <Button
          type="button"
          :label="displayName"
          icon="pi pi-user"
          severity="secondary"
          text
          @click="toggleUserMenu"
          aria-haspopup="true"
          aria-controls="user-menu"
        />
        <Menu id="user-menu" ref="userMenu" :model="userMenuItems" :popup="true" />
      </div>
    </template>
  </Menubar>
</template>

<style scoped>
.app-navbar {
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-top: none;
}

.app-title {
  font-weight: 600;
  font-size: 1.25rem;
  margin-right: 2rem;
  color: var(--primary-color);
}

.navbar-end {
  display: flex;
  align-items: center;
  gap: 1rem;
}
</style>
