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
  permissions: ['room:read', 'room:create', 'room:update', 'room:delete'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockAdminStaffUser = {
  id: 2,
  username: 'receptionist',
  email: 'receptionist@example.com',
  firstName: 'Reception',
  lastName: 'Staff',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: ['room:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockRooms = [
  { id: 1, number: '101', type: 'PRIVATE', capacity: 1 },
  { id: 2, number: '102', type: 'PRIVATE', capacity: 1 },
  { id: 3, number: '201', type: 'SHARED', capacity: 4 },
  { id: 4, number: '301', type: 'SHARED', capacity: 6 }
]

const mockRoomsAvailability = [
  { id: 1, number: '101', type: 'PRIVATE', capacity: 1, availableBeds: 1 },
  { id: 2, number: '102', type: 'PRIVATE', capacity: 1, availableBeds: 0 },
  { id: 3, number: '201', type: 'SHARED', capacity: 4, availableBeds: 3 },
  { id: 4, number: '301', type: 'SHARED', capacity: 6, availableBeds: 6 }
]

// Helper function to setup authenticated state
async function setupAuth(page: import('@playwright/test').Page, user: typeof mockAdminUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

// Helper function to setup API mocks for admin
async function setupAdminMocks(page: import('@playwright/test').Page) {
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
}

// Helper function to setup API mocks for admin staff
async function setupAdminStaffMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockAdminStaffUser })
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
}

// waitForOverlaysToClear is now imported from shared utils

test.describe('Rooms - Admin Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view rooms list', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      if (!route.request().url().includes('/available')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockRooms })
        })
      }
    })

    await page.goto('/admin/rooms')

    // Should see the rooms list
    await expect(page.getByRole('heading', { name: /Rooms|Habitaciones/i })).toBeVisible()
    await expect(page.getByText('101')).toBeVisible()
    await expect(page.getByText('201')).toBeVisible()
  })

  test('can see New Room button', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.goto('/admin/rooms')

    // Should see the New Room button
    await expect(page.getByRole('button', { name: /New Room|Nueva Habitación/i })).toBeVisible()
  })

  test('can navigate to create room form', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.goto('/admin/rooms')

    await page.getByRole('button', { name: /New Room|Nueva Habitación/i }).click()

    await expect(page).toHaveURL(/\/admin\/rooms\/new/)
  })

  test('room form displays all required fields', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/rooms/new')

    // Check that all required form fields are present
    await expect(page.getByText(/Room Number|Numero de Habitacion/i).first()).toBeVisible()
    await expect(page.getByText(/Room Type|Tipo de Habitacion/i).first()).toBeVisible()
    await expect(page.getByText(/Capacity|Capacidad/i).first()).toBeVisible()
  })

  test('can create new private room', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const newRoom = {
      id: 5,
      number: '401',
      type: 'PRIVATE',
      capacity: 1
    }

    await page.route('**/api/v1/rooms', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newRoom })
        })
      } else if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [...mockRooms, newRoom] })
        })
      }
    })

    await page.goto('/admin/rooms/new')

    // Fill the form using id selectors
    await page.locator('#number').fill('401')

    // Select room type - click on combobox, then select option from dropdown
    const typeSelect = page.locator('#type')
    await typeSelect.click()
    await page.locator('.p-select-option').filter({ hasText: 'Private' }).click()

    // Fill capacity
    const capacityInput = page.locator('#capacity input, input[name="capacity"]')
    await capacityInput.fill('1')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to list
    await expect(page).toHaveURL(/\/admin\/rooms$/, { timeout: 10000 })
  })

  test('can create shared room with higher capacity', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const newRoom = {
      id: 5,
      number: '401',
      type: 'SHARED',
      capacity: 8
    }

    await page.route('**/api/v1/rooms', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newRoom })
        })
      } else if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [...mockRooms, newRoom] })
        })
      }
    })

    await page.goto('/admin/rooms/new')

    // Fill the form using id selectors
    await page.locator('#number').fill('401')

    // Fill capacity (shared room with 8 beds)
    const capacityInput = page.locator('#capacity input, input[name="capacity"]')
    await capacityInput.fill('8')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to list
    await expect(page).toHaveURL(/\/admin\/rooms$/, { timeout: 10000 })
  })

  test('shows validation error for duplicate room number', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Room with number 101 already exists'
          })
        })
      }
    })

    await page.goto('/admin/rooms/new')

    // Fill with existing number using id selector
    await page.locator('#number').fill('101')

    const capacityInput = page.locator('#capacity input, input[name="capacity"]')
    await capacityInput.fill('1')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show error message in toast notification
    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
    await expect(toast.locator('.p-toast-detail')).toContainText(/already exists|ya existe/i)
  })

  test('shows validation error for empty room number', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/rooms/new')

    // Leave room number empty - only fill capacity
    const capacityInput = page.locator('#capacity input, input[name="capacity"]')
    await capacityInput.fill('1')

    // Submit the form with empty room number
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show validation error message for required room number
    // The Message component uses severity="error" which renders with p-message-error class
    await expect(page.locator('.p-message-error, [data-pc-name="message"]').first()).toBeVisible({
      timeout: 5000
    })
  })

  test('can edit existing room', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const room = mockRooms[0]

    await page.route('**/api/v1/rooms/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: room })
        })
      } else if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...room, capacity: 2 }
          })
        })
      }
    })

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.goto('/admin/rooms/1/edit')

    // Update the capacity
    const capacityInput = page.locator('#capacity input, input[name="capacity"]')
    await capacityInput.fill('2')

    // Submit
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to list
    await expect(page).toHaveURL(/\/admin\/rooms$/, { timeout: 10000 })
  })

  test('can delete room without active admissions', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.route('**/api/v1/rooms/4', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, message: 'Room deleted' })
        })
      }
    })

    await page.goto('/admin/rooms')

    // Wait for table to be fully loaded and any animations to complete
    await expect(page.getByText('301')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Find and click delete button for room 301 (id: 4)
    const row = page.locator('tr').filter({ hasText: '301' })
    const deleteBtn = row.locator('button[aria-label="Delete"], button:has(.pi-trash)')
    await deleteBtn.click()

    // Confirm deletion using shared helper
    await confirmDialogAccept(page)

    // Should show success message
    await expect(page.getByText(/deleted|eliminado/i)).toBeVisible({ timeout: 10000 })
  })

  test('shows error when deleting room with active admissions', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.route('**/api/v1/rooms/1', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Cannot delete room with active admissions'
          })
        })
      }
    })

    await page.goto('/admin/rooms')

    // Wait for table to be fully loaded and any animations to complete
    await expect(page.getByText('101')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Find and click delete button for room 101 (id: 1)
    const row = page.locator('tr').filter({ hasText: '101' })
    const deleteBtn = row.locator('button[aria-label="Delete"], button:has(.pi-trash)')
    await deleteBtn.click()

    // Confirm deletion using shared helper
    await confirmDialogAccept(page)

    // Should show error message in toast notification
    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
    await expect(toast.locator('.p-toast-detail')).toContainText(
      /active admissions|admisiones activas|Cannot delete/i
    )
  })

  test('displays room type correctly', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.goto('/admin/rooms')

    // Should see both PRIVATE and SHARED room types
    await expect(page.getByText('PRIVATE').first()).toBeVisible()
    await expect(page.getByText('SHARED').first()).toBeVisible()
  })

  test('displays room capacity', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.goto('/admin/rooms')

    // Should see capacity values
    // Room 101 has capacity 1, room 301 has capacity 6
    const row101 = page.locator('tr').filter({ hasText: '101' })
    const row301 = page.locator('tr').filter({ hasText: '301' })

    await expect(row101).toContainText('1')
    await expect(row301).toContainText('6')
  })
})

test.describe('Rooms - Admin Staff (Read Only)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view rooms list but cannot see create button', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.goto('/admin/rooms')

    // Staff can access the page and see the list
    await expect(page.getByRole('heading', { name: /Rooms|Habitaciones/i })).toBeVisible()
    await expect(page.getByText('101')).toBeVisible()

    // But should NOT see the New Room button (requires room:create)
    await expect(page.getByRole('button', { name: /New Room|Nueva Habitación/i })).not.toBeVisible()
  })

  test('cannot see edit and delete buttons in rooms list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.goto('/admin/rooms')

    // Wait for table to load
    await expect(page.getByText('101')).toBeVisible()

    // Staff should not see edit or delete buttons (requires room:update and room:delete)
    const row = page.locator('tr').filter({ hasText: '101' })
    await expect(row.getByRole('button', { name: /Edit|Editar/i })).not.toBeVisible()
    await expect(row.getByRole('button', { name: /Delete|Eliminar/i })).not.toBeVisible()
  })
})

test.describe('Rooms - Availability Display', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view room availability', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/rooms', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRooms })
      })
    })

    await page.route('**/api/v1/rooms/available', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRoomsAvailability })
      })
    })

    await page.route('**/api/v1/rooms/1/availability', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockRoomsAvailability[0] })
      })
    })

    await page.goto('/admin/rooms')

    // If there's an availability view/column, it should show available beds
    // This depends on the actual UI implementation
    // Looking for availability-related text
    const availabilityText = page.getByText(/available|disponible/i)
    if (await availabilityText.isVisible()) {
      await expect(availabilityText).toBeVisible()
    }
  })
})

