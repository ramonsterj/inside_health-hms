import { test, expect } from '@playwright/test'
import {
  waitForOverlaysToClear,
  waitForMedicalRecordTabs,
  selectMedicalRecordTab,
  expandAccordionPanel
} from './utils/test-helpers'

// Mock users with different permission levels
const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'admission:read',
    'clinical-history:create',
    'clinical-history:read',
    'clinical-history:update',
    'progress-note:create',
    'progress-note:read',
    'progress-note:update',
    'medical-order:create',
    'medical-order:read',
    'medical-order:update',
    'medical-order:discontinue'
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
    'clinical-history:create',
    'clinical-history:read',
    'progress-note:create',
    'progress-note:read',
    'medical-order:create',
    'medical-order:read',
    'medical-order:discontinue'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

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
    'clinical-history:read',
    'progress-note:create',
    'progress-note:read',
    'medical-order:read'
  ],
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

const mockMedicationOrder = {
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
  observations: 'Monitor for side effects',
  status: 'AUTORIZADO',
  discontinuedAt: null,
  discontinuedBy: null,
  createdAt: '2026-01-24T10:00:00',
  createdBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  },
  updatedAt: '2026-01-24T10:00:00',
  updatedBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  }
}

const mockDietOrder = {
  id: 2,
  admissionId: 1,
  category: 'DIETA',
  startDate: '2026-01-24',
  endDate: null,
  medication: null,
  dosage: null,
  route: null,
  frequency: null,
  schedule: 'Low sodium diet',
  observations: 'Avoid processed foods',
  status: 'ACTIVA',
  discontinuedAt: null,
  discontinuedBy: null,
  createdAt: '2026-01-24T11:00:00',
  createdBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  },
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
  status: 'DESCONTINUADO',
  discontinuedAt: '2026-01-25T14:00:00',
  discontinuedBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  },
  createdAt: '2026-01-23T10:00:00',
  createdBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  },
  updatedAt: '2026-01-25T14:00:00',
  updatedBy: null
}

// Helper function to setup authenticated state
async function setupAuth(page: import('@playwright/test').Page, user: typeof mockAdminUser) {
  await page.addInitScript(
    userData => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

// Helper function to setup user API mock
async function setupUserMock(page: import('@playwright/test').Page, user: typeof mockAdminUser) {
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
}

// Helper function to setup admission mock
async function setupAdmissionMock(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/admissions/1', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockAdmission })
      })
    }
  })

  // Mock documents endpoint
  await page.route('**/api/v1/admissions/1/documents', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [] })
    })
  })

  // Mock clinical history
  await page.route('**/api/v1/admissions/1/clinical-history', async route => {
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'Not found' })
    })
  })

  // Mock progress notes
  await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
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

// Create grouped orders response
function createGroupedOrders(orders: typeof mockMedicationOrder[]) {
  const grouped: Record<string, typeof mockMedicationOrder[]> = {
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

test.describe('Medical Record - Medical Orders', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('doctor can create medical order', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    const orders: typeof mockDietOrder[] = []

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: createGroupedOrders(orders) })
        })
      } else if (route.request().method() === 'POST') {
        orders.push(mockDietOrder)
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDietOrder })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Should see add button
    const addButton = page.getByRole('button', { name: /Add Order|Agregar Orden/i })
    await expect(addButton).toBeVisible()

    // Click add button
    await addButton.click()

    // Dialog should open
    await expect(page.getByRole('dialog')).toBeVisible()

    // Select category (Diet)
    await page.locator('#category').click()
    await page.locator('.p-select-overlay').getByText(/Diet|Dieta/i).click()

    // Start date should be pre-filled

    // Fill schedule
    await page.locator('#schedule').fill('Low sodium diet')

    // Submit
    await page.getByRole('dialog').getByRole('button', { name: /Save|Guardar/i }).click()

    // Should see success message
    await expect(page.getByText(/created successfully|creado exitosamente/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('doctor creates medication order with medication fields', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: createGroupedOrders([]) })
        })
      } else if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockMedicationOrder })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Click add first order button
    await page.getByRole('button', { name: /Add First Order|Agregar Primera Orden/i }).click()

    // Dialog should open
    await expect(page.getByRole('dialog')).toBeVisible()

    // Select MEDICAMENTOS category
    await page.locator('#category').click()
    await page.locator('.p-select-overlay').getByText(/Medications|Medicamentos/i).click()

    // Medication fields should now be visible
    await expect(page.locator('.medication-fields')).toBeVisible()

    // Fill medication fields
    await page.locator('#medication').fill('Sertraline')
    await page.locator('#dosage').fill('50mg')

    // Select route
    await page.locator('#route').click()
    await page.locator('.p-select-overlay').getByText(/Oral/i).click()

    // Fill frequency and schedule
    await page.locator('#frequency').fill('Once daily')
    await page.locator('#schedule').fill('Morning with breakfast')

    // Submit
    await page.getByRole('dialog').getByRole('button', { name: /Save|Guardar/i }).click()

    // Should see success message
    await expect(page.getByText(/created successfully|creado exitosamente/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('admin can discontinue active order', async ({ page }) => {
    // Note: Using admin because the component requires medical-order:update permission
    // to show the discontinue button (canEdit prop controls both edit and discontinue)
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    let orderDiscontinued = false

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      if (route.request().method() === 'GET') {
        const orders = orderDiscontinued
          ? [{ ...mockMedicationOrder, status: 'DESCONTINUADO', discontinuedAt: '2026-01-25T14:00:00' }]
          : [mockMedicationOrder]
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: createGroupedOrders(orders) })
        })
      }
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1/discontinue', async route => {
      orderDiscontinued = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { ...mockMedicationOrder, status: 'DESCONTINUADO' }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Expand medications category
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    // Wait for order card
    await expect(page.locator('.medical-order-card')).toBeVisible()
    await waitForOverlaysToClear(page)

    // Click discontinue button (in the order card, not the filter)
    const orderCard = page.locator('.medical-order-card').first()
    await orderCard.getByRole('button', { name: /Discontinue|Suspender/i }).click()

    // Wait for confirm dialog animation to settle
    await page.waitForTimeout(500)

    // Click Yes button using JavaScript to bypass overlay issues
    // Note: MedicalOrderCard renders ConfirmDialog per-card, so multiple dialogs exist
    await page.evaluate(() => {
      // Find the first visible confirm dialog's accept button
      const dialogs = document.querySelectorAll('.p-confirmdialog')
      for (const dialog of dialogs) {
        const acceptBtn = dialog.querySelector('.p-confirmdialog-accept-button') as HTMLButtonElement
        if (acceptBtn && dialog.checkVisibility()) {
          acceptBtn.click()
          return
        }
      }
    })

    // Wait for API call to complete and status to update
    await page.waitForTimeout(500)

    // Verify status changed to Discontinued in the order card
    await expect(orderCard.getByText(/^Discontinued$|^Descontinuado$/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('doctor cannot edit medical orders', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Expand medications category
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    // Wait for order card
    await expect(page.locator('.medical-order-card')).toBeVisible()

    // Doctor should NOT see edit button (no medical-order:update permission)
    const orderCard = page.locator('.medical-order-card').first()
    await expect(
      orderCard.getByRole('button', { name: /^Edit$|^Editar$/i })
    ).not.toBeVisible()

    // But should see discontinue button (doctor has medical-order:discontinue)
    // Note: The component uses canEdit prop for discontinue, which maps to medical-order:update
    // So doctors without update permission also won't see discontinue
    // This test verifies the current behavior
  })

  test('nurse cannot create medical orders', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([]) })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Should see empty state
    await expect(page.getByText(/No medical orders recorded/i)).toBeVisible()

    // Should NOT see add button (nurse has no medical-order:create)
    await expect(page.getByRole('button', { name: /Add.*Order/i })).not.toBeVisible()
  })

  test('nurse can view medical orders', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrder, mockDietOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Should see categories with counts
    await expect(page.getByText(/Medications|Medicamentos/i).first()).toBeVisible()
    await expect(page.getByText(/Diet|Dieta/i).first()).toBeVisible()

    // Expand medications to see order
    await expandAccordionPanel(page, /Medications|Medicamentos/i)
    await expect(page.getByText('Sertraline')).toBeVisible()
  })

  test('nurse cannot discontinue orders', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Expand medications category
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    // Wait for order card
    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()

    // Nurse should NOT see edit or discontinue buttons in the order card
    // (no medical-order:update permission, which is used for both)
    await expect(orderCard.getByRole('button', { name: /Edit|Editar/i })).not.toBeVisible()
    await expect(orderCard.locator('button:has(.pi-ban)')).not.toBeVisible()
  })

  test('admin can edit medical order', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: createGroupedOrders([mockMedicationOrder])
          })
        })
      }
    })

    await page.route('**/api/v1/admissions/1/medical-orders/1', async route => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockMedicationOrder })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await waitForOverlaysToClear(page)

    // Expand medications category
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    // Wait for order card
    await expect(page.locator('.medical-order-card')).toBeVisible()

    // Admin should see edit button
    const editButton = page.locator('.medical-order-card').getByRole('button', { name: /^Edit$|^Editar$/i })
    await expect(editButton).toBeVisible()

    // Click edit
    await editButton.click()

    // Dialog should open with Edit title
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/Edit Medical Order|Editar Orden/i)).toBeVisible()

    // Submit
    await page.getByRole('dialog').getByRole('button', { name: /Save|Guardar/i }).click()

    // Should see success message
    await expect(page.getByText(/updated successfully|actualizado exitosamente/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('orders grouped by category', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrder, mockDiscontinuedOrder, mockDietOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Should see total orders count
    await expect(page.getByText(/3 orders|3 órdenes/i)).toBeVisible()

    // Should see category headers with counts
    // Medications has 2 orders
    const medicationsAccordion = page.locator('.p-accordionheader').filter({ hasText: /Medications|Medicamentos/i })
    await expect(medicationsAccordion.locator('.p-tag')).toHaveText('2')

    // Diet has 1 order
    const dietAccordion = page.locator('.p-accordionheader').filter({ hasText: /Diet|Dieta/i })
    await expect(dietAccordion.locator('.p-tag')).toHaveText('1')
  })

  test('filter shows only authorized orders', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrder, mockDiscontinuedOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // By default, all orders shown
    await expect(page.getByText(/2 orders|2 órdenes/i)).toBeVisible()

    // Click Authorized filter (in the SelectButton)
    await page.locator('.p-selectbutton').getByText(/^Authorized$|^Autorizado$/i).click()

    // Wait for filter to apply
    await page.waitForTimeout(300)

    // Should now show only 1 order (the authorized medication)
    await expect(page.getByText(/1 orders|1 órdenes/i)).toBeVisible()

    // Expand medications
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    // Only Sertraline should be visible, not Lorazepam (discontinued)
    await expect(page.getByText('Sertraline')).toBeVisible()
    await expect(page.getByText('Lorazepam')).not.toBeVisible()
  })

  test('filter shows only discontinued orders', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockMedicationOrder, mockDiscontinuedOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Click Discontinued filter (in the SelectButton)
    await page.locator('.p-selectbutton').getByText(/Discontinued|Descontinuado/i).click()

    // Wait for filter to apply
    await page.waitForTimeout(300)

    // Should now show only 1 order (the discontinued one)
    await expect(page.getByText(/1 orders|1 órdenes/i)).toBeVisible()

    // Expand medications
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    // Only Lorazepam should be visible
    await expect(page.getByText('Lorazepam')).toBeVisible()
    await expect(page.getByText('Sertraline')).not.toBeVisible()
  })

  test('medication fields hidden for non-medication categories', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([]) })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Open add dialog
    await page.getByRole('button', { name: /Add First Order|Agregar Primera Orden/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Default category is ORDENES_MEDICAS - medication fields should be hidden
    await expect(page.locator('.medication-fields')).not.toBeVisible()

    // Select MEDICAMENTOS
    await page.locator('#category').click()
    await page.locator('.p-select-overlay').getByText(/Medications|Medicamentos/i).click()

    // Medication fields should now be visible
    await expect(page.locator('.medication-fields')).toBeVisible()
    await expect(page.locator('#medication')).toBeVisible()
    await expect(page.locator('#dosage')).toBeVisible()
    await expect(page.locator('#route')).toBeVisible()
    await expect(page.locator('#frequency')).toBeVisible()

    // Switch to Diet category
    await page.locator('#category').click()
    await page.locator('.p-select-overlay').getByText(/Diet|Dieta/i).click()

    // Medication fields should be hidden again
    await expect(page.locator('.medication-fields')).not.toBeVisible()
  })
})

// =====================================================================
// v1.2 workflow states (authorize / reject / emergency-authorize /
// mark-in-progress / discontinue blocked from EN_PROCESO).
//
// These suites use the same mock/setup pattern as the v1.0 suite above
// but exercise category-driven order shapes:
//   - MEDICAMENTOS  → SOLICITADO → AUTORIZADO (terminal for meds)
//   - LABORATORIOS  → SOLICITADO → AUTORIZADO → EN_PROCESO → RESULTADOS_RECIBIDOS
//   - DIETA         → ACTIVA (directive, no auth flow)
// =====================================================================

const mockAdminStaffUser = {
  id: 4,
  username: 'staff',
  email: 'staff@example.com',
  firstName: 'Maria',
  lastName: 'Garcia',
  salutation: 'Sra.',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: [
    'admission:read',
    'medical-order:read',
    'medical-order:authorize'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockDoctorWithEmergency = {
  ...mockDoctorUser,
  permissions: [
    ...mockDoctorUser.permissions,
    'medical-order:emergency-authorize',
    'medical-order:mark-in-progress'
  ]
}

const mockNurseWithMarkInProgress = {
  ...mockNurseUser,
  permissions: [...mockNurseUser.permissions, 'medical-order:mark-in-progress']
}

const mockSolicitadoMedicationOrder = {
  ...mockMedicationOrder,
  id: 10,
  status: 'SOLICITADO',
  authorizedAt: null,
  authorizedBy: null,
  inProgressAt: null,
  inProgressBy: null,
  resultsReceivedAt: null,
  resultsReceivedBy: null,
  rejectionReason: null,
  emergencyAuthorized: false,
  emergencyReason: null,
  emergencyReasonNote: null,
  emergencyAt: null,
  emergencyBy: null
}

const mockSolicitadoLabOrder = {
  id: 11,
  admissionId: 1,
  category: 'LABORATORIOS',
  startDate: '2026-01-24',
  endDate: null,
  medication: null,
  dosage: null,
  route: null,
  frequency: null,
  schedule: null,
  observations: 'Hemograma completo',
  status: 'SOLICITADO',
  authorizedAt: null,
  authorizedBy: null,
  inProgressAt: null,
  inProgressBy: null,
  resultsReceivedAt: null,
  resultsReceivedBy: null,
  rejectionReason: null,
  emergencyAuthorized: false,
  emergencyReason: null,
  emergencyReasonNote: null,
  emergencyAt: null,
  emergencyBy: null,
  discontinuedAt: null,
  discontinuedBy: null,
  createdAt: '2026-01-24T10:00:00',
  createdBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  },
  updatedAt: '2026-01-24T10:00:00',
  updatedBy: null
}

const mockAutorizadoLabOrder = {
  ...mockSolicitadoLabOrder,
  status: 'AUTORIZADO',
  authorizedAt: '2026-01-24T10:30:00',
  authorizedBy: {
    id: 4,
    salutation: 'Sra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['ADMINISTRATIVE_STAFF']
  }
}

const mockEnProcesoLabOrder = {
  ...mockAutorizadoLabOrder,
  status: 'EN_PROCESO',
  inProgressAt: '2026-01-24T11:00:00',
  inProgressBy: {
    id: 3,
    salutation: 'Lic.',
    firstName: 'Ana',
    lastName: 'Lopez',
    roles: ['NURSE']
  }
}

// Click the accept button of the first visible PrimeVue confirm dialog.
// MedicalOrderCard mounts a ConfirmDialog per card, so multiple may exist
// in the DOM — same pattern as the existing discontinue test (line ~478).
async function acceptFirstConfirmDialog(page: import('@playwright/test').Page) {
  await page.waitForTimeout(500)
  await page.evaluate(() => {
    const dialogs = document.querySelectorAll('.p-confirmdialog')
    for (const dialog of dialogs) {
      const acceptBtn = dialog.querySelector(
        '.p-confirmdialog-accept-button'
      ) as HTMLButtonElement | null
      if (acceptBtn && dialog.checkVisibility()) {
        acceptBtn.click()
        return
      }
    }
  })
}

test.describe('Medical Orders v1.2 - Authorize / Reject', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('admin staff can authorize a SOLICITADO medication order', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupUserMock(page, mockAdminStaffUser)
    await setupAdmissionMock(page)

    let authorized = false
    let authorizeCalled = false

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      const order = authorized
        ? {
            ...mockSolicitadoMedicationOrder,
            status: 'AUTORIZADO',
            authorizedAt: '2026-01-24T10:30:00',
            authorizedBy: mockAdminStaffUser
          }
        : mockSolicitadoMedicationOrder
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([order]) })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/10/authorize', async route => {
      authorizeCalled = true
      authorized = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { ...mockSolicitadoMedicationOrder, status: 'AUTORIZADO' }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    // SOLICITADO badge visible (English label is "Requested")
    await expect(orderCard.getByText(/^Requested$|^Solicitado$/i)).toBeVisible()
    await waitForOverlaysToClear(page)

    // Authorize button visible for staff with the permission
    const authorizeBtn = orderCard.getByRole('button', { name: /^Authorize$|^Autorizar$/i })
    await expect(authorizeBtn).toBeVisible()
    await authorizeBtn.click()

    await acceptFirstConfirmDialog(page)

    await expect.poll(() => authorizeCalled, { timeout: 5000 }).toBe(true)

    // Status flips to AUTORIZADO
    await expect(orderCard.getByText(/^Authorized$|^Autorizado$/i)).toBeVisible({ timeout: 10000 })
  })

  test('admin staff can reject a SOLICITADO order with a reason', async ({ page }) => {
    await setupAuth(page, mockAdminStaffUser)
    await setupUserMock(page, mockAdminStaffUser)
    await setupAdmissionMock(page)

    let rejected = false
    let capturedReason: string | null = null

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      const order = rejected
        ? {
            ...mockSolicitadoMedicationOrder,
            status: 'NO_AUTORIZADO',
            rejectionReason: capturedReason
          }
        : mockSolicitadoMedicationOrder
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([order]) })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/10/reject', async route => {
      const body = route.request().postDataJSON() as { reason?: string | null }
      capturedReason = body.reason ?? null
      rejected = true
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            ...mockSolicitadoMedicationOrder,
            status: 'NO_AUTORIZADO',
            rejectionReason: capturedReason
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await waitForOverlaysToClear(page)

    // Click Reject in the card
    await orderCard.getByRole('button', { name: /^Reject$|^No autorizar$/i }).click()

    // Reject dialog opens with the reason textarea
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await dialog.locator('#medical-order-reject-reason').fill('Pendiente de cobertura del seguro')

    // Click the Reject button in the dialog footer
    await dialog.getByRole('button', { name: /^Reject$|^No autorizar$/i }).click()

    await expect.poll(() => capturedReason, { timeout: 5000 }).toBe(
      'Pendiente de cobertura del seguro'
    )
    await expect(orderCard.getByText(/^Not Authorized$|^No autorizado$/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('doctor cannot authorize (no medical-order:authorize permission)', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockSolicitadoMedicationOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()

    await expect(
      orderCard.getByRole('button', { name: /^Authorize$|^Autorizar$/i })
    ).not.toBeVisible()
    await expect(
      orderCard.getByRole('button', { name: /^Reject$|^No autorizar$/i })
    ).not.toBeVisible()
  })
})

test.describe('Medical Orders v1.2 - Emergency Authorize', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('doctor emergency-authorizes with PATIENT_IN_CRISIS', async ({ page }) => {
    await setupAuth(page, mockDoctorWithEmergency)
    await setupUserMock(page, mockDoctorWithEmergency)
    await setupAdmissionMock(page)

    let authorized = false
    let capturedBody: { reason?: string; reasonNote?: string | null } = {}

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      const order = authorized
        ? {
            ...mockSolicitadoMedicationOrder,
            status: 'AUTORIZADO',
            authorizedAt: '2026-01-24T02:00:00',
            authorizedBy: mockDoctorWithEmergency,
            emergencyAuthorized: true,
            emergencyReason: capturedBody.reason ?? null,
            emergencyReasonNote: capturedBody.reasonNote ?? null,
            emergencyAt: '2026-01-24T02:00:00',
            emergencyBy: mockDoctorWithEmergency
          }
        : mockSolicitadoMedicationOrder
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([order]) })
      })
    })

    await page.route(
      '**/api/v1/admissions/1/medical-orders/10/emergency-authorize',
      async route => {
        capturedBody = route.request().postDataJSON()
        authorized = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              ...mockSolicitadoMedicationOrder,
              status: 'AUTORIZADO',
              emergencyAuthorized: true,
              emergencyReason: capturedBody.reason,
              emergencyReasonNote: capturedBody.reasonNote ?? null
            }
          })
        })
      }
    )

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await waitForOverlaysToClear(page)

    await orderCard
      .getByRole('button', { name: /Emergency authorize|Autorización de emergencia/i })
      .click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    // Warning banner present
    await expect(dialog.getByText(/no admin staff is available|sin personal administrativo/i))
      .toBeVisible()

    // Submit is disabled until a reason is selected
    const submitBtn = dialog.getByRole('button', {
      name: /Emergency authorize|Autorización de emergencia/i
    })
    await expect(submitBtn).toBeDisabled()

    // Pick "Patient in crisis"
    await dialog.locator('label[for="reason-PATIENT_IN_CRISIS"]').click()

    // Now enabled (note is optional for non-OTHER reasons)
    await expect(submitBtn).toBeEnabled()
    await submitBtn.click()

    await expect.poll(() => capturedBody.reason, { timeout: 5000 }).toBe('PATIENT_IN_CRISIS')

    // Card now shows AUTORIZADO + emergency banner
    await expect(orderCard.getByText(/^Authorized$|^Autorizado$/i)).toBeVisible({
      timeout: 10000
    })
    await expect(orderCard.getByText(/Emergency-authorized|Autorización de emergencia/i))
      .toBeVisible()
  })

  test('emergency-authorize with OTHER reason requires a note', async ({ page }) => {
    await setupAuth(page, mockDoctorWithEmergency)
    await setupUserMock(page, mockDoctorWithEmergency)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockSolicitadoMedicationOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').first()
    await waitForOverlaysToClear(page)
    await orderCard
      .getByRole('button', { name: /Emergency authorize|Autorización de emergencia/i })
      .click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()

    const submitBtn = dialog.getByRole('button', {
      name: /Emergency authorize|Autorización de emergencia/i
    })

    // Pick OTHER, leave the note empty → submit should stay disabled
    await dialog.locator('label[for="reason-OTHER"]').click()
    await expect(submitBtn).toBeDisabled()

    // Required marker (*) appears next to the Notes label when reason is OTHER
    await expect(dialog.locator('.required-marker')).toBeVisible()

    // Type a note → submit becomes enabled
    await dialog.locator('#emergency-reason-note').fill('Patient agitated, no admin on call')
    await expect(submitBtn).toBeEnabled()
  })

  test('nurse cannot emergency-authorize', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockSolicitadoMedicationOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await expect(
      orderCard.getByRole('button', { name: /Emergency authorize|Autorización de emergencia/i })
    ).not.toBeVisible()
  })
})

test.describe('Medical Orders v1.2 - Mark in Progress (results-bearing)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('nurse marks an AUTORIZADO lab order as EN_PROCESO ("Sample taken")', async ({ page }) => {
    await setupAuth(page, mockNurseWithMarkInProgress)
    await setupUserMock(page, mockNurseWithMarkInProgress)
    await setupAdmissionMock(page)

    let inProgress = false
    let markCalled = false

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      const order = inProgress ? mockEnProcesoLabOrder : mockAutorizadoLabOrder
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([order]) })
      })
    })

    await page.route(
      '**/api/v1/admissions/1/medical-orders/11/mark-in-progress',
      async route => {
        markCalled = true
        inProgress = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockEnProcesoLabOrder })
        })
      }
    )

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Laboratory|Laboratorios/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await waitForOverlaysToClear(page)

    // Per-category label for LABORATORIOS is "Sample taken" (es: "Muestra tomada")
    const markBtn = orderCard.getByRole('button', { name: /Sample taken|Muestra tomada/i })
    await expect(markBtn).toBeVisible()
    await markBtn.click()

    await acceptFirstConfirmDialog(page)

    await expect.poll(() => markCalled, { timeout: 5000 }).toBe(true)
    await expect(orderCard.getByText(/^In progress$|^En proceso$/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('mark-in-progress button is hidden for non-results categories', async ({ page }) => {
    await setupAuth(page, mockNurseWithMarkInProgress)
    await setupUserMock(page, mockNurseWithMarkInProgress)
    await setupAdmissionMock(page)

    // An AUTORIZADO medication order — meds are auth-only, so mark-in-progress
    // must NOT appear (would 400 on the backend).
    const autorizadoMed = {
      ...mockSolicitadoMedicationOrder,
      status: 'AUTORIZADO',
      authorizedAt: '2026-01-24T10:30:00',
      authorizedBy: mockAdminStaffUser
    }

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([autorizadoMed]) })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Medications|Medicamentos/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()

    // None of the per-category mark-in-progress labels should be visible
    await expect(
      orderCard.getByRole('button', {
        name: /Mark in progress|Marcar en proceso|Sample taken|Muestra tomada|Patient referred|Paciente referido/i
      })
    ).not.toBeVisible()
  })

  test('discontinue is not offered once the lab order is EN_PROCESO', async ({ page }) => {
    // Spec rule: once the sample has been taken, the order can no longer be cancelled.
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockEnProcesoLabOrder])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')
    await expandAccordionPanel(page, /Laboratory|Laboratorios/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await expect(orderCard.getByText(/^In progress$|^En proceso$/i)).toBeVisible()

    // Discontinue button must not render in EN_PROCESO
    await expect(
      orderCard.getByRole('button', { name: /^Discontinue$|^Suspender$/i })
    ).not.toBeVisible()
  })
})
