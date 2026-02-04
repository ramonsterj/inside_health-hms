import { test, expect } from '@playwright/test'
import { confirmDialogAccept, waitForOverlaysToClear } from './utils/test-helpers'

// Mock user data for different roles
const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'admission:create',
    'admission:read',
    'admission:update',
    'admission:delete',
    'admission:upload-consent',
    'admission:view-consent',
    'patient:read',
    'patient:create',
    'room:read',
    'room:create',
    'room:update',
    'room:delete',
    'triage-code:read',
    'triage-code:create',
    'triage-code:update',
    'triage-code:delete'
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
  permissions: [
    'admission:create',
    'admission:read',
    'admission:update',
    'admission:upload-consent',
    'admission:view-consent',
    'patient:read',
    'patient:create',
    'room:read',
    'triage-code:read'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockDoctorUser = {
  id: 3,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Dr. Maria',
  lastName: 'Garcia',
  roles: ['DOCTOR'],
  permissions: ['patient:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockPatient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  age: 45,
  idDocumentNumber: '1234567890101'
}

const mockTriageCodes = [
  { id: 1, code: 'A', color: '#FF0000', description: 'Critical', displayOrder: 1 },
  { id: 2, code: 'B', color: '#FFA500', description: 'Urgent', displayOrder: 2 },
  { id: 3, code: 'C', color: '#FFFF00', description: 'Semi-urgent', displayOrder: 3 }
]

const mockRooms = [
  { id: 1, number: '101', type: 'PRIVATE', capacity: 1, availableBeds: 1 },
  { id: 2, number: '201', type: 'SHARED', capacity: 4, availableBeds: 3 }
]

const mockDoctors = [
  { id: 3, firstName: 'Dr. Maria', lastName: 'Garcia', salutation: 'Dr.' },
  { id: 4, firstName: 'Dr. Carlos', lastName: 'Lopez', salutation: 'Dr.' }
]

const mockAdmission = {
  id: 1,
  patient: mockPatient,
  triageCode: mockTriageCodes[0],
  room: { ...mockRooms[0], availableBeds: 0 },
  treatingPhysician: mockDoctors[0],
  admissionDate: '2026-01-23T10:30:00',
  dischargeDate: null,
  status: 'ACTIVE',
  type: 'HOSPITALIZATION',
  inventory: 'Wallet, phone, glasses',
  hasConsentDocument: false,
  consultingPhysicians: [],
  createdAt: '2026-01-23T10:35:00',
  createdBy: { id: 2, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' },
  updatedAt: '2026-01-23T10:35:00',
  updatedBy: { id: 2, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' }
}

const mockAdmissionsPage = {
  content: [
    {
      id: 1,
      patient: mockPatient,
      triageCode: mockTriageCodes[0],
      room: mockRooms[0],
      treatingPhysician: mockDoctors[0],
      admissionDate: '2026-01-23T10:30:00',
      status: 'ACTIVE',
      type: 'HOSPITALIZATION'
    }
  ],
  page: {
    totalElements: 1,
    totalPages: 1,
    size: 20,
    number: 0
  }
}

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

// Helper function to setup doctor mocks
async function setupDoctorMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockDoctorUser })
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

// Helper function to setup common admission API mocks
async function setupAdmissionMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/triage-codes', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockTriageCodes })
    })
  })

  await page.route('**/api/v1/rooms/available', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockRooms })
    })
  })

  await page.route('**/api/v1/admissions/doctors', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockDoctors })
    })
  })

  await page.route('**/api/v1/admissions/patients/search*', async (route) => {
    const url = new URL(route.request().url())
    const query = url.searchParams.get('q') || ''

    if (query.toLowerCase().includes('juan')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [mockPatient] })
      })
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    }
  })
}

test.describe('Admissions - Administrative Staff', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view admission list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/admissions*', async (route) => {
      if (route.request().method() === 'GET' && !route.request().url().includes('/admissions/')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmissionsPage })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/admissions')
    await expect(page).toHaveURL(/\/admissions/)

    // Should see the admission list
    await expect(page.getByRole('heading', { name: /Admissions/i })).toBeVisible()
    await expect(page.getByText('Juan')).toBeVisible()
  })

  // NOTE: The "New Admission" button was removed from the admissions list.
  // Admissions are now created from the Patient Detail view by clicking "Admit Patient".
  // Tests for the old wizard flow have been removed.

  test('can view admission details', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    // Should see admission details
    await expect(page.getByText('Juan Pérez García')).toBeVisible()
    await expect(page.getByText(/ACTIVE|Activo/i)).toBeVisible()
  })

  test('can see discharge button on active admission', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    // Should see discharge button
    await expect(page.getByRole('button', { name: /Discharge|Alta/i })).toBeVisible()
  })

  test('can discharge patient', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const dischargedAdmission = {
      ...mockAdmission,
      status: 'DISCHARGED',
      dischargeDate: '2026-01-23T15:00:00'
    }

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/1/discharge', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: dischargedAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    // Wait for page to load and overlays to clear
    await expect(page.getByRole('button', { name: /Discharge|Alta/i })).toBeVisible()
    await waitForOverlaysToClear(page)

    // Click discharge button
    await page.getByRole('button', { name: /Discharge|Alta/i }).click()

    // Confirm discharge using shared helper
    await confirmDialogAccept(page)

    // Should see success message or status change
    await expect(page.getByText(/discharged|dado de alta/i).first()).toBeVisible({ timeout: 10000 })
  })

  test('cannot see delete button (admin only)', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    // Should NOT see delete button
    await expect(page.getByRole('button', { name: /Delete|Eliminar/i })).not.toBeVisible()
  })

  test('shows audit information on admission detail', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    // Should see audit information
    await expect(page.getByText(/Created by|Creado por/i)).toBeVisible()
    await expect(page.getByText('receptionist').first()).toBeVisible()
  })
})

test.describe('Admissions - Admin User', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can see delete button', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    // Admin should see delete button
    await expect(page.getByRole('button', { name: /Delete|Eliminar/i })).toBeVisible()
  })

  test('can delete admission', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupAdminMocks(page)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockAdmission })
        })
      } else if (route.request().method() === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, message: 'Admission deleted successfully' })
        })
      }
    })

    await page.route('**/api/v1/admissions?*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }
        })
      })
    })

    await page.goto('/admissions/1')

    // Wait for page to load and overlays to clear
    await expect(page.getByRole('button', { name: /Delete|Eliminar/i })).toBeVisible()
    await waitForOverlaysToClear(page)

    // Click delete button
    await page.getByRole('button', { name: /Delete|Eliminar/i }).click()

    // Confirm deletion using shared helper
    await confirmDialogAccept(page)

    // Should redirect to list
    await expect(page).toHaveURL(/\/admissions$/, { timeout: 10000 })
  })
})

test.describe('Admissions - Doctor (No Access)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('cannot access admissions list', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupDoctorMocks(page)

    await page.goto('/admissions')

    // Should be redirected or see access denied
    await expect(page).not.toHaveURL(/\/admissions$/)
  })
})

test.describe('Admissions - Status Filter', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can filter admissions by status', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const dischargedAdmission = {
      ...mockAdmissionsPage.content[0],
      status: 'DISCHARGED'
    }

    await page.route('**/api/v1/admissions*', async (route) => {
      const url = new URL(route.request().url())
      const status = url.searchParams.get('status')

      if (status === 'DISCHARGED') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: [dischargedAdmission],
              page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      } else if (status === 'ACTIVE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockAdmissionsPage
          })
        })
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: [...mockAdmissionsPage.content, dischargedAdmission],
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      }
    })

    await page.goto('/admissions')

    // Find and click status filter
    const statusFilter = page.locator('select, [role="combobox"]').filter({ hasText: /Status|Estado/i })
    if (await statusFilter.isVisible()) {
      await statusFilter.click()
      await page.getByText('DISCHARGED').click()

      // Should show only discharged admissions
      await expect(page.getByText('DISCHARGED')).toBeVisible()
    }
  })
})

test.describe('Admissions - Room Availability', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('room dropdown shows available beds', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)
    await setupAdmissionMocks(page)

    // Mock patient summary endpoint for pre-selected patient
    await page.route('**/api/v1/admissions/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { ...mockPatient, hasIdDocument: false, hasActiveAdmission: false }
        })
      })
    })

    // Navigate directly with patientId (new flow)
    await page.goto('/admissions/new?patientId=1')

    // Patient should be pre-selected, verify patient name is shown
    await expect(page.getByText('Juan Pérez García')).toBeVisible()

    // Select HOSPITALIZATION type (which requires room)
    await page.locator('.p-select').first().click() // Type dropdown
    await page.getByText('Hospitalization', { exact: false }).first().click()

    // Click on Room dropdown to open it
    await page.locator('.p-select').nth(2).click() // Room dropdown (after triage code)

    // Room dropdown should show available beds
    await expect(page.getByText(/101.*1.*available|101.*1.*disponible/i)).toBeVisible()
    await expect(page.getByText(/201.*3.*available|201.*3.*disponible/i)).toBeVisible()
  })

  test('full rooms do not appear in dropdown', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    // Mock patient summary
    await page.route('**/api/v1/admissions/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { ...mockPatient, hasIdDocument: false, hasActiveAdmission: false }
        })
      })
    })

    await page.route('**/api/v1/triage-codes', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockTriageCodes })
      })
    })

    await page.route('**/api/v1/rooms/available', async (route) => {
      // Only return rooms with available beds (room 101 has 0, so only 201 returned)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [{ id: 2, number: '201', type: 'SHARED', capacity: 4, availableBeds: 2 }]
        })
      })
    })

    await page.route('**/api/v1/admissions/doctors', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDoctors })
      })
    })

    // Navigate directly with patientId (new flow)
    await page.goto('/admissions/new?patientId=1')

    // Patient should be pre-selected
    await expect(page.getByText('Juan Pérez García')).toBeVisible()

    // Select HOSPITALIZATION type (which requires room)
    await page.locator('.p-select').first().click() // Type dropdown
    await page.getByText('Hospitalization', { exact: false }).first().click()

    // Click on Room dropdown to open it
    await page.locator('.p-select').nth(2).click() // Room dropdown

    // Room 201 should appear, 101 (full) should not
    await expect(page.getByText('201')).toBeVisible()
    await expect(page.getByText('101')).not.toBeVisible()
  })
})

// NOTE: Consent document tests have been moved to documents.spec.ts
// Documents (including consent forms) are now managed in the AdmissionDetailView
// via the DocumentList component, not in the admission creation form.

test.describe('Admissions - Patient-Centric Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can click Admit button from patient list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)
    await setupAdmissionMocks(page)

    const mockPatientsPage = {
      content: [{ ...mockPatient, hasIdDocument: false, hasActiveAdmission: false }],
      page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
    }

    await page.route('**/api/v1/patients*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatientsPage })
        })
      }
    })

    // Mock patient summary endpoint for the wizard
    await page.route('**/api/v1/admissions/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { ...mockPatient, hasIdDocument: false, hasActiveAdmission: false }
        })
      })
    })

    await page.goto('/patients')

    // Should see Admit button
    const admitButton = page.locator('button[class*="success"]').filter({ has: page.locator('.pi-user-plus') })
    await expect(admitButton).toBeVisible()

    // Click Admit button
    await admitButton.click()

    // Should navigate to admission wizard with patientId query param
    await expect(page).toHaveURL(/\/admissions\/new\?patientId=1/)
  })

  test('admission wizard pre-selects patient from query param', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)
    await setupAdmissionMocks(page)

    const mockPatientSummary = { ...mockPatient, hasIdDocument: false, hasActiveAdmission: false }

    await page.route('**/api/v1/admissions/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatientSummary })
      })
    })

    await page.goto('/admissions/new?patientId=1')

    // Should see patient name pre-selected (not the search input)
    await expect(page.getByText('Juan Pérez García')).toBeVisible()

    // Search section should be hidden
    await expect(page.getByPlaceholder(/Search by name|Buscar por nombre/i)).not.toBeVisible()
  })

  test('invalid patientId redirects to patients list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)
    await setupAdmissionMocks(page)

    await page.route('**/api/v1/admissions/patients/999', async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Patient not found' })
      })
    })

    const mockPatientsPage = {
      content: [{ ...mockPatient, hasIdDocument: false, hasActiveAdmission: false }],
      page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
    }

    await page.route('**/api/v1/patients*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatientsPage })
        })
      }
    })

    await page.goto('/admissions/new?patientId=999')

    // Should redirect to patients list
    await expect(page).toHaveURL(/\/patients/, { timeout: 10000 })
  })

  test('no patientId redirects to patients page', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)
    await setupAdmissionMocks(page)

    const mockPatientsPage = {
      content: [{ ...mockPatient, hasIdDocument: false, hasActiveAdmission: false }],
      page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
    }

    await page.route('**/api/v1/patients*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatientsPage })
        })
      }
    })

    await page.goto('/admissions/new')

    // Without patientId, should redirect to patients page
    await expect(page).toHaveURL(/\/patients/, { timeout: 10000 })
  })

  test('patient with active admission shows warning when pre-selected', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)
    await setupAdmissionMocks(page)

    const mockPatientWithActiveAdmission = {
      ...mockPatient,
      hasIdDocument: false,
      hasActiveAdmission: true
    }

    await page.route('**/api/v1/admissions/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatientWithActiveAdmission })
      })
    })

    await page.goto('/admissions/new?patientId=1')

    // Should see warning message
    await expect(page.getByText(/already has an active admission|ya tiene una admisión activa/i)).toBeVisible()

    // Submit button should be disabled (form has no Next button, just Create Admission button)
    const submitButton = page.getByRole('button', { name: /Create Admission|Crear Admision/i })
    await expect(submitButton).toBeDisabled()
  })
})
