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
    'document-type:read',
    'document-type:create',
    'document-type:update',
    'document-type:delete'
  ],
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
  permissions: ['document-type:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockDocumentTypes = [
  {
    id: 1,
    code: 'CONSENT_ADMISSION',
    name: 'Admission Consent',
    description: 'General admission consent form',
    displayOrder: 1,
    createdAt: '2026-01-01T00:00:00Z',
    createdBy: null,
    updatedAt: '2026-01-01T00:00:00Z',
    updatedBy: null
  },
  {
    id: 2,
    code: 'CONSENT_ISOLATION',
    name: 'Isolation Consent',
    description: 'Consent for patient isolation/seclusion',
    displayOrder: 2,
    createdAt: '2026-01-01T00:00:00Z',
    createdBy: null,
    updatedAt: '2026-01-01T00:00:00Z',
    updatedBy: null
  },
  {
    id: 3,
    code: 'CONSENT_RESTRAINT',
    name: 'Restraint Consent',
    description: 'Consent for physical restraints',
    displayOrder: 3,
    createdAt: '2026-01-01T00:00:00Z',
    createdBy: null,
    updatedAt: '2026-01-01T00:00:00Z',
    updatedBy: null
  },
  {
    id: 4,
    code: 'INVENTORY_LIST',
    name: 'Inventory List',
    description: 'Written inventory of patient belongings',
    displayOrder: 5,
    createdAt: '2026-01-01T00:00:00Z',
    createdBy: null,
    updatedAt: '2026-01-01T00:00:00Z',
    updatedBy: null
  },
  {
    id: 5,
    code: 'OTHER',
    name: 'Other Document',
    description: 'Other admission-related documents',
    displayOrder: 99,
    createdAt: '2026-01-01T00:00:00Z',
    createdBy: null,
    updatedAt: '2026-01-01T00:00:00Z',
    updatedBy: null
  }
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

test.describe('Document Types - Admin Management', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view document types list', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      if (!route.request().url().includes('/summary')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDocumentTypes })
        })
      }
    })

    await page.goto('/admin/document-types')

    // Should see the document types list
    await expect(page.getByRole('heading', { name: /Document Types/i })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'CONSENT_ADMISSION' })).toBeVisible()
    await expect(page.getByRole('cell', { name: 'Admission Consent', exact: true })).toBeVisible()
  })

  test('can see New Document Type button', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.goto('/admin/document-types')

    // Should see the New Document Type button
    await expect(page.getByRole('button', { name: /New|Nuevo/i })).toBeVisible()
  })

  test('can navigate to create document type form', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.goto('/admin/document-types')

    await page.getByRole('button', { name: /New|Nuevo/i }).click()

    await expect(page).toHaveURL(/\/admin\/document-types\/new/)
  })

  test('document type form displays all required fields', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/document-types/new')

    // Check that all required form fields are present
    await expect(page.getByText(/^Code|Código$/i).first()).toBeVisible()
    await expect(page.getByText(/^Name|Nombre$/i).first()).toBeVisible()
    await expect(page.getByText(/Description|Descripción/i).first()).toBeVisible()
    await expect(page.getByText(/Display Order|Orden/i).first()).toBeVisible()
  })

  test('can create new document type', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const newDocumentType = {
      id: 6,
      code: 'CONSENT_ELECTROSHOCK',
      name: 'ECT Consent',
      description: 'Consent for electroconvulsive therapy',
      displayOrder: 4,
      createdAt: '2026-01-24T10:00:00Z',
      createdBy: { id: 1, username: 'admin' },
      updatedAt: '2026-01-24T10:00:00Z',
      updatedBy: { id: 1, username: 'admin' }
    }

    await page.route('**/api/v1/document-types', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newDocumentType })
        })
      } else if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [...mockDocumentTypes, newDocumentType] })
        })
      }
    })

    await page.goto('/admin/document-types/new')

    // Fill the form
    await page.locator('#code').fill('CONSENT_ELECTROSHOCK')
    await page.locator('#name').fill('ECT Consent')
    await page.locator('#description').fill('Consent for electroconvulsive therapy')

    // Fill display order
    const displayOrderInput = page.locator('#displayOrder input, input[name="displayOrder"]')
    await displayOrderInput.fill('4')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to list
    await expect(page).toHaveURL(/\/admin\/document-types$/, { timeout: 10000 })
  })

  test('shows validation error for empty code', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/document-types/new')

    // Fill only name, leave code empty
    await page.locator('#name').fill('Test Type')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show validation error
    await expect(page.locator('.p-message-error, [data-pc-name="message"]').first()).toBeVisible({
      timeout: 5000
    })
  })

  test('shows validation error for invalid code format', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.goto('/admin/document-types/new')

    // Fill with invalid code (lowercase, contains invalid chars)
    await page.locator('#code').fill('invalid-code')
    await page.locator('#name').fill('Test Type')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show validation error about code format
    await expect(
      page.locator('.p-message-error').filter({ hasText: /uppercase|mayúsculas|pattern|formato/i })
    ).toBeVisible({
      timeout: 5000
    })
  })

  test('shows error for duplicate code', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: "Document type with code 'CONSENT_ADMISSION' already exists"
          })
        })
      }
    })

    await page.goto('/admin/document-types/new')

    // Fill with existing code
    await page.locator('#code').fill('CONSENT_ADMISSION')
    await page.locator('#name').fill('Duplicate Type')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show error message in toast
    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
    await expect(toast.locator('.p-toast-detail')).toContainText(/already exists|ya existe/i)
  })

  test('can edit existing document type', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    const docType = mockDocumentTypes[0]

    await page.route('**/api/v1/document-types/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: docType })
        })
      } else if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...docType, name: 'Updated Consent' }
          })
        })
      }
    })

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.goto('/admin/document-types/1/edit')

    // Form should be pre-filled
    await expect(page.locator('#code')).toHaveValue('CONSENT_ADMISSION')
    await expect(page.locator('#name')).toHaveValue('Admission Consent')

    // Update the name
    await page.locator('#name').fill('Updated Consent')

    // Submit
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to list
    await expect(page).toHaveURL(/\/admin\/document-types$/, { timeout: 10000 })
  })

  test('can delete document type without documents', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.route('**/api/v1/document-types/5', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, message: 'Document type deleted' })
        })
      }
    })

    await page.goto('/admin/document-types')

    // Wait for table to load - use role to be more specific
    await expect(page.getByRole('cell', { name: 'OTHER', exact: true })).toBeVisible()
    await waitForOverlaysToClear(page)

    // Find and click delete button for "OTHER" document type (id: 5)
    const row = page.locator('tr').filter({ has: page.getByRole('cell', { name: 'OTHER', exact: true }) })
    const deleteBtn = row.locator('button:has(.pi-trash)')
    await deleteBtn.click()

    // Confirm deletion
    await confirmDialogAccept(page)

    // Should show success message
    await expect(page.getByText(/deleted|eliminado/i)).toBeVisible({ timeout: 10000 })
  })

  test('shows error when deleting document type with existing documents', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.route('**/api/v1/document-types/1', async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Cannot delete document type. Documents of this type exist.'
          })
        })
      }
    })

    await page.goto('/admin/document-types')

    // Wait for table to load
    await expect(page.getByText('CONSENT_ADMISSION')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Find and click delete button for CONSENT_ADMISSION (id: 1)
    const row = page.locator('tr').filter({ hasText: 'CONSENT_ADMISSION' })
    const deleteBtn = row.locator('button:has(.pi-trash)')
    await deleteBtn.click()

    // Confirm deletion
    await confirmDialogAccept(page)

    // Should show error message in toast
    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
    await expect(toast.locator('.p-toast-detail')).toContainText(
      /Cannot delete|documents.*exist|no se puede eliminar/i
    )
  })

  test('displays description and display order columns', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.goto('/admin/document-types')

    // Should see table headers
    await expect(page.getByRole('columnheader', { name: /Code|Código/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /Name|Nombre/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /Description|Descripción/i })).toBeVisible()
    await expect(
      page.getByRole('columnheader', { name: /Display Order|Orden/i })
    ).toBeVisible()

    // Should see data
    await expect(page.getByText('General admission consent form')).toBeVisible()
  })
})

test.describe('Document Types - Admin Staff (Read Only)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view document types list but cannot see create button', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.goto('/admin/document-types')

    // Staff can access the page and see the list
    await expect(page.getByRole('heading', { name: /Document Types/i })).toBeVisible()
    await expect(page.getByText('CONSENT_ADMISSION')).toBeVisible()

    // But should NOT see the New button (requires document-type:create)
    await expect(page.getByRole('button', { name: /New|Nuevo/i })).not.toBeVisible()
  })

  test('cannot see edit and delete buttons in document types list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.goto('/admin/document-types')

    // Wait for table to load
    await expect(page.getByText('CONSENT_ADMISSION')).toBeVisible()

    // Staff should not see edit or delete buttons
    const row = page.locator('tr').filter({ hasText: 'CONSENT_ADMISSION' })
    await expect(row.locator('button:has(.pi-pencil)')).not.toBeVisible()
    await expect(row.locator('button:has(.pi-trash)')).not.toBeVisible()
  })

  test('cannot access create document type page directly', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/document-types', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocumentTypes })
      })
    })

    await page.goto('/admin/document-types/new')

    // Should be redirected away (to dashboard)
    await expect(page).not.toHaveURL(/\/admin\/document-types\/new/)
  })
})
