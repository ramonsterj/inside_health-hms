import { test, expect, type Page } from '@playwright/test'
import { confirmDialogAccept } from './utils/test-helpers'

// --- Mock users ---

const mockAdmin = {
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

const mockMedication = {
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
  reviewStatus: 'NEEDS_REVIEW',
  reviewNotes: 'Backfilled from legacy free text'
}

const mockLots = [
  {
    id: 5001,
    itemId: 101,
    itemName: 'OLANZAPINA 5MG',
    itemSku: 'A12',
    lotNumber: 'L-2025-08',
    expirationDate: '2026-07-01',
    quantityOnHand: 14,
    receivedAt: '2025-08-01',
    supplier: 'Distribuidora ABC',
    notes: null,
    recalled: false,
    recalledReason: null,
    syntheticLegacy: false,
    createdAt: '2025-08-01T08:00:00',
    updatedAt: '2025-08-01T08:00:00'
  },
  {
    id: 5002,
    itemId: 101,
    itemName: 'OLANZAPINA 5MG',
    itemSku: 'A12',
    lotNumber: 'L-2024-12',
    expirationDate: '2026-06-15',
    quantityOnHand: 0,
    receivedAt: '2024-12-01',
    supplier: 'Distribuidora ABC',
    notes: null,
    recalled: true,
    recalledReason: 'Vendor recall notice',
    syntheticLegacy: false,
    createdAt: '2024-12-01T08:00:00',
    updatedAt: '2026-01-15T08:00:00'
  }
]

// --- Helpers ---

async function setupAuth(page: Page, user: typeof mockAdmin) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

async function setupCommonMocks(page: Page, user: typeof mockAdmin) {
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
  await page.route('**/api/v1/medications/101', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockMedication })
    })
  })
}

async function setupLotsList(page: Page, lots = mockLots) {
  await page.route('**/api/v1/inventory/items/101/lots', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: lots })
      })
      return
    }
    await route.continue()
  })
}

// --- Tests ---

test.describe('PharmacyDetailView + LotListPanel (ADMIN)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('renders medication header, NEEDS_REVIEW badge, and lot rows', async ({ page }) => {
    await setupAuth(page, mockAdmin)
    await setupCommonMocks(page, mockAdmin)
    await setupLotsList(page)

    await page.goto('/pharmacy/medications/101')

    // Header
    await expect(page.getByRole('heading', { name: 'OLANZAPINA' })).toBeVisible()

    // NEEDS_REVIEW badge on the medication card
    await expect(page.getByText(/Needs review/i).first()).toBeVisible()

    // Lot table content
    const lotPanel = page.locator('.lot-panel')
    await expect(lotPanel).toBeVisible()
    await expect(lotPanel.getByText('L-2025-08')).toBeVisible()
    await expect(lotPanel.getByText('L-2024-12')).toBeVisible()

    // Recalled badge on the second lot row
    const recalledRow = lotPanel.locator('tr', { hasText: 'L-2024-12' })
    await expect(recalledRow.getByText(/^Yes$/)).toBeVisible()
  })

  test('"New Lot" dialog opens and hides recall toggle in create mode', async ({ page }) => {
    await setupAuth(page, mockAdmin)
    await setupCommonMocks(page, mockAdmin)
    await setupLotsList(page)

    await page.goto('/pharmacy/medications/101')

    await page.getByRole('button', { name: /New Lot/i }).click()

    const dialog = page.locator('[role="dialog"]').filter({ hasText: /New Lot/i })
    await expect(dialog).toBeVisible()

    // Create-mode fields
    await expect(dialog.getByText(/Lot Number/i)).toBeVisible()
    await expect(dialog.getByText(/Quantity/i)).toBeVisible()
    await expect(dialog.getByText(/Received On/i)).toBeVisible()

    // Recall toggle is only available when editing an existing lot
    await expect(dialog.getByText(/^Recalled$/)).toHaveCount(0)
  })

  test('edit dialog exposes the recall toggle (admin has inventory-lot:update)', async ({ page }) => {
    await setupAuth(page, mockAdmin)
    await setupCommonMocks(page, mockAdmin)
    await setupLotsList(page)

    await page.goto('/pharmacy/medications/101')

    // Click the pencil (edit) icon on the first lot row
    const lotPanel = page.locator('.lot-panel')
    const firstRow = lotPanel.locator('tr', { hasText: 'L-2025-08' })
    await firstRow.locator('button .pi-pencil').click()

    const dialog = page.locator('[role="dialog"]').filter({ hasText: /Edit Lot/i })
    await expect(dialog).toBeVisible()

    // Recall toggle is rendered for edit + canRecall
    await expect(dialog.getByText(/^Recalled$/)).toBeVisible()

    // Quantity / Received On are hidden in edit mode (only set at creation)
    await expect(dialog.getByText(/^Quantity$/)).toHaveCount(0)
    await expect(dialog.getByText(/Received On/i)).toHaveCount(0)
  })

  test('deleting a lot triggers a confirm dialog and calls DELETE', async ({ page }) => {
    await setupAuth(page, mockAdmin)
    await setupCommonMocks(page, mockAdmin)
    await setupLotsList(page)

    let deleteCalled = false
    await page.route('**/api/v1/inventory/lots/5001', async route => {
      if (route.request().method() === 'DELETE') {
        deleteCalled = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
        return
      }
      await route.continue()
    })

    await page.goto('/pharmacy/medications/101')

    const firstRow = page.locator('.lot-panel tr', { hasText: 'L-2025-08' })
    await firstRow.locator('button .pi-trash').click()

    await confirmDialogAccept(page)

    await expect.poll(() => deleteCalled, { timeout: 5000 }).toBeTruthy()
  })
})

test.describe('LotListPanel - read-only nurse', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('hides "New Lot" button and edit/delete actions without create/update perms', async ({
    page
  }) => {
    await setupAuth(page, mockNurseReadOnly)
    await setupCommonMocks(page, mockNurseReadOnly)
    await setupLotsList(page)

    await page.goto('/pharmacy/medications/101')

    const lotPanel = page.locator('.lot-panel')
    await expect(lotPanel).toBeVisible()
    await expect(lotPanel.getByText('L-2025-08')).toBeVisible()

    // No "New Lot" button (lacks inventory-lot:create)
    await expect(page.getByRole('button', { name: /New Lot/i })).toHaveCount(0)
    // No row actions (lacks inventory-lot:update)
    await expect(lotPanel.locator('button .pi-pencil')).toHaveCount(0)
    await expect(lotPanel.locator('button .pi-trash')).toHaveCount(0)
  })
})
