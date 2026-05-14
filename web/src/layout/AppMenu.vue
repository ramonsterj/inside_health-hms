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
      label: 'nav.home',
      items: [
        {
          label: 'nav.dashboard',
          icon: 'pi pi-fw pi-home',
          to: '/dashboard'
        }
      ]
    },
    {
      label: 'nav.account',
      items: [
        {
          label: 'nav.profile',
          icon: 'pi pi-fw pi-user',
          to: '/profile'
        }
      ]
    }
  ]

  // Clinical section - visible to users with patient or admission permissions
  const clinicalItems: MenuItem[] = []
  if (authStore.hasPermission('patient:read')) {
    clinicalItems.push({
      label: 'nav.patients',
      icon: 'pi pi-fw pi-id-card',
      to: '/patients'
    })
  }
  if (authStore.hasPermission('admission:read')) {
    clinicalItems.push({
      label: 'nav.admissions',
      icon: 'pi pi-fw pi-building',
      to: '/admissions'
    })
    clinicalItems.push({
      label: 'nav.nursingKardex',
      icon: 'pi pi-fw pi-clipboard',
      to: '/nursing-kardex'
    })
  }
  if (authStore.hasPermission('room:occupancy-view')) {
    clinicalItems.push({
      label: 'nav.bedOccupancy',
      icon: 'pi pi-fw pi-th-large',
      to: '/bed-occupancy'
    })
  }
  if (authStore.hasPermission('medical-order:read')) {
    clinicalItems.push({
      label: 'nav.medicalOrdersByState',
      icon: 'pi pi-fw pi-file-edit',
      to: '/medical-orders'
    })
  }
  if (clinicalItems.length > 0) {
    items.push({
      label: 'nav.clinical',
      items: clinicalItems
    })
  }

  // Inventory section - visible to users with inventory permissions
  if (authStore.hasPermission('inventory-item:read')) {
    items.push({
      label: 'nav.inventory',
      items: [
        {
          label: 'nav.inventoryItems',
          icon: 'pi pi-fw pi-box',
          to: '/inventory'
        },
        {
          label: 'nav.lowStockReport',
          icon: 'pi pi-fw pi-exclamation-triangle',
          to: '/inventory/low-stock'
        }
      ]
    })
  }

  // Pharmacy section - visible to users with medication:read permission
  if (authStore.hasPermission('medication:read')) {
    const pharmacyItems: MenuItem[] = [
      {
        label: 'nav.pharmacyMedications',
        icon: 'pi pi-fw pi-mortar-pestle',
        to: '/pharmacy'
      }
    ]
    if (authStore.hasPermission('medication:expiry-report')) {
      pharmacyItems.push({
        label: 'nav.pharmacyExpiryReport',
        icon: 'pi pi-fw pi-calendar-clock',
        to: '/pharmacy/expiry-report'
      })
    }
    items.push({ label: 'nav.pharmacy', items: pharmacyItems })
  }

  // Treasury section - visible to users with treasury permissions
  const hasTreasuryRead = authStore.hasPermission('treasury:read')
  const hasTreasuryReport = authStore.hasPermission('treasury:report')
  if (hasTreasuryRead || hasTreasuryReport) {
    const treasuryItems: MenuItem[] = []

    if (hasTreasuryReport) {
      treasuryItems.push({
        label: 'nav.treasuryDashboard',
        icon: 'pi pi-fw pi-chart-bar',
        to: '/treasury'
      })
    }

    if (hasTreasuryRead) {
      treasuryItems.push(
        {
          label: 'nav.bankAccounts',
          icon: 'pi pi-fw pi-building-columns',
          to: '/treasury/bank-accounts'
        },
        {
          label: 'nav.expenses',
          icon: 'pi pi-fw pi-receipt',
          to: '/treasury/expenses'
        },
        {
          label: 'nav.income',
          icon: 'pi pi-fw pi-arrow-circle-down',
          to: '/treasury/income'
        },
        {
          label: 'nav.employees',
          icon: 'pi pi-fw pi-id-card',
          to: '/treasury/employees'
        }
      )
    }

    if (hasTreasuryReport) {
      treasuryItems.push({
        label: 'nav.reports',
        icon: 'pi pi-fw pi-chart-line',
        path: '/reports',
        items: [
          {
            label: 'nav.monthlyReport',
            to: '/treasury/reports/monthly'
          },
          {
            label: 'nav.upcomingPayments',
            to: '/treasury/reports/upcoming'
          },
          {
            label: 'nav.bankSummary',
            to: '/treasury/reports/bank-summary'
          },
          {
            label: 'nav.compensationSummary',
            to: '/treasury/reports/compensation'
          },
          {
            label: 'nav.indemnizacionReport',
            to: '/treasury/reports/indemnizacion'
          },
          {
            label: 'nav.reconciliationSummary',
            to: '/treasury/reports/reconciliation'
          }
        ]
      })
    }

    items.push({
      label: 'nav.treasury',
      items: treasuryItems
    })
  }

  // Admin section - only visible to admins
  if (authStore.isAdmin) {
    items.push({
      label: 'nav.administration',
      items: [
        {
          label: 'nav.users',
          icon: 'pi pi-fw pi-users',
          to: '/users'
        },
        {
          label: 'nav.roles',
          icon: 'pi pi-fw pi-shield',
          to: '/roles'
        },
        {
          label: 'nav.triageCodes',
          icon: 'pi pi-fw pi-tags',
          to: '/admin/triage-codes'
        },
        {
          label: 'nav.rooms',
          icon: 'pi pi-fw pi-th-large',
          to: '/admin/rooms'
        },
        {
          label: 'nav.activityCategories',
          icon: 'pi pi-fw pi-heart',
          to: '/admin/psychotherapy-categories'
        },
        {
          label: 'nav.inventoryCategories',
          icon: 'pi pi-fw pi-list',
          to: '/admin/inventory-categories'
        },
        {
          label: 'nav.auditLogs',
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
      <AppMenuItem v-if="!item.separator" :item="item" :root="true" />
      <li v-if="item.separator" class="menu-separator"></li>
    </template>
  </ul>
</template>

<style lang="scss" scoped></style>
