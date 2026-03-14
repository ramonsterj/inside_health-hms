import { test, expect } from '@playwright/test'

// Mock psychologist user - only has patient:read and admission:read
const mockPsychologistUser = {
  id: 5,
  username: 'psychologist',
  email: 'psychologist@example.com',
  firstName: 'Ana',
  lastName: 'Martínez',
  roles: ['PSYCHOLOGIST'],
  permissions: ['patient:read', 'admission:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'patient:read',
    'patient:create',
    'patient:update',
    'admission:read',
    'admission:create',
    'admission:update',
    'admission:delete'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockActivePatient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  age: 45,
  idDocumentNumber: '1234567890101',
  hasIdDocument: false,
  hasActiveAdmission: true
}

const mockDischargedPatient = {
  id: 2,
  firstName: 'María',
  lastName: 'López',
  age: 32,
  idDocumentNumber: '9876543210101',
  hasIdDocument: false,
  hasActiveAdmission: false
}

const mockPatientDetail = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  age: 45,
  sex: 'MALE',
  gender: 'Masculino',
  maritalStatus: 'MARRIED',
  religion: 'Católica',
  educationLevel: 'UNIVERSITY',
  occupation: 'Ingeniero',
  address: '4a Calle 5-67 Zona 1, Guatemala',
  email: 'juan.perez@email.com',
  idDocumentNumber: '1234567890101',
  notes: 'Paciente referido',
  hasIdDocument: false,
  hasActiveAdmission: true,
  emergencyContacts: [
    { id: 1, name: 'María de Pérez', relationship: 'Esposa', phone: '+502 5555-1234' }
  ],
  createdAt: '2026-01-21T10:00:00Z',
  createdBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' },
  updatedAt: '2026-01-21T10:00:00Z',
  updatedBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' }
}

const mockActiveAdmission = {
  id: 1,
  patient: { id: 1, firstName: 'Juan', lastName: 'Pérez García', age: 45 },
  triageCode: { id: 1, code: 'A', color: '#FF0000', description: 'Critical', displayOrder: 1 },
  room: { id: 1, number: '101', type: 'PRIVATE', capacity: 1, availableBeds: 0 },
  treatingPhysician: { id: 3, firstName: 'Dr. Maria', lastName: 'Garcia', salutation: 'Dr.' },
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

const mockDischargedAdmission = {
  ...mockActiveAdmission,
  id: 2,
  patient: { id: 2, firstName: 'María', lastName: 'López', age: 32 },
  status: 'DISCHARGED',
  dischargeDate: '2026-01-25T14:00:00'
}

async function setupAuth(page: import('@playwright/test').Page, user: typeof mockPsychologistUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

async function setupUserMocks(page: import('@playwright/test').Page, user: typeof mockPsychologistUser) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: user })
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

// ============================================================
// Patient Visibility Tests
// ============================================================

test.describe('Psychologist - Patient Visibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view patient list (only patients with active admissions)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    // Backend returns only patients with active admissions for psychologists
    const psychologistPatientsPage = {
      content: [mockActivePatient],
      page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
    }

    await page.route('**/api/v1/patients*', async (route) => {
      if (route.request().method() === 'GET' && !route.request().url().includes('/patients/')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: psychologistPatientsPage })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/patients')
    await expect(page).toHaveURL(/\/patients/)

    // Should see patients heading and active patient
    await expect(page.getByRole('heading', { name: /Patients/i })).toBeVisible()
    await expect(page.getByText('Juan')).toBeVisible()
    await expect(page.getByText('Pérez García')).toBeVisible()

    // Discharged patient should NOT appear (backend filters)
    await expect(page.getByText('María')).not.toBeVisible()
  })

  test('cannot see New Patient button (no patient:create permission)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/patients*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [mockActivePatient], page: { totalElements: 1, totalPages: 1, size: 20, number: 0 } }
        })
      })
    })

    await page.goto('/patients')

    await expect(page.getByRole('button', { name: /New Patient|Nuevo Paciente/i })).not.toBeVisible()
  })

  test('cannot see Admit button (no admission:create permission)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/patients*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [mockActivePatient], page: { totalElements: 1, totalPages: 1, size: 20, number: 0 } }
        })
      })
    })

    await page.goto('/patients')
    await expect(page.getByText('Juan')).toBeVisible()

    // Admit button (pi-user-plus) should not be visible
    const admitButton = page.locator('button').filter({ has: page.locator('.pi-user-plus') })
    await expect(admitButton).not.toBeVisible()
  })

  test('can view patient details for patient with active admission', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatientDetail })
        })
      }
    })

    await page.goto('/patients/1')

    // Should see patient details
    await expect(page.getByRole('heading', { name: 'Juan Pérez García' })).toBeVisible()
    await expect(page.getByText('45', { exact: true }).first()).toBeVisible()
  })

  test('cannot see Edit button on patient details (no patient:update permission)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatientDetail })
        })
      }
    })

    await page.goto('/patients/1')

    await expect(page.getByRole('heading', { name: 'Juan Pérez García' })).toBeVisible()
    await expect(page.getByRole('button', { name: /Edit|Editar/i })).not.toBeVisible()
  })

  test('denied access to patient without active admission (403)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    // Backend returns 403 for psychologist accessing patient without active admission
    await page.route('**/api/v1/patients/2', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 403,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Access denied' })
        })
      }
    })

    await page.goto('/patients/2')

    // Should see access denied or be redirected
    await expect(
      page.getByText(/Access denied|Acceso denegado|Forbidden|not authorized|No autorizado/i).first()
    ).toBeVisible({ timeout: 10000 })
  })

  test('cannot access patient create page', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.goto('/patients/new')

    // Should be redirected away from create page
    await expect(page).not.toHaveURL(/\/patients\/new/)
  })

  test('search filters work within active-admission patients only', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    let searchQuery = ''
    await page.route('**/api/v1/patients*', async (route) => {
      const url = new URL(route.request().url())
      searchQuery = url.searchParams.get('search') || ''

      const response =
        searchQuery.toLowerCase().includes('juan')
          ? { content: [mockActivePatient], page: { totalElements: 1, totalPages: 1, size: 20, number: 0 } }
          : { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: response })
      })
    })

    await page.goto('/patients')

    const searchInput = page.getByPlaceholder(/Search patients|Buscar pacientes/i)
    await searchInput.fill('NonExistent')

    // Wait for debounced search
    await page.waitForTimeout(500)

    await expect(page.getByText(/No patients found|No se encontraron pacientes/i)).toBeVisible()
  })
})

// ============================================================
// Admission Visibility Tests
// ============================================================

test.describe('Psychologist - Admission Visibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view admission list (only active admissions)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    // Backend returns only ACTIVE admissions for psychologists
    const activeOnlyPage = {
      content: [
        {
          id: 1,
          patient: { id: 1, firstName: 'Juan', lastName: 'Pérez García', age: 45 },
          triageCode: { id: 1, code: 'A', color: '#FF0000', description: 'Critical' },
          room: { id: 1, number: '101', type: 'PRIVATE', capacity: 1 },
          treatingPhysician: { id: 3, firstName: 'Dr. Maria', lastName: 'Garcia' },
          admissionDate: '2026-01-23T10:30:00',
          status: 'ACTIVE',
          type: 'HOSPITALIZATION'
        }
      ],
      page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
    }

    await page.route('**/api/v1/admissions*', async (route) => {
      if (route.request().method() === 'GET' && !route.request().url().includes('/admissions/')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: activeOnlyPage })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/admissions')
    await expect(page).toHaveURL(/\/admissions/)

    await expect(page.getByRole('heading', { name: /Admissions/i })).toBeVisible()
    await expect(page.getByText('Juan')).toBeVisible()

    // Only active admissions should be shown - no discharged
    await expect(page.getByText('DISCHARGED')).not.toBeVisible()
  })

  test('can view active admission details', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    await expect(page.getByText('Juan Pérez García')).toBeVisible()
    await expect(page.getByText(/ACTIVE|Activo/i)).toBeVisible()
  })

  test('cannot access discharged admission details (403)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    // Backend returns 403 for psychologist accessing discharged admission
    await page.route('**/api/v1/admissions/2', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 403,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Access denied' })
        })
      }
    })

    await page.goto('/admissions/2')

    await expect(
      page.getByText(/Access denied|Acceso denegado|Forbidden|not authorized|No autorizado/i).first()
    ).toBeVisible({ timeout: 10000 })
  })

  test('cannot see discharge button (no admission:update permission)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    await expect(page.getByText('Juan Pérez García')).toBeVisible()
    await expect(page.getByRole('button', { name: /Discharge|Alta/i })).not.toBeVisible()
  })

  test('cannot see delete button (no admission:delete permission)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    await expect(page.getByText('Juan Pérez García')).toBeVisible()
    await expect(page.getByRole('button', { name: /Delete|Eliminar/i })).not.toBeVisible()
  })

  test('status filter is not available or forced to ACTIVE', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    const activeOnlyPage = {
      content: [
        {
          id: 1,
          patient: { id: 1, firstName: 'Juan', lastName: 'Pérez García', age: 45 },
          triageCode: { id: 1, code: 'A', color: '#FF0000', description: 'Critical' },
          room: { id: 1, number: '101', type: 'PRIVATE', capacity: 1 },
          treatingPhysician: { id: 3, firstName: 'Dr. Maria', lastName: 'Garcia' },
          admissionDate: '2026-01-23T10:30:00',
          status: 'ACTIVE',
          type: 'HOSPITALIZATION'
        }
      ],
      page: { totalElements: 1, totalPages: 1, size: 20, number: 0 }
    }

    // Even if status filter sends a different value, backend overrides to ACTIVE
    await page.route('**/api/v1/admissions*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: activeOnlyPage })
        })
      }
    })

    await page.goto('/admissions')

    await expect(page.getByText('Juan')).toBeVisible()

    // All visible admissions should be ACTIVE
    const rows = page.locator('tbody tr')
    const rowCount = await rows.count()
    for (let i = 0; i < rowCount; i++) {
      await expect(rows.nth(i).getByText(/DISCHARGED/)).not.toBeVisible()
    }
  })
})

// ============================================================
// Compare: Admin sees everything, Psychologist sees subset
// ============================================================

test.describe('Psychologist vs Admin - Visibility Comparison', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('admin sees all patients including those without active admissions', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMocks(page, mockAdminUser)

    const allPatientsPage = {
      content: [mockActivePatient, mockDischargedPatient],
      page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
    }

    await page.route('**/api/v1/patients*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: allPatientsPage })
        })
      }
    })

    await page.goto('/patients')

    // Admin sees both patients
    await expect(page.getByText('Juan')).toBeVisible()
    await expect(page.getByText('María')).toBeVisible()
  })

  test('admin sees all admissions including discharged', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMocks(page, mockAdminUser)

    const allAdmissionsPage = {
      content: [
        {
          id: 1,
          patient: { id: 1, firstName: 'Juan', lastName: 'Pérez García', age: 45 },
          triageCode: { id: 1, code: 'A', color: '#FF0000', description: 'Critical' },
          room: { id: 1, number: '101', type: 'PRIVATE', capacity: 1 },
          treatingPhysician: { id: 3, firstName: 'Dr. Maria', lastName: 'Garcia' },
          admissionDate: '2026-01-23T10:30:00',
          status: 'ACTIVE',
          type: 'HOSPITALIZATION'
        },
        {
          id: 2,
          patient: { id: 2, firstName: 'María', lastName: 'López', age: 32 },
          triageCode: { id: 2, code: 'B', color: '#FFA500', description: 'Urgent' },
          room: { id: 2, number: '201', type: 'SHARED', capacity: 4 },
          treatingPhysician: { id: 3, firstName: 'Dr. Maria', lastName: 'Garcia' },
          admissionDate: '2026-01-20T08:00:00',
          status: 'DISCHARGED',
          type: 'HOSPITALIZATION'
        }
      ],
      page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
    }

    await page.route('**/api/v1/admissions*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: allAdmissionsPage })
        })
      }
    })

    await page.goto('/admissions')

    // Admin sees both active and discharged
    await expect(page.getByText('Juan')).toBeVisible()
    await expect(page.getByText('María')).toBeVisible()
  })

  test('admin can view discharged admission details', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMocks(page, mockAdminUser)

    await page.route('**/api/v1/admissions/2', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDischargedAdmission })
        })
      }
    })

    await page.goto('/admissions/2')

    // Admin can see discharged admission
    await expect(page.getByText('María López')).toBeVisible()
    await expect(page.getByText(/DISCHARGED|Dado de alta/i)).toBeVisible()
  })
})

// ============================================================
// Navigation Guard Tests
// ============================================================

test.describe('Psychologist - Navigation Guards', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can access patients route with patient:read permission', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/patients*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }
        })
      })
    })

    await page.goto('/patients')
    await expect(page).toHaveURL(/\/patients/)
  })

  test('can access admissions route with admission:read permission', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/admissions*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }
        })
      })
    })

    await page.goto('/admissions')
    await expect(page).toHaveURL(/\/admissions/)
  })

  test('cannot access admin routes (no admin permissions)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.goto('/admin/rooms')

    // Should be redirected away from admin page
    await expect(page).not.toHaveURL(/\/admin\/rooms/)
  })

  test('cannot access user management (no user permissions)', async ({ page }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.goto('/users')

    // Should be redirected away
    await expect(page).not.toHaveURL(/\/users$/)
  })
})

// ============================================================
// Read-Only UI Verification
// ============================================================

test.describe('Psychologist - Read-Only UI', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('patient detail does not show ID document section (no patient:view-id permission)', async ({
    page
  }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    const patientWithDoc = { ...mockPatientDetail, hasIdDocument: true }

    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: patientWithDoc })
        })
      }
    })

    await page.goto('/patients/1')

    await expect(page.getByRole('heading', { name: 'Juan Pérez García' })).toBeVisible()

    // Should NOT see ID document section
    await expect(
      page.getByRole('heading', { name: /ID Document|Documento de Identidad/i })
    ).not.toBeVisible()
  })

  test('admission detail does not show consent document actions (no upload-consent permission)', async ({
    page
  }) => {
    await setupAuth(page, mockPsychologistUser)
    await setupUserMocks(page, mockPsychologistUser)

    await page.route('**/api/v1/admissions/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.goto('/admissions/1')

    await expect(page.getByText('Juan Pérez García')).toBeVisible()

    // Should NOT see upload consent button
    await expect(
      page.getByRole('button', { name: /Upload Consent|Subir Consentimiento/i })
    ).not.toBeVisible()
  })
})
