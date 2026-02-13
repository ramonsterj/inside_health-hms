import { test, expect } from '@playwright/test'
import {
  confirmDialogAccept,
  confirmDialogCancel,
  waitForMedicalRecordTabs,
  selectMedicalRecordTab,
  expandAccordionPanel
} from './utils/test-helpers'

// --- Mock Users ---

const mockNurseUser = {
  id: 3,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Ana',
  lastName: 'Lopez',
  salutation: 'Lic.',
  roles: ['NURSE'],
  permissions: [
    'admission:read',
    'medical-order:read',
    'medication-administration:create',
    'medication-administration:read'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockDoctorUser = {
  id: 2,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Maria',
  lastName: 'Garcia',
  salutation: 'Dra.',
  roles: ['DOCTOR'],
  permissions: [
    'admission:read',
    'medical-order:read',
    'medication-administration:read'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// --- Mock Patient & Admission ---

const mockPatient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'P\u00e9rez Garc\u00eda',
  age: 45,
  idDocumentNumber: '1234567890101'
}

const mockAdmission = {
  id: 1,
  patient: mockPatient,
  triageCode: { id: 1, code: 'A', color: '#FF0000', description: 'Critical', displayOrder: 1 },
  room: { id: 1, number: '101', type: 'PRIVATE', capacity: 1, availableBeds: 0 },
  treatingPhysician: { id: 2, firstName: 'Maria', lastName: 'Garcia', salutation: 'Dra.' },
  admissionDate: '2026-01-23T10:30:00',
  dischargeDate: null,
  status: 'ACTIVE',
  type: 'HOSPITALIZATION',
  hasConsentDocument: false,
  consultingPhysicians: [],
  createdAt: '2026-01-23T10:35:00',
  createdBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' },
  updatedAt: '2026-01-23T10:35:00',
  updatedBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' }
}

// --- Mock Medical Orders ---

const mockMedicationOrderWithInventory = {
  id: 1,
  admissionId: 1,
  category: 'MEDICAMENTOS',
  startDate: '2026-01-24',
  endDate: null,
  medication: 'Sertraline',
  dosage: '50mg',
  route: 'ORAL',
  frequency: 'Once daily',
  schedule: 'Morning with breakfast',
  observations: null,
  status: 'ACTIVE',
  inventoryItemId: 10,
  discontinuedAt: null,
  discontinuedBy: null,
  createdAt: '2026-01-24T10:00:00',
  createdBy: { id: 2, salutation: 'Dra.', firstName: 'Maria', lastName: 'Garcia', roles: ['DOCTOR'] },
  updatedAt: '2026-01-24T10:00:00',
  updatedBy: null
}

const mockMedicationOrderWithoutInventory = {
  id: 2,
  admissionId: 1,
  category: 'MEDICAMENTOS',
  startDate: '2026-01-24',
  endDate: null,
  medication: 'Ibuprofen',
  dosage: '400mg',
  route: 'ORAL',
  frequency: 'Every 8 hours',
  schedule: 'With meals',
  observations: null,
  status: 'ACTIVE',
  inventoryItemId: null,
  discontinuedAt: null,
  discontinuedBy: null,
  createdAt: '2026-01-24T11:00:00',
  createdBy: { id: 2, salutation: 'Dra.', firstName: 'Maria', lastName: 'Garcia', roles: ['DOCTOR'] },
  updatedAt: '2026-01-24T11:00:00',
  updatedBy: null
}

const mockDiscontinuedOrder = {
  id: 3,
  admissionId: 1,
  category: 'MEDICAMENTOS',
  startDate: '2026-01-23',
  endDate: '2026-01-25',
  medication: 'Lorazepam',
  dosage: '1mg',
  route: 'ORAL',
  frequency: 'PRN',
  schedule: 'For acute anxiety',
  observations: null,
  status: 'DISCONTINUED',
  inventoryItemId: 11,
  discontinuedAt: '2026-01-25T14:00:00',
  discontinuedBy: { id: 2, salutation: 'Dra.', firstName: 'Maria', lastName: 'Garcia', roles: ['DOCTOR'] },
  createdAt: '2026-01-23T10:00:00',
  createdBy: { id: 2, salutation: 'Dra.', firstName: 'Maria', lastName: 'Garcia', roles: ['DOCTOR'] },
  updatedAt: '2026-01-25T14:00:00',
  updatedBy: null
}

// --- Mock Administration Records ---

const mockAdministrations = {
  content: [
    {
      id: 1,
      orderId: 1,
      status: 'GIVEN',
      notes: 'Patient took medication without issues',
      administeredAt: '2026-01-25T08:00:00',
      administeredByName: 'Ana Lopez',
      billable: true
    },
    {
      id: 2,
      orderId: 1,
      status: 'REFUSED',
      notes: 'Patient declined',
      administeredAt: '2026-01-25T20:00:00',
      administeredByName: 'Ana Lopez',
      billable: false
    }
  ],
  page: {
    totalElements: 2,
    totalPages: 1,
    size: 20,
    number: 0
  }
}

const mockEmptyAdministrations = {
  content: [],
  page: {
    totalElements: 0,
    totalPages: 0,
    size: 20,
    number: 0
  }
}

// --- Helper Functions ---

async function setupAuth(page: import('@playwright/test').Page, user: typeof mockNurseUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

async function setupUserMock(page: import('@playwright/test').Page, user: typeof mockNurseUser) {
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

async function setupAdmissionMock(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/admissions/1', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockAdmission })
      })
    }
  })

  await page.route('**/api/v1/admissions/1/documents', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [] })
    })
  })

  await page.route('**/api/v1/admissions/1/clinical-history', async (route) => {
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'Not found' })
    })
  })

  await page.route('**/api/v1/admissions/1/progress-notes*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { content: [], page: { totalElements: 0, totalPages: 0, size: 10, number: 0 } }
      })
    })
  })
}

function createGroupedOrders(orders: typeof mockMedicationOrderWithInventory[]) {
  const grouped: Record<string, typeof mockMedicationOrderWithInventory[]> = {
    ORDENES_MEDICAS: [],
    MEDICAMENTOS: [],
    LABORATORIOS: [],
    REFERENCIAS_MEDICAS: [],
    PRUEBAS_PSICOMETRICAS: [],
    ACTIVIDAD_FISICA: [],
    CUIDADOS_ESPECIALES: [],
    DIETA: [],
    RESTRICCIONES_MOVILIDAD: [],
    PERMISOS_VISITA: [],
    OTRAS: []
  }

  for (const order of orders) {
    if (grouped[order.category]) {
      grouped[order.category].push(order)
    }
  }

  return { orders: grouped }
}

// --- Test Suites ---

test.describe('Medication Administration - Nurse (Create Permission)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('shows Administer button for active medication order with inventory item', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await expect(orderCard).toBeVisible()
    await expect(orderCard.getByRole('button', { name: /Administer|Administrar/i })).toBeVisible()
  })

  test('hides Administer button for order without inventory item', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithoutInventory])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Ibuprofen' })
    await expect(orderCard).toBeVisible()
    await expect(
      orderCard.getByRole('button', { name: /Administer|Administrar/i })
    ).not.toBeVisible()
  })

  test('hides Administer button for discontinued order', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockDiscontinuedOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Lorazepam' })
    await expect(orderCard).toBeVisible()
    await expect(
      orderCard.getByRole('button', { name: /Administer|Administrar/i })
    ).not.toBeVisible()
  })

  test('opens administration dialog showing medication name', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()
    await expect(dialog.locator('.medication-info strong')).toHaveText('Sertraline')
  })

  test('creates GIVEN administration with confirmation dialog', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 3,
              orderId: 1,
              status: 'GIVEN',
              notes: '',
              administeredAt: '2026-01-26T08:00:00',
              administeredByName: 'Ana Lopez',
              billable: true
            }
          })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    // Open administer dialog
    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()

    // Status defaults to GIVEN - click Save
    await dialog.getByRole('button', { name: /Save|Guardar/i }).click()

    // Confirmation dialog appears for GIVEN status
    await confirmDialogAccept(page)

    // Dialog should close after successful submission
    await expect(dialog).not.toBeVisible({ timeout: 10000 })
  })

  test('creates MISSED administration (no confirmation dialog)', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 4,
              orderId: 1,
              status: 'MISSED',
              notes: '',
              administeredAt: '2026-01-26T08:00:00',
              administeredByName: 'Ana Lopez',
              billable: false
            }
          })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()

    // Change status to MISSED
    await dialog.locator('#status').click()
    await page.locator('.p-select-overlay').getByText('Missed').click()

    // Click Save - no confirmation dialog for MISSED
    await dialog.getByRole('button', { name: /Save|Guardar/i }).click()

    // Dialog should close directly (no confirm dialog)
    await expect(dialog).not.toBeVisible({ timeout: 10000 })
  })

  test('creates REFUSED administration', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 5,
              orderId: 1,
              status: 'REFUSED',
              notes: '',
              administeredAt: '2026-01-26T08:00:00',
              administeredByName: 'Ana Lopez',
              billable: false
            }
          })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()

    // Change status to REFUSED
    await dialog.locator('#status').click()
    await page.locator('.p-select-overlay').getByText('Refused').click()

    // Click Save
    await dialog.getByRole('button', { name: /Save|Guardar/i }).click()

    // Dialog should close directly
    await expect(dialog).not.toBeVisible({ timeout: 10000 })
  })

  test('creates HELD administration', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 6,
              orderId: 1,
              status: 'HELD',
              notes: '',
              administeredAt: '2026-01-26T08:00:00',
              administeredByName: 'Ana Lopez',
              billable: false
            }
          })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()

    // Change status to HELD
    await dialog.locator('#status').click()
    await page.locator('.p-select-overlay').getByText('Held').click()

    // Click Save
    await dialog.getByRole('button', { name: /Save|Guardar/i }).click()

    // Dialog should close directly
    await expect(dialog).not.toBeVisible({ timeout: 10000 })
  })

  test('cancels GIVEN confirmation dialog (dialog stays open)', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

    const dialog = page.locator('[role="dialog"]')
    await expect(dialog).toBeVisible()

    // Status is GIVEN by default - click Save
    await dialog.getByRole('button', { name: /Save|Guardar/i }).click()

    // Confirmation dialog appears - cancel it
    await confirmDialogCancel(page)

    // The administration dialog should still be visible
    await expect(dialog).toBeVisible()
  })

  test('shows administration history table with status badges and billable column', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockAdministrations })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await expect(orderCard).toBeVisible()

    // Click History button
    await orderCard.getByRole('button', { name: /History|Historial/i }).click()

    // Verify DataTable appears in administration history
    const historySection = page.locator('.administration-history')
    await expect(historySection).toBeVisible()
    await expect(historySection.locator('.p-datatable')).toBeVisible()

    // Verify data is displayed
    await expect(historySection.getByText('Ana Lopez').first()).toBeVisible()
    await expect(historySection.getByText('Given')).toBeVisible()
    await expect(historySection.getByText('Refused')).toBeVisible()
  })

  test('shows empty state in history when no administrations', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockEmptyAdministrations })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /History|Historial/i }).click()

    const historySection = page.locator('.administration-history')
    await expect(historySection).toBeVisible()

    // Verify empty state text
    await expect(historySection.getByText(/no.*administration|no.*registros/i)).toBeVisible()
  })
})

test.describe('Medication Administration - Doctor (Read Only)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('hides Administer button (no create permission)', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await expect(orderCard).toBeVisible()

    // Doctor lacks medication-administration:create, so Administer button should be hidden
    await expect(
      orderCard.getByRole('button', { name: /Administer|Administrar/i })
    ).not.toBeVisible()
  })

  test('shows History button and can view history', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockAdministrations })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await expect(orderCard).toBeVisible()

    // History button is visible (controlled by isMedicationCategory && order.inventoryItemId, no permission check)
    const historyButton = orderCard.getByRole('button', { name: /History|Historial/i })
    await expect(historyButton).toBeVisible()

    // Click History and verify data appears
    await historyButton.click()

    const historySection = page.locator('.administration-history')
    await expect(historySection).toBeVisible()
    await expect(historySection.getByText('Ana Lopez').first()).toBeVisible()
  })

  test('history displays correct columns', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrderWithInventory])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockAdministrations })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Sertraline' })
    await orderCard.getByRole('button', { name: /History|Historial/i }).click()

    const historySection = page.locator('.administration-history')
    await expect(historySection).toBeVisible()

    // Verify column headers
    const table = historySection.locator('.p-datatable')
    await expect(table.getByText(/Administered At/i)).toBeVisible()
    await expect(table.getByText(/Status/i)).toBeVisible()
    await expect(table.getByText(/Notes/i)).toBeVisible()
    await expect(table.getByText(/Administered By/i)).toBeVisible()
    await expect(table.getByText(/Billable/i)).toBeVisible()
  })
})
