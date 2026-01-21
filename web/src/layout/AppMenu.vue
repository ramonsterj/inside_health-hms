<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import AppMenuItem from './AppMenuItem.vue'

export interface MenuItem {
  label: string
  icon?: string
  to?: string
  url?: string
  target?: string
  path?: string
  items?: MenuItem[]
  separator?: boolean
  visible?: boolean
  class?: string
  disabled?: boolean
  command?: (event: { originalEvent: Event; item: MenuItem }) => void
}

const authStore = useAuthStore()

const model = computed<MenuItem[]>(() => {
  const items: MenuItem[] = [
    {
      label: 'Home',
      items: [
        {
          label: 'Dashboard',
          icon: 'pi pi-fw pi-home',
          to: '/dashboard'
        }
      ]
    },
    {
      label: 'Account',
      items: [
        {
          label: 'Profile',
          icon: 'pi pi-fw pi-user',
          to: '/profile'
        }
      ]
    }
  ]

  // Admin section - only visible to admins
  if (authStore.isAdmin) {
    items.push({
      label: 'Administration',
      items: [
        {
          label: 'Users',
          icon: 'pi pi-fw pi-users',
          to: '/users'
        },
        {
          label: 'Roles',
          icon: 'pi pi-fw pi-shield',
          to: '/roles'
        },
        {
          label: 'Audit Logs',
          icon: 'pi pi-fw pi-history',
          to: '/audit-logs'
        }
      ]
    })
  }

  return items
})
</script>

<template>
  <ul class="layout-menu">
    <template v-for="(item, i) in model" :key="item.label || i">
      <AppMenuItem v-if="!item.separator" :item="item" :index="i" />
      <li v-if="item.separator" class="menu-separator"></li>
    </template>
  </ul>
</template>

<style lang="scss" scoped></style>
