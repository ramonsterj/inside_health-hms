import { test, expect } from '@playwright/test'
import { waitForOverlaysToClear } from './utils/test-helpers'

// =====================================================================
// E2E coverage for the Laboratory Orders catalog admin surface at
// /lab/catalog (LabCatalogView -> providers/tests/provider-tests/panels
// tabs). Closes the feature doc's E2E checklist items:
//   - Lab catalog READ allowed for DOCTOR / RESIDENT_DOCTOR / ADMIN and
//     denied for roles holding only medical-order:read (AC13).
//   - Catalog MANAGEMENT affordances shown for ADMIN, hidden for a
//     read-only DOCTOR; create flow reaches POST /v1/lab/providers (AC5/AC10).
//   - Side-nav "Lab Catalog" entry gated by lab-catalog:read.
//
// These cover the route-guard + permission + store/API wiring that the
// component-level Vitest specs (form branch, store) cannot exercise.
// Follows the mock-API convention used across the e2e suite: a mock_user
// in localStorage plus per-endpoint route fulfilment (no live backend).
// =====================================================================

const READ_ONLY = ['admission:read', 'medical-order:read', 'lab-catalog:read']
const MANAGE = [...READ_ONLY, 'lab-catalog:manage']

const mockDoctorReadOnly = {
  id: 2,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Maria',
  lastName: 'Garcia',
  salutation: 'Dra.',
  roles: ['MEDICO'],
  permissions: READ_ONLY,
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockResidentReadOnly = {
  ...mockDoctorReadOnly,
  id: 5,
  username: 'resident',
  firstName: 'Roberto',
  lastName: 'Hernandez',
  roles: ['MEDICO_RESIDENTE']
}

const mockAdmin = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMINISTRADOR'],
  permissions: MANAGE,
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// ADMINISTRATIVE_STAFF holds medical-order:read but NOT lab-catalog:read — a
// representative of the roles denied catalog access (AC13). Using this role
// (rather than NURSE) keeps the redirect target deterministic: nursing/resident
// roles are home-redirected to /bed-occupancy, ADMINISTRATIVE_STAFF stays on the
// standard /dashboard.
const mockDeniedStaff = {
  id: 4,
  username: 'staff',
  email: 'staff@example.com',
  firstName: 'Lucia',
  lastName: 'Morales',
  salutation: 'Sra.',
  roles: ['PERSONAL_ADMINISTRATIVO'],
  permissions: ['admission:read', 'medical-order:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const seededProviders = [
  { id: 12, name: 'CLONY', code: 'CLONY', active: true },
  { id: 13, name: 'HOSPITAL HERRERA LLERANDI', code: 'HERRERA', active: true }
]

function ok<T>(data: T) {
  return { success: true, data }
}

async function setupAuth(page: import('@playwright/test').Page, user: typeof mockAdmin) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok(user))
    })
  })
  await page.route('**/api/auth/refresh', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok({ accessToken: 'new-mock-token', refreshToken: 'new-mock-refresh' }))
    })
  })
}

// Catch-all for any lab catalog GET so whichever tab panels mount don't hit
// the live network. Specific overrides (e.g. POST) are registered after this
// and therefore win (Playwright matches most-recently-added first).
async function setupCatalogReads(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/lab/providers/*/tests*', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok([]))
    })
  })
  await page.route('**/api/v1/lab/providers**', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok(seededProviders))
      })
    } else {
      await route.fallback()
    }
  })
  await page.route('**/api/v1/lab/tests**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok([]))
    })
  })
  await page.route('**/api/v1/lab/panels**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok([]))
    })
  })
}

test.describe('Lab catalog - access control & management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('doctor with lab-catalog:read can open the catalog and see providers', async ({ page }) => {
    await setupAuth(page, mockDoctorReadOnly)
    await setupCatalogReads(page)

    await page.goto('/lab/catalog')

    await expect(
      page.getByRole('heading', { name: /Lab Catalog|Catálogo de Laboratorio/i })
    ).toBeVisible()
    // Provider rows loaded from the (mocked) store call. Scope to table cells —
    // PrimeVue Tabs mounts every panel, so the provider name also appears in the
    // provider-tests panel's Select label.
    await expect(page.getByRole('cell', { name: 'CLONY' }).first()).toBeVisible()
    await expect(
      page.getByRole('cell', { name: 'HOSPITAL HERRERA LLERANDI' }).first()
    ).toBeVisible()

    // Side-nav entry is present for a reader.
    await expect(page.locator('a[href="/lab/catalog"]').first()).toBeVisible()
  })

  test('resident doctor with lab-catalog:read can open the catalog', async ({ page }) => {
    await setupAuth(page, mockResidentReadOnly)
    await setupCatalogReads(page)

    await page.goto('/lab/catalog')

    await expect(
      page.getByRole('heading', { name: /Lab Catalog|Catálogo de Laboratorio/i })
    ).toBeVisible()
    await expect(page.getByRole('cell', { name: 'CLONY' }).first()).toBeVisible()
  })

  test('read-only doctor sees no management affordances', async ({ page }) => {
    await setupAuth(page, mockDoctorReadOnly)
    await setupCatalogReads(page)

    await page.goto('/lab/catalog')
    await expect(page.getByRole('cell', { name: 'CLONY' }).first()).toBeVisible()

    // canManage === false → no "New Provider" button, no edit/delete actions
    // anywhere across the (eagerly-mounted) catalog tab panels.
    await expect(page.getByRole('button', { name: 'New Provider', exact: true })).toHaveCount(0)
    await expect(page.locator('button:has(.pi-trash)')).toHaveCount(0)
  })

  test('admin sees management affordances and can create a provider', async ({ page }) => {
    await setupAuth(page, mockAdmin)
    await setupCatalogReads(page)

    let createBody: { name?: string; code?: string | null; active?: boolean } = {}
    const created = { id: 99, name: 'LABORATORIO NUEVO', code: 'NUEVO', active: true }

    // Use a ** suffix so the provider-list refetch (sent as
    // `/lab/providers?activeOnly=false`) is matched, and a pathname guard so
    // provider sub-routes (e.g. /providers/12/tests) fall through to the
    // catalog-reads handler. Registered after setupCatalogReads → wins.
    let postDone = false
    await page.route('**/api/v1/lab/providers**', async route => {
      const pathname = new URL(route.request().url()).pathname
      if (!pathname.endsWith('/lab/providers')) {
        return route.fallback()
      }
      if (route.request().method() === 'POST') {
        createBody = route.request().postDataJSON()
        postDone = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ok(created))
        })
      } else {
        // Refetch after create returns the augmented list.
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ok(postDone ? [...seededProviders, created] : seededProviders))
        })
      }
    })

    await page.goto('/lab/catalog')
    await expect(page.getByRole('cell', { name: 'CLONY' }).first()).toBeVisible()

    await page.getByRole('button', { name: 'New Provider', exact: true }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await dialog.locator('#provider-name').fill('LABORATORIO NUEVO')
    await dialog.getByRole('button', { name: /Save|Guardar/i }).click()

    await expect.poll(() => createBody.name, { timeout: 5000 }).toBe('LABORATORIO NUEVO')
    await waitForOverlaysToClear(page)
    // New row appears after the post-create refetch.
    await expect(page.getByRole('cell', { name: 'LABORATORIO NUEVO' }).first()).toBeVisible({
      timeout: 10000
    })
  })

  test('a medical-order:read holder without lab-catalog:read is denied the catalog', async ({
    page
  }) => {
    await setupAuth(page, mockDeniedStaff)
    // Prevent any dashboard data calls from hitting the live network.
    await page.route('**/api/v1/**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(
          ok({ content: [], page: { totalElements: 0, totalPages: 0, size: 10, number: 0 } })
        )
      })
    })

    await page.goto('/lab/catalog')

    // Route guard bounces requiresPermission failures to /dashboard.
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
    await expect(
      page.getByRole('heading', { name: /Lab Catalog|Catálogo de Laboratorio/i })
    ).toHaveCount(0)
    // No side-nav entry either.
    await expect(page.locator('a[href="/lab/catalog"]')).toHaveCount(0)
  })
})
