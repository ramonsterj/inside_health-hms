import { test, expect, type Page } from '@playwright/test'

const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'admission:read',
    'billing:read',
    'billing:create',
    'billing:adjust',
    'invoice:read',
    'invoice:create'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockReadOnlyUser = {
  id: 2,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Nurse',
  lastName: 'User',
  roles: ['NURSE'],
  permissions: ['admission:read', 'billing:read', 'invoice:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockCharges = [
  {
    id: 1,
    admissionId: 10,
    chargeType: 'SERVICE',
    description: 'Physical therapy session',
    quantity: 1,
    unitPrice: 150.0,
    totalAmount: 150.0,
    inventoryItemName: null,
    roomNumber: null,
    invoiced: false,
    reason: null,
    chargeDate: '2026-02-07',
    createdAt: '2026-02-07T14:30:00',
    createdByName: 'Admin User'
  },
  {
    id: 2,
    admissionId: 10,
    chargeType: 'MEDICATION',
    description: 'Amoxicillin 500mg',
    quantity: 3,
    unitPrice: 25.0,
    totalAmount: 75.0,
    inventoryItemName: 'Amoxicillin 500mg',
    roomNumber: null,
    invoiced: false,
    reason: null,
    chargeDate: '2026-02-07',
    createdAt: '2026-02-07T14:30:00',
    createdByName: 'Admin User'
  }
]

const mockBalance = {
  admissionId: 10,
  patientName: 'Juan Perez',
  admissionDate: '2026-02-01',
  totalBalance: 225.0,
  dailyBreakdown: [
    {
      date: '2026-02-07',
      charges: [
        {
          id: 1,
          chargeType: 'SERVICE',
          description: 'Physical therapy session',
          quantity: 1,
          unitPrice: 150.0,
          totalAmount: 150.0
        },
        {
          id: 2,
          chargeType: 'MEDICATION',
          description: 'Amoxicillin 500mg',
          quantity: 3,
          unitPrice: 25.0,
          totalAmount: 75.0
        }
      ],
      dailyTotal: 225.0,
      cumulativeTotal: 225.0
    }
  ]
}

const mockInvoice = {
  id: 1,
  invoiceNumber: 'INV-2026-0001',
  admissionId: 10,
  patientName: 'Juan Perez',
  admissionDate: '2026-02-01',
  dischargeDate: '2026-02-07',
  totalAmount: 225.0,
  chargeCount: 2,
  chargeSummary: [
    { chargeType: 'MEDICATION', count: 1, subtotal: 75.0 },
    { chargeType: 'SERVICE', count: 1, subtotal: 150.0 }
  ],
  generatedAt: '2026-02-07T15:00:00',
  generatedByName: 'Admin User'
}

async function setupAuth(page: Page, user: typeof mockAdminUser = mockAdminUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

async function setupCommonMocks(page: Page, user: typeof mockAdminUser = mockAdminUser) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: user })
    })
  })

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

async function setupReadOnlyMocks(page: Page) {
  await setupAuth(page, mockReadOnlyUser)
  await setupCommonMocks(page, mockReadOnlyUser)
}

test.describe('Billing - Charges View', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
  })

  test('should display charges list', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/charges', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCharges })
      })
    )

    await page.goto('/admissions/10/charges')
    await expect(page.getByText('Physical therapy session')).toBeVisible()
    await expect(page.getByText('Amoxicillin 500mg')).toBeVisible()
  })

  test('should show empty state when no charges', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/charges', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/admissions/10/charges')
    await expect(page.getByText('No charges found')).toBeVisible()
  })

  test('should open create charge dialog', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/charges', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/admissions/10/charges')
    await page.getByRole('button', { name: 'New Charge' }).click()
    await expect(page.locator('.p-dialog')).toBeVisible()
  })

  test('should create a manual charge', async ({ page }) => {
    const newCharge = {
      id: 3,
      admissionId: 10,
      chargeType: 'SERVICE',
      description: 'Lab work',
      quantity: 1,
      unitPrice: 200.0,
      totalAmount: 200.0,
      inventoryItemName: null,
      roomNumber: null,
      invoiced: false,
      reason: null,
      chargeDate: '2026-02-08',
      createdAt: '2026-02-08T10:00:00',
      createdByName: 'Admin User'
    }

    await page.route('**/api/v1/admissions/10/charges', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newCharge })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockCharges })
        })
      }
    })

    await page.goto('/admissions/10/charges')
    await page.getByRole('button', { name: 'New Charge' }).click()

    // Fill charge form
    await page.locator('#chargeType').click()
    await page.locator('.p-select-option').filter({ hasText: 'Service' }).click()
    await page.locator('#description').fill('Lab work')
    await page.locator('#quantity input').fill('1')
    await page.locator('#unitPrice input').fill('200')

    await page.getByRole('button', { name: 'Save' }).click()

    // Assert success toast
    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
  })

  test('should create an adjustment', async ({ page }) => {
    const newAdjustment = {
      id: 4,
      admissionId: 10,
      chargeType: 'ADJUSTMENT',
      description: 'Billing correction',
      quantity: 1,
      unitPrice: -50.0,
      totalAmount: -50.0,
      inventoryItemName: null,
      roomNumber: null,
      invoiced: false,
      reason: 'Overcharged on previous visit',
      chargeDate: '2026-02-08',
      createdAt: '2026-02-08T10:00:00',
      createdByName: 'Admin User'
    }

    await page.route('**/api/v1/admissions/10/charges', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCharges })
      })
    })

    await page.route('**/api/v1/admissions/10/adjustments', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newAdjustment })
        })
      }
    })

    await page.goto('/admissions/10/charges')
    await page.getByRole('button', { name: 'New Adjustment' }).click()

    // Fill adjustment form
    await page.locator('#description').fill('Billing correction')
    await page.locator('#amount input').fill('-50')
    await page.locator('#reason').fill('Overcharged on previous visit')

    await page.getByRole('button', { name: 'Save' }).click()

    // Assert success toast
    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
  })

  test('should show validation errors when creating charge without fields', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/charges', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/admissions/10/charges')
    await page.getByRole('button', { name: 'New Charge' }).click()

    // Submit without filling fields
    await page.getByRole('button', { name: 'Save' }).click()

    // Assert validation error messages appear
    await expect(page.locator('.p-message').first()).toBeVisible({ timeout: 5000 })
  })

  test('should show validation error when adjustment reason is empty', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/charges', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/admissions/10/charges')
    await page.getByRole('button', { name: 'New Adjustment' }).click()

    // Fill description and amount but leave reason empty
    await page.locator('#description').fill('Some adjustment')
    await page.locator('#amount input').fill('-25')

    await page.getByRole('button', { name: 'Save' }).click()

    // Assert reason validation error visible
    const reasonError = page.locator('#reason').locator('..').locator('.p-message')
    await expect(reasonError).toBeVisible({ timeout: 5000 })
  })
})

test.describe('Billing - Balance View', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
  })

  test('should display balance with daily breakdown', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/balance', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockBalance })
      })
    )

    await page.goto('/admissions/10/balance')
    await expect(page.getByText('Juan Perez')).toBeVisible()
    await expect(page.getByText('Physical therapy session')).toBeVisible()
  })

  test('should show zero balance when no charges', async ({ page }) => {
    const emptyBalance = {
      ...mockBalance,
      totalBalance: 0,
      dailyBreakdown: []
    }

    await page.route('**/api/v1/admissions/10/balance', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: emptyBalance })
      })
    )

    await page.goto('/admissions/10/balance')
    await expect(page.getByText('No charges found')).toBeVisible()
  })
})

test.describe('Billing - Invoice View', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
  })

  test('should display invoice details', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/invoice', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockInvoice })
      })
    )

    await page.goto('/admissions/10/invoice')
    await expect(page.getByText('INV-2026-0001')).toBeVisible()
    await expect(page.getByText('Juan Perez')).toBeVisible()
  })

  test('should show no invoice message and generate button', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/invoice', (route) =>
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          error: { code: 'NOT_FOUND', message: 'Invoice not found' }
        })
      })
    )

    await page.goto('/admissions/10/invoice')
    await expect(
      page.getByText('No invoice has been generated for this admission yet.')
    ).toBeVisible()
    await expect(page.getByRole('button', { name: 'Generate Invoice' })).toBeVisible()
  })

  test('should generate invoice', async ({ page }) => {
    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().url().endsWith('/admissions/10')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 10,
              status: 'DISCHARGED',
              patientName: 'Juan Perez',
              admissionDate: '2026-02-01',
              dischargeDate: '2026-02-07'
            }
          })
        })
      } else {
        await route.continue()
      }
    })

    await page.route('**/api/v1/admissions/10/invoice', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockInvoice })
        })
      } else {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            error: { code: 'NOT_FOUND', message: 'Invoice not found' }
          })
        })
      }
    })

    await page.goto('/admissions/10/invoice')

    // Verify "No invoice" message and generate button visible
    await expect(
      page.getByText('No invoice has been generated for this admission yet.')
    ).toBeVisible()
    const generateBtn = page.getByRole('button', { name: 'Generate Invoice' })
    await expect(generateBtn).toBeVisible()

    // Click generate
    await generateBtn.click()

    // Confirmation dialog should appear
    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Click "Generate" button in dialog
    await dialog.getByRole('button', { name: 'Generate' }).click()

    // Assert success toast
    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
  })
})

test.describe('Billing - Permission Denied (read-only user)', () => {
  test.beforeEach(async ({ page }) => {
    await setupReadOnlyMocks(page)
  })

  test('should not show create/adjust buttons for read-only user', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/charges', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCharges })
      })
    )

    await page.goto('/admissions/10/charges')
    await expect(page.getByText('Physical therapy session')).toBeVisible()

    // Buttons should NOT be visible
    await expect(page.getByRole('button', { name: 'New Charge' })).not.toBeVisible()
    await expect(page.getByRole('button', { name: 'New Adjustment' })).not.toBeVisible()
  })

  test('should not show generate invoice button for read-only user', async ({ page }) => {
    await page.route('**/api/v1/admissions/10/invoice', (route) =>
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          error: { code: 'NOT_FOUND', message: 'Invoice not found' }
        })
      })
    )

    await page.goto('/admissions/10/invoice')
    await expect(
      page.getByText('No invoice has been generated for this admission yet.')
    ).toBeVisible()

    // Generate button should NOT be visible for read-only user
    await expect(page.getByRole('button', { name: 'Generate Invoice' })).not.toBeVisible()
  })
})
