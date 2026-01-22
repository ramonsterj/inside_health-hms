import { test, expect } from '@playwright/test'

// Buffer is available globally in Node.js runtime (Playwright tests run in Node)
declare const Buffer: {
  from(data: number[]): ArrayBuffer
}

// Mock user data for different roles
// Note: Frontend expects flat arrays for roles and permissions (see types/user.ts)
const mockAdminStaffUser = {
  id: 1,
  username: 'receptionist',
  email: 'receptionist@example.com',
  firstName: 'Reception',
  lastName: 'Staff',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: ['patient:create', 'patient:read', 'patient:update', 'patient:upload-id', 'patient:view-id'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockDoctorUser = {
  id: 2,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Dr.',
  lastName: 'Smith',
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
  emergencyContacts: [
    { id: 1, name: 'María de Pérez', relationship: 'Esposa', phone: '+502 5555-1234' }
  ],
  createdAt: '2026-01-21T10:00:00Z',
  createdBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' },
  updatedAt: '2026-01-21T10:00:00Z',
  updatedBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' }
}

const mockPatientsPage = {
  content: [
    {
      id: 1,
      firstName: 'Juan',
      lastName: 'Pérez García',
      age: 45,
      idDocumentNumber: '1234567890101',
      hasIdDocument: false
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
async function setupAuth(page: import('@playwright/test').Page, user: typeof mockAdminStaffUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      // Store user in a way that can be used by the app
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

// Helper function to setup API mocks for administrative staff
async function setupAdminStaffMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockAdminStaffUser })
    })
  })
}

// Helper function to setup API mocks for doctor
async function setupDoctorMocks(page: import('@playwright/test').Page) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockDoctorUser })
    })
  })
}

// Helper function to fill the patient registration form
async function fillPatientForm(
  page: import('@playwright/test').Page,
  patientData: {
    firstName: string
    lastName: string
    age: number
    gender: string
    religion: string
    occupation: string
    address: string
    email: string
    emergencyContact?: { name: string; relationship: string; phone: string }
  }
) {
  await page.getByLabel(/First Name|Nombres/i).fill(patientData.firstName)
  await page.getByLabel(/Last Name|Apellidos/i).fill(patientData.lastName)

  // Fill age using InputNumber - need to clear first and type
  const ageInput = page.locator('#age input')
  await ageInput.clear()
  await ageInput.fill(patientData.age.toString())

  await page.getByLabel(/Gender|Género/i).fill(patientData.gender)
  await page.getByLabel(/Religion|Religión/i).fill(patientData.religion)
  await page.getByLabel(/Occupation|Ocupación/i).fill(patientData.occupation)
  await page.getByLabel(/Address|Dirección/i).fill(patientData.address)
  await page.getByLabel(/Email|Correo/i).fill(patientData.email)

  // Fill emergency contact if provided
  if (patientData.emergencyContact) {
    const contactInputs = page.locator('.contact-row input')
    await contactInputs.nth(0).fill(patientData.emergencyContact.name)
    await contactInputs.nth(1).fill(patientData.emergencyContact.relationship)
    await contactInputs.nth(2).fill(patientData.emergencyContact.phone)
  }
}

test.describe('Patients - Administrative Staff', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view patient list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/patients*', async (route) => {
      if (route.request().method() === 'GET' && !route.request().url().includes('/patients/')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatientsPage })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/patients')
    await expect(page).toHaveURL(/\/patients/)

    // Should see the patient list
    await expect(page.getByRole('heading', { name: /Patients/i })).toBeVisible()
    await expect(page.getByText('Juan')).toBeVisible()
    await expect(page.getByText('Pérez García')).toBeVisible()
  })

  test('can see New Patient button', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/patients*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatientsPage })
      })
    })

    await page.goto('/patients')

    // Should see the New Patient button
    await expect(page.getByRole('button', { name: /New Patient|Nuevo Paciente/i })).toBeVisible()
  })

  test('can navigate to create patient form', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/patients*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatientsPage })
      })
    })

    await page.goto('/patients')

    await page.getByRole('button', { name: /New Patient|Nuevo Paciente/i }).click()

    await expect(page).toHaveURL(/\/patients\/new/)
  })

  test('patient form displays all required fields', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.goto('/patients/new')

    // Check that all required form fields are present
    // Note: Using getByText for labels because PrimeVue components don't always
    // associate labels properly with getByLabel (especially InputNumber, Select)
    await expect(page.getByText(/First Name|Nombres/i).first()).toBeVisible()
    await expect(page.getByText(/Last Name|Apellidos/i).first()).toBeVisible()
    await expect(page.locator('label[for="age"]')).toBeVisible() // InputNumber label
    await expect(page.locator('#age')).toBeVisible() // InputNumber component
    await expect(page.getByText(/^Sex|Sexo$/i).first()).toBeVisible()
    await expect(page.getByText(/^Gender|Género$/i).first()).toBeVisible()
    await expect(page.getByText(/Marital Status|Estado Civil/i).first()).toBeVisible()
    await expect(page.getByText(/^Religion|Religión$/i).first()).toBeVisible()
    await expect(page.getByText(/Education Level|Escolaridad/i).first()).toBeVisible()
    await expect(page.getByText(/^Occupation|Ocupación$/i).first()).toBeVisible()
    await expect(page.locator('label[for="address"]')).toBeVisible()
    await expect(page.locator('label[for="email"]')).toBeVisible()

    // Emergency contacts section
    await expect(page.getByText(/Emergency Contacts|Contactos de Emergencia/i)).toBeVisible()
  })

  test('shows validation errors for empty form submission', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.goto('/patients/new')

    // Clear any default values and try to submit
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show validation errors (at least one should appear)
    await expect(page.locator('.p-error').first()).toBeVisible()
  })

  test('can view patient details', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatient })
        })
      }
    })

    await page.goto('/patients/1')

    // Should see patient details - use heading for full name to avoid strict mode issues
    await expect(page.getByRole('heading', { name: 'Juan Pérez García' })).toBeVisible()
    await expect(page.getByText('45', { exact: true }).first()).toBeVisible()
    await expect(page.getByText('Ingeniero', { exact: true })).toBeVisible()

    // Should see emergency contacts
    await expect(page.getByText('María de Pérez')).toBeVisible()
    await expect(page.getByText('Esposa', { exact: true })).toBeVisible()
  })

  test('can see edit button on patient details', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockPatient })
        })
      }
    })

    await page.goto('/patients/1')

    // Should see edit button
    await expect(page.getByRole('button', { name: /Edit|Editar/i })).toBeVisible()
  })

  test('search filters patient list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    let searchQuery = ''
    await page.route('**/api/v1/patients*', async (route) => {
      const url = new URL(route.request().url())
      searchQuery = url.searchParams.get('search') || ''

      const response =
        searchQuery === 'NonExistent'
          ? { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }
          : mockPatientsPage

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: response })
      })
    })

    await page.goto('/patients')

    // Type in search field
    const searchInput = page.getByPlaceholder(/Search patients|Buscar pacientes/i)
    await searchInput.fill('NonExistent')

    // Wait for debounced search
    await page.waitForTimeout(500)

    // Should show empty state
    await expect(page.getByText(/No patients found|No se encontraron pacientes/i)).toBeVisible()
  })

  test('can successfully create a new patient', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const newPatientData = {
      firstName: 'María',
      lastName: 'López Hernández',
      age: 32,
      gender: 'Femenino',
      religion: 'Evangélica',
      occupation: 'Maestra',
      address: '2a Avenida 3-45 Zona 2, Guatemala',
      email: 'maria.lopez@email.com',
      emergencyContact: { name: 'Pedro López', relationship: 'Hermano', phone: '+502 4444-5678' }
    }

    const createdPatient = {
      id: 2,
      ...newPatientData,
      sex: 'FEMALE',
      maritalStatus: 'SINGLE',
      educationLevel: 'UNIVERSITY',
      hasIdDocument: false,
      emergencyContacts: [
        { id: 1, name: 'Pedro López', relationship: 'Hermano', phone: '+502 4444-5678' }
      ],
      createdAt: '2026-01-22T10:00:00Z',
      createdBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' },
      updatedAt: '2026-01-22T10:00:00Z',
      updatedBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' }
    }

    // Mock POST to create patient
    await page.route('**/api/v1/patients', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: createdPatient })
        })
      }
    })

    // Mock GET for patient list after redirect
    const updatedPatientsPage = {
      content: [
        ...mockPatientsPage.content,
        {
          id: 2,
          firstName: 'María',
          lastName: 'López Hernández',
          age: 32,
          idDocumentNumber: null,
          hasIdDocument: false
        }
      ],
      page: {
        totalElements: 2,
        totalPages: 1,
        size: 20,
        number: 0
      }
    }

    await page.route('**/api/v1/patients?*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: updatedPatientsPage })
        })
      }
    })

    await page.goto('/patients/new')

    // Fill the form
    await fillPatientForm(page, newPatientData)

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to the patient list
    await expect(page).toHaveURL(/\/patients$/, { timeout: 10000 })

    // Should see the created patient in the list
    await expect(page.getByText('María')).toBeVisible()
    await expect(page.getByText('López Hernández')).toBeVisible()
  })

  test('can create patient with multiple emergency contacts', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const createdPatient = {
      id: 3,
      firstName: 'Carlos',
      lastName: 'Ramírez',
      age: 55,
      sex: 'MALE',
      gender: 'Masculino',
      maritalStatus: 'MARRIED',
      religion: 'Católica',
      educationLevel: 'SECONDARY',
      occupation: 'Comerciante',
      address: '5a Calle 10-20 Zona 5, Guatemala',
      email: 'carlos.ramirez@email.com',
      hasIdDocument: false,
      emergencyContacts: [
        { id: 1, name: 'Ana Ramírez', relationship: 'Esposa', phone: '+502 3333-1111' },
        { id: 2, name: 'Luis Ramírez', relationship: 'Hijo', phone: '+502 3333-2222' }
      ],
      createdAt: '2026-01-22T11:00:00Z',
      createdBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' },
      updatedAt: '2026-01-22T11:00:00Z',
      updatedBy: { id: 1, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' }
    }

    // Mock POST to create patient
    await page.route('**/api/v1/patients', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: createdPatient })
        })
      }
    })

    // Mock GET for patient list after redirect
    const updatedPatientsPage = {
      content: [
        ...mockPatientsPage.content,
        {
          id: 3,
          firstName: 'Carlos',
          lastName: 'Ramírez',
          age: 55,
          idDocumentNumber: null,
          hasIdDocument: false
        }
      ],
      page: {
        totalElements: 2,
        totalPages: 1,
        size: 20,
        number: 0
      }
    }

    await page.route('**/api/v1/patients?*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: updatedPatientsPage })
        })
      }
    })

    await page.goto('/patients/new')

    // Fill basic patient info
    await page.getByLabel(/First Name|Nombres/i).fill('Carlos')
    await page.getByLabel(/Last Name|Apellidos/i).fill('Ramírez')
    const ageInput = page.locator('#age input')
    await ageInput.clear()
    await ageInput.fill('55')
    await page.getByLabel(/Gender|Género/i).fill('Masculino')
    await page.getByLabel(/Religion|Religión/i).fill('Católica')
    await page.getByLabel(/Occupation|Ocupación/i).fill('Comerciante')
    await page.getByLabel(/Address|Dirección/i).fill('5a Calle 10-20 Zona 5, Guatemala')
    await page.getByLabel(/Email|Correo/i).fill('carlos.ramirez@email.com')

    // Fill first emergency contact
    const contactInputs = page.locator('.contact-row input')
    await contactInputs.nth(0).fill('Ana Ramírez')
    await contactInputs.nth(1).fill('Esposa')
    await contactInputs.nth(2).fill('+502 3333-1111')

    // Add second emergency contact
    await page.getByRole('button', { name: /Add Contact|Agregar Contacto/i }).click()

    // Fill second emergency contact (new row should appear)
    const allContactInputs = page.locator('.contact-row input')
    await allContactInputs.nth(3).fill('Luis Ramírez')
    await allContactInputs.nth(4).fill('Hijo')
    await allContactInputs.nth(5).fill('+502 3333-2222')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should redirect to the patient list
    await expect(page).toHaveURL(/\/patients$/, { timeout: 10000 })

    // Should see the created patient in the list (name appears as "Carlos Ramírez" in the Full Name column)
    await expect(page.getByRole('cell', { name: 'Carlos Ramírez' })).toBeVisible()
  })
})

test.describe('Patients - Doctor (Clinical Staff)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can view patient list', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupDoctorMocks(page)

    await page.route('**/api/v1/patients*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatientsPage })
      })
    })

    await page.goto('/patients')

    // Should see the patient list
    await expect(page.getByText('Juan')).toBeVisible()
  })

  test('cannot see New Patient button', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupDoctorMocks(page)

    await page.route('**/api/v1/patients*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatientsPage })
      })
    })

    await page.goto('/patients')

    // Should NOT see the New Patient button
    await expect(page.getByRole('button', { name: /New Patient|Nuevo Paciente/i })).not.toBeVisible()
  })

  test('can view patient details but not edit button', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupDoctorMocks(page)

    await page.route('**/api/v1/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatient })
      })
    })

    await page.goto('/patients/1')

    // Should see patient details - use heading for full name to avoid strict mode issues
    await expect(page.getByRole('heading', { name: 'Juan Pérez García' })).toBeVisible()

    // Should NOT see edit button
    await expect(page.getByRole('button', { name: /Edit|Editar/i })).not.toBeVisible()
  })

  test('cannot access patient create page', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupDoctorMocks(page)

    await page.goto('/patients/new')

    // Should be redirected to dashboard or patients list
    await expect(page).not.toHaveURL(/\/patients\/new/)
  })

  test('cannot see ID document section', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupDoctorMocks(page)

    const patientWithDoc = { ...mockPatient, hasIdDocument: true }
    await page.route('**/api/v1/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: patientWithDoc })
      })
    })

    await page.goto('/patients/1')

    // Should NOT see ID document section (doctor doesn't have patient:view-id permission)
    await expect(
      page.getByRole('heading', { name: /ID Document|Documento de Identidad/i })
    ).not.toBeVisible()
  })
})

test.describe('Patients - Duplicate Detection', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('shows duplicate warning dialog on conflict', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/patients', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            message: 'Potential duplicate patient found',
            data: {
              potentialDuplicates: [
                {
                  id: 99,
                  firstName: 'Juan',
                  lastName: 'Pérez García',
                  age: 45,
                  idDocumentNumber: '1234567890101'
                }
              ]
            }
          })
        })
      }
    })

    await page.goto('/patients/new')

    // Fill in the form with valid data
    await page.getByLabel(/First Name|Nombres/i).fill('Juan')
    await page.getByLabel(/Last Name|Apellidos/i).fill('Pérez García')

    // Fill age using InputNumber - need to clear first and type
    const ageInput = page.locator('#age input')
    await ageInput.clear()
    await ageInput.fill('45')

    await page.getByLabel(/Gender|Género/i).fill('Masculino')
    await page.getByLabel(/Religion|Religión/i).fill('Católica')
    await page.getByLabel(/Occupation|Ocupación/i).fill('Ingeniero')
    await page.getByLabel(/Address|Dirección/i).fill('4a Calle 5-67 Zona 1, Guatemala')
    await page.getByLabel(/Email|Correo/i).fill('juan.perez@email.com')

    // Fill emergency contact
    const contactNameInput = page.locator('.contact-row input').first()
    await contactNameInput.fill('María de Pérez')

    // Get the relationship and phone inputs
    const contactInputs = page.locator('.contact-row input')
    await contactInputs.nth(1).fill('Esposa')
    await contactInputs.nth(2).fill('+502 5555-1234')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should show duplicate dialog
    await expect(
      page.getByText(/Potential Duplicate|Posible Duplicado/i)
    ).toBeVisible({ timeout: 10000 })
  })
})

test.describe('Patients - Audit Trail Display', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('patient detail shows audit information', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    await page.route('**/api/v1/patients/1', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockPatient })
      })
    })

    await page.goto('/patients/1')

    // Should see audit information section
    await expect(
      page.getByText(/Audit Information|Información de Auditoría/i)
    ).toBeVisible()

    // Should see registered by info
    await expect(page.getByText(/Registered by|Registrado por/i)).toBeVisible()
    // Reception Staff appears twice (created by and updated by), use first()
    await expect(page.getByText('Reception Staff').first()).toBeVisible()

    // Should see last modified by info
    await expect(page.getByText(/Last modified by|Última modificación por/i)).toBeVisible()
  })
})

test.describe('Patients - ID Document Upload', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('can upload patient ID document from patient list', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const patientsPageWithoutDoc = {
      content: [
        {
          id: 1,
          firstName: 'Juan',
          lastName: 'Pérez García',
          age: 45,
          idDocumentNumber: '1234567890101',
          hasIdDocument: false
        }
      ],
      page: {
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0
      }
    }

    // Mock GET for patient list
    await page.route('**/api/v1/patients?*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: patientsPageWithoutDoc })
        })
      }
    })

    // Mock POST for document upload
    await page.route('**/api/v1/patients/1/id-document', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...mockPatient, hasIdDocument: true }
          })
        })
      }
    })

    await page.goto('/patients')

    // Should see upload icon for patient without ID document (pi-id-card icon)
    const uploadIcon = page.locator('button[class*="p-button"]').filter({ has: page.locator('.pi-id-card') })
    await expect(uploadIcon).toBeVisible()

    // Click the upload icon to open dialog
    await uploadIcon.click()

    // Should see upload dialog with patient name (use dialog locator to be specific)
    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('Juan Pérez García')).toBeVisible()

    // Should see the drag & drop area
    await expect(
      dialog.getByText(/Drag and drop|Arrastra y suelta/i)
    ).toBeVisible()
  })

  test('upload icon disappears after ID document is uploaded', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    let hasIdDocument = false

    // Mock GET for patient list - returns updated data after upload
    await page.route('**/api/v1/patients?*', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: [
                {
                  id: 1,
                  firstName: 'Juan',
                  lastName: 'Pérez García',
                  age: 45,
                  idDocumentNumber: '1234567890101',
                  hasIdDocument: hasIdDocument
                }
              ],
              page: {
                totalElements: 1,
                totalPages: 1,
                size: 20,
                number: 0
              }
            }
          })
        })
      }
    })

    // Mock POST for document upload - sets hasIdDocument to true
    await page.route('**/api/v1/patients/1/id-document', async (route) => {
      if (route.request().method() === 'POST') {
        hasIdDocument = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...mockPatient, hasIdDocument: true }
          })
        })
      }
    })

    await page.goto('/patients')

    // Should see upload icon initially
    const uploadIcon = page.locator('button[class*="p-button"]').filter({ has: page.locator('.pi-id-card') })
    await expect(uploadIcon).toBeVisible()

    // Click the upload icon to open dialog
    await uploadIcon.click()

    // Wait for dialog to be visible
    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('Juan Pérez García')).toBeVisible()

    // Find the file input and upload a file
    const fileInput = dialog.locator('input[type="file"]')
    await fileInput.setInputFiles({
      name: 'test-id.jpg',
      mimeType: 'image/jpeg',
      buffer: Buffer.from([0xff, 0xd8, 0xff, 0xe0])
    })

    // Wait for success message
    await expect(
      page.getByText(/uploaded successfully|subido exitosamente/i)
    ).toBeVisible({ timeout: 10000 })

    // Upload icon should no longer be visible (dialog closes and list refreshes)
    await expect(uploadIcon).not.toBeVisible({ timeout: 5000 })
  })

  test('can upload ID document from edit patient view', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const patientWithoutDoc = { ...mockPatient, hasIdDocument: false }

    // Mock GET for patient detail
    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: patientWithoutDoc })
        })
      }
    })

    // Mock POST for document upload
    await page.route('**/api/v1/patients/1/id-document', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...mockPatient, hasIdDocument: true }
          })
        })
      }
    })

    await page.goto('/patients/1/edit')

    // Should see ID Document section in edit mode (card title)
    await expect(page.locator('.p-card-title').filter({ hasText: /^ID Document$|^Documento de Identidad$/ })).toBeVisible()

    // Should see message that no document is uploaded
    await expect(
      page.getByText('No ID document uploaded')
    ).toBeVisible()

    // Should see file upload component with upload button
    await expect(page.getByRole('button', { name: /Upload ID Document|Subir Documento de Identidad/i })).toBeVisible()
  })

  test('can view uploaded ID document on patient detail', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const patientWithDoc = { ...mockPatient, hasIdDocument: true }

    // Mock GET for patient detail
    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: patientWithDoc })
        })
      }
    })

    // Mock GET for ID document image
    await page.route('**/api/v1/patients/1/id-document', async (route) => {
      if (route.request().method() === 'GET') {
        const pngBytes = new Uint8Array([
          0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        ])
        await route.fulfill({
          status: 200,
          contentType: 'image/png',
          body: pngBytes
        })
      }
    })

    await page.goto('/patients/1')

    // Should see ID document section (user has patient:view-id permission)
    // PrimeVue Card title is not a heading element, so we look for the card title
    await expect(page.locator('.p-card-title').filter({ hasText: /^ID Document$|^Documento de Identidad$/ })).toBeVisible()

    // Should see the view button
    const viewButton = page.getByRole('button', { name: /View ID Document|Ver Documento de Identidad/i })
    await expect(viewButton).toBeVisible()
  })

  test('shows placeholder when no ID document uploaded on patient detail', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupAdminStaffMocks(page)

    const patientWithoutDoc = { ...mockPatient, hasIdDocument: false }

    await page.route('**/api/v1/patients/1', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: patientWithoutDoc })
        })
      }
    })

    await page.goto('/patients/1')

    // Should see message that no document is uploaded
    await expect(
      page.getByText(/No ID document|Sin documento|No document uploaded|Ningún documento/i)
    ).toBeVisible()
  })
})
