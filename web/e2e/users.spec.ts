import { test, expect } from '@playwright/test'

test.describe('User Management - Phone Number Primary Flag', () => {
  async function loginAsAdmin(page: import('@playwright/test').Page) {
    await page.goto('/login')

    // Clear localStorage once at the start (not on every page load)
    await page.evaluate(() => localStorage.clear())

    // Reload to apply the cleared state
    await page.reload()
    await page.waitForLoadState('networkidle')

    // Wait for login form to be visible
    const identifierInput = page.getByLabel('Email or Username')
    await expect(identifierInput).toBeVisible({ timeout: 10000 })

    // Fill in credentials for admin user
    await identifierInput.fill('admin')

    // PrimeVue Password wraps the input, so we need to find the actual input element
    const passwordInput = page.locator('#password input')
    await expect(passwordInput).toBeVisible({ timeout: 5000 })
    await passwordInput.fill('admin123')

    // Submit
    const signInButton = page.getByRole('button', { name: 'Sign In' })
    await expect(signInButton).toBeVisible()
    await signInButton.click()

    // Wait for redirect - could be dashboard or force password change
    await page.waitForURL(/\/(dashboard|auth\/change-password)/, { timeout: 15000 })

    // If redirected to password change, handle it
    if (page.url().includes('change-password')) {
      await page.waitForLoadState('networkidle')
      const passwordInputs = page.locator('input[type="password"]')
      await passwordInputs.first().fill('admin123')
      await passwordInputs.nth(1).fill('NewAdmin123!')
      await passwordInputs.nth(2).fill('NewAdmin123!')
      await page.getByRole('button', { name: /change|cambiar/i }).click()
      await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
    }
  }

  test('primary phone number flag persists after save', async ({ page }) => {
    // Login as admin
    await loginAsAdmin(page)

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

    // Find the phone number input and fill it if empty (required for save to be enabled)
    const phoneInput = dialog.locator('.phone-numbers-list input[type="text"]').first()
    await expect(phoneInput).toBeVisible({ timeout: 5000 })
    const phoneValue = await phoneInput.inputValue()
    if (!phoneValue || phoneValue.trim() === '') {
      await phoneInput.fill('12345678')
    }

    // Find the primary checkbox in the phone numbers section
    const primaryCheckbox = dialog.locator('.phone-numbers-list input[type="checkbox"]').first()
    await expect(primaryCheckbox).toBeVisible({ timeout: 5000 })

    // Get the current checked state
    const wasChecked = await primaryCheckbox.isChecked()

    // Toggle the checkbox to ensure we're making a change
    if (wasChecked) {
      // If checked, uncheck it first then recheck
      await primaryCheckbox.uncheck()
    }
    // Now check it
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
    // Login as admin
    await loginAsAdmin(page)

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

    // Generate unique username and email
    const timestamp = Date.now()
    const username = `testuser${timestamp}`
    const email = `testuser${timestamp}@example.com`

    // Fill in required fields - use id selector for username input
    const usernameInput = dialog.locator('#username')
    await expect(usernameInput).toBeVisible({ timeout: 5000 })
    await usernameInput.fill(username)

    const emailInput = dialog.locator('input[type="email"]')
    await expect(emailInput).toBeVisible()
    await emailInput.fill(email)

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
    await searchInput.fill(username)

    // Wait for search results
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)

    // Find the edit button for the new user
    const newUserRow = page.locator('.p-datatable-tbody tr').filter({ hasText: email })
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
