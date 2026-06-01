import { test, expect, type Page } from '@playwright/test'

// E2E coverage for the "Admissions history" section on the Patient Detail view.
// All backend calls are mocked via page.route, so no live API/DB is required — the
// patient-access gate (which is enforced server-side) is simulated by the mocked
// status of GET /api/v1/patients/{id} (200 = openable, 403 = denied).

// ---------- Users (flat roles/permissions arrays, see types/user.ts) ----------

const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: ['patient:read', 'patient:view-id', 'admission:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// CHIEF_NURSE: holds patient:read AND admission:read → full history, navigable rows.
const mockNurseUser = {
  ...mockAdminUser,
  id: 2,
  username: 'chiefnurse',
  email: 'chiefnurse@example.com',
  firstName: 'Carmen',
  lastName: 'Díaz',
  roles: ['CHIEF_NURSE'],
  permissions: ['patient:read', 'admission:read']
}

// ADMINISTRATIVE_STAFF: holds patient:read but NOT admission:read → rows non-navigable.
const mockStaffUser = {
  ...mockAdminUser,
  id: 3,
  username: 'receptionist',
  email: 'receptionist@example.com',
  firstName: 'Reception',
  lastName: 'Staff',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: ['patient:read']
}

// Standalone DOCTOR: patient:read; backend denies opening an unassigned patient.
const mockDoctorUser = {
  ...mockAdminUser,
  id: 4,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Dr.',
  lastName: 'Smith',
  roles: ['DOCTOR'],
  permissions: ['patient:read', 'admission:read']
}

// PSYCHOLOGIST: patient:read + admission:read; backend gate depends on active admission.
const mockPsychologistUser = {
  ...mockAdminUser,
  id: 5,
  username: 'psychologist',
  email: 'psychologist@example.com',
  firstName: 'Ana',
  lastName: 'Martínez',
  roles: ['PSYCHOLOGIST'],
  permissions: ['patient:read', 'admission:read']
}

// ---------- Domain fixtures ----------

const mockPatient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  dateOfBirth: '1981-01-01',
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
  notes: null,
  hasIdDocument: false,
  hasActiveAdmission: true,
  emergencyContacts: [],
  createdAt: '2026-01-21T10:00:00Z',
  createdBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' },
  updatedAt: '2026-01-21T10:00:00Z',
  updatedBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' }
}

const activeAdmission = {
  id: 310,
  patient: { id: 1, firstName: 'Juan', lastName: 'Pérez García', hasActiveAdmission: true },
  triageCode: { id: 2, code: 'II', color: '#FF0000', description: 'Emergencia' },
  room: { id: 7, number: '204', type: 'PRIVATE', gender: 'MALE' },
  treatingPhysician: {
    id: 9,
    firstName: 'Juan',
    lastName: 'Pérez',
    salutation: 'DR',
    username: 'jperez'
  },
  resident: { id: 15, firstName: 'Ana', lastName: 'Ruiz', salutation: 'DR', username: 'aruiz' },
  admissionDate: '2026-05-20T14:30:00',
  dischargeDate: null,
  status: 'ACTIVE',
  type: 'HOSPITALIZATION',
  hasConsentDocument: true,
  createdAt: '2026-05-20T14:31:02'
}

const dischargedAdmission = {
  id: 188,
  patient: { id: 1, firstName: 'Juan', lastName: 'Pérez García', hasActiveAdmission: true },
  triageCode: null,
  room: null,
  treatingPhysician: {
    id: 9,
    firstName: 'Juan',
    lastName: 'Pérez',
    salutation: 'DR',
    username: 'jperez'
  },
  resident: { id: 15, firstName: 'Ana', lastName: 'Ruiz', salutation: 'DR', username: 'aruiz' },
  admissionDate: '2026-01-10T09:00:00',
  dischargeDate: '2026-01-18T11:00:00',
  status: 'DISCHARGED',
  type: 'AMBULATORY',
  hasConsentDocument: false,
  createdAt: '2026-01-10T09:02:10'
}

// ---------- Mock helpers ----------

async function setupAuth(page: Page, user: typeof mockAdminUser) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: user })
    })
  })
}

async function mockPatientDetail(page: Page, status = 200, body?: object, id = 1) {
  await page.route(`**/api/v1/patients/${id}`, async route => {
    if (route.request().method() !== 'GET') return route.continue()
    await route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(body ?? { success: true, data: mockPatient })
    })
  })
}

async function mockHistory(
  page: Page,
  options: { content: object[]; total?: number; status?: number } = { content: [] }
) {
  await page.route('**/api/v1/admissions/patients/*/admissions*', async route => {
    if (route.request().method() !== 'GET') return route.continue()
    if (options.status && options.status !== 200) {
      return route.fulfill({
        status: options.status,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'error' })
      })
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          content: options.content,
          page: {
            number: 0,
            size: 20,
            totalElements: options.total ?? options.content.length,
            totalPages: 1
          }
        }
      })
    })
  })
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => localStorage.clear())
})

test.describe('Patient Admissions History', () => {
  test('happy path — admin sees the full history most-recent-first', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await mockPatientDetail(page)
    await mockHistory(page, { content: [activeAdmission, dischargedAdmission] })

    await page.goto('/patients/1')

    const section = page.locator('.admissions-history-card')
    await expect(section.getByText(/Admissions history|Historial de ingresos/i)).toBeVisible()

    const rows = section.locator('tbody tr')
    await expect(rows).toHaveCount(2)

    // Row 0 is the active admission, formatted dd/MM/yyyy - HH:mm, with an "Active" badge.
    await expect(rows.nth(0)).toContainText('20/05/2026 - 14:30')
    await expect(rows.nth(0).getByText(/^Active$|^Activo$/)).toBeVisible()
    await expect(rows.nth(0)).toContainText('204') // room number
    await expect(rows.nth(0)).toContainText('Juan Pérez') // treating physician

    // Row 1 is the discharged admission, showing the discharge timestamp.
    await expect(rows.nth(1)).toContainText('10/01/2026 - 09:00')
    await expect(rows.nth(1)).toContainText('18/01/2026 - 11:00')
  })

  test('navigation — with admission:read a row navigates to the admission detail', async ({
    page
  }) => {
    await setupAuth(page, mockAdminUser)
    await mockPatientDetail(page)
    await mockHistory(page, { content: [activeAdmission] })
    // Keep the admission-detail view from redirecting on a failed fetch.
    await page.route('**/api/v1/admissions/310', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: activeAdmission })
      })
    })

    await page.goto('/patients/1')
    const row = page.locator('.admissions-history-card tbody tr').first()
    await expect(row).toBeVisible()
    await row.click()

    await expect(page).toHaveURL(/\/admissions\/310/)
  })

  test('navigation suppressed — without admission:read rows are non-navigable', async ({
    page
  }) => {
    await setupAuth(page, mockStaffUser)
    await mockPatientDetail(page)
    await mockHistory(page, { content: [activeAdmission] })

    await page.goto('/patients/1')
    const row = page.locator('.admissions-history-card tbody tr').first()
    await expect(row).toBeVisible()
    await row.click()

    // Still on the patient detail page — no link into a 403.
    await expect(page).toHaveURL(/\/patients\/1$/)
  })

  test('empty state — accessible patient with no admissions', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await mockPatientDetail(page)
    await mockHistory(page, { content: [] })

    await page.goto('/patients/1')

    const section = page.locator('.admissions-history-card')
    await expect(
      section.getByText(/No admissions recorded|Sin ingresos registrados/i)
    ).toBeVisible()
    await expect(section.locator('tbody tr')).toHaveCount(0)
  })

  test('patient-access allowed — a chief nurse sees the full history', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await mockPatientDetail(page)
    await mockHistory(page, { content: [activeAdmission, dischargedAdmission] })

    await page.goto('/patients/1')

    await expect(page.locator('.admissions-history-card tbody tr')).toHaveCount(2)
  })

  test('patient-access denied — unassigned doctor cannot open the patient (403)', async ({
    page
  }) => {
    await setupAuth(page, mockDoctorUser)
    // Backend gate: a standalone doctor not assigned to the patient is forbidden.
    await mockPatientDetail(page, 403, { success: false, message: 'Access denied' })
    await mockHistory(page, { content: [activeAdmission] })

    await page.goto('/patients/1')

    // The detail page itself is unreachable → redirected to the patient list,
    // so the history section never renders.
    await expect(page).toHaveURL(/\/patients$/)
    await expect(page.locator('.admissions-history-card')).toHaveCount(0)
  })

  test('patient-access — psychologist gate follows the active-admission rule', async ({ page }) => {
    // No active admission → backend denies opening the patient.
    await setupAuth(page, mockPsychologistUser)
    await mockPatientDetail(page, 403, { success: false, message: 'Access denied' })
    await mockHistory(page, { content: [dischargedAdmission] })

    await page.goto('/patients/1')
    await expect(page).toHaveURL(/\/patients$/)

    // With an active admission the patient opens and the FULL history (incl. the
    // prior DISCHARGED admission) is shown.
    await page.unroute('**/api/v1/patients/1')
    await mockPatientDetail(page)
    await page.unroute('**/api/v1/admissions/patients/*/admissions*')
    await mockHistory(page, { content: [activeAdmission, dischargedAdmission] })

    await page.goto('/patients/1')
    const rows = page.locator('.admissions-history-card tbody tr')
    await expect(rows).toHaveCount(2)
    await expect(rows.nth(1).getByText(/AMBULATORY|Ambulatorio/i)).toBeVisible()
  })

  test('error handling — a failed history fetch leaves the section recoverable', async ({
    page
  }) => {
    await setupAuth(page, mockAdminUser)
    await mockPatientDetail(page)
    await mockHistory(page, { content: [], status: 500 })

    await page.goto('/patients/1')

    // The patient page still renders; the section shows no rows (state cleared on failure).
    await expect(page.getByRole('heading', { name: 'Juan Pérez García' })).toBeVisible()
    await expect(page.locator('.admissions-history-card tbody tr')).toHaveCount(0)
  })

  test('not-found patient — surfaces the not-found flow (redirect to list)', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await mockPatientDetail(page, 404, { success: false, message: 'Patient not found' }, 99999)
    await mockHistory(page, { content: [] })

    await page.goto('/patients/99999')

    await expect(page).toHaveURL(/\/patients$/)
  })
})
