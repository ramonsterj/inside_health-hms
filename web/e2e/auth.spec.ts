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

    // Check page title
    await expect(page.getByRole('heading', { name: 'Welcome Back' })).toBeVisible()

    // Check form fields exist
    await expect(page.getByLabel('Email or Username')).toBeVisible()
    // PrimeVue Password component uses id instead of proper label association
    await expect(page.locator('#password')).toBeVisible()

    // Check submit button
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible()

    // Check register link
    await expect(page.getByRole('link', { name: 'Create one' })).toBeVisible()
  })

  test('shows validation errors for empty form submission', async ({ page }) => {
    await page.goto('/login')

    // Click submit without filling fields
    await page.getByRole('button', { name: 'Sign In' }).click()

    // Should show validation errors
    await expect(page.getByText(/required/i).first()).toBeVisible()
  })

  test('navigates to register page', async ({ page }) => {
    await page.goto('/login')

    // Click the register link
    await page.getByRole('link', { name: 'Create one' }).click()

    // Should navigate to register
    await expect(page).toHaveURL(/\/register/)
  })

  test('preserves redirect query param', async ({ page }) => {
    // Try to access a protected page
    await page.goto('/profile')

    // Should redirect to login with redirect param (may or may not be URL-encoded)
    await expect(page).toHaveURL(/\/login\?redirect=.*profile/)
  })
})
