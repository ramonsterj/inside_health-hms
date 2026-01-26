import { test, expect } from '@playwright/test'
import {
  confirmDialogAccept,
  waitForOverlaysToClear,
  waitForTableData
} from './utils/test-helpers'

// Mock user data
const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'triage-code:read',
    'triage-code:create',
    'triage-code:update',
    'triage-code:delete'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockAdminStaffUser = {
  id: 2,
  username: 'receptionist',
  email: 'receptionist@example.com',
  firstName: 'Reception',
  lastName: 'Staff',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: ['triage-code:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockTriageCodes = [
  { id: 1, code: 'A', color: '#FF0000', description: 'Critical - Immediate attention', displayOrder: 1 },
  { id: 2, code: 'B', color: '#FFA500', description: 'Urgent', displayOrder: 2 },
  { id: 3, code: 'C', color: '#FFFF00', description: 'Semi-urgent', displayOrder: 3 },
  { id: 4, code: 'D', color: '#00FF00', description: 'Standard', displayOrder: 4 },
  { id: 5, code: 'E', color: '#0000FF', description: 'Non-urgent', displayOrder: 5 }
]

// Helper function to setup authenticated state
async function setupAuth(page: import('@playwright/test').Page, user: typeof mockAdminUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

// Helper function to setup API mocks for admin
async function setupAdminMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockAdminUser })
    })
  })

  // Mock auth refresh to prevent session expiration
  await page.route('**/api/auth/refresh', async (route) => {
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

// Helper function to setup API mocks for admin staff
async function setupAdminStaffMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockAdminStaffUser })
    })
  })

  // Mock auth refresh to prevent session expiration
  await page.route('**/api/auth/refresh', async (route) => {
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

// waitForOverlaysToClear is now imported from shared utils

test.describe('Triage Codes - Admin Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view triage codes list', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes')

    // Should see the triage codes list
    await expect(page.getByRole('heading', { name: /Triage Codes/i })).toBeVisible()
    await expect(page.getByText('Critical - Immediate attention')).toBeVisible()
  })

  test('can see New Triage Code button', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes')

    // Should see the New Triage Code button
    await expect(
      page.getByRole('button', { name: /New Triage Code|Nuevo Código de Triaje/i })
    ).toBeVisible()
  })

  test('can navigate to create triage code form', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes')

    await page.getByRole('button', { name: /New Triage Code|Nuevo Código de Triaje/i }).click()

    await expect(page).toHaveURL(/\/admin\/triage-codes\/new/)
  })

  test('triage code form displays all required fields', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/triage-codes/new')

    // Check that all required form fields are present by looking for labels
    await expect(page.locator('label[for="code"]')).toBeVisible()
    await expect(page.locator('label[for="color"]')).toBeVisible()
    await expect(page.locator('label[for="description"]')).toBeVisible()
    await expect(page.locator('label[for="displayOrder"]')).toBeVisible()
  })

  test('can create new triage code', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const newTriageCode = {
      id: 6,
      code: 'X',
      color: '#FF00FF',
      description: 'Test triage code',
      displayOrder: 99
    }

    await page.route('**/api/v1/triage-codes', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newTriageCode })
        })
      } else if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [...mockTriageCodes, newTriageCode] })
        })
      }
    })

    await page.goto('/admin/triage-codes/new')

    // Fill the form using id selectors
    await page.locator('#code').fill('X')
    await page.locator('#description').fill('Test triage code')

    // Fill display order - find the InputNumber component by its id
    const displayOrderInput = page.locator('#displayOrder input, input[name="displayOrder"]')
    await displayOrderInput.fill('99')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to list
    await expect(page).toHaveURL(/\/admin\/triage-codes$/, { timeout: 10000 })
  })

  test('shows validation errors for invalid color format', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/triage-codes/new')

    // Fill with valid code
    await page.locator('#code').fill('X')

    // Clear the color field (it has a default value) and set invalid value
    await page.locator('.color-input').clear()
    await page.locator('.color-input').fill('invalid-color')

    // Try to submit
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show validation error for invalid color format
    await expect(page.locator('.p-message-error').first()).toBeVisible()
  })

  test('can edit existing triage code', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const triageCode = mockTriageCodes[0]

    await page.route('**/api/v1/triage-codes/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: triageCode })
        })
      } else if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...triageCode, description: 'Updated description' }
          })
        })
      }
    })

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes/1/edit')

    // Update the description using id selector
    await page.locator('#description').fill('Updated description')

    // Submit
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to list
    await expect(page).toHaveURL(/\/admin\/triage-codes$/, { timeout: 10000 })
  })

  test('can delete triage code', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.route('**/api/v1/triage-codes/5', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, message: 'Triage code deleted' })
        })
      }
    })

    await page.goto('/admin/triage-codes')

    // Wait for table to load and overlays to clear
    await expect(page.getByText('Non-urgent')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Find and click delete button for code E (id: 5, not in use)
    const row = page.locator('tr').filter({ hasText: 'Non-urgent' })
    const deleteBtn = row.locator('button[aria-label="Delete"], button:has(.pi-trash)')
    await deleteBtn.click()

    // Confirm deletion using shared helper
    await confirmDialogAccept(page)

    // Should show success message
    await expect(page.getByText(/deleted|eliminado/i)).toBeVisible({ timeout: 10000 })
  })

  test('shows error when deleting triage code in use', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.route('**/api/v1/triage-codes/1', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Cannot delete triage code that is in use by active admissions'
          })
        })
      }
    })

    await page.goto('/admin/triage-codes')

    // Wait for table to load and overlays to clear
    await expect(page.getByText('Critical')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Find and click delete button for code A (id: 1)
    const row = page.locator('tr').filter({ hasText: 'Critical' })
    const deleteBtn = row.locator('button[aria-label="Delete"], button:has(.pi-trash)')
    await deleteBtn.click()

    // Confirm deletion using shared helper
    await confirmDialogAccept(page)

    // Should show error message
    await expect(page.getByText(/in use|en uso|Cannot delete/i)).toBeVisible({ timeout: 10000 })
  })

  test('triage codes are sorted by display order', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes')

    // Wait for data to load before reading rows
    await waitForTableData(page, 'Critical')

    // Get all code cells
    const rows = page.locator('tbody tr')
    const firstCode = await rows.nth(0).locator('td').first().textContent()
    const lastCode = await rows.nth(4).locator('td').first().textContent()

    // First should be A (displayOrder: 1), last should be E (displayOrder: 5)
    expect(firstCode).toContain('A')
    expect(lastCode).toContain('E')
  })
})

test.describe('Triage Codes - Admin Staff (Read Only)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view triage codes list but cannot see create button', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes')

    // Staff can access the page and see the list
    await expect(page.getByRole('heading', { name: /Triage Codes/i })).toBeVisible()
    await expect(page.getByText('Critical - Immediate attention')).toBeVisible()

    // But should NOT see the New Triage Code button (requires triage-code:create)
    await expect(
      page.getByRole('button', { name: /New Triage Code|Nuevo Código de Triaje/i })
    ).not.toBeVisible()
  })

  test('cannot see edit and delete buttons in triage codes list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes')

    // Wait for table to load
    await expect(page.getByText('Critical - Immediate attention')).toBeVisible()

    // Staff should not see edit or delete buttons (requires triage-code:update and triage-code:delete)
    const row = page.locator('tr').filter({ hasText: 'Critical' })
    await expect(row.getByRole('button', { name: /Edit|Editar/i })).not.toBeVisible()
    await expect(row.getByRole('button', { name: /Delete|Eliminar/i })).not.toBeVisible()
  })
})

test.describe('Triage Codes - Color Display', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('triage codes display color indicators', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.goto('/admin/triage-codes')

    // Should see color indicators (badges, dots, or styled elements)
    // Look for elements with background color style
    const colorIndicator = page.locator('[style*="background"]').first()
    await expect(colorIndicator).toBeVisible()
  })
})
