import { test, expect } from '@playwright/test'
import { waitForOverlaysToClear } from './utils/test-helpers'

// =====================================================================
// E2E coverage for the v1.2 cross-admission "Medical Orders by State"
// dashboard at /medical-orders. The view paginates GET /v1/medical-orders
// and exposes per-row authorize / reject / emergency-authorize /
// mark-in-progress / discontinue actions, gated by both permission AND
// current state. Default filter on mount is SOLICITADO + AUTORIZADO +
// EN_PROCESO (the action-needed buckets).
// =====================================================================

const mockAdminStaffUser = {
  id: 4,
  username: 'staff',
  email: 'staff@example.com',
  firstName: 'Maria',
  lastName: 'Garcia',
  salutation: 'Sra.',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: [
    'admission:read',
    'medical-order:read',
    'medical-order:authorize'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockNurseUser = {
  id: 3,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Ana',
  lastName: 'Lopez',
  salutation: 'Lic.',
  roles: ['NURSE'],
  permissions: [
    'admission:read',
    'medical-order:read',
    'medical-order:mark-in-progress',
    'medical-order:upload-document'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockDoctorWithEmergency = {
  id: 2,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Maria',
  lastName: 'Garcia',
  salutation: 'Dra.',
  roles: ['DOCTOR'],
  permissions: [
    'admission:read',
    'medical-order:read',
    'medical-order:emergency-authorize',
    'medical-order:mark-in-progress',
    'medical-order:discontinue'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const requestedMed = {
  id: 100,
  admissionId: 1,
  patientId: 1,
  patientFirstName: 'Juan',
  patientLastName: 'Pérez',
  category: 'MEDICAMENTOS',
  status: 'SOLICITADO',
  startDate: '2026-01-24',
  summary: null,
  medication: 'Sertraline',
  dosage: '50mg',
  createdAt: '2026-01-24T08:00:00',
  createdBy: { id: 2, firstName: 'Maria', lastName: 'Garcia', salutation: 'Dra.' },
  authorizedAt: null,
  inProgressAt: null,
  resultsReceivedAt: null,
  discontinuedAt: null,
  emergencyAuthorized: false,
  documentCount: 0
}

const authorizedLab = {
  id: 101,
  admissionId: 2,
  patientId: 2,
  patientFirstName: 'Ana',
  patientLastName: 'García',
  category: 'LABORATORIOS',
  status: 'AUTORIZADO',
  startDate: '2026-01-24',
  summary: 'Hemograma completo',
  medication: null,
  dosage: null,
  createdAt: '2026-01-24T09:00:00',
  createdBy: { id: 5, firstName: 'Roberto', lastName: 'Hernandez', salutation: 'Dr.' },
  authorizedAt: '2026-01-24T09:30:00',
  inProgressAt: null,
  resultsReceivedAt: null,
  discontinuedAt: null,
  emergencyAuthorized: false,
  documentCount: 0
}

const enProcesoLab = {
  ...authorizedLab,
  id: 102,
  status: 'EN_PROCESO',
  inProgressAt: '2026-01-24T10:00:00'
}

function pagedResponse<T>(content: T[]) {
  return {
    success: true,
    data: {
      content,
      page: { totalElements: content.length, totalPages: 1, size: 20, number: 0 }
    }
  }
}

async function setupAuth(
  page: import('@playwright/test').Page,
  user: typeof mockAdminStaffUser
) {
  await page.addInitScript(
    userData => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: user })
    })
  })
  await page.route('**/api/auth/refresh', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { accessToken: 'new-mock-token', refreshToken: 'new-mock-refresh' }
      })
    })
  })
}

test.describe('Medical Orders by State - cross-admission dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('loads with default action-needed filter and shows pending rows', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)

    let lastQuery = ''
    await page.route('**/api/v1/medical-orders**', async route => {
      lastQuery = new URL(route.request().url()).search
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(pagedResponse([requestedMed, authorizedLab, enProcesoLab]))
      })
    })

    await page.goto('/medical-orders')

    await expect(
      page.getByRole('heading', { name: /Medical Orders by State|Órdenes Médicas por Estado/i })
    ).toBeVisible()

    // Default filter sends status=SOLICITADO&status=AUTORIZADO&status=EN_PROCESO
    await expect.poll(() => lastQuery, { timeout: 5000 }).toContain('status=SOLICITADO')
    expect(lastQuery).toContain('status=AUTORIZADO')
    expect(lastQuery).toContain('status=EN_PROCESO')

    // All three rows show up (Ana García appears in two — the lab is in
    // both AUTORIZADO and EN_PROCESO buckets via authorizedLab/enProcesoLab).
    await expect(page.getByText('Juan Pérez')).toBeVisible()
    await expect(page.getByText('Ana García').first()).toBeVisible()
    await expect(page.getByText('Sertraline')).toBeVisible()
    await expect(page.getByText('Hemograma completo').first()).toBeVisible()
  })

  test('admin staff authorizes a SOLICITADO row from the dashboard', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)

    let authorized = false
    let authorizeCalled = false

    await page.route('**/api/v1/medical-orders**', async route => {
      const row = authorized
        ? { ...requestedMed, status: 'AUTORIZADO', authorizedAt: '2026-01-24T08:30:00' }
        : requestedMed
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(pagedResponse([row]))
      })
    })

    // Store auto-refreshes per-admission cache after every transition;
    // mock it with empty data so the dashboard reload isn't aborted.
    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { orders: {} } })
      })
    })

    await page.route(
      '**/api/v1/admissions/1/medical-orders/100/authorize',
      async route => {
        authorizeCalled = true
        authorized = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...requestedMed, status: 'AUTORIZADO' }
          })
        })
      }
    )

    await page.goto('/medical-orders')
    await expect(page.getByText('Sertraline')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Authorize is the first action button on the row (icon: pi-check, tooltip Authorize)
    const row = page.locator('tr').filter({ hasText: 'Sertraline' })
    await row.locator('button:has(.pi-check)').first().click()

    // ConfirmDialog appears
    await page.waitForTimeout(300)
    await page.evaluate(() => {
      const dialogs = document.querySelectorAll('.p-confirmdialog')
      for (const dialog of dialogs) {
        const acceptBtn = dialog.querySelector(
          '.p-confirmdialog-accept-button'
        ) as HTMLButtonElement | null
        if (acceptBtn && dialog.checkVisibility()) {
          acceptBtn.click()
          return
        }
      }
    })

    await expect.poll(() => authorizeCalled, { timeout: 5000 }).toBe(true)
    // Row reflects new state (label "Authorized")
    await expect(row.getByText(/^Authorized$|^Autorizado$/i)).toBeVisible({ timeout: 10000 })
  })

  test('nurse sees mark-in-progress on AUTORIZADO lab row but not on AUTORIZADO med', async ({
    page
  }) => {
    await setupAuth(page, mockNurseUser)

    // Lab in AUTORIZADO + a med in AUTORIZADO. Only the lab row should
    // surface the mark-in-progress action (results-bearing only).
    const authorizedMed = {
      ...requestedMed,
      id: 103,
      status: 'AUTORIZADO',
      authorizedAt: '2026-01-24T08:30:00'
    }

    await page.route('**/api/v1/medical-orders**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(pagedResponse([authorizedMed, authorizedLab]))
      })
    })

    await page.goto('/medical-orders')
    await expect(page.getByText('Hemograma completo')).toBeVisible()
    await expect(page.getByText('Sertraline')).toBeVisible()

    const labRow = page.locator('tr').filter({ hasText: 'Hemograma completo' })
    const medRow = page.locator('tr').filter({ hasText: 'Sertraline' })

    // Mark-in-progress button uses the play icon
    await expect(labRow.locator('button:has(.pi-play)')).toBeVisible()
    await expect(medRow.locator('button:has(.pi-play)')).not.toBeVisible()
  })

  test('doctor can emergency-authorize from the dashboard', async ({ page }) => {
    await setupAuth(page, mockDoctorWithEmergency)

    let authorized = false
    let capturedBody: { reason?: string; reasonNote?: string | null } = {}

    await page.route('**/api/v1/medical-orders**', async route => {
      const row = authorized
        ? {
            ...requestedMed,
            status: 'AUTORIZADO',
            authorizedAt: '2026-01-24T02:00:00',
            emergencyAuthorized: true
          }
        : requestedMed
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(pagedResponse([row]))
      })
    })

    await page.route(
      '**/api/v1/admissions/1/medical-orders/100/emergency-authorize',
      async route => {
        capturedBody = route.request().postDataJSON()
        authorized = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              ...requestedMed,
              status: 'AUTORIZADO',
              emergencyAuthorized: true,
              emergencyReason: capturedBody.reason,
              emergencyReasonNote: capturedBody.reasonNote ?? null
            }
          })
        })
      }
    )

    await page.goto('/medical-orders')
    await expect(page.getByText('Sertraline')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Emergency-authorize button (bolt icon) on the SOLICITADO row
    const row = page.locator('tr').filter({ hasText: 'Sertraline' })
    await row.locator('button:has(.pi-bolt)').click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await dialog.locator('label[for="reason-AFTER_HOURS_NO_ADMIN"]').click()
    await dialog
      .getByRole('button', { name: /Emergency authorize|Autorización de emergencia/i })
      .click()

    await expect
      .poll(() => capturedBody.reason, { timeout: 5000 })
      .toBe('AFTER_HOURS_NO_ADMIN')
  })

  test('clear filters fetches with no status/category params', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)

    const queries: string[] = []
    await page.route('**/api/v1/medical-orders**', async route => {
      queries.push(new URL(route.request().url()).search)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(pagedResponse([requestedMed]))
      })
    })

    await page.goto('/medical-orders')
    await expect(page.getByText('Sertraline')).toBeVisible()

    // Click "Clear filters" — it forces a refetch with no filters.
    await page
      .getByRole('button', { name: /Clear filters|Limpiar filtros/i })
      .click()

    await expect.poll(() => queries.length, { timeout: 5000 }).toBeGreaterThanOrEqual(2)
    const lastQuery = queries[queries.length - 1]
    expect(lastQuery).not.toContain('status=')
    expect(lastQuery).not.toContain('category=')
  })

  test('row without permission/state shows only the open-admission action', async ({ page }) => {
    // Nurse without authorize/discontinue/upload, viewing a SOLICITADO med:
    // no actionable buttons should appear, only the "Open admission" link icon.
    await setupAuth(page, mockNurseUser)

    await page.route('**/api/v1/medical-orders**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(pagedResponse([requestedMed]))
      })
    })

    await page.goto('/medical-orders')
    await expect(page.getByText('Sertraline')).toBeVisible()

    const row = page.locator('tr').filter({ hasText: 'Sertraline' })
    // The open-admission button (external-link icon) should be there
    await expect(row.locator('button:has(.pi-external-link)')).toBeVisible()
    // None of the workflow buttons
    await expect(row.locator('button:has(.pi-check)')).not.toBeVisible()
    await expect(row.locator('button:has(.pi-times)')).not.toBeVisible()
    await expect(row.locator('button:has(.pi-bolt)')).not.toBeVisible()
    await expect(row.locator('button:has(.pi-ban)')).not.toBeVisible()
  })
})
