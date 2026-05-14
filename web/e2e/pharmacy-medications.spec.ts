import { test, expect, type Page } from '@playwright/test'

// --- Mock users ---

const mockPharmacistAdmin = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  // Admin bypasses hasPermission, but list anyway for symmetry
  permissions: [
    'medication:read',
    'medication:create',
    'medication:update',
    'medication:expiry-report',
    'inventory-lot:read',
    'inventory-lot:create',
    'inventory-lot:update'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockNurseReadOnly = {
  id: 3,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Ana',
  lastName: 'Lopez',
  roles: ['NURSE'],
  permissions: ['medication:read', 'inventory-lot:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockUserNoPharmacy = {
  id: 4,
  username: 'psych',
  email: 'psych@example.com',
  firstName: 'Pablo',
  lastName: 'Solis',
  roles: ['PSYCHOLOGIST'],
  permissions: ['admission:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// --- Mock medications page ---

const mockMedicationsPage = {
  content: [
    {
      id: 11,
      itemId: 101,
      name: 'OLANZAPINA 5MG',
      description: null,
      sku: 'A12',
      price: 2.5,
      cost: 1.2,
      restockLevel: 10,
      quantity: 80,
      active: true,
      genericName: 'OLANZAPINA',
      commercialName: 'ZYPREXA',
      strength: '5 MG',
      dosageForm: 'TABLET',
      route: 'ORAL',
      controlled: false,
      atcCode: null,
      section: 'PSIQUIATRICO',
      reviewStatus: 'CONFIRMED',
      reviewNotes: null
    },
    {
      id: 12,
      itemId: 102,
      name: 'CLONAZEPAM 2MG',
      description: null,
      sku: 'A47',
      price: 1.8,
      cost: 0.9,
      restockLevel: 5,
      quantity: 40,
      active: true,
      genericName: 'CLONAZEPAM',
      commercialName: 'RIVOTRIL',
      strength: '2 MG',
      dosageForm: 'TABLET',
      route: 'ORAL',
      controlled: true,
      atcCode: null,
      section: 'PSIQUIATRICO',
      reviewStatus: 'NEEDS_REVIEW',
      reviewNotes: 'Backfilled from legacy free text'
    }
  ],
  page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
}

// --- Helpers ---

async function setupAuth(page: Page, user: typeof mockPharmacistAdmin) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

async function setupCommonMocks(page: Page, user: typeof mockPharmacistAdmin) {
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

async function setupMedicationsListMock(
  page: Page,
  body: typeof mockMedicationsPage = mockMedicationsPage,
  onRequest?: (url: string) => void
) {
  await page.route('**/api/v1/medications**', async route => {
    const url = route.request().url()
    onRequest?.(url)
    // Detail endpoint or expiry-report etc are handled by other routes; only intercept the list.
    if (/\/v1\/medications(\?|$)/.test(url) || url.match(/\/v1\/medications\?/)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: body })
      })
      return
    }
    await route.continue()
  })
}

// --- Tests ---

test.describe('Pharmacy medications - list (ADMIN with full permissions)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('renders rows, NEEDS_REVIEW badge and controlled badge', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)
    await setupMedicationsListMock(page)

    await page.goto('/pharmacy')

    await expect(page.getByRole('heading', { name: /Pharmacy/i })).toBeVisible()
    await expect(page.getByText('OLANZAPINA')).toBeVisible()
    await expect(page.getByText('CLONAZEPAM')).toBeVisible()

    // Needs-review badge is rendered only for the CLONAZEPAM row
    const clonRow = page.locator('tr', { hasText: 'CLONAZEPAM' })
    await expect(clonRow.getByText(/Needs review/i)).toBeVisible()
    // Controlled column shows "Yes" tag for controlled drug
    await expect(clonRow.getByText(/^Yes$/)).toBeVisible()

    // OLANZAPINA row has neither
    const olRow = page.locator('tr', { hasText: 'OLANZAPINA' })
    await expect(olRow.getByText(/Needs review/i)).toHaveCount(0)
  })

  test('section filter tabs scope the request', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)

    const seen: string[] = []
    await setupMedicationsListMock(page, mockMedicationsPage, url => seen.push(url))

    await page.goto('/pharmacy')
    await expect(page.getByText('OLANZAPINA')).toBeVisible()

    // Click the AMPOLLA tab
    await page.locator('.p-tab').filter({ hasText: /Ampoules|AMPOLLA/i }).click()

    await page.waitForFunction(
      () =>
        Array.from(performance.getEntries())
          .map(e => e.name)
          .some(n => n.includes('section=AMPOLLA')),
      undefined,
      { timeout: 5000 }
    ).catch(() => {
      // Fall back to inspecting the captured URLs
    })

    expect(seen.some(u => u.includes('section=AMPOLLA'))).toBeTruthy()
  })

  test('search query is forwarded to the API', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)

    const seen: string[] = []
    await setupMedicationsListMock(page, mockMedicationsPage, url => seen.push(url))

    await page.goto('/pharmacy')
    await expect(page.getByText('OLANZAPINA')).toBeVisible()

    const searchInput = page.locator('.filters input[type="text"]')
    await searchInput.fill('olanz')
    await searchInput.press('Enter')

    await expect
      .poll(() => seen.some(u => u.includes('search=olanz')), { timeout: 5000 })
      .toBeTruthy()
  })

  test('clicking the row eye icon routes to /pharmacy/medications/:itemId', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)
    await setupMedicationsListMock(page)

    // Detail endpoint mock (single medication)
    await page.route('**/api/v1/medications/101', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMedicationsPage.content[0] })
      })
    })
    await page.route('**/api/v1/inventory/items/101/lots', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.goto('/pharmacy')
    await expect(page.getByText('OLANZAPINA')).toBeVisible()

    const olRow = page.locator('tr', { hasText: 'OLANZAPINA' })
    await olRow.locator('button .pi-eye').click()

    await expect(page).toHaveURL(/\/pharmacy\/medications\/101$/, { timeout: 10000 })
  })

  test('"New Medication" dialog has NO category selector (FR-9)', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)
    await setupMedicationsListMock(page)

    await page.goto('/pharmacy')
    await page.getByRole('button', { name: /New Medication/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()

    // FR-9: category selector removed; only kind-default routing remains.
    // The form must NOT expose any field labeled "Category" / "Categoría".
    await expect(dialog.getByText(/^Category$/i)).toHaveCount(0)
    await expect(dialog.getByText(/^Categoría$/i)).toHaveCount(0)

    // Other expected fields are still present
    await expect(dialog.getByText(/Generic Name/i)).toBeVisible()
    await expect(dialog.getByText(/^Section$/i)).toBeVisible()
    await expect(dialog.getByText(/Dosage Form/i)).toBeVisible()
  })

  test('"New Medication" POST omits categoryId in payload', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)
    await setupMedicationsListMock(page)

    let capturedBody: Record<string, unknown> | null = null
    await page.route('**/api/v1/medications', async route => {
      if (route.request().method() === 'POST') {
        capturedBody = (await route.request().postDataJSON()) as Record<string, unknown>
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...mockMedicationsPage.content[0], id: 99, itemId: 999 }
          })
        })
        return
      }
      await route.continue()
    })

    await page.goto('/pharmacy')
    await page.getByRole('button', { name: /New Medication/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()

    await dialog.locator('input').nth(0).fill('TEST MED 10MG') // name
    // genericName is the 3rd InputText field (after name, sku)
    await dialog.locator('input').nth(2).fill('TEST') // generic name

    await dialog.getByRole('button', { name: /^Save$/i }).click()

    await expect
      .poll(() => capturedBody, { timeout: 5000 })
      .not.toBeNull()
    expect(capturedBody).not.toHaveProperty('categoryId')
    // Sanity: required fields are present
    expect(capturedBody).toMatchObject({
      genericName: 'TEST',
      section: expect.any(String),
      dosageForm: expect.any(String)
    })
  })
})

test.describe('Pharmacy medications - nav and permissions', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('side-nav Pharmacy section is visible with medication:read', async ({ page }) => {
    await setupAuth(page, mockNurseReadOnly)
    await setupCommonMocks(page, mockNurseReadOnly)
    await setupMedicationsListMock(page, { ...mockMedicationsPage, content: [] })

    await page.goto('/pharmacy')
    // Side-nav root group label "Pharmacy"
    await expect(page.locator('.layout-sidebar').getByText(/^Pharmacy$/).first()).toBeVisible()
    // Sub-item "Medications"
    await expect(page.locator('.layout-sidebar').getByText(/^Medications$/).first()).toBeVisible()
    // Without medication:expiry-report, the "Expirations" sub-item is hidden
    await expect(page.locator('.layout-sidebar').getByText(/^Expirations$/)).toHaveCount(0)
  })

  test('nurse without medication:create does NOT see "New Medication" button', async ({ page }) => {
    await setupAuth(page, mockNurseReadOnly)
    await setupCommonMocks(page, mockNurseReadOnly)
    await setupMedicationsListMock(page)

    await page.goto('/pharmacy')
    await expect(page.getByText('OLANZAPINA')).toBeVisible()
    await expect(page.getByRole('button', { name: /New Medication/i })).toHaveCount(0)
  })

  test('user without medication:read is redirected away from /pharmacy', async ({ page }) => {
    await setupAuth(page, mockUserNoPharmacy)
    await setupCommonMocks(page, mockUserNoPharmacy)

    await page.goto('/pharmacy')
    // Route guard redirects users lacking the required permission.
    // We just assert that we did NOT land on the pharmacy list.
    await page.waitForLoadState('networkidle')
    expect(page.url()).not.toMatch(/\/pharmacy(\?|$)/)
  })
})
