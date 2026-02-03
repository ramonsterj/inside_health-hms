import { test, expect } from '@playwright/test'

// Mock user data
const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: ['user:read', 'user:create', 'user:update', 'user:delete'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockUserWithPhone = {
  id: 2,
  username: 'testuser',
  email: 'testuser@example.com',
  firstName: 'Test',
  lastName: 'User',
  roles: ['USER'],
  permissions: [],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-15T10:00:00Z',
  localePreference: 'en',
  phoneNumbers: [
    { id: 1, phoneNumber: '12345678', phoneType: 'MOBILE', isPrimary: false }
  ]
}

const mockRoles = [
  { id: 1, name: 'Admin', code: 'ADMIN', description: 'Administrator' },
  { id: 2, name: 'User', code: 'USER', description: 'Regular user' },
  { id: 3, name: 'Doctor', code: 'DOCTOR', description: 'Doctor' }
]

const mockUsersPage = {
  content: [
    {
      id: 2,
      username: 'testuser',
      email: 'testuser@example.com',
      firstName: 'Test',
      lastName: 'User',
      roles: ['USER'],
      status: 'ACTIVE',
      createdAt: '2026-01-15T10:00:00Z'
    }
  ],
  page: {
    totalElements: 1,
    totalPages: 1,
    size: 10,
    number: 0
  }
}

// Helper function to setup authenticated state
async function setupAuth(page: import('@playwright/test').Page) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    mockAdminUser
  )
}

// Helper function to setup API mocks
async function setupApiMocks(page: import('@playwright/test').Page) {
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

  // Mock roles list
  await page.route('**/api/roles', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockRoles })
    })
  })
}

test.describe('User Management - Phone Number Primary Flag', () => {
  // Track state changes for persistence simulation
  let userPhoneIsPrimary = false

  test('primary phone number flag persists after save', async ({ page }) => {
    // Setup mock authentication
    await setupAuth(page)
    await setupApiMocks(page)

    // Mock users list endpoint
    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    // Mock get single user endpoint - returns current state
    await page.route('**/api/users/2', async (route) => {
      if (route.request().method() === 'GET') {
        const userData = {
          ...mockUserWithPhone,
          phoneNumbers: [
            { id: 1, phoneNumber: '12345678', phoneType: 'MOBILE', isPrimary: userPhoneIsPrimary }
          ]
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: userData })
        })
      } else if (route.request().method() === 'PUT') {
        // Parse the request body to extract isPrimary state
        const requestBody = route.request().postDataJSON()
        if (requestBody?.phoneNumbers?.[0]?.isPrimary !== undefined) {
          userPhoneIsPrimary = requestBody.phoneNumbers[0].isPrimary
        }
        const updatedUser = {
          ...mockUserWithPhone,
          phoneNumbers: [
            { id: 1, phoneNumber: '12345678', phoneType: 'MOBILE', isPrimary: userPhoneIsPrimary }
          ]
        }
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: updatedUser })
        })
      }
    })

    // Navigate to users page
    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Wait for users table to load - PrimeVue DataTable renders a table inside
    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Find and click the edit button for first user row
    const firstRow = page.locator('.p-datatable-tbody tr').first()
    await expect(firstRow).toBeVisible({ timeout: 10000 })

    const editButton = firstRow.locator('button').filter({ has: page.locator('.pi-pencil') })
    await expect(editButton).toBeVisible({ timeout: 10000 })
    await editButton.click()

    // Wait for edit dialog to open
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Wait for loading spinner to disappear (user data is being fetched)
    await expect(dialog.locator('.p-progress-spinner')).toBeHidden({ timeout: 10000 })

    // Find the phone number input and verify it has value
    const phoneInput = dialog.locator('.phone-numbers-list input[type="text"]').first()
    await expect(phoneInput).toBeVisible({ timeout: 5000 })

    // Find the primary checkbox in the phone numbers section
    const primaryCheckbox = dialog.locator('.phone-numbers-list input[type="checkbox"]').first()
    await expect(primaryCheckbox).toBeVisible({ timeout: 5000 })

    // The checkbox should not be checked initially (isPrimary: false in mock)
    await expect(primaryCheckbox).not.toBeChecked()

    // Check it
    await primaryCheckbox.check()

    // Verify it's now checked before save
    await expect(primaryCheckbox).toBeChecked()

    // Wait for the save button to be enabled
    const saveButton = dialog.getByRole('button', { name: /save|guardar/i })
    await expect(saveButton).toBeVisible()
    await expect(saveButton).toBeEnabled({ timeout: 5000 })
    await saveButton.click()

    // Wait for dialog to close (indicates save was successful)
    await expect(dialog).toBeHidden({ timeout: 10000 })

    // Wait for table to reload
    await page.waitForLoadState('networkidle')

    // Re-open the edit dialog for the same user
    await expect(editButton).toBeVisible({ timeout: 5000 })
    await editButton.click()

    // Wait for dialog to open
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Wait for loading spinner to disappear
    await expect(dialog.locator('.p-progress-spinner')).toBeHidden({ timeout: 10000 })

    // Find the primary checkbox again
    const primaryCheckboxAfterReopen = dialog.locator('.phone-numbers-list input[type="checkbox"]').first()
    await expect(primaryCheckboxAfterReopen).toBeVisible({ timeout: 5000 })

    // CRITICAL ASSERTION: The checkbox should still be checked after save and reload
    await expect(primaryCheckboxAfterReopen).toBeChecked()
  })

  test('primary phone number flag is correctly set when creating a new user', async ({ page }) => {
    // Setup mock authentication
    await setupAuth(page)
    await setupApiMocks(page)

    const timestamp = Date.now()
    const newUsername = `testuser${timestamp}`
    const newEmail = `testuser${timestamp}@example.com`
    let createdUser: typeof mockUserWithPhone | null = null

    // Mock users list endpoint
    await page.route('**/api/users?*', async (route) => {
      const usersData = createdUser
        ? {
            content: [
              ...mockUsersPage.content,
              {
                id: 3,
                username: newUsername,
                email: newEmail,
                firstName: '',
                lastName: '',
                roles: ['USER'],
                status: 'ACTIVE',
                createdAt: new Date().toISOString()
              }
            ],
            page: { ...mockUsersPage.page, totalElements: 2 }
          }
        : mockUsersPage
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: usersData })
      })
    })

    // Mock create user endpoint
    await page.route('**/api/users', async (route) => {
      if (route.request().method() === 'POST') {
        const requestBody = route.request().postDataJSON()
        createdUser = {
          id: 3,
          username: requestBody.username,
          email: requestBody.email,
          firstName: requestBody.firstName || '',
          lastName: requestBody.lastName || '',
          roles: requestBody.roles || ['USER'],
          permissions: [],
          status: 'ACTIVE',
          emailVerified: false,
          createdAt: new Date().toISOString(),
          localePreference: 'en',
          phoneNumbers: requestBody.phoneNumbers || []
        }
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: createdUser })
        })
      }
    })

    // Mock get single user endpoint for the newly created user
    await page.route('**/api/users/3', async (route) => {
      if (route.request().method() === 'GET' && createdUser) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: createdUser })
        })
      }
    })

    // Mock username availability check
    await page.route('**/api/users/check-username-availability*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { available: true } })
      })
    })

    // Navigate to users page
    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Wait for page to load - PrimeVue DataTable
    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Click add user button
    const addUserButton = page.getByRole('button', { name: /add user|agregar usuario/i })
    await expect(addUserButton).toBeVisible()
    await addUserButton.click()

    // Wait for add dialog to open
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Fill in required fields - use id selector for username input
    const usernameInput = dialog.locator('#username')
    await expect(usernameInput).toBeVisible({ timeout: 5000 })
    await usernameInput.fill(newUsername)

    const emailInput = dialog.locator('input[type="email"]')
    await expect(emailInput).toBeVisible()
    await emailInput.fill(newEmail)

    // Fill password fields - using InputText type="password"
    const passwordInputs = dialog.locator('input[type="password"]')
    await expect(passwordInputs.first()).toBeVisible()
    await passwordInputs.first().fill('TestPass123!')
    await passwordInputs.nth(1).fill('TestPass123!')

    // Fill phone number
    const phoneInput = dialog.locator('.phone-numbers-list input[type="text"]').first()
    await expect(phoneInput).toBeVisible({ timeout: 5000 })
    await phoneInput.fill('12345678')

    // Find the primary checkbox and verify it's checked by default (first phone should be primary)
    const primaryCheckbox = dialog.locator('.phone-numbers-list input[type="checkbox"]').first()
    await expect(primaryCheckbox).toBeVisible({ timeout: 5000 })
    // For a new user, first phone should be primary by default
    await expect(primaryCheckbox).toBeChecked()

    // Wait for username availability check
    await page.waitForTimeout(1500)

    // Click create/save button
    const createButton = dialog.getByRole('button', { name: /create|crear/i })
    await expect(createButton).toBeVisible()
    await expect(createButton).toBeEnabled({ timeout: 5000 })
    await createButton.click()

    // Wait for dialog to close
    await expect(dialog).toBeHidden({ timeout: 10000 })

    // Wait for table to reload
    await page.waitForLoadState('networkidle')

    // Search for the newly created user
    const searchInput = page.locator('.filter-bar input[type="text"]').first()
    await expect(searchInput).toBeVisible()
    await searchInput.fill(newUsername)

    // Wait for search results
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)

    // Find the edit button for the new user
    const newUserRow = page.locator('.p-datatable-tbody tr').filter({ hasText: newEmail })
    await expect(newUserRow).toBeVisible({ timeout: 10000 })

    const editNewUserButton = newUserRow.locator('button').filter({ has: page.locator('.pi-pencil') })
    await expect(editNewUserButton).toBeVisible()
    await editNewUserButton.click()

    // Wait for edit dialog
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog.locator('.p-progress-spinner')).toBeHidden({ timeout: 10000 })

    // Verify the primary checkbox is still checked
    const primaryCheckboxAfterReload = dialog.locator('.phone-numbers-list input[type="checkbox"]').first()
    await expect(primaryCheckboxAfterReload).toBeVisible({ timeout: 5000 })
    await expect(primaryCheckboxAfterReload).toBeChecked()
  })
})
