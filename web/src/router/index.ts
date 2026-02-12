import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { tokenStorage } from '@/utils/tokenStorage'
import AppLayout from '@/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    // Guest routes (no layout wrapper)
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guest: true }
    },
    // Force password change route (requires auth, but no layout)
    {
      path: '/auth/change-password',
      name: 'forcePasswordChange',
      component: () => import('@/views/auth/ForcePasswordChangeView.vue'),
      meta: { requiresAuth: true, forcePasswordChange: true }
    },
    // Authenticated routes (with AppLayout)
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue')
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue')
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'roles',
          name: 'roles',
          component: () => import('@/views/RolesView.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'audit-logs',
          name: 'audit-logs',
          component: () => import('@/views/AuditLogsView.vue'),
          meta: { requiresAdmin: true }
        },
        // Patient routes
        {
          path: 'patients',
          name: 'patients',
          component: () => import('@/views/patients/PatientsView.vue'),
          meta: { requiresPermission: 'patient:read' }
        },
        {
          path: 'patients/new',
          name: 'patient-create',
          component: () => import('@/views/patients/PatientFormView.vue'),
          meta: { requiresPermission: 'patient:create' }
        },
        {
          path: 'patients/:id',
          name: 'patient-detail',
          component: () => import('@/views/patients/PatientDetailView.vue'),
          meta: { requiresPermission: 'patient:read' }
        },
        {
          path: 'patients/:id/edit',
          name: 'patient-edit',
          component: () => import('@/views/patients/PatientFormView.vue'),
          meta: { requiresPermission: 'patient:update' }
        },
        // Admission routes
        {
          path: 'admissions',
          name: 'admissions',
          component: () => import('@/views/admissions/AdmissionsView.vue'),
          meta: { requiresPermission: 'admission:read' }
        },
        {
          path: 'admissions/new',
          name: 'admission-create',
          component: () => import('@/views/admissions/AdmissionFormView.vue'),
          meta: { requiresPermission: 'admission:create' }
        },
        {
          path: 'admissions/:id',
          name: 'admission-detail',
          component: () => import('@/views/admissions/AdmissionDetailView.vue'),
          meta: { requiresPermission: 'admission:read' }
        },
        {
          path: 'admissions/:id/edit',
          name: 'admission-edit',
          component: () => import('@/views/admissions/AdmissionFormView.vue'),
          meta: { requiresPermission: 'admission:update' }
        },
        // Billing routes
        {
          path: 'admissions/:id/charges',
          name: 'admission-charges',
          component: () => import('@/views/billing/AdmissionCharges.vue'),
          meta: { requiresPermission: 'billing:read' }
        },
        {
          path: 'admissions/:id/balance',
          name: 'admission-balance',
          component: () => import('@/views/billing/AdmissionBalance.vue'),
          meta: { requiresPermission: 'billing:read' }
        },
        {
          path: 'admissions/:id/invoice',
          name: 'admission-invoice',
          component: () => import('@/views/billing/InvoiceView.vue'),
          meta: { requiresPermission: 'invoice:read' }
        },
        // Admin - Triage Codes
        {
          path: 'admin/triage-codes',
          name: 'triage-codes',
          component: () => import('@/views/admin/TriageCodesView.vue'),
          meta: { requiresPermission: 'triage-code:read' }
        },
        {
          path: 'admin/triage-codes/new',
          name: 'triage-code-create',
          component: () => import('@/views/admin/TriageCodeFormView.vue'),
          meta: { requiresPermission: 'triage-code:create' }
        },
        {
          path: 'admin/triage-codes/:id/edit',
          name: 'triage-code-edit',
          component: () => import('@/views/admin/TriageCodeFormView.vue'),
          meta: { requiresPermission: 'triage-code:update' }
        },
        // Admin - Rooms
        {
          path: 'admin/rooms',
          name: 'rooms',
          component: () => import('@/views/admin/RoomsView.vue'),
          meta: { requiresPermission: 'room:read' }
        },
        {
          path: 'admin/rooms/new',
          name: 'room-create',
          component: () => import('@/views/admin/RoomFormView.vue'),
          meta: { requiresPermission: 'room:create' }
        },
        {
          path: 'admin/rooms/:id/edit',
          name: 'room-edit',
          component: () => import('@/views/admin/RoomFormView.vue'),
          meta: { requiresPermission: 'room:update' }
        },
        // Admin - Document Types
        {
          path: 'admin/document-types',
          name: 'document-types',
          component: () => import('@/views/admin/DocumentTypesView.vue'),
          meta: { requiresPermission: 'document-type:read' }
        },
        {
          path: 'admin/document-types/new',
          name: 'document-type-create',
          component: () => import('@/views/admin/DocumentTypeFormView.vue'),
          meta: { requiresPermission: 'document-type:create' }
        },
        {
          path: 'admin/document-types/:id/edit',
          name: 'document-type-edit',
          component: () => import('@/views/admin/DocumentTypeFormView.vue'),
          meta: { requiresPermission: 'document-type:update' }
        },
        // Admin - Psychotherapy Categories
        {
          path: 'admin/psychotherapy-categories',
          name: 'psychotherapy-categories',
          component: () => import('@/views/admin/PsychotherapyCategoriesView.vue'),
          meta: { requiresPermission: 'psychotherapy-category:read' }
        },
        {
          path: 'admin/psychotherapy-categories/new',
          name: 'psychotherapy-category-create',
          component: () => import('@/views/admin/PsychotherapyCategoryFormView.vue'),
          meta: { requiresPermission: 'psychotherapy-category:create' }
        },
        {
          path: 'admin/psychotherapy-categories/:id/edit',
          name: 'psychotherapy-category-edit',
          component: () => import('@/views/admin/PsychotherapyCategoryFormView.vue'),
          meta: { requiresPermission: 'psychotherapy-category:update' }
        },
        // Admin - Inventory Categories
        {
          path: 'admin/inventory-categories',
          name: 'inventory-categories',
          component: () => import('@/views/admin/InventoryCategoriesView.vue'),
          meta: { requiresPermission: 'inventory-category:read' }
        },
        {
          path: 'admin/inventory-categories/new',
          name: 'inventory-category-create',
          component: () => import('@/views/admin/InventoryCategoryFormView.vue'),
          meta: { requiresPermission: 'inventory-category:create' }
        },
        {
          path: 'admin/inventory-categories/:id/edit',
          name: 'inventory-category-edit',
          component: () => import('@/views/admin/InventoryCategoryFormView.vue'),
          meta: { requiresPermission: 'inventory-category:update' }
        },
        // Inventory Items (static routes before dynamic)
        {
          path: 'inventory',
          name: 'inventory-items',
          component: () => import('@/views/inventory/InventoryItemsView.vue'),
          meta: { requiresPermission: 'inventory-item:read' }
        },
        {
          path: 'inventory/low-stock',
          name: 'inventory-low-stock',
          component: () => import('@/views/inventory/LowStockReportView.vue'),
          meta: { requiresPermission: 'inventory-item:read' }
        },
        {
          path: 'inventory/new',
          name: 'inventory-item-create',
          component: () => import('@/views/inventory/InventoryItemFormView.vue'),
          meta: { requiresPermission: 'inventory-item:create' }
        },
        {
          path: 'inventory/:id',
          name: 'inventory-item-detail',
          component: () => import('@/views/inventory/InventoryItemDetailView.vue'),
          meta: { requiresPermission: 'inventory-item:read' }
        },
        {
          path: 'inventory/:id/edit',
          name: 'inventory-item-edit',
          component: () => import('@/views/inventory/InventoryItemFormView.vue'),
          meta: { requiresPermission: 'inventory-item:update' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      redirect: '/dashboard'
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  const hasTokens = tokenStorage.hasTokens()

  // If user has tokens but no user data, fetch user
  if (hasTokens && !authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      tokenStorage.clearTokens()
    }
  }

  const isAuthenticated = authStore.isAuthenticated

  // Guest routes (login, register) - redirect to dashboard if already logged in
  if (to.meta.guest && isAuthenticated) {
    return next({ name: 'dashboard' })
  }

  // Protected routes - redirect to login if not authenticated
  if (to.meta.requiresAuth && !isAuthenticated) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  // Force password change - redirect to change password page if user must change password
  if (isAuthenticated && authStore.mustChangePassword && !to.meta.forcePasswordChange) {
    return next({ name: 'forcePasswordChange' })
  }

  // Admin routes - redirect to dashboard if not admin
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return next({ name: 'dashboard' })
  }

  // Permission-based routes - redirect to dashboard if user lacks permission
  if (to.meta.requiresPermission && !authStore.hasPermission(to.meta.requiresPermission)) {
    return next({ name: 'dashboard' })
  }

  next()
})

export default router

// Type augmentation for route meta
declare module 'vue-router' {
  interface RouteMeta {
    guest?: boolean
    requiresAuth?: boolean
    requiresAdmin?: boolean
    requiresPermission?: string
    forcePasswordChange?: boolean
  }
}
