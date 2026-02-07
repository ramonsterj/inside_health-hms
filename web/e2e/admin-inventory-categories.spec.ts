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
    'inventory-category:read',
    'inventory-category:create',
    'inventory-category:update',
    'inventory-category:delete'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockReadOnlyUser = {
  id: 2,
  username: 'staff',
  email: 'staff@example.com',
  firstName: 'Staff',
  lastName: 'User',
  roles: ['USER'],
  permissions: ['inventory-category:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockCategories = [
  {
    id: 1,
    name: 'Medicamentos',
    description: 'Pharmaceutical products',
    displayOrder: 1,
    active: true,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  },
  {
    id: 2,
    name: 'Suministros',
    description: 'Medical supplies',
    displayOrder: 2,
    active: true,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  },
  {
    id: 3,
    name: 'Equipos',
    description: 'Medical equipment',
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

// Helper function to setup API mocks for read-only user
async function setupReadOnlyMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockReadOnlyUser })
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

test.describe('Inventory Categories - Admin Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view inventory categories list', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/inventory-categories')

    await expect(page.getByRole('heading', { name: /Inventory Categories/i })).toBeVisible()
    await expect(page.getByText('Medicamentos')).toBeVisible()
    await expect(page.getByText('Suministros')).toBeVisible()
  })

  test('can see New Category button as admin', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/inventory-categories')

    await expect(page.getByRole('button', { name: /New Category/i })).toBeVisible()
  })

  test('can navigate to create category form', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/inventory-categories')
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

    await page.route('**/api/v1/admin/inventory-categories', async route => {
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

    await page.goto('/admin/inventory-categories/new')

    await page.locator('#name').fill('New Test Category')
    await page.locator('#description').fill('Test description')
    await page.locator('#displayOrder input').fill('10')

    await page.getByRole('button', { name: /Save/i }).click()

    await expect(page).toHaveURL(/\/admin\/inventory-categories$/)
  })

  test('shows validation error for blank name', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/inventory-categories/new')

    await page.getByRole('button', { name: /Save/i }).click()

    await expect(page.getByText(/name.*required/i)).toBeVisible()
  })

  test('shows error for duplicate category name', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Category with name Medicamentos already exists'
          })
        })
      }
    })

    await page.goto('/admin/inventory-categories/new')

    await page.locator('#name').fill('Medicamentos')
    await page.getByRole('button', { name: /Save/i }).click()

    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
    await expect(toast.locator('.p-toast-detail')).toContainText(/already exists/i)
  })

  test('can edit an existing category', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const updatedCategory = {
      ...mockCategories[0],
      name: 'Updated Medicamentos',
      description: 'Updated description'
    }

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.route('**/api/v1/admin/inventory-categories/1', async route => {
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

    await page.goto('/admin/inventory-categories/1/edit')

    await expect(page.locator('#name')).toHaveValue('Medicamentos')

    await page.locator('#name').fill('Updated Medicamentos')
    await page.getByRole('button', { name: /Save/i }).click()

    await expect(page).toHaveURL(/\/admin\/inventory-categories$/)
  })

  test('can delete an unused category', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.route('**/api/v1/admin/inventory-categories/1', async route => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 204,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
      }
    })

    await page.goto('/admin/inventory-categories')

    const firstRow = page.locator('tbody tr').first()
    await firstRow.getByRole('button', { name: /delete/i }).click()

    await confirmDialogAccept(page)

    await expect(page).toHaveURL(/\/admin\/inventory-categories$/)
  })

  test('shows error when deleting category with items', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.route('**/api/v1/admin/inventory-categories/1', async route => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Cannot delete category that has inventory items'
          })
        })
      }
    })

    await page.goto('/admin/inventory-categories')

    const firstRow = page.locator('tbody tr').first()
    await firstRow.getByRole('button', { name: /delete/i }).click()

    await confirmDialogAccept(page)

    await expect(page.getByText(/Cannot delete category|has inventory items/i)).toBeVisible({
      timeout: 5000
    })
  })

  test('displays active status tags correctly', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/inventory-categories')

    const activeRow = page.locator('tbody tr').filter({ hasText: 'Medicamentos' })
    await expect(activeRow.getByText(/Yes|Sí/i)).toBeVisible()

    const inactiveRow = page.locator('tbody tr').filter({ hasText: 'Equipos' })
    await expect(inactiveRow.getByText(/No/i)).toBeVisible()
  })
})

test.describe('Inventory Categories - Read-Only User', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('read-only user can view categories but cannot create/edit/delete', async ({ page }) => {
    await setupAuth(page, mockReadOnlyUser)
    await setupReadOnlyMocks(page)

    await page.route('**/api/v1/admin/inventory-categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCategories })
      })
    })

    await page.goto('/admin/inventory-categories')

    await expect(page.getByRole('heading', { name: /Inventory Categories/i })).toBeVisible()

    await expect(page.getByRole('button', { name: /New Category/i })).not.toBeVisible()
    await expect(page.getByRole('button', { name: /Edit/i })).not.toBeVisible()
    await expect(page.getByRole('button', { name: /Delete/i })).not.toBeVisible()
  })
})
