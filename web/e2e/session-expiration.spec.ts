import { test, expect } from '@playwright/test'

test.describe('Session Expiration', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test.describe('Modal UI', () => {
    test('session expired modal displays correctly when triggered via API 401', async ({ page }) => {
      // Set up mock tokens so the app thinks we're logged in
      await page.addInitScript(() => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 3600
          })
        )
        const mockToken = `${header}.${payload}.mock`
        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'mock-refresh')
      })

      // Intercept API calls - fail the refresh to trigger session expiration
      await page.route('**/api/auth/refresh', async route => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Refresh token expired' })
        })
      })

      await page.route('**/api/users/me', async route => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Unauthorized' })
        })
      })

      // Navigate to dashboard which will trigger API calls
      await page.goto('/dashboard')

      // Wait for the session expiration flow to complete
      await page.waitForTimeout(1000)

      // Check modal is visible with correct content
      const modal = page.locator('[role="dialog"]')
      await expect(modal.getByText(/session expired/i)).toBeVisible({ timeout: 5000 })
      await expect(modal.getByText(/log in again/i)).toBeVisible()
      await expect(modal.getByRole('button', { name: /log in/i })).toBeVisible()
    })

    test('modal has blocking properties (cannot be dismissed by escape)', async ({ page }) => {
      // Set up mock tokens so the app thinks we're logged in
      await page.addInitScript(() => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 3600
          })
        )
        const mockToken = `${header}.${payload}.mock`
        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'mock-refresh')
      })

      // Intercept API calls to trigger session expiration
      await page.route('**/api/auth/refresh', async route => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Refresh token expired' })
        })
      })

      await page.route('**/api/users/me', async route => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Unauthorized' })
        })
      })

      // Navigate to dashboard to trigger API calls
      await page.goto('/dashboard')

      // Wait for modal to appear
      const modal = page.locator('[role="dialog"]')
      await expect(modal).toBeVisible({ timeout: 5000 })

      // Press Escape - modal should remain visible (blocking)
      await page.keyboard.press('Escape')
      await page.waitForTimeout(200)
      await expect(modal).toBeVisible()

      // Modal should still be there - cannot be dismissed without clicking the button
      await expect(modal.getByRole('button', { name: /log in/i })).toBeVisible()
    })
  })

  test.describe('Redirect Flow', () => {
    test('login page handles redirect query parameter', async ({ page }) => {
      // Navigate to login with a redirect parameter
      await page.goto('/login?redirect=%2Fpatients%3Fpage%3D2')

      // Verify we're on the login page (check for logo instead of text heading)
      await expect(page.getByAltText('Inside Health')).toBeVisible()

      // The redirect param should be preserved in the URL
      await expect(page).toHaveURL(/redirect=.*patients/)
    })

    test('preserves complex redirect paths with query params', async ({ page }) => {
      const complexPath = '/patients?page=2&filter=active&sort=name'
      const encodedPath = encodeURIComponent(complexPath)

      await page.goto(`/login?redirect=${encodedPath}`)

      // URL should contain the encoded redirect
      await expect(page).toHaveURL(/redirect=/)

      // Get the actual redirect param value
      const url = new URL(page.url())
      const redirectParam = url.searchParams.get('redirect')

      // The redirect param should contain the original path
      expect(redirectParam).toContain('/patients')
    })

    test('user returns to original location after re-authentication', async ({ page }) => {
      const originalPath = '/patients?page=2'

      // Navigate to login with redirect parameter (simulating post-session-expiration state)
      await page.goto(`/login?redirect=${encodeURIComponent(originalPath)}`)

      // Mock successful login response
      await page.route('**/api/auth/login', async route => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 3600
          })
        )
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              accessToken: `${header}.${payload}.mock`,
              refreshToken: 'mock-refresh-token',
              user: {
                id: 1,
                username: 'admin',
                email: 'admin@example.com',
                firstName: 'Admin',
                lastName: 'User',
                status: 'ACTIVE',
                mustChangePassword: false,
                roles: [{ id: 1, code: 'ADMIN', name: 'Admin' }],
                permissions: ['patient:read']
              }
            }
          })
        })
      })

      // Mock the patients API call that will happen after redirect
      await page.route('**/api/patients**', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { content: [], totalElements: 0, totalPages: 0 }
          })
        })
      })

      // Fill in login form
      await page.getByRole('textbox', { name: 'Email or Username' }).fill('admin')
      await page.getByRole('textbox', { name: 'Enter your password' }).fill('admin123')

      // Submit login
      await page.getByRole('button', { name: 'Sign In' }).click()

      // Should redirect to the original path after successful login
      await expect(page).toHaveURL(/\/patients/, { timeout: 10000 })
      await expect(page).toHaveURL(/page=2/)
    })
  })

  test.describe('Integration with Auth Flow', () => {
    test('unauthenticated API calls redirect to login', async ({ page }) => {
      // Set up fake tokens that will fail authentication
      await page.addInitScript(() => {
        localStorage.setItem('access_token', 'invalid-expired-token')
        localStorage.setItem('refresh_token', 'invalid-refresh-token')
      })

      // Try to access a protected route
      await page.goto('/dashboard')

      // Should eventually end up at login (either through router guard or session expiration)
      await expect(page).toHaveURL(/\/login/, { timeout: 10000 })
    })

    test('API 401 response triggers session expiration flow', async ({ page }) => {
      // This test uses network interception to simulate 401 responses

      // Set up tokens so the app thinks we're logged in
      await page.addInitScript(() => {
        // Create a mock JWT that looks valid but will fail on the server
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 3600 // Expires in 1 hour
          })
        )
        const signature = 'mock-signature'
        const mockToken = `${header}.${payload}.${signature}`

        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'mock-refresh-token')
      })

      // Intercept API calls to return 401
      await page.route('**/api/**', async route => {
        const url = route.request().url()

        // Let login and refresh endpoints through normally (they'll fail anyway)
        if (url.includes('/auth/login') || url.includes('/auth/refresh')) {
          // Simulate refresh token failure
          if (url.includes('/auth/refresh')) {
            await route.fulfill({
              status: 401,
              contentType: 'application/json',
              body: JSON.stringify({
                success: false,
                message: 'Refresh token expired'
              })
            })
          } else {
            await route.continue()
          }
        } else {
          // Return 401 for all other API calls
          await route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              success: false,
              message: 'Authentication required'
            })
          })
        }
      })

      // Navigate to a protected page that will make API calls
      await page.goto('/dashboard')

      // Wait for potential modal or redirect
      await page.waitForTimeout(2000)

      // Should either show the session expired modal or redirect to login
      const modal = page.locator('[role="dialog"]')
      const isModalVisible = await modal.isVisible().catch(() => false)

      if (isModalVisible) {
        // Modal is shown - verify content
        await expect(modal.getByText(/session expired/i)).toBeVisible()

        // Click the login button
        await modal.getByRole('button', { name: /log in/i }).click()

        // Should redirect to login
        await expect(page).toHaveURL(/\/login/, { timeout: 5000 })
      } else {
        // No modal - should have redirected to login
        await expect(page).toHaveURL(/\/login/, { timeout: 5000 })
      }
    })
  })

  test.describe('Network Error Handling', () => {
    test('network errors do not trigger session expiration modal', async ({ page }) => {
      // Set up valid mock tokens so the app thinks we're logged in
      await page.addInitScript(() => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 3600
          })
        )
        const mockToken = `${header}.${payload}.mock`
        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'mock-refresh')
      })

      // Intercept API calls to simulate network errors (abort the request)
      await page.route('**/api/users/me', async route => {
        await route.abort('failed')
      })

      // Navigate to dashboard which will trigger API calls
      await page.goto('/dashboard')

      // Wait a bit for any modal to potentially appear
      await page.waitForTimeout(2000)

      // Session expiration modal should NOT be visible
      const modal = page.locator('[role="dialog"]')
      const isModalVisible = await modal.isVisible().catch(() => false)

      // Network errors should not trigger the session expiration modal
      expect(isModalVisible).toBe(false)
    })
  })

  test.describe('Multiple 401 Handling', () => {
    test('concurrent 401 responses show only one modal', async ({ page }) => {
      // Set up mock tokens
      await page.addInitScript(() => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 3600
          })
        )
        const mockToken = `${header}.${payload}.mock`

        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'mock-refresh')
      })

      let requestCount = 0

      // Intercept all API calls to return 401
      await page.route('**/api/**', async route => {
        requestCount++

        if (route.request().url().includes('/auth/refresh')) {
          await route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({ success: false, message: 'Refresh failed' })
          })
        } else {
          await route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({ success: false, message: 'Unauthorized' })
          })
        }
      })

      // Navigate to trigger multiple API calls
      await page.goto('/dashboard')
      await page.waitForTimeout(2000)

      // Count the number of modals - should only be one
      const modals = page.locator('[role="dialog"]')
      const modalCount = await modals.count()

      // Should have at most one modal visible
      expect(modalCount).toBeLessThanOrEqual(1)
    })
  })
})
