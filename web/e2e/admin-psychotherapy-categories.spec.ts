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
    'psychotherapy-category:read',
    'psychotherapy-category:create',
    'psychotherapy-category:update',
    'psychotherapy-category:delete'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockPsychologistUser = {
  id: 2,
  username: 'psychologist',
  email: 'psychologist@example.com',
  firstName: 'Sofia',
  lastName: 'Martinez',
  salutation: 'LICDA',
  roles: ['PSYCHOLOGIST'],
  permissions: ['psychotherapy-category:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockCategories = [
  {
    id: 1,
    name: 'Taller',
    description: 'Workshop activities',
    displayOrder: 1,
    active: true,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  },
  {
    id: 2,
    name: 'Sesión individual',
    description: 'Private one-on-one sessions',
    displayOrder: 2,
    active: true,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  },
  {
    id: 3,
    name: 'Terapia grupal',
    description: 'Group therapy sessions',
    displayOrder: 3,
    active: false,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  }
]

// Helper function to setup authenticated state
async function setupAuth(page: import('@playwright/test').Page, user: typeof mockAdminUser) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

// Helper function to setup API mocks for admin
async function setupAdminMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockAdminUser })
    })
  })

  // Mock auth refresh to prevent session expiration
  await page.route('**/api/auth/refresh', async route => {
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

// Helper function to setup API mocks for psychologist
async function setupPsychologistMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockPsychologistUser })
    })
  })

  await page.route('**/api/auth/refresh', async route => {
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

test.describe('Psychotherapy Categories - Admin Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view psychotherapy categories list', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/psychotherapy-categories')

    // Should see the categories list
    await expect(page.getByRole('heading', { name: /Activity Categories/i })).toBeVisible()
    await expect(page.getByText('Taller')).toBeVisible()
    await expect(page.getByText('Sesión individual')).toBeVisible()
  })

  test('can see New Category button as admin', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/psychotherapy-categories')

    // Should see the New Category button
    await expect(page.getByRole('button', { name: /New Category/i })).toBeVisible()
  })

  test('can navigate to create category form', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/psychotherapy-categories')
    await page.getByRole('button', { name: /New Category/i }).click()

    await expect(page.getByRole('heading', { name: /New Category/i })).toBeVisible()
  })

  test('can create a new category', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const newCategory = {
      id: 4,
      name: 'New Test Category',
      description: 'Test description',
      displayOrder: 10,
      active: true,
      createdAt: '2026-02-05T00:00:00Z',
      updatedAt: '2026-02-05T00:00:00Z'
    }

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newCategory })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [...mockCategories, newCategory] })
        })
      }
    })

    await page.goto('/admin/psychotherapy-categories/new')

    // Fill form
    await page.locator('#name').fill('New Test Category')
    await page.locator('#description').fill('Test description')
    await page.locator('#displayOrder input').fill('10')

    // Submit
    await page.getByRole('button', { name: /Save/i }).click()

    // Should redirect back to list
    await expect(page).toHaveURL(/\/admin\/psychotherapy-categories$/)
  })

  test('shows validation error for blank name', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/psychotherapy-categories/new')

    // Submit without filling name
    await page.getByRole('button', { name: /Save/i }).click()

    // Should show validation error
    await expect(page.getByText(/name.*required/i)).toBeVisible()
  })

  test('can edit an existing category', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const updatedCategory = {
      ...mockCategories[0],
      name: 'Updated Workshop',
      description: 'Updated description'
    }

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.route('**/api/v1/admin/psychotherapy-categories/1', async route => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: updatedCategory })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockCategories[0] })
        })
      }
    })

    await page.goto('/admin/psychotherapy-categories/1/edit')

    // Verify form is pre-filled
    await expect(page.locator('#name')).toHaveValue('Taller')

    // Update the name
    await page.locator('#name').fill('Updated Workshop')
    await page.getByRole('button', { name: /Save/i }).click()

    // Should redirect back to list
    await expect(page).toHaveURL(/\/admin\/psychotherapy-categories$/)
  })

  test('can delete a category', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.route('**/api/v1/admin/psychotherapy-categories/1', async route => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 204,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
      }
    })

    await page.goto('/admin/psychotherapy-categories')

    // Click delete button on first row
    const firstRow = page.locator('tbody tr').first()
    await firstRow.getByRole('button', { name: /delete/i }).click()

    // Confirm deletion
    await confirmDialogAccept(page)

    // Should still be on the list page
    await expect(page).toHaveURL(/\/admin\/psychotherapy-categories$/)
  })

  test('shows error when deleting category in use', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.route('**/api/v1/admin/psychotherapy-categories/1', async route => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Cannot delete category that is in use by existing activities'
          })
        })
      }
    })

    await page.goto('/admin/psychotherapy-categories')

    // Click delete button
    const firstRow = page.locator('tbody tr').first()
    await firstRow.getByRole('button', { name: /delete/i }).click()

    // Confirm deletion
    await confirmDialogAccept(page)

    // Should show error message (toast or inline)
    await expect(page.getByText(/Cannot delete category|in use/i)).toBeVisible({ timeout: 5000 })
  })

  test('displays active status tags correctly', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/psychotherapy-categories')

    // Active categories should show "Yes"
    const activeRow = page.locator('tbody tr').filter({ hasText: 'Taller' })
    await expect(activeRow.getByText(/Yes|Sí/i)).toBeVisible()

    // Inactive categories should show "No"
    const inactiveRow = page.locator('tbody tr').filter({ hasText: 'Terapia grupal' })
    await expect(inactiveRow.getByText(/No/i)).toBeVisible()
  })
})

test.describe('Psychotherapy Categories - Psychologist View', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('psychologist can view categories but cannot create/edit/delete', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupPsychologistMocks(page)

    await page.route('**/api/v1/admin/psychotherapy-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/psychotherapy-categories')

    // Psychologist can view the page (has read permission)
    await expect(page.getByRole('heading', { name: /Activity Categories/i })).toBeVisible()

    // But should NOT see New Category button (requires create permission)
    await expect(page.getByRole('button', { name: /New Category/i })).not.toBeVisible()

    // And should NOT see Edit/Delete buttons in the table (requires update/delete permissions)
    await expect(page.getByRole('button', { name: /Edit/i })).not.toBeVisible()
    await expect(page.getByRole('button', { name: /Delete/i })).not.toBeVisible()
  })
})
