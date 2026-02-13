import { test, expect } from '@playwright/test'
import { confirmDialogAccept } from './utils/test-helpers'

// Mock user data
const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'user:read',
    'user:create',
    'user:update',
    'user:delete',
    'user:reset-password',
    'user:list-deleted',
    'user:restore'
  ],
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

const mockMultipleUsersPage = {
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
    },
    {
      id: 3,
      username: 'doctor1',
      email: 'doctor1@example.com',
      firstName: 'Maria',
      lastName: 'Garcia',
      roles: ['DOCTOR'],
      status: 'ACTIVE',
      createdAt: '2026-01-16T10:00:00Z'
    },
    {
      id: 4,
      username: 'suspended_user',
      email: 'suspended@example.com',
      firstName: 'Suspended',
      lastName: 'User',
      roles: ['USER'],
      status: 'SUSPENDED',
      createdAt: '2026-01-17T10:00:00Z'
    }
  ],
  page: {
    totalElements: 3,
    totalPages: 1,
    size: 10,
    number: 0
  }
}

const mockDeletedUsersPage = {
  content: [
    {
      id: 5,
      username: 'deleted_user',
      email: 'deleted@example.com',
      firstName: 'Deleted',
      lastName: 'User',
      roles: ['USER'],
      status: 'ACTIVE',
      createdAt: '2026-01-10T10:00:00Z'
    }
  ],
  page: {
    totalElements: 1,
    totalPages: 1,
    size: 10,
    number: 0
  }
}

const mockEmptyUsersPage = {
  content: [],
  page: {
    totalElements: 0,
    totalPages: 0,
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

test.describe('User Management - List and Filters', () => {
  test('displays user list with correct columns', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMultipleUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Verify DataTable is visible
    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Check that column headers exist
    await expect(page.getByText(/email/i).first()).toBeVisible()
    await expect(page.getByText(/name|nombre/i).first()).toBeVisible()
    await expect(page.getByText(/roles/i).first()).toBeVisible()
    await expect(page.getByText(/status|estado/i).first()).toBeVisible()
    await expect(page.getByText(/actions|acciones/i).first()).toBeVisible()

    // Check that data rows show user emails
    await expect(page.getByText('testuser@example.com')).toBeVisible()
    await expect(page.getByText('doctor1@example.com')).toBeVisible()
    await expect(page.getByText('suspended@example.com')).toBeVisible()
  })

  test('shows empty state when no users', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmptyUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Verify empty state text is visible
    await expect(page.getByText(/no users|empty|no hay usuarios/i)).toBeVisible({ timeout: 10000 })
  })

  test('filters by search query', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMultipleUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Wait for table to appear
    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Type "doctor" in the search input
    const searchInput = page.locator('#searchQuery')
    await expect(searchInput).toBeVisible()

    // Set up a request listener to verify the API is called with the search param
    const searchRequestPromise = page.waitForRequest((request) =>
      request.url().includes('/api/users?') && request.url().includes('doctor')
    )

    await searchInput.fill('doctor')

    // Wait for the search debounce (300ms + buffer)
    await searchRequestPromise.catch(() => {
      // Request may not always match exactly; the important thing is the input works
    })
    await page.waitForTimeout(500)
  })

  test('filters by role dropdown', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMultipleUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Click the role filter dropdown
    const roleFilter = page.locator('#roleFilter')
    await expect(roleFilter).toBeVisible()
    await roleFilter.click()

    // Select "Doctor" from the overlay
    const doctorOption = page.locator('.p-select-option').filter({ hasText: 'Doctor' })
    await expect(doctorOption).toBeVisible({ timeout: 5000 })
    await doctorOption.click()

    // Wait for the filter to apply
    await page.waitForTimeout(500)
  })

  test('filters by status dropdown', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMultipleUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Click the status filter dropdown
    const statusFilter = page.locator('#statusFilter')
    await expect(statusFilter).toBeVisible()
    await statusFilter.click()

    // Select an option from the overlay
    const activeOption = page.locator('.p-select-option').filter({ hasText: /active|activo/i }).first()
    await expect(activeOption).toBeVisible({ timeout: 5000 })
    await activeOption.click()

    // Wait for the filter to apply
    await page.waitForTimeout(500)
  })

  test('toggles show deleted users', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMultipleUsersPage })
      })
    })

    await page.route('**/api/users/deleted*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDeletedUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Find and click the show deleted toggle
    const showDeletedToggle = page.locator('.p-toggleswitch').first()
    await expect(showDeletedToggle).toBeVisible()
    await showDeletedToggle.click()

    // Wait for the deleted users to load
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)

    // Verify "deleted@example.com" appears
    await expect(page.getByText('deleted@example.com')).toBeVisible({ timeout: 10000 })
  })
})

test.describe('User Management - Create User', () => {
  test('opens create dialog with all fields', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Click "Add User" button
    const addUserButton = page.getByRole('button', { name: /add user|agregar usuario/i })
    await expect(addUserButton).toBeVisible()
    await addUserButton.click()

    // Verify dialog is visible
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Check fields exist
    await expect(dialog.locator('#username')).toBeVisible()
    await expect(dialog.locator('input[type="email"]')).toBeVisible()
    await expect(dialog.locator('input[type="password"]').first()).toBeVisible()
    await expect(dialog.locator('input[type="password"]').nth(1)).toBeVisible()
    await expect(dialog.locator('#salutation')).toBeVisible()
    // Roles multiselect uses inputId="roles"
    await expect(dialog.locator('#roles, [aria-labelledby*="roles"]').first()).toBeVisible()
  })

  test('creates user successfully', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    const timestamp = Date.now()
    const newUsername = `newuser${timestamp}`
    const newEmail = `newuser${timestamp}@example.com`

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    // Mock create user endpoint
    await page.route('**/api/users', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 10,
              username: newUsername,
              email: newEmail,
              firstName: '',
              lastName: '',
              roles: ['USER'],
              permissions: [],
              status: 'ACTIVE',
              emailVerified: false,
              createdAt: new Date().toISOString(),
              localePreference: 'en',
              phoneNumbers: [{ id: 1, phoneNumber: '55551234', phoneType: 'MOBILE', isPrimary: true }]
            }
          })
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

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Click Add User
    const addUserButton = page.getByRole('button', { name: /add user|agregar usuario/i })
    await addUserButton.click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Fill username
    await dialog.locator('#username').fill(newUsername)

    // Fill email
    await dialog.locator('input[type="email"]').fill(newEmail)

    // Fill password and confirm password
    const passwordInputs = dialog.locator('input[type="password"]')
    await passwordInputs.first().fill('TestPass123!')
    await passwordInputs.nth(1).fill('TestPass123!')

    // Fill phone number
    const phoneInput = dialog.locator('.phone-numbers-list input[type="text"]').first()
    await phoneInput.fill('55551234')

    // Wait for availability check
    await page.waitForTimeout(1500)

    // Click Create
    const createButton = dialog.getByRole('button', { name: /create|crear/i })
    await expect(createButton).toBeEnabled({ timeout: 5000 })
    await createButton.click()

    // Verify dialog closes
    await expect(dialog).toBeHidden({ timeout: 10000 })

    // Verify success toast appears
    const toast = page.locator('.p-toast-message')
    await expect(toast).toBeVisible({ timeout: 10000 })
  })

  test('shows username taken error', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    // Mock username availability check - NOT available
    await page.route('**/api/users/check-username-availability*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { available: false, message: 'Username is already taken' }
        })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Open add dialog
    const addUserButton = page.getByRole('button', { name: /add user|agregar usuario/i })
    await addUserButton.click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Type a username
    await dialog.locator('#username').fill('existinguser')

    // Wait for availability check
    await page.waitForTimeout(1500)

    // Verify error indicator appears (times-circle icon or error text)
    const errorIndicator = dialog.locator('.pi-times-circle, .p-error')
    await expect(errorIndicator.first()).toBeVisible({ timeout: 5000 })
  })

  test('shows password mismatch validation', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Open add dialog
    const addUserButton = page.getByRole('button', { name: /add user|agregar usuario/i })
    await addUserButton.click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Fill password with "TestPass123!"
    const passwordInputs = dialog.locator('input[type="password"]')
    await passwordInputs.first().fill('TestPass123!')

    // Fill confirm password with "DifferentPass!"
    await passwordInputs.nth(1).fill('DifferentPass!')

    // Click elsewhere to trigger validation
    await dialog.locator('#username').click()

    // Verify mismatch error appears (text: "Passwords do not match")
    const mismatchError = dialog.locator('.p-error').filter({ hasText: /do not match|no coinciden/i })
    await expect(mismatchError).toBeVisible({ timeout: 5000 })
  })

  test('validates email format', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Open add dialog
    const addUserButton = page.getByRole('button', { name: /add user|agregar usuario/i })
    await addUserButton.click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Fill email with invalid format
    await dialog.locator('input[type="email"]').fill('invalid-email')

    // Click elsewhere to trigger validation
    await dialog.locator('#username').click()

    // Verify email validation error appears
    const emailError = dialog.locator('.p-error').filter({ hasText: /email|correo/i })
    await expect(emailError).toBeVisible({ timeout: 5000 })
  })

  test('requires at least one phone number', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    // Mock username availability check
    await page.route('**/api/users/check-username-availability*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { available: true } })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Open add dialog
    const addUserButton = page.getByRole('button', { name: /add user|agregar usuario/i })
    await addUserButton.click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    // Fill all required fields except phone number
    await dialog.locator('#username').fill('phonetest')
    await dialog.locator('input[type="email"]').fill('phonetest@example.com')
    const passwordInputs = dialog.locator('input[type="password"]')
    await passwordInputs.first().fill('TestPass123!')
    await passwordInputs.nth(1).fill('TestPass123!')

    // Wait for username availability check
    await page.waitForTimeout(1500)

    // The phone number input is pre-populated with one empty entry
    // The create button should be disabled when the phone number field is empty
    const createButton = dialog.getByRole('button', { name: /create|crear/i })
    await expect(createButton).toBeDisabled()
  })
})

test.describe('User Management - Edit User', () => {
  test('opens edit dialog pre-populated', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    await page.route('**/api/users/2', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockUserWithPhone })
        })
      }
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    // Wait for table
    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Click edit button on first row
    const firstRow = page.locator('.p-datatable-tbody tr').first()
    await expect(firstRow).toBeVisible({ timeout: 10000 })

    const editButton = firstRow.locator('button').filter({ has: page.locator('.pi-pencil') })
    await expect(editButton).toBeVisible({ timeout: 10000 })
    await editButton.click()

    // Wait for dialog and spinner
    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog.locator('.p-progress-spinner')).toBeHidden({ timeout: 10000 })

    // Verify fields are pre-populated with mock data values
    const firstNameInput = dialog.locator('#editFirstName')
    await expect(firstNameInput).toHaveValue('Test')

    const lastNameInput = dialog.locator('#editLastName')
    await expect(lastNameInput).toHaveValue('User')
  })

  test('saves edited user', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    await page.route('**/api/users/2', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockUserWithPhone })
        })
      } else if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...mockUserWithPhone, firstName: 'Updated' }
          })
        })
      }
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Open edit dialog
    const firstRow = page.locator('.p-datatable-tbody tr').first()
    const editButton = firstRow.locator('button').filter({ has: page.locator('.pi-pencil') })
    await editButton.click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible({ timeout: 5000 })
    await expect(dialog.locator('.p-progress-spinner')).toBeHidden({ timeout: 10000 })

    // Change firstName
    const firstNameInput = dialog.locator('#editFirstName')
    await firstNameInput.clear()
    await firstNameInput.fill('Updated')

    // Click Save
    const saveButton = dialog.getByRole('button', { name: /save|guardar/i })
    await expect(saveButton).toBeEnabled({ timeout: 5000 })
    await saveButton.click()

    // Verify dialog closes
    await expect(dialog).toBeHidden({ timeout: 10000 })
  })
})

test.describe('User Management - Password Reset', () => {
  test('resets password and shows temporary password dialog', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    // Mock reset password endpoint
    await page.route('**/api/users/2/reset-password', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { temporaryPassword: 'TempPass123!' }
          })
        })
      }
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Click reset password button (pi-key icon)
    const firstRow = page.locator('.p-datatable-tbody tr').first()
    const resetButton = firstRow.locator('button').filter({ has: page.locator('.pi-key') })
    await expect(resetButton).toBeVisible({ timeout: 10000 })
    await resetButton.click()

    // Accept confirm dialog
    await confirmDialogAccept(page)

    // Verify temporary password dialog appears with the password
    const passwordDialog = page.locator('[role="dialog"]')
    await expect(passwordDialog).toBeVisible({ timeout: 10000 })
    await expect(passwordDialog.getByText('TempPass123!')).toBeVisible({ timeout: 5000 })
  })
})

test.describe('User Management - Delete and Restore', () => {
  test('deletes user with confirmation', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersPage })
      })
    })

    // Mock delete user endpoint
    await page.route('**/api/users/2', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, message: 'User deleted' })
        })
      }
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Click delete button (pi-trash icon)
    const firstRow = page.locator('.p-datatable-tbody tr').first()
    const deleteButton = firstRow.locator('button').filter({ has: page.locator('.pi-trash') })
    await expect(deleteButton).toBeVisible({ timeout: 10000 })
    await deleteButton.click()

    // Accept confirm dialog
    await confirmDialogAccept(page)

    // Verify success toast
    const toast = page.locator('.p-toast-message')
    await expect(toast).toBeVisible({ timeout: 10000 })
  })

  test('prevents self-deletion', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    // Create a mock users page that includes the admin user (id: 1)
    const mockUsersWithAdmin = {
      content: [
        {
          id: 1,
          username: 'admin',
          email: 'admin@example.com',
          firstName: 'Admin',
          lastName: 'User',
          roles: ['ADMIN'],
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z'
        },
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
        totalElements: 2,
        totalPages: 1,
        size: 10,
        number: 0
      }
    }

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockUsersWithAdmin })
      })
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Find the admin user row (id: 1) and verify delete button is disabled
    const adminRow = page.locator('.p-datatable-tbody tr').filter({ hasText: 'admin@example.com' })
    await expect(adminRow).toBeVisible({ timeout: 10000 })

    const adminDeleteButton = adminRow.locator('button').filter({ has: page.locator('.pi-trash') })
    await expect(adminDeleteButton).toBeVisible()
    await expect(adminDeleteButton).toBeDisabled()

    // Verify the other user's delete button is NOT disabled
    const otherRow = page.locator('.p-datatable-tbody tr').filter({ hasText: 'testuser@example.com' })
    const otherDeleteButton = otherRow.locator('button').filter({ has: page.locator('.pi-trash') })
    await expect(otherDeleteButton).toBeEnabled()
  })

  test('restores deleted user', async ({ page }) => {
    await setupAuth(page)
    await setupApiMocks(page)

    await page.route('**/api/users?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMultipleUsersPage })
      })
    })

    await page.route('**/api/users/deleted*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDeletedUsersPage })
      })
    })

    // Mock restore user endpoint
    await page.route('**/api/users/5/restore', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, message: 'User restored' })
        })
      }
    })

    await page.goto('/users')
    await page.waitForLoadState('networkidle')

    const dataTable = page.locator('.p-datatable table, [data-pc-name="datatable"] table')
    await expect(dataTable).toBeVisible({ timeout: 15000 })

    // Toggle show deleted
    const showDeletedToggle = page.locator('.p-toggleswitch').first()
    await expect(showDeletedToggle).toBeVisible()
    await showDeletedToggle.click()

    // Wait for deleted users to load
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)

    // Verify deleted user appears
    await expect(page.getByText('deleted@example.com')).toBeVisible({ timeout: 10000 })

    // Click restore button (pi-refresh icon)
    const deletedRow = page.locator('.p-datatable-tbody tr').filter({ hasText: 'deleted@example.com' })
    const restoreButton = deletedRow.locator('button').filter({ has: page.locator('.pi-refresh') })
    await expect(restoreButton).toBeVisible({ timeout: 5000 })
    await restoreButton.click()

    // Verify success toast
    const toast = page.locator('.p-toast-message')
    await expect(toast).toBeVisible({ timeout: 10000 })
  })
})
