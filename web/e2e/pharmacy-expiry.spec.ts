import { test, expect, type Page } from '@playwright/test'

// --- Mock users ---

const mockPharmacistAdmin = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockNurseNoExpiry = {
  id: 3,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Ana',
  lastName: 'Lopez',
  roles: ['NURSE'],
  // `admission:read` is granted so the post-redirect destination
  // (nurse → /dashboard → /nursing-kardex) can resolve. Without it the
  // router thrashes between dashboard and nursing-kardex and the URL
  // never leaves /pharmacy/expiry-report.
  permissions: ['medication:read', 'inventory-lot:read', 'admission:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// --- Mock report payload ---

const mockReport = {
  generatedAt: '2026-05-14T10:15:00',
  totals: { EXPIRED: 1, RED: 2, YELLOW: 3, GREEN: 4, NO_EXPIRY: 1 },
  items: [
    {
      lotId: 9001,
      itemId: 101,
      sku: 'A12',
      genericName: 'OLANZAPINA',
      commercialName: 'ZYPREXA',
      strength: '5 MG',
      section: 'PSIQUIATRICO',
      lotNumber: 'L-EXP',
      expirationDate: '2025-12-01',
      daysToExpiry: -164,
      status: 'EXPIRED',
      quantityOnHand: 5,
      recalled: false
    },
    {
      lotId: 9002,
      itemId: 102,
      sku: 'A47',
      genericName: 'CLONAZEPAM',
      commercialName: 'RIVOTRIL',
      strength: '2 MG',
      section: 'PSIQUIATRICO',
      lotNumber: 'L-RED',
      expirationDate: '2026-06-01',
      daysToExpiry: 18,
      status: 'RED',
      quantityOnHand: 30,
      recalled: false
    },
    {
      lotId: 9003,
      itemId: 103,
      sku: 'C04',
      genericName: 'AMOXICILINA',
      commercialName: null,
      strength: '500 MG',
      section: 'NO_PSIQUIATRICO',
      lotNumber: 'L-YEL',
      expirationDate: '2026-07-15',
      daysToExpiry: 62,
      status: 'YELLOW',
      quantityOnHand: 120,
      recalled: false
    },
    {
      lotId: 9004,
      itemId: 104,
      sku: 'D11',
      genericName: 'DICLOFENACO',
      commercialName: 'VOLTAREN',
      strength: '75MG/3ML',
      section: 'AMPOLLA',
      lotNumber: 'L-GRN',
      expirationDate: '2027-04-30',
      daysToExpiry: 351,
      status: 'GREEN',
      quantityOnHand: 50,
      recalled: false
    },
    {
      lotId: 9005,
      itemId: 105,
      sku: 'A99',
      genericName: 'LEGACY-DRUG',
      commercialName: null,
      strength: null,
      section: 'PSIQUIATRICO',
      lotNumber: null,
      expirationDate: '9999-12-31',
      daysToExpiry: null,
      status: 'NO_EXPIRY',
      quantityOnHand: 2,
      recalled: false
    }
  ]
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

async function setupExpiryReport(
  page: Page,
  body = mockReport,
  onRequest?: (url: string) => void
) {
  await page.route('**/api/v1/medications/expiry-report**', async route => {
    onRequest?.(route.request().url())
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: body })
    })
  })
}

// --- Tests ---

test.describe('Expiry dashboard (ADMIN)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('renders totals chips and rows for every status bucket', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)
    await setupExpiryReport(page)

    await page.goto('/pharmacy/expiry-report')

    await expect(page.getByRole('heading', { name: /Expirations/i })).toBeVisible()

    // One row per lot
    await expect(page.getByText('L-EXP')).toBeVisible()
    await expect(page.getByText('L-RED')).toBeVisible()
    await expect(page.getByText('L-YEL')).toBeVisible()
    await expect(page.getByText('L-GRN')).toBeVisible()

    // Color-coded chips on each row (the ExpiryStatusChip is a PrimeVue Tag)
    const expRow = page.locator('tr', { hasText: 'L-EXP' })
    await expect(expRow.getByText(/^Expired$/i)).toBeVisible()

    const redRow = page.locator('tr', { hasText: 'L-RED' })
    await expect(redRow.getByText(/^Critical$/i)).toBeVisible()

    const yelRow = page.locator('tr', { hasText: 'L-YEL' })
    await expect(yelRow.getByText(/^Upcoming$/i)).toBeVisible()

    const grnRow = page.locator('tr', { hasText: 'L-GRN' })
    await expect(grnRow.getByText(/^Fresh$/i)).toBeVisible()

    // Totals section shows one chip per status with its count
    const totals = page.locator('.totals')
    await expect(totals.locator('.total-cell')).toHaveCount(5)
    await expect(totals.locator('.total-cell', { hasText: /Expired/i }).locator('.count'))
      .toHaveText('1')
    await expect(totals.locator('.total-cell', { hasText: /Critical/i }).locator('.count'))
      .toHaveText('2')
    await expect(totals.locator('.total-cell', { hasText: /Upcoming/i }).locator('.count'))
      .toHaveText('3')
    await expect(totals.locator('.total-cell', { hasText: /Fresh/i }).locator('.count'))
      .toHaveText('4')
    await expect(totals.locator('.total-cell', { hasText: /No expiry/i }).locator('.count'))
      .toHaveText('1')
  })

  test('section filter is forwarded to the backend', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)

    const seen: string[] = []
    await setupExpiryReport(page, mockReport, url => seen.push(url))

    await page.goto('/pharmacy/expiry-report')
    await expect(page.getByText('L-EXP')).toBeVisible()

    // Change the Section dropdown to AMPOLLA. The filters row has two
    // PrimeVue Selects: index 0 = Section, index 1 = Controlled.
    const sectionSelect = page.locator('.filters .p-select').nth(0)
    await sectionSelect.click()
    await page.locator('.p-select-option').filter({ hasText: /Ampoules/i }).click()

    // Click Search button
    await page.getByRole('button', { name: /Search/i }).click()

    await expect
      .poll(() => seen.some(u => u.includes('section=AMPOLLA')), { timeout: 5000 })
      .toBeTruthy()
  })

  test('default window params (90 / 30) are sent on first load', async ({ page }) => {
    await setupAuth(page, mockPharmacistAdmin)
    await setupCommonMocks(page, mockPharmacistAdmin)

    const seen: string[] = []
    await setupExpiryReport(page, mockReport, url => seen.push(url))

    await page.goto('/pharmacy/expiry-report')
    await expect(page.getByText('L-EXP')).toBeVisible()

    expect(seen.some(u => u.includes('window=90'))).toBeTruthy()
    expect(seen.some(u => u.includes('urgentWindow=30'))).toBeTruthy()
  })
})

test.describe('Expiry dashboard - permission gating', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('user without medication:expiry-report is redirected away', async ({ page }) => {
    await setupAuth(page, mockNurseNoExpiry)
    await setupCommonMocks(page, mockNurseNoExpiry)
    await setupExpiryReport(page)

    await page.goto('/pharmacy/expiry-report')
    // The route guard redirects users who lack `medication:expiry-report`;
    // the post-redirect destination (e.g. nursing-kardex) keeps polling so
    // `networkidle` never settles. Just poll the URL instead.
    await expect
      .poll(() => page.url(), { timeout: 10000 })
      .not.toMatch(/\/pharmacy\/expiry-report/)
  })
})
