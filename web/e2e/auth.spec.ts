import { test, expect } from '@playwright/test'

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('redirects to login when accessing protected route without auth', async ({ page }) => {
    await page.goto('/dashboard')

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('login page renders correctly', async ({ page }) => {
    await page.goto('/login')

    // Check logo is visible (design uses logo instead of text heading)
    await expect(page.getByAltText('Inside Health')).toBeVisible()

    // Check form fields exist
    await expect(page.getByLabel('Email or Username')).toBeVisible()
    // PrimeVue Password component uses id instead of proper label association
    await expect(page.locator('#password')).toBeVisible()

    // Check submit button
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible()

    // Note: Public registration has been removed (FR7), so no register link exists
  })

  test('shows validation errors for empty form submission', async ({ page }) => {
    await page.goto('/login')

    // Click submit without filling fields
    await page.getByRole('button', { name: 'Sign In' }).click()

    // Should show validation errors
    await expect(page.getByText(/required/i).first()).toBeVisible()
  })

  // Note: Public registration has been removed (FR7), so register page test is no longer applicable

  test('preserves redirect query param', async ({ page }) => {
    // Try to access a protected page
    await page.goto('/profile')

    // Should redirect to login with redirect param (may or may not be URL-encoded)
    await expect(page).toHaveURL(/\/login\?redirect=.*profile/)
  })
})

test.describe('Authentication - Login Errors', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('does not redirect on invalid credentials (401)', async ({ page }) => {
    // Mock login endpoint to return 401
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Invalid credentials' })
      })
    })

    await page.goto('/login')

    // Fill in the login form
    await page.getByLabel('Email or Username').fill('wronguser')
    await page.locator('#password input').fill('wrongpassword')
    await page.getByRole('button', { name: 'Sign In' }).click()

    // Wait for the request to complete
    await page.waitForTimeout(1000)

    // Should stay on login page (not redirect to dashboard)
    await expect(page).toHaveURL(/\/login/)

    // Sign In button should no longer be in loading state
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeEnabled()
  })

  test('does not redirect on API error with specific message', async ({ page }) => {
    // Mock login endpoint to return 401 with specific message
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Account is suspended' })
      })
    })

    await page.goto('/login')

    // Fill in the login form
    await page.getByLabel('Email or Username').fill('suspended@example.com')
    await page.locator('#password input').fill('somepassword')
    await page.getByRole('button', { name: 'Sign In' }).click()

    // Wait for the request to complete
    await page.waitForTimeout(1000)

    // Should stay on login page
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeEnabled()
  })

  test('does not redirect on network failure', async ({ page }) => {
    // Mock login endpoint to simulate network failure
    await page.route('**/api/auth/login', async (route) => {
      await route.abort()
    })

    await page.goto('/login')

    // Fill in the login form
    await page.getByLabel('Email or Username').fill('user@example.com')
    await page.locator('#password input').fill('password123')
    await page.getByRole('button', { name: 'Sign In' }).click()

    // Wait for the request to fail
    await page.waitForTimeout(1000)

    // Should stay on login page
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeEnabled()
  })
})

test.describe('Authentication - Password Field', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('password field is masked by default', async ({ page }) => {
    await page.goto('/login')

    // PrimeVue Password wraps input inside a div; verify the input type is password
    const passwordInput = page.locator('#password input')
    await expect(passwordInput).toHaveAttribute('type', 'password')
  })

  test('toggle mask button reveals password text', async ({ page }) => {
    await page.goto('/login')

    const passwordInput = page.locator('#password input')

    // Verify initially masked
    await expect(passwordInput).toHaveAttribute('type', 'password')

    // Click the toggle mask button inside the PrimeVue Password component
    // PrimeVue 4.5 uses a button with an eye icon next to the input
    await page.locator('#password button, #password [data-pc-section="maskicon"], #password [data-pc-section="unmaskicon"]').first().click()

    // After toggle, input type should be text
    await expect(passwordInput).toHaveAttribute('type', 'text')
  })
})

test.describe('Authentication - Force Password Change', () => {
  const mockForceChangeUser = {
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
    localePreference: 'en',
    mustChangePassword: true
  }

  async function setupForceChangeAuth(page: import('@playwright/test').Page) {
    await page.addInitScript(
      (userData) => {
        localStorage.setItem('access_token', 'mock-access-token')
        localStorage.setItem('refresh_token', 'mock-refresh-token')
        localStorage.setItem('mock_user', JSON.stringify(userData))
      },
      mockForceChangeUser
    )

    await page.route('**/api/users/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockForceChangeUser })
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

  test('redirects to change-password when mustChangePassword is true', async ({ page }) => {
    await setupForceChangeAuth(page)

    await page.goto('/dashboard')

    // Router guard should redirect to force password change
    await expect(page).toHaveURL(/\/auth\/change-password/)
  })

  test('force password change page renders correctly', async ({ page }) => {
    await setupForceChangeAuth(page)

    await page.goto('/auth/change-password')

    // Verify page title
    await expect(page.locator('h1')).toContainText('Change Password')

    // Verify description paragraph
    await expect(page.getByText(/You must change your password before continuing/i)).toBeVisible()

    // Verify the 3 password fields exist
    await expect(page.locator('#currentPassword')).toBeVisible()
    await expect(page.locator('#newPassword')).toBeVisible()
    await expect(page.locator('#confirmNewPassword')).toBeVisible()

    // Verify submit button
    await expect(page.getByRole('button', { name: 'Change Password' })).toBeVisible()
  })

  test('shows validation errors on empty submission', async ({ page }) => {
    await setupForceChangeAuth(page)

    await page.goto('/auth/change-password')

    // Click submit without filling any fields
    await page.getByRole('button', { name: 'Change Password' }).click()

    // Verify validation error messages appear
    const errors = page.locator('.p-error')
    await expect(errors.first()).toBeVisible({ timeout: 5000 })
  })

  test('shows error when passwords do not match', async ({ page }) => {
    await setupForceChangeAuth(page)

    await page.goto('/auth/change-password')

    // Fill current password and new password
    await page.locator('#currentPassword input').fill('OldPass123!')
    await page.locator('#newPassword input').fill('NewPass456!')
    // Fill confirm with a different value
    await page.locator('#confirmNewPassword input').fill('DifferentPass789!')

    // Submit
    await page.getByRole('button', { name: 'Change Password' }).click()

    // Verify mismatch error appears
    const errors = page.locator('.p-error')
    await expect(errors.first()).toBeVisible({ timeout: 5000 })
  })

  test('successful password change redirects to dashboard', async ({ page }) => {
    await setupForceChangeAuth(page)

    const updatedUser = { ...mockForceChangeUser, mustChangePassword: false }

    // Mock the password change API endpoint
    await page.route('**/api/users/me/password', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: updatedUser })
      })
    })

    // Also update the /users/me mock to return the updated user after password change
    // so the router guard does not redirect back to change-password
    await page.route('**/api/users/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: updatedUser })
      })
    })

    await page.goto('/auth/change-password')

    // Fill all three fields correctly
    await page.locator('#currentPassword input').fill('OldPass123!')
    await page.locator('#newPassword input').fill('NewPass456!')
    await page.locator('#confirmNewPassword input').fill('NewPass456!')

    // Submit
    await page.getByRole('button', { name: 'Change Password' }).click()

    // Should redirect to dashboard after successful password change
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
  })

  test('shows error message on API failure', async ({ page }) => {
    await setupForceChangeAuth(page)

    // Mock the password change API to return an error
    await page.route('**/api/users/me/password', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Current password is incorrect' })
      })
    })

    await page.goto('/auth/change-password')

    // Fill all three fields
    await page.locator('#currentPassword input').fill('WrongPass123!')
    await page.locator('#newPassword input').fill('NewPass456!')
    await page.locator('#confirmNewPassword input').fill('NewPass456!')

    // Submit
    await page.getByRole('button', { name: 'Change Password' }).click()

    // Verify an error message appears in the Message component
    // Note: Axios wraps non-2xx responses with its own error message
    await expect(page.locator('.p-message')).toBeVisible({ timeout: 5000 })
  })
})
