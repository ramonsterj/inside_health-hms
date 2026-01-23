import { test, expect } from '@playwright/test'

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('root redirects to dashboard (which redirects to login)', async ({ page }) => {
    await page.goto('/')

    // Should ultimately end up at login (since not authenticated)
    await expect(page).toHaveURL(/\/login/)
  })

  test('unknown routes redirect to dashboard', async ({ page }) => {
    await page.goto('/unknown-page')

    // Should redirect through dashboard to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('login page is accessible without auth', async ({ page }) => {
    await page.goto('/login')
    await expect(page).toHaveURL(/\/login/)
  })

  // Note: Public registration has been removed (FR7), so /register route no longer exists
})

test.describe('Navigation with mock auth', () => {
  test.beforeEach(async ({ page }) => {
    // Set up mock tokens to simulate authenticated state
    await page.addInitScript(() => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
    })
  })

  test('authenticated users are redirected from login to dashboard', async ({ page }) => {
    // Mock the API response for fetching user
    await page.route('**/api/users/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: 1,
            username: 'testuser',
            email: 'test@example.com',
            roles: ['USER']
          }
        })
      })
    })

    await page.goto('/login')

    // Should redirect to dashboard
    await expect(page).toHaveURL(/\/dashboard/)
  })
})
