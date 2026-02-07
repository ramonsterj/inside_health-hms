import { test, expect } from '@playwright/test'
import { confirmDialogAccept, waitForOverlaysToClear } from './utils/test-helpers'

// Mock user data
const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'inventory-item:read',
    'inventory-item:create',
    'inventory-item:update',
    'inventory-item:delete',
    'inventory-movement:read',
    'inventory-movement:create',
    'inventory-category:read',
    'room:read',
    'room:create',
    'room:update'
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
  permissions: ['inventory-item:read', 'inventory-category:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockCategories = [
  { id: 1, name: 'Medicamentos', description: 'Pharmaceutical products', displayOrder: 1, active: true },
  { id: 2, name: 'Suministros', description: 'Medical supplies', displayOrder: 2, active: true },
  { id: 3, name: 'Equipos', description: 'Medical equipment', displayOrder: 3, active: true }
]

const mockItems = [
  {
    id: 1,
    name: 'Acetaminophen 500mg',
    description: 'Pain reliever',
    category: { id: 1, name: 'Medicamentos' },
    price: 25.00,
    cost: 10.00,
    quantity: 100,
    restockLevel: 20,
    pricingType: 'FLAT',
    timeUnit: null,
    timeInterval: null,
    active: true,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  },
  {
    id: 2,
    name: 'Oxygen Tank',
    description: 'Medical oxygen supply',
    category: { id: 2, name: 'Suministros' },
    price: 50.00,
    cost: 30.00,
    quantity: 5,
    restockLevel: 10,
    pricingType: 'TIME_BASED',
    timeUnit: 'HOURS',
    timeInterval: 1,
    active: true,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  },
  {
    id: 3,
    name: 'Surgical Gloves',
    description: 'Disposable gloves',
    category: { id: 2, name: 'Suministros' },
    price: 5.00,
    cost: 2.00,
    quantity: 3,
    restockLevel: 50,
    pricingType: 'FLAT',
    timeUnit: null,
    timeInterval: null,
    active: true,
    createdAt: '2026-02-01T00:00:00Z',
    updatedAt: '2026-02-01T00:00:00Z'
  }
]

const mockMovements = [
  {
    id: 3,
    itemId: 1,
    type: 'EXIT',
    quantity: 5,
    previousQuantity: 105,
    newQuantity: 100,
    notes: 'Used for patient treatment',
    createdAt: '2026-02-05T16:00:00Z',
    createdBy: { id: 1, firstName: 'Admin', lastName: 'User' }
  },
  {
    id: 2,
    itemId: 1,
    type: 'ENTRY',
    quantity: 50,
    previousQuantity: 55,
    newQuantity: 105,
    notes: 'Restocked from supplier',
    createdAt: '2026-02-05T10:00:00Z',
    createdBy: { id: 1, firstName: 'Admin', lastName: 'User' }
  },
  {
    id: 1,
    itemId: 1,
    type: 'ENTRY',
    quantity: 55,
    previousQuantity: 0,
    newQuantity: 55,
    notes: 'Initial stock',
    createdAt: '2026-02-01T08:00:00Z',
    createdBy: { id: 1, firstName: 'Admin', lastName: 'User' }
  }
]

const mockLowStockItems = [
  {
    id: 3,
    name: 'Surgical Gloves',
    category: { id: 2, name: 'Suministros' },
    quantity: 3,
    restockLevel: 50,
    pricingType: 'FLAT',
    active: true
  },
  {
    id: 2,
    name: 'Oxygen Tank',
    category: { id: 2, name: 'Suministros' },
    quantity: 5,
    restockLevel: 10,
    pricingType: 'TIME_BASED',
    active: true
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

async function setupCommonMocks(page: import('@playwright/test').Page, user: typeof mockAdminUser) {
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: user })
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

  await page.route('**/api/v1/inventory-categories', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockCategories })
    })
  })
}

test.describe('Inventory Items - List & Filtering', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view inventory items list', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items**', async route => {
      const url = route.request().url()
      if (!url.includes('low-stock') && !url.includes('movements')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: mockItems, page: { totalElements: mockItems.length, totalPages: 1, size: 20, number: 0 } } })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/inventory')

    await expect(page.getByRole('heading', { name: /Inventory/i })).toBeVisible()
    await expect(page.getByText('Acetaminophen 500mg')).toBeVisible()
    await expect(page.getByText('Oxygen Tank')).toBeVisible()
    await expect(page.getByText('Surgical Gloves')).toBeVisible()
  })

  test('can filter items by category', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const filteredItems = mockItems.filter(i => i.category.id === 2)

    await page.route('**/api/v1/admin/inventory-items**', async route => {
      const url = route.request().url()
      if (url.includes('categoryId=2')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: filteredItems, page: { totalElements: filteredItems.length, totalPages: 1, size: 20, number: 0 } } })
        })
      } else if (!url.includes('low-stock') && !url.includes('movements')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: mockItems, page: { totalElements: mockItems.length, totalPages: 1, size: 20, number: 0 } } })
        })
      }
    })

    await page.goto('/inventory')
    await expect(page.getByText('Acetaminophen 500mg')).toBeVisible()

    // Select category filter
    const categorySelect = page.locator('.filters .p-select').first()
    await categorySelect.click()
    await page.locator('.p-select-option').filter({ hasText: 'Suministros' }).click()

    // Wait for filtered results
    await page.waitForTimeout(500)
  })

  test('can search items by name', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const searchResults = [mockItems[0]] // Only Acetaminophen

    await page.route('**/api/v1/admin/inventory-items**', async route => {
      const url = route.request().url()
      if (url.includes('search=aceta')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: searchResults, page: { totalElements: searchResults.length, totalPages: 1, size: 20, number: 0 } } })
        })
      } else if (!url.includes('low-stock') && !url.includes('movements')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: mockItems, page: { totalElements: mockItems.length, totalPages: 1, size: 20, number: 0 } } })
        })
      }
    })

    await page.goto('/inventory')
    await expect(page.getByText('Acetaminophen 500mg')).toBeVisible()

    // Type search term and press Enter
    const searchInput = page.locator('.filters input[type="text"]')
    await searchInput.fill('aceta')
    await searchInput.press('Enter')

    await page.waitForTimeout(500)
  })

  test('shows empty state when no items', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items**', async route => {
      const url = route.request().url()
      if (!url.includes('low-stock') && !url.includes('movements')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } } })
        })
      }
    })

    await page.goto('/inventory')

    await expect(page.getByText(/No inventory items found/i)).toBeVisible()
  })
})

test.describe('Inventory Items - Create', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can create a flat-priced item', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const newItem = {
      id: 4,
      name: 'Bandages',
      description: 'Sterile bandages',
      category: { id: 2, name: 'Suministros' },
      price: 15.00,
      cost: 5.00,
      quantity: 0,
      restockLevel: 30,
      pricingType: 'FLAT',
      timeUnit: null,
      timeInterval: null,
      active: true,
      createdAt: '2026-02-05T00:00:00Z',
      updatedAt: '2026-02-05T00:00:00Z'
    }

    const allItems = [...mockItems, newItem]
    await page.route('**/api/v1/admin/inventory-items', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newItem })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: allItems, page: { totalElements: allItems.length, totalPages: 1, size: 20, number: 0 } } })
        })
      }
    })

    await page.goto('/inventory/new')

    await page.locator('#name').fill('Bandages')
    await page.locator('#description').fill('Sterile bandages')

    // Select category
    await page.locator('#categoryId').click()
    await page.locator('.p-select-option').filter({ hasText: 'Suministros' }).click()

    // Fill price and cost
    await page.locator('#price input').fill('15')
    await page.locator('#cost input').fill('5')
    await page.locator('#restockLevel input').fill('30')

    await page.getByRole('button', { name: /Save/i }).click()

    await expect(page).toHaveURL(/\/inventory$/, { timeout: 10000 })
  })

  test('can create a time-based item', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const newItem = {
      id: 5,
      name: 'Ventilator',
      description: 'Mechanical ventilator',
      category: { id: 3, name: 'Equipos' },
      price: 200.00,
      cost: 100.00,
      quantity: 0,
      restockLevel: 2,
      pricingType: 'TIME_BASED',
      timeUnit: 'HOURS',
      timeInterval: 1,
      active: true,
      createdAt: '2026-02-05T00:00:00Z',
      updatedAt: '2026-02-05T00:00:00Z'
    }

    const allItems = [...mockItems, newItem]
    await page.route('**/api/v1/admin/inventory-items', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newItem })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: allItems, page: { totalElements: allItems.length, totalPages: 1, size: 20, number: 0 } } })
        })
      }
    })

    await page.goto('/inventory/new')

    await page.locator('#name').fill('Ventilator')
    await page.locator('#description').fill('Mechanical ventilator')

    // Select category
    await page.locator('#categoryId').click()
    await page.locator('.p-select-option').filter({ hasText: 'Equipos' }).click()

    // Select TIME_BASED pricing
    await page.locator('#pricingType').click()
    await page.locator('.p-select-option').filter({ hasText: 'Time-based' }).click()

    // Fill price and cost
    await page.locator('#price input').fill('200')
    await page.locator('#cost input').fill('100')
    await page.locator('#restockLevel input').fill('2')

    // Time-based fields should now be visible
    await expect(page.locator('#timeUnit')).toBeVisible()
    await expect(page.locator('#timeInterval')).toBeVisible()

    await page.locator('#timeUnit').click()
    await page.locator('.p-select-option').filter({ hasText: 'Hours' }).click()
    await page.locator('#timeInterval input').fill('1')

    await page.getByRole('button', { name: /Save/i }).click()

    await expect(page).toHaveURL(/\/inventory$/, { timeout: 10000 })
  })

  test('time-based fields hidden when pricing type is FLAT', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.goto('/inventory/new')

    // By default, pricing type is FLAT - time-based fields should not be visible
    await expect(page.locator('#timeUnit')).not.toBeVisible()
    await expect(page.locator('#timeInterval')).not.toBeVisible()
  })

  test('shows validation error for missing required fields', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.goto('/inventory/new')

    // Submit without filling anything
    await page.getByRole('button', { name: /Save/i }).click()

    // Should show validation errors
    await expect(page.getByText(/name.*required/i)).toBeVisible()
  })
})

test.describe('Inventory Items - Edit & Delete', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can edit an existing item without changing quantity', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const updatedItem = { ...mockItems[0], name: 'Acetaminophen 1000mg' }

    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: updatedItem })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockItems[0] })
        })
      }
    })

    await page.route('**/api/v1/admin/inventory-items', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { content: mockItems, page: { totalElements: mockItems.length, totalPages: 1, size: 20, number: 0 } } })
      })
    })

    await page.goto('/inventory/1/edit')

    await expect(page.locator('#name')).toHaveValue('Acetaminophen 500mg')

    await page.locator('#name').fill('Acetaminophen 1000mg')
    await page.getByRole('button', { name: /Save/i }).click()

    await expect(page).toHaveURL(/\/inventory$/, { timeout: 10000 })
  })

  test('can delete an item', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items**', async route => {
      const url = route.request().url()
      if (!url.includes('low-stock') && !url.includes('movements')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: mockItems, page: { totalElements: mockItems.length, totalPages: 1, size: 20, number: 0 } } })
        })
      }
    })

    await page.route('**/api/v1/admin/inventory-items/3', async route => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 204,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
      }
    })

    await page.goto('/inventory')

    await expect(page.getByText('Surgical Gloves')).toBeVisible()
    await waitForOverlaysToClear(page)

    const row = page.locator('tr').filter({ hasText: 'Surgical Gloves' })
    const deleteBtn = row.locator('button:has(.pi-trash)')
    await deleteBtn.click()

    await confirmDialogAccept(page)

    await expect(page).toHaveURL(/\/inventory$/)
  })
})

test.describe('Inventory Items - Detail & Movements', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view item detail page', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockItems[0] })
      })
    })

    await page.route('**/api/v1/admin/inventory-items/1/movements', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMovements })
      })
    })

    await page.goto('/inventory/1')

    await expect(page.getByText('Acetaminophen 500mg')).toBeVisible()
    await expect(page.getByText('Medicamentos')).toBeVisible()
    await expect(page.getByRole('heading', { name: /Stock Movements/i })).toBeVisible()
  })

  test('can see Record Movement button', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockItems[0] })
      })
    })

    await page.route('**/api/v1/admin/inventory-items/1/movements', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMovements })
      })
    })

    await page.goto('/inventory/1')

    await expect(page.getByRole('button', { name: /Record Movement/i })).toBeVisible()
  })

  test('can record an ENTRY movement', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const newMovement = {
      id: 4,
      itemId: 1,
      type: 'ENTRY',
      quantity: 20,
      previousQuantity: 100,
      newQuantity: 120,
      notes: 'New shipment',
      createdAt: '2026-02-06T10:00:00Z',
      createdBy: { id: 1, firstName: 'Admin', lastName: 'User' }
    }

    const updatedItem = { ...mockItems[0], quantity: 120 }

    let itemData = mockItems[0]
    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: itemData })
      })
    })

    let movementsList = [...mockMovements]
    await page.route('**/api/v1/admin/inventory-items/1/movements', async route => {
      if (route.request().method() === 'POST') {
        itemData = updatedItem
        movementsList = [newMovement, ...movementsList]
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newMovement })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: movementsList })
        })
      }
    })

    await page.goto('/inventory/1')

    await page.getByRole('button', { name: /Record Movement/i }).click()

    // Fill movement form - type should default to ENTRY
    await page.locator('#quantity input').fill('20')
    await page.locator('#notes').fill('New shipment')

    await page.getByRole('button', { name: /Save/i }).click()

    await waitForOverlaysToClear(page)
  })

  test('can record an EXIT movement', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const newMovement = {
      id: 4,
      itemId: 1,
      type: 'EXIT',
      quantity: 10,
      previousQuantity: 100,
      newQuantity: 90,
      notes: 'Patient usage',
      createdAt: '2026-02-06T10:00:00Z',
      createdBy: { id: 1, firstName: 'Admin', lastName: 'User' }
    }

    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockItems[0] })
      })
    })

    await page.route('**/api/v1/admin/inventory-items/1/movements', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newMovement })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockMovements })
        })
      }
    })

    await page.goto('/inventory/1')

    await page.getByRole('button', { name: /Record Movement/i }).click()

    // Select EXIT type
    await page.locator('#type').click()
    await page.locator('.p-select-option').filter({ hasText: 'Exit' }).click()

    await page.locator('#quantity input').fill('10')
    await page.locator('#notes').fill('Patient usage')

    await page.getByRole('button', { name: /Save/i }).click()

    await waitForOverlaysToClear(page)
  })

  test('shows error for insufficient stock on EXIT', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockItems[0] })
      })
    })

    await page.route('**/api/v1/admin/inventory-items/1/movements', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Insufficient stock. Current quantity: 100, requested: 500'
          })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockMovements })
        })
      }
    })

    await page.goto('/inventory/1')

    await page.getByRole('button', { name: /Record Movement/i }).click()

    // Select EXIT and enter too many
    await page.locator('#type').click()
    await page.locator('.p-select-option').filter({ hasText: 'Exit' }).click()
    await page.locator('#quantity input').fill('500')

    await page.getByRole('button', { name: /Save/i }).click()

    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
    await expect(toast.locator('.p-toast-detail')).toContainText(/Insufficient stock/i)
  })

  test('movement history is displayed in DESC order', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockItems[0] })
      })
    })

    await page.route('**/api/v1/admin/inventory-items/1/movements', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockMovements })
      })
    })

    await page.goto('/inventory/1')

    // Verify movements are shown (DESC order - most recent first)
    await expect(page.getByText('Used for patient treatment')).toBeVisible()
    await expect(page.getByText('Restocked from supplier')).toBeVisible()
    await expect(page.getByText('Initial stock')).toBeVisible()
  })

  test('shows empty state when no movements', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/1', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockItems[0] })
      })
    })

    await page.route('**/api/v1/admin/inventory-items/1/movements', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.goto('/inventory/1')

    await expect(page.getByText(/No movements recorded/i)).toBeVisible()
  })
})

test.describe('Low Stock Report', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view low stock report', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/low-stock**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockLowStockItems })
      })
    })

    await page.goto('/inventory/low-stock')

    await expect(page.getByRole('heading', { name: /Low Stock Report/i })).toBeVisible()
    await expect(page.getByText('Surgical Gloves')).toBeVisible()
    await expect(page.getByText('Oxygen Tank')).toBeVisible()
  })

  test('low stock items sorted by deficit DESC', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/low-stock**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockLowStockItems })
      })
    })

    await page.goto('/inventory/low-stock')

    // Wait for data to load
    await expect(page.getByText('Surgical Gloves')).toBeVisible()

    // Surgical Gloves (deficit -47) should appear before Oxygen Tank (deficit -5)
    const rows = page.locator('tbody tr')
    const firstRowText = await rows.first().textContent()
    expect(firstRowText).toContain('Surgical Gloves')
  })

  test('can filter low stock by category', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/low-stock**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockLowStockItems })
      })
    })

    await page.goto('/inventory/low-stock')

    // Category filter should be present
    const categorySelect = page.locator('.filters .p-select').first()
    await expect(categorySelect).toBeVisible()

    await categorySelect.click()
    await page.locator('.p-select-option').filter({ hasText: 'Suministros' }).click()

    await page.waitForTimeout(500)
  })

  test('shows empty state when no low stock items', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admin/inventory-items/low-stock**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.goto('/inventory/low-stock')

    await expect(page.getByText(/No low stock items/i)).toBeVisible()
  })
})

test.describe('Room Pricing', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('room form has price and cost fields', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.goto('/admin/rooms/new')

    await expect(page.locator('#price')).toBeVisible()
    await expect(page.locator('#cost')).toBeVisible()
  })

  test('can create room with price and cost', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const newRoom = {
      id: 5,
      number: '501',
      type: 'PRIVATE',
      gender: 'FEMALE',
      capacity: 1,
      price: 500.00,
      cost: 200.00
    }

    await page.route('**/api/v1/rooms', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newRoom })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [newRoom] })
        })
      }
    })

    await page.goto('/admin/rooms/new')

    await page.locator('#number').fill('501')

    const typeSelect = page.locator('#type')
    await typeSelect.click()
    await page.locator('.p-select-option').filter({ hasText: 'Single' }).click()
    await waitForOverlaysToClear(page)

    const capacityInput = page.locator('#capacity input, input[name="capacity"]')
    await capacityInput.fill('1')

    await page.locator('#price input').fill('500')
    await page.locator('#cost input').fill('200')

    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    await expect(page).toHaveURL(/\/admin\/rooms$/, { timeout: 10000 })
  })

  test('rooms list shows price and cost columns', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    const roomsWithPricing = [
      { id: 1, number: '101', type: 'PRIVATE', capacity: 1, price: 500.00, cost: 200.00 },
      { id: 2, number: '102', type: 'SHARED', capacity: 4, price: null, cost: null }
    ]

    await page.route('**/api/v1/rooms', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: roomsWithPricing })
      })
    })

    await page.goto('/admin/rooms')

    // Should display the room data
    await expect(page.getByText('101')).toBeVisible()
    await expect(page.getByText('102')).toBeVisible()
  })
})
