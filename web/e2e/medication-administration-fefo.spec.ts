import { test, expect, type Page } from '@playwright/test'
import {
  confirmDialogAccept,
  waitForMedicalRecordTabs,
  selectMedicalRecordTab,
  expandAccordionPanel
} from './utils/test-helpers'

// --- Mock users ---

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
    'medication-administration:read',
    'medication:read',
    'inventory-lot:read'
  ],
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
  salutation: 'Dr.',
  roles: ['ADMIN'],
  permissions: [],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// --- Mock domain objects ---

const mockPatient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  dateOfBirth: '1980-06-12',
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
  medication: 'Olanzapine 5mg',
  dosage: '5mg',
  route: 'ORAL',
  frequency: 'Once daily',
  schedule: 'Morning',
  observations: null,
  status: 'AUTORIZADO',
  inventoryItemId: 101,
  discontinuedAt: null,
  discontinuedBy: null,
  createdAt: '2026-01-24T10:00:00',
  createdBy: { id: 2, salutation: 'Dra.', firstName: 'Maria', lastName: 'Garcia', roles: ['DOCTOR'] },
  updatedAt: '2026-01-24T10:00:00',
  updatedBy: null
}

const mockFefoLot = {
  id: 5001,
  itemId: 101,
  itemName: 'Olanzapine 5mg',
  itemSku: 'A12',
  lotNumber: 'L-2025-08',
  expirationDate: '2026-07-01',
  quantityOnHand: 14,
  receivedAt: '2025-08-01',
  supplier: 'Distribuidora ABC',
  notes: null,
  recalled: false,
  recalledReason: null,
  syntheticLegacy: false,
  createdAt: '2025-08-01T08:00:00',
  updatedAt: '2025-08-01T08:00:00'
}

const mockSecondLot = {
  ...mockFefoLot,
  id: 5002,
  lotNumber: 'L-2026-01',
  expirationDate: '2027-03-15',
  quantityOnHand: 30
}

// --- Helpers ---

async function setupAuth(page: Page, user: typeof mockNurseUser) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

async function setupUserMock(page: Page, user: typeof mockNurseUser) {
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

async function setupAdmissionMock(page: Page) {
  await page.route('**/api/v1/admissions/1', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockAdmission })
      })
    }
  })
  await page.route('**/api/v1/admissions/1/documents', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [] })
    })
  })
  await page.route('**/api/v1/admissions/1/clinical-history', async route => {
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'Not found' })
    })
  })
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
  await page.route('**/api/v1/admissions/1/medical-orders', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          orders: {
            ORDENES_MEDICAS: [],
            MEDICAMENTOS: [mockMedicationOrder],
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
        }
      })
    })
  })
}

async function openAdministerDialog(page: Page) {
  await page.goto('/admissions/1')
  await waitForMedicalRecordTabs(page)
  await selectMedicalRecordTab(page, 'medicalOrders')
  await expandAccordionPanel(page, /Medications|Medicamentos/i)

  const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Olanzapine 5mg' })
  await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

  const dialog = page.locator('[role="dialog"]').filter({ hasText: /Administer/i })
  await expect(dialog).toBeVisible()
  return dialog
}

// --- Tests ---

test.describe('Medication administration FEFO (NURSE)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('FEFO preview chip renders when GIVEN', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/medications/101/fefo-preview**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockFefoLot })
      })
    })
    // Nurse has inventory-lot:read but NOT update; fetchByItem is only called when canOverrideLot.
    // Still safe to register the route in case it is called.
    await page.route('**/api/v1/inventory/items/101/lots', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [mockFefoLot] })
      })
    })

    const dialog = await openAdministerDialog(page)

    // Quantity field appears for GIVEN status
    await expect(dialog.locator('#quantity input')).toBeVisible()

    // FEFO preview row shows the lot number + chip
    const fefoRow = dialog.locator('.fefo-row')
    await expect(fefoRow).toBeVisible()
    await expect(fefoRow).toContainText('L-2025-08')
  })

  test('quantity > 1 is forwarded in the POST payload', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/medications/101/fefo-preview**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockFefoLot })
      })
    })

    let posted: Record<string, unknown> | null = null
    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations', async route => {
      if (route.request().method() === 'POST') {
        posted = (await route.request().postDataJSON()) as Record<string, unknown>
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 99,
              orderId: 1,
              status: 'GIVEN',
              notes: null,
              administeredAt: '2026-01-26T08:00:00',
              administeredByName: 'Ana Lopez',
              billable: true,
              quantity: posted?.quantity ?? 1
            }
          })
        })
        return
      }
      await route.continue()
    })

    const dialog = await openAdministerDialog(page)

    // Set quantity to 3
    await dialog.locator('#quantity input').fill('3')

    await dialog.getByRole('button', { name: /^Save$/i }).click()
    await confirmDialogAccept(page)

    await expect.poll(() => posted, { timeout: 5000 }).not.toBeNull()
    expect(posted).toMatchObject({ status: 'GIVEN', quantity: 3 })
  })

  test('lot override select is hidden for nurses (no inventory-lot:update)', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/medications/101/fefo-preview**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockFefoLot })
      })
    })

    const dialog = await openAdministerDialog(page)

    // The "Override lot (admin)" label is not rendered for nurses
    await expect(dialog.locator('#overrideLot')).toHaveCount(0)
    await expect(dialog.getByText(/Override lot/i)).toHaveCount(0)
  })

  test('quantity and FEFO preview hidden when status is MISSED', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/medications/101/fefo-preview**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockFefoLot })
      })
    })

    const dialog = await openAdministerDialog(page)

    // Switch status from GIVEN to MISSED
    await dialog.locator('#status').click()
    await page.locator('.p-select-overlay').getByText('Missed').click()

    // Quantity field hidden
    await expect(dialog.locator('#quantity input')).toHaveCount(0)
    // FEFO preview hidden
    await expect(dialog.locator('.fefo-row')).toHaveCount(0)
  })
})

test.describe('Medication administration FEFO (ADMIN)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('admin sees the lot-override Select populated with active non-recalled lots', async ({
    page
  }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/medications/101/fefo-preview**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockFefoLot })
      })
    })

    await page.route('**/api/v1/inventory/items/101/lots', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            mockFefoLot,
            mockSecondLot,
            // recalled lot must be filtered out of the override options
            { ...mockFefoLot, id: 5003, lotNumber: 'L-RECALL', recalled: true }
          ]
        })
      })
    })

    const dialog = await openAdministerDialog(page)

    // Override field is rendered for admins
    const override = dialog.locator('#overrideLot')
    await expect(override).toBeVisible()

    await override.click()
    const overlay = page.locator('.p-select-overlay')

    // Auto-FEFO option always present
    await expect(overlay.getByText(/Auto \(FEFO\)/i)).toBeVisible()
    // Active lots present
    await expect(overlay.getByText(/L-2025-08/)).toBeVisible()
    await expect(overlay.getByText(/L-2026-01/)).toBeVisible()
    // Recalled lot filtered out
    await expect(overlay.getByText(/L-RECALL/)).toHaveCount(0)
  })

  test('admin lot override is included in the POST payload', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/medications/101/fefo-preview**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockFefoLot })
      })
    })

    await page.route('**/api/v1/inventory/items/101/lots', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [mockFefoLot, mockSecondLot] })
      })
    })

    let posted: Record<string, unknown> | null = null
    await page.route('**/api/v1/admissions/1/medical-orders/1/administrations', async route => {
      if (route.request().method() === 'POST') {
        posted = (await route.request().postDataJSON()) as Record<string, unknown>
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 99,
              orderId: 1,
              status: 'GIVEN',
              notes: null,
              administeredAt: '2026-01-26T08:00:00',
              administeredByName: 'Admin User',
              billable: true
            }
          })
        })
        return
      }
      await route.continue()
    })

    const dialog = await openAdministerDialog(page)

    // Wait for the lot list to land in the select
    const override = dialog.locator('#overrideLot')
    await expect(override).toBeVisible()
    await override.click()
    await page.locator('.p-select-overlay').getByText(/L-2026-01/).click()

    await dialog.getByRole('button', { name: /^Save$/i }).click()
    await confirmDialogAccept(page)

    await expect.poll(() => posted, { timeout: 5000 }).not.toBeNull()
    expect(posted).toMatchObject({ status: 'GIVEN', lotId: 5002 })
  })
})
