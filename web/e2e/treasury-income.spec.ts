import { test, expect, type Page } from '@playwright/test'

const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: ['treasury:read', 'treasury:write', 'treasury:delete', 'treasury:configure'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockReadOnlyUser = {
  id: 2,
  username: 'viewer',
  email: 'viewer@example.com',
  firstName: 'View',
  lastName: 'Only',
  roles: ['NURSE'],
  permissions: ['treasury:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockBankAccounts = [
  { id: 1, name: 'Main Account', bankName: 'Banco Industrial', accountType: 'CHECKING', active: true },
  { id: 2, name: 'Savings', bankName: 'BAM', accountType: 'SAVINGS', active: true }
]

const mockIncomes = {
  content: [
    {
      id: 1,
      description: 'Patient payment - Juan Perez',
      category: 'PATIENT_PAYMENT',
      amount: 1500.0,
      incomeDate: '2026-03-01',
      reference: 'REC-001',
      bankAccountId: 1,
      bankAccountName: 'Main Account',
      invoiceId: null,
      invoiceNumber: null,
      notes: null,
      createdByName: 'Admin User'
    },
    {
      id: 2,
      description: 'Insurance reimbursement',
      category: 'INSURANCE_PAYMENT',
      amount: 3200.0,
      incomeDate: '2026-03-02',
      reference: 'INS-045',
      bankAccountId: 1,
      bankAccountName: 'Main Account',
      invoiceId: null,
      invoiceNumber: null,
      notes: null,
      createdByName: 'Admin User'
    }
  ],
  totalElements: 2,
  totalPages: 1,
  size: 20,
  number: 0
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

  await page.route('**/api/v1/treasury/bank-accounts/active', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockBankAccounts })
    })
  })
}

test.describe('Treasury Income - List View', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
  })

  test('should display income records list', async ({ page }) => {
    await page.route('**/api/v1/treasury/income?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockIncomes })
      })
    )

    await page.goto('/treasury/income')
    await expect(page.getByText('Patient payment - Juan Perez')).toBeVisible()
    await expect(page.getByText('Insurance reimbursement')).toBeVisible()
  })

  test('should show empty state when no income records', async ({ page }) => {
    await page.route('**/api/v1/treasury/income?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 }
        })
      })
    )

    await page.goto('/treasury/income')
    await expect(page.getByText('No income records found')).toBeVisible()
  })

  test('should open create income dialog when clicking New Income', async ({ page }) => {
    await page.route('**/api/v1/treasury/income?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 }
        })
      })
    )

    await page.goto('/treasury/income')
    await page.getByRole('button', { name: 'New Income' }).click()
    await expect(page.locator('.p-dialog')).toBeVisible()
  })

  test('should show filter controls', async ({ page }) => {
    await page.route('**/api/v1/treasury/income?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockIncomes })
      })
    )

    await page.goto('/treasury/income')
    // Verify filter controls are present
    await expect(page.getByText('Category', { exact: false }).first()).toBeVisible()
    await expect(page.getByText('Bank Account', { exact: false }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: 'Filter' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Clear' })).toBeVisible()
  })

  test('should display category labels in table', async ({ page }) => {
    await page.route('**/api/v1/treasury/income?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockIncomes })
      })
    )

    await page.goto('/treasury/income')
    await expect(page.getByRole('cell', { name: 'Patient Payment', exact: true })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Insurance Payment', exact: true })).toBeVisible()
  })
})

test.describe('Treasury Income - Permissions (read-only user)', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page, mockReadOnlyUser)
    await setupCommonMocks(page, mockReadOnlyUser)
  })

  test('should not show New Income button for read-only user', async ({ page }) => {
    await page.route('**/api/v1/treasury/income?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockIncomes })
      })
    )

    await page.goto('/treasury/income')
    await expect(page.getByText('Patient payment - Juan Perez')).toBeVisible()
    await expect(page.getByRole('button', { name: 'New Income' })).not.toBeVisible()
  })

  test('should not show edit and delete buttons for read-only user', async ({ page }) => {
    await page.route('**/api/v1/treasury/income?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockIncomes })
      })
    )

    await page.goto('/treasury/income')
    await expect(page.getByText('Patient payment - Juan Perez')).toBeVisible()

    // Edit and delete buttons should not be visible
    await expect(page.locator('button .pi-pencil')).not.toBeVisible()
    await expect(page.locator('button .pi-trash')).not.toBeVisible()
  })
})
