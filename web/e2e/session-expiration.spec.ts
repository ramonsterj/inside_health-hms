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

  test.describe('Activity-Based Session Extension', () => {
    test('active user gets silent token refresh (no modal)', async ({ page }) => {
      let refreshCallCount = 0

      // Set up token that expires in 15 seconds (short for testing)
      await page.addInitScript(() => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            // Token expires in 15 seconds (proactive refresh at ~15-90 = immediate)
            exp: Math.floor(Date.now() / 1000) + 15
          })
        )
        const mockToken = `${header}.${payload}.mock`
        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'mock-refresh')
      })

      // Mock successful refresh - returns token that expires in 15 minutes
      await page.route('**/api/auth/refresh', async route => {
        refreshCallCount++
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 900 // New token: 15 min
          })
        )
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              accessToken: `${header}.${payload}.new`,
              refreshToken: 'new-refresh-token',
              user: {
                id: 1,
                username: 'admin',
                email: 'admin@example.com',
                status: 'ACTIVE',
                mustChangePassword: false,
                roles: ['ADMIN'],
                permissions: []
              }
            }
          })
        })
      })

      // Mock dashboard API calls
      await page.route('**/api/users/me', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 1,
              username: 'admin',
              email: 'admin@example.com',
              status: 'ACTIVE',
              mustChangePassword: false,
              roles: ['ADMIN'],
              permissions: []
            }
          })
        })
      })

      await page.goto('/dashboard')

      // Simulate user activity (mouse movements and clicks)
      await page.mouse.move(100, 100)
      await page.mouse.move(200, 200)
      await page.mouse.click(150, 150)

      // Wait for the proactive refresh to occur (token expires in 15s, refresh happens ~90s before or immediately)
      // Since token expires in 15s which is less than the 90s buffer, refresh should happen immediately
      await page.waitForTimeout(5000)

      // Modal should NOT be visible (silent refresh should have occurred)
      const modal = page.locator('[role="dialog"]')
      const isModalVisible = await modal.isVisible().catch(() => false)
      expect(isModalVisible).toBe(false)

      // Verify refresh was called (proactive refresh happened)
      expect(refreshCallCount).toBeGreaterThanOrEqual(1)

      // Verify new token was stored
      const newToken = await page.evaluate(() => localStorage.getItem('access_token'))
      expect(newToken).toContain('.new') // Our mock new token ends with .new
    })

    test('session expired modal shows when refresh fails', async ({ page }) => {
      // Set up token that expires in 5 seconds (very short for testing)
      await page.addInitScript(() => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 5 // Expires in 5 seconds
          })
        )
        const mockToken = `${header}.${payload}.mock`
        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'mock-refresh')
      })

      // Mock users/me to succeed initially
      await page.route('**/api/users/me', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 1,
              username: 'admin',
              email: 'admin@example.com',
              status: 'ACTIVE',
              mustChangePassword: false,
              roles: ['ADMIN'],
              permissions: []
            }
          })
        })
      })

      // Mock refresh to FAIL - simulates expired refresh token or server error
      await page.route('**/api/auth/refresh', async route => {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Refresh token expired'
          })
        })
      })

      await page.goto('/dashboard')

      // Wait for token to expire and proactive refresh to fail
      // Token expires in 5s, proactive refresh happens immediately (since 5s < 90s buffer)
      await page.waitForTimeout(3000)

      // Modal should be visible because refresh failed
      const modal = page.locator('[role="dialog"]')
      await expect(modal).toBeVisible({ timeout: 10000 })
      await expect(modal.getByText(/session expired/i)).toBeVisible()
    })

    test('proactive refresh updates stored tokens', async ({ page }) => {
      // Set up token that expires soon
      await page.addInitScript(() => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 10 // Expires in 10 seconds
          })
        )
        const mockToken = `${header}.${payload}.initial`
        localStorage.setItem('access_token', mockToken)
        localStorage.setItem('refresh_token', 'initial-refresh')
      })

      // Mock successful refresh with new tokens
      await page.route('**/api/auth/refresh', async route => {
        const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
        const payload = btoa(
          JSON.stringify({
            sub: '1',
            exp: Math.floor(Date.now() / 1000) + 900
          })
        )
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              accessToken: `${header}.${payload}.refreshed`,
              refreshToken: 'refreshed-refresh-token',
              user: {
                id: 1,
                username: 'admin',
                email: 'admin@example.com',
                status: 'ACTIVE',
                mustChangePassword: false,
                roles: ['ADMIN'],
                permissions: []
              }
            }
          })
        })
      })

      await page.route('**/api/users/me', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 1,
              username: 'admin',
              email: 'admin@example.com',
              status: 'ACTIVE',
              mustChangePassword: false,
              roles: ['ADMIN'],
              permissions: []
            }
          })
        })
      })

      await page.goto('/dashboard')

      // Simulate activity to trigger proactive refresh
      await page.mouse.move(100, 100)
      await page.waitForTimeout(500)
      await page.mouse.move(200, 200)

      // Wait for proactive refresh
      await page.waitForTimeout(5000)

      // Verify tokens were updated
      const accessToken = await page.evaluate(() => localStorage.getItem('access_token'))
      const refreshToken = await page.evaluate(() => localStorage.getItem('refresh_token'))

      // Should have the refreshed tokens
      expect(accessToken).toContain('.refreshed')
      expect(refreshToken).toBe('refreshed-refresh-token')
    })
  })
})
