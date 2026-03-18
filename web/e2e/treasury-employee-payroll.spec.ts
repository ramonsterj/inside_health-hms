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

const mockPayrollEmployee = {
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
}

const mockContractorEmployee = {
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
}

const mockPayrollEntries = [
  {
    id: 10,
    employeeId: 1,
    periodLabel: 'Enero 2026 - Quincena 1',
    grossAmount: 2500.0,
    dueDate: '2026-01-15',
    status: 'PAID',
    paidDate: '2026-01-15',
    bankAccountName: 'Main Account'
  },
  {
    id: 11,
    employeeId: 1,
    periodLabel: 'Enero 2026 - Quincena 2',
    grossAmount: 2500.0,
    dueDate: '2026-01-31',
    status: 'PENDING',
    paidDate: null,
    bankAccountName: null
  }
]

const mockSalaryHistory = [
  {
    id: 1,
    baseSalary: 5000.0,
    effectiveFrom: '2025-01-15',
    effectiveTo: null,
    notes: 'Initial salary'
  }
]

const mockPaymentHistory = [
  {
    type: 'PAYROLL_ENTRY',
    date: '2026-01-15',
    amount: 2500.0,
    reference: 'Enero 2026 - Quincena 1',
    status: 'PAID',
    relatedEntityId: 10
  }
]

const mockBankAccounts = [
  { id: 1, name: 'Main Account', active: true }
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

  await page.route('**/api/v1/treasury/bank-accounts/active', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockBankAccounts })
    })
  })
}

function setupPayrollEmployeeMocks(page: Page) {
  return Promise.all([
    page.route('**/api/v1/treasury/employees/1', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPayrollEmployee })
      })
    ),
    page.route('**/api/v1/treasury/employees/1/payroll?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPayrollEntries })
      })
    ),
    page.route('**/api/v1/treasury/employees/1/salary-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockSalaryHistory })
      })
    ),
    page.route('**/api/v1/treasury/employees/1/payment-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPaymentHistory })
      })
    ),
    page.route('**/api/v1/treasury/employees/1/indemnizacion', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { daysWorked: 430, liability: 2500.0 }
        })
      })
    )
  ])
}

test.describe('Employee Payroll View - Payroll Employee', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
    await setupPayrollEmployeeMocks(page)
  })

  test('should display employee info card', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('Maria Lopez')).toBeVisible()
    await expect(page.getByText('Payroll', { exact: true }).first()).toBeVisible()
    await expect(page.getByText('Nurse')).toBeVisible()
  })

  test('should display salary history table', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByRole('heading', { name: 'Salary History' })).toBeVisible()
    await expect(page.getByText('Initial salary')).toBeVisible()
    await expect(page.getByText('2025-01-15', { exact: true })).toBeVisible()
  })

  test('should display payroll schedule table', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('Payroll Schedule')).toBeVisible()
    await expect(page.getByText('Enero 2026 - Quincena 1')).toBeVisible()
    await expect(page.getByText('Enero 2026 - Quincena 2')).toBeVisible()
  })

  test('should show pay button only for pending entries', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('Enero 2026 - Quincena 2')).toBeVisible()

    // Only one pay button should be visible (for the PENDING entry)
    const payButtons = page.locator('button .pi-dollar')
    await expect(payButtons).toHaveCount(1)
  })

  test('should show status tags in payroll table', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('Paid', { exact: true }).first()).toBeVisible()
    await expect(page.getByText('Pending', { exact: true })).toBeVisible()
  })

  test('should open payment dialog when clicking pay button', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('Enero 2026 - Quincena 2')).toBeVisible()

    await page.locator('button .pi-dollar').click()

    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText('Pay Payroll Entry')).toBeVisible()
  })

  test('should show Update Salary button', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByRole('button', { name: 'Update Salary' })).toBeVisible()
  })

  test('should open salary update dialog', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await page.getByRole('button', { name: 'Update Salary' }).click()

    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText('Update Salary')).toBeVisible()
  })

  test('should display payment history table', async ({ page }) => {
    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('Payment History')).toBeVisible()
  })
})

test.describe('Employee Payroll View - Empty Payroll', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
  })

  test('should show empty payroll state', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees/1', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPayrollEmployee })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/payroll?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/salary-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/payment-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('No payroll entries found')).toBeVisible()
  })
})

test.describe('Employee Payroll View - Contractor Employee', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
  })

  test('should display contractor payments panel', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees/2', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockContractorEmployee })
      })
    )
    await page.route('**/api/v1/treasury/employees/2/payment-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/treasury/employees/2/payroll')
    await expect(page.getByText('Carlos Mendez')).toBeVisible()
    await expect(page.getByText('Contractor', { exact: true }).first()).toBeVisible()
    await expect(page.getByText('Contractor Payments')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Record Payment' })).toBeVisible()
  })

  test('should open contractor payment dialog', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees/2', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockContractorEmployee })
      })
    )
    await page.route('**/api/v1/treasury/employees/2/payment-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/treasury/employees/2/payroll')
    await page.getByRole('button', { name: 'Record Payment' }).click()

    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText('Record Payment')).toBeVisible()
  })
})

test.describe('Employee Payroll View - Permissions (read-only user)', () => {
  test.beforeEach(async ({ page }) => {
    await setupAuth(page, mockReadOnlyUser)
    await setupCommonMocks(page, mockReadOnlyUser)
  })

  test('should not show pay button for read-only user', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees/1', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPayrollEmployee })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/payroll?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPayrollEntries })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/salary-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockSalaryHistory })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/payment-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPaymentHistory })
      })
    )

    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByText('Enero 2026 - Quincena 2')).toBeVisible()

    // Pay button should not be visible
    await expect(page.locator('button .pi-dollar')).not.toBeVisible()
  })

  test('should not show Update Salary button for read-only user', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees/1', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPayrollEmployee })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/payroll?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPayrollEntries })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/salary-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockSalaryHistory })
      })
    )
    await page.route('**/api/v1/treasury/employees/1/payment-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPaymentHistory })
      })
    )

    await page.goto('/treasury/employees/1/payroll')
    await expect(page.getByRole('heading', { name: 'Salary History' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Update Salary' })).not.toBeVisible()
  })

  test('should not show Record Payment button for contractor as read-only user', async ({ page }) => {
    await page.route('**/api/v1/treasury/employees/2', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockContractorEmployee })
      })
    )
    await page.route('**/api/v1/treasury/employees/2/payment-history', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    )

    await page.goto('/treasury/employees/2/payroll')
    await expect(page.getByText('Carlos Mendez')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Record Payment' })).not.toBeVisible()
  })
})
