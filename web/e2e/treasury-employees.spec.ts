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

const mockEmployees = [
  {
    id: 1,
    fullName: 'Maria Lopez',
    employeeType: 'PAYROLL',
    position: 'Nurse',
    baseSalary: 5000.0,
    contractedRate: null,
    hireDate: '2025-01-15',
    active: true,
    taxId: '123456-7',
    doctorFeeArrangement: null,
    hospitalCommissionPct: 0,
    terminationDate: null,
    terminationReason: null,
    indemnizacionLiability: 2500.0,
    notes: null
  },
  {
    id: 2,
    fullName: 'Carlos Mendez',
    employeeType: 'CONTRACTOR',
    position: 'IT Support',
    baseSalary: null,
    contractedRate: 3000.0,
    hireDate: '2025-06-01',
    active: true,
    taxId: null,
    doctorFeeArrangement: null,
    hospitalCommissionPct: 0,
    terminationDate: null,
    terminationReason: null,
    indemnizacionLiability: null,
    notes: null
  },
  {
    id: 3,
    fullName: 'Dr. Roberto Castillo',
    employeeType: 'DOCTOR',
    position: 'Cardiologist',
    baseSalary: null,
    contractedRate: null,
    hireDate: '2024-03-01',
    active: true,
    taxId: '789012-3',
    doctorFeeArrangement: 'HOSPITAL_BILLED',
    hospitalCommissionPct: 30,
    terminationDate: null,
    terminationReason: null,
    indemnizacionLiability: null,
    notes: null
  }
]

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

test.describe('Treasury Employees - List View', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
  })

  test('should display employee list', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmployees })
      })
    )

    await page.goto('/treasury/employees')
    await expect(page.getByText('Maria Lopez')).toBeVisible()
    await expect(page.getByText('Carlos Mendez')).toBeVisible()
    await expect(page.getByText('Dr. Roberto Castillo')).toBeVisible()
  })

  test('should show empty state when no employees', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/treasury/employees')
    await expect(page.getByText('No employees found')).toBeVisible()
  })

  test('should show employee type tags', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmployees })
      })
    )

    await page.goto('/treasury/employees')
    await expect(page.getByText('Payroll', { exact: true }).first()).toBeVisible()
    await expect(page.getByText('Contractor', { exact: true })).toBeVisible()
    await expect(page.getByText('Doctor', { exact: true })).toBeVisible()
  })

  test('should open create employee dialog when clicking New Employee', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/treasury/employees')
    await page.getByRole('button', { name: 'New Employee' }).click()
    await expect(page.locator('.p-dialog')).toBeVisible()
  })

  test('should show terminate dialog when clicking terminate button', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmployees })
      })
    )

    await page.goto('/treasury/employees')
    await expect(page.getByText('Maria Lopez')).toBeVisible()

    // Click the terminate button (times-circle icon) for the first active employee
    await page.locator('button .pi-times-circle').first().click()

    // Terminate dialog should appear with warning
    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText('Terminate Employee')).toBeVisible()
    await expect(dialog.getByText('You are about to terminate Maria Lopez')).toBeVisible()
  })

  test('should show filter controls', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmployees })
      })
    )

    await page.goto('/treasury/employees')
    await expect(page.getByRole('button', { name: 'Filter' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Clear' })).toBeVisible()
    await expect(page.getByText('Active Only')).toBeVisible()
  })
})

test.describe('Treasury Employees - Permissions (read-only user)', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page, mockReadOnlyUser)
    await setupCommonMocks(page, mockReadOnlyUser)
  })

  test('should not show New Employee button for read-only user', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmployees })
      })
    )

    await page.goto('/treasury/employees')
    await expect(page.getByText('Maria Lopez')).toBeVisible()
    await expect(page.getByRole('button', { name: 'New Employee' })).not.toBeVisible()
  })

  test('should not show edit and terminate buttons for read-only user', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmployees })
      })
    )

    await page.goto('/treasury/employees')
    await expect(page.getByText('Maria Lopez')).toBeVisible()

    // Edit and terminate buttons should not be visible (only payroll/contractor calendar buttons visible)
    await expect(page.locator('button .pi-pencil')).not.toBeVisible()
    await expect(page.locator('button .pi-times-circle')).not.toBeVisible()
  })
})
