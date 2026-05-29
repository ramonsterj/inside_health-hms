import { test, expect, type Page } from '@playwright/test'
import {
  confirmDialogAccept,
  waitForMedicalRecordTabs,
  selectMedicalRecordTab,
  expandAccordionPanel
} from './utils/test-helpers'

/**
 * E2E coverage for the Warehouse-Scoped Inventory (Bodegas) feature
 * (docs/features/warehouse-inventory-management.md — "E2E (Playwright)" checklist).
 *
 * These tests follow the repo's mock-driven E2E convention: the Vue app runs against
 * the dev server while every API call is fulfilled via `page.route`. They exercise the
 * UI behaviour the four mandated scenarios describe — payloads sent, success/error
 * feedback, permission-gated visibility, and the warehouse-scope denials. The backend
 * transactional guarantees (atomic transfer, FEFO scoping, billing event) are covered
 * by the integration tests `WarehouseDispenseIT` / `WarehouseIntegrationTest`.
 *
 * Mandated scenarios:
 *   1. Admin happy path: create warehouse -> transfer to ENFERMERIA -> nurse dispenses.
 *   2. Nurse out-of-stock: dispense returns an error naming the nurse's warehouse.
 *   3. Maintenance happy path: charge a consumable to an admission -> bill shows the line.
 *   4. Scope denials: NURSE cannot issue a transfer; MAINTENANCE only sees its bodegas.
 */

// ── Mock users (permissions mirror the V119 seed grants) ──

const adminUser = {
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

// NURSE per V119: warehouse:read + transfer read/receive, but NOT transfer:create.
// room:occupancy-view mirrors the real NURSE seed grant — the bed occupancy screen
// is the nurse's default landing page (bed-occupancy-view), so the mock needs it to
// avoid a dashboard -> bed-occupancy redirect loop.
const nurseUser = {
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
    'inventory-lot:read',
    'room:occupancy-view',
    'warehouse:read',
    'warehouse-transfer:read',
    'warehouse-transfer:receive'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// MAINTENANCE per V119: read, transfer create/read/receive, charge.
const maintenanceUser = {
  id: 50,
  username: 'maintenance',
  email: 'maintenance@example.com',
  firstName: 'Mario',
  lastName: 'Fix',
  salutation: 'Sr.',
  roles: ['MAINTENANCE'],
  permissions: [
    'warehouse:read',
    'warehouse-transfer:create',
    'warehouse-transfer:read',
    'warehouse-transfer:receive',
    'warehouse-charge:create'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// ── Mock domain data ──

const administracion = {
  id: 1,
  code: 'ADMINISTRACION',
  name: 'Administración',
  description: 'Master / receiving warehouse.',
  active: true,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00'
}
const enfermeria = {
  id: 2,
  code: 'ENFERMERIA',
  name: 'Enfermería',
  description: 'Nursing warehouse.',
  active: true,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00'
}
const mantenimiento1 = {
  id: 3,
  code: 'MANTENIMIENTO_1',
  name: 'Mantenimiento 1',
  description: 'Maintenance warehouse 1.',
  active: true,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00'
}

const gasas = {
  id: 201,
  name: 'Gasas estériles',
  sku: 'S1',
  kind: 'SUPPLY',
  lotTrackingEnabled: false,
  price: 5,
  restockLevel: 10
}

const mockPatient = {
  id: 1,
  firstName: 'Juan',
  lastName: 'Pérez García',
  dateOfBirth: '1980-06-12',
  idDocumentNumber: '1234567890101'
}

const mockAdmissionListItem = {
  id: 10,
  patient: mockPatient,
  triageCode: { id: 1, code: 'A', color: '#FF0000', description: 'Critical', displayOrder: 1 },
  room: { id: 1, number: '101', type: 'PRIVATE', capacity: 1, availableBeds: 0 },
  treatingPhysician: { id: 2, firstName: 'Maria', lastName: 'Garcia', salutation: 'Dra.' },
  resident: { id: 9, firstName: 'Pedro', lastName: 'Solis', salutation: 'Dr.' },
  admissionDate: '2026-01-23T10:30:00',
  dischargeDate: null,
  status: 'ACTIVE',
  type: 'HOSPITALIZATION',
  hasConsentDocument: false,
  createdAt: '2026-01-23T10:35:00'
}

// Admission detail + medical order used to drive the nurse's medication-administration dialog.
const mockAdmissionDetail = {
  ...mockAdmissionListItem,
  inventory: null,
  consultingPhysicians: [],
  createdBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' },
  updatedAt: '2026-01-23T10:35:00',
  updatedBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' }
}

const mockMedicationOrder = {
  id: 1,
  admissionId: 10,
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

// ── Helpers ──

async function setupAuth(page: Page, user: typeof adminUser) {
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

function ok(data: unknown) {
  return { status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data }) }
}

function page0(content: unknown[]) {
  return {
    content,
    page: { totalElements: content.length, totalPages: 1, size: 20, number: 0 }
  }
}

/** Mock the warehouse catalog returned to the caller (already scoped server-side). */
async function mockWarehouses(page: Page, warehouses: unknown[]) {
  await page.route('**/api/v1/warehouses', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill(ok(warehouses))
      return
    }
    await route.continue()
  })
}

/** Item catalog used by the transfer / charge dialogs. */
async function mockItems(page: Page) {
  await page.route('**/api/v1/admin/inventory-items**', async route => {
    await route.fulfill(ok(page0([gasas])))
  })
}

/** Admissions list used by the charge dialog admission picker. */
async function mockAdmissionsList(page: Page) {
  await page.route('**/api/v1/admissions?*', async route => {
    await route.fulfill(ok(page0([mockAdmissionListItem])))
  })
  await page.route('**/api/v1/admissions', async route => {
    await route.fulfill(ok(page0([mockAdmissionListItem])))
  })
}

/** Everything the medical-record screen needs so the administer dialog can open. */
async function mockAdmissionMedicalRecord(page: Page) {
  await page.route('**/api/v1/admissions/10', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill(ok(mockAdmissionDetail))
      return
    }
    await route.continue()
  })
  await page.route('**/api/v1/admissions/10/documents', route => route.fulfill(ok([])))
  await page.route('**/api/v1/admissions/10/clinical-history', route =>
    route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'Not found' })
    })
  )
  await page.route('**/api/v1/admissions/10/progress-notes*', route =>
    route.fulfill(ok({ content: [], page: { totalElements: 0, totalPages: 0, size: 10, number: 0 } }))
  )
  await page.route('**/api/v1/admissions/10/medical-orders', route =>
    route.fulfill(
      ok({
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
      })
    )
  )
  await page.route('**/api/v1/medications/101/fefo-preview**', route =>
    route.fulfill(ok(mockFefoLot))
  )
}

async function openAdministerDialog(page: Page) {
  await page.goto('/admissions/10')
  await waitForMedicalRecordTabs(page)
  await selectMedicalRecordTab(page, 'medicalOrders')
  await expandAccordionPanel(page, /Medications|Medicamentos/i)

  const orderCard = page.locator('.medical-order-card').filter({ hasText: 'Olanzapine 5mg' })
  await orderCard.getByRole('button', { name: /Administer|Administrar/i }).click()

  const dialog = page.locator('[role="dialog"]').filter({ hasText: /Administer/i })
  await expect(dialog).toBeVisible()
  return dialog
}

/** Pick an option from the nth PrimeVue Select inside a dialog. */
async function selectOption(page: Page, dialog: ReturnType<Page['locator']>, index: number, optionText: RegExp) {
  await dialog.locator('.p-select').nth(index).click()
  await page.locator('.p-select-overlay').getByText(optionText).first().click()
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => localStorage.clear())
})

// ── Scenario 1: Admin happy path -> nurse dispenses ──

test.describe('Warehouse — admin happy path then nurse dispense', () => {
  test('admin creates a new warehouse', async ({ page }) => {
    await setupAuth(page, adminUser)
    await mockWarehouses(page, [administracion, enfermeria])

    let posted: Record<string, unknown> | null = null
    await page.route('**/api/v1/warehouses', async route => {
      if (route.request().method() === 'POST') {
        posted = (await route.request().postDataJSON()) as Record<string, unknown>
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { id: 9, code: posted.code, name: posted.name, description: null, active: true }
          })
        })
        return
      }
      await route.fulfill(ok([administracion, enfermeria]))
    })

    await page.goto('/warehouses')
    await page.getByRole('button', { name: /New Warehouse/i }).click()

    const dialog = page.locator('[role="dialog"]').filter({ hasText: /New Warehouse/i })
    await expect(dialog).toBeVisible()
    await dialog.locator('.field').filter({ hasText: 'Code' }).locator('input').fill('FARMACIA')
    await dialog.locator('.field').filter({ hasText: 'Name' }).locator('input').fill('Farmacia Central')
    await dialog.getByRole('button', { name: /^Save$/i }).click()

    await expect.poll(() => posted, { timeout: 5000 }).not.toBeNull()
    expect(posted).toMatchObject({ code: 'FARMACIA', name: 'Farmacia Central', active: true })
    await expect(page.locator('.p-toast')).toContainText(/created successfully/i)
  })

  test('admin transfers stock to ENFERMERIA', async ({ page }) => {
    await setupAuth(page, adminUser)
    await mockWarehouses(page, [administracion, enfermeria, mantenimiento1])
    await mockItems(page)
    await page.route('**/api/v1/warehouse-transfers?*', route => route.fulfill(ok(page0([]))))

    let posted: Record<string, unknown> | null = null
    await page.route('**/api/v1/warehouse-transfers', async route => {
      if (route.request().method() === 'POST') {
        posted = (await route.request().postDataJSON()) as Record<string, unknown>
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 1,
              status: 'COMPLETED',
              sourceWarehouse: administracion,
              destinationWarehouse: enfermeria,
              item: { id: gasas.id, name: gasas.name, sku: gasas.sku },
              lot: null,
              quantity: posted?.quantity ?? 5,
              notes: null,
              issuedBy: null,
              issuedAt: '2026-05-29T09:00:00',
              completedAt: '2026-05-29T09:00:00'
            }
          })
        })
        return
      }
      await route.fulfill(ok(page0([])))
    })

    await page.goto('/warehouse-transfers')
    await page.getByRole('button', { name: /New Transfer/i }).click()

    const dialog = page.locator('[role="dialog"]').filter({ hasText: /New Transfer/i })
    await expect(dialog).toBeVisible()

    // Selects in order: source, destination, item.
    await selectOption(page, dialog, 0, /ADMINISTRACION/)
    await selectOption(page, dialog, 1, /ENFERMERIA/)
    await selectOption(page, dialog, 2, /Gasas/)
    await dialog.locator('.field').filter({ hasText: 'Quantity' }).locator('input').fill('5')

    await dialog.getByRole('button', { name: /^Save$/i }).click()

    await expect.poll(() => posted, { timeout: 5000 }).not.toBeNull()
    expect(posted).toMatchObject({
      sourceWarehouseId: administracion.id,
      destinationWarehouseId: enfermeria.id,
      itemId: gasas.id,
      quantity: 5
    })
    await expect(page.locator('.p-toast')).toContainText(/Transfer created successfully/i)
  })

  test('nurse dispenses successfully from ENFERMERIA stock', async ({ page }) => {
    await setupAuth(page, nurseUser)
    await mockAdmissionMedicalRecord(page)

    let posted: Record<string, unknown> | null = null
    await page.route('**/api/v1/admissions/10/medical-orders/1/administrations', async route => {
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
              administeredAt: '2026-05-29T08:00:00',
              administeredByName: 'Ana Lopez',
              billable: true,
              quantity: 1
            }
          })
        })
        return
      }
      await route.continue()
    })

    const dialog = await openAdministerDialog(page)
    await dialog.getByRole('button', { name: /^Save$/i }).click()
    await confirmDialogAccept(page)

    await expect.poll(() => posted, { timeout: 5000 }).not.toBeNull()
    expect(posted).toMatchObject({ status: 'GIVEN', quantity: 1 })
    // Dialog closes on success.
    await expect(dialog).not.toBeVisible({ timeout: 5000 })
  })
})

// ── Scenario 2: Nurse out-of-stock (warehouse-scoped) ──

test.describe('Warehouse — nurse out-of-stock dispense', () => {
  test('dispense error names the nurse warehouse and dialog stays open', async ({ page }) => {
    await setupAuth(page, nurseUser)
    await mockAdmissionMedicalRecord(page)

    // Backend returns the warehouse-scoped out-of-stock error
    // (error.warehouse.out.of.stock = "Out of stock in warehouse '{0}' for '{1}'").
    await page.route('**/api/v1/admissions/10/medical-orders/1/administrations', async route => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 422,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            error: {
              code: 'UNPROCESSABLE_ENTITY',
              message: "Out of stock in warehouse 'ENFERMERIA' for 'Olanzapine 5mg'"
            }
          })
        })
        return
      }
      await route.continue()
    })

    const dialog = await openAdministerDialog(page)
    await dialog.getByRole('button', { name: /^Save$/i }).click()

    // Accept the "confirm GIVEN" prompt. We do NOT use the confirmDialogAccept helper here
    // because its overlay-clearing wait would block ~5s on the still-open (error case) modal,
    // outliving the error toast (5s life) before we can assert it.
    const confirmDialog = page.locator('.p-confirmdialog').first()
    await expect(confirmDialog).toBeVisible({ timeout: 5000 })
    await page.waitForTimeout(300)
    await page.evaluate(() => {
      const btn = document.querySelector(
        '.p-confirmdialog .p-confirmdialog-accept-button'
      ) as HTMLButtonElement | null
      btn?.click()
    })

    // The error surfaces with the warehouse name so the nurse knows which bodega is empty.
    await expect(page.locator('.p-toast')).toContainText(/ENFERMERIA/, { timeout: 5000 })
    // The dialog remains open so she can retry / change status.
    await expect(dialog).toBeVisible()
  })
})

// ── Scenario 3: Maintenance charge -> admission bill ──

test.describe('Warehouse — maintenance consumable charge', () => {
  test('maintenance charges a consumable to an admission', async ({ page }) => {
    await setupAuth(page, maintenanceUser)
    // Server scopes the catalog to the maintenance user's assigned bodega.
    await mockWarehouses(page, [mantenimiento1])
    await mockItems(page)
    await mockAdmissionsList(page)
    await page.route('**/api/v1/warehouse-transfers?*', route => route.fulfill(ok(page0([]))))

    let posted: Record<string, unknown> | null = null
    await page.route('**/api/v1/warehouse-charges', async route => {
      if (route.request().method() === 'POST') {
        posted = (await route.request().postDataJSON()) as Record<string, unknown>
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 7,
              warehouse: mantenimiento1,
              item: { id: gasas.id, name: gasas.name, sku: gasas.sku },
              admission: { id: 10, patientName: 'Juan Pérez García', roomNumber: '101' },
              quantity: 1,
              amount: 5,
              reason: 'Broken towel',
              notes: null,
              chargeId: 555,
              createdBy: null,
              createdAt: '2026-05-29T10:00:00'
            }
          })
        })
        return
      }
      await route.continue()
    })

    await page.goto('/warehouse-charges')
    await page.getByRole('button', { name: /Charge Consumable/i }).first().click()

    const dialog = page.locator('[role="dialog"]').filter({ hasText: /Charge Consumable/i })
    await expect(dialog).toBeVisible()

    // Selects in order: warehouse, item, admission.
    await selectOption(page, dialog, 0, /MANTENIMIENTO_1/)
    await selectOption(page, dialog, 1, /Gasas/)
    await selectOption(page, dialog, 2, /Juan Pérez/)
    await dialog.locator('.field').filter({ hasText: 'Reason' }).locator('input').fill('Broken towel')

    await dialog.getByRole('button', { name: /^Save$/i }).click()

    await expect.poll(() => posted, { timeout: 5000 }).not.toBeNull()
    expect(posted).toMatchObject({
      warehouseId: mantenimiento1.id,
      itemId: gasas.id,
      admissionId: 10,
      quantity: 1,
      reason: 'Broken towel'
    })
    await expect(page.locator('.p-toast')).toContainText(/Consumable charged successfully/i)
  })

  test("the charge appears as a SERVICE line on the admission's bill", async ({ page }) => {
    // The maintenance charge creates a PatientCharge of type SERVICE; verified here from
    // the billing view (which a billing-capable user — e.g. admin — can read).
    await setupAuth(page, adminUser)
    await page.route('**/api/v1/admissions/10/charges', route =>
      route.fulfill(
        ok([
          {
            id: 555,
            admissionId: 10,
            chargeType: 'SERVICE',
            description: 'Gasas estériles — Broken towel',
            quantity: 1,
            unitPrice: 5,
            totalAmount: 5,
            inventoryItemName: 'Gasas estériles',
            roomNumber: null,
            invoiced: false,
            reason: 'Broken towel',
            chargeDate: '2026-05-29',
            createdAt: '2026-05-29T10:00:00',
            createdByName: 'Mario Fix'
          }
        ])
      )
    )

    await page.goto('/admissions/10/charges')
    await expect(page.getByText(/Broken towel/i)).toBeVisible()
    // chargeType renders via the translated label (billing.chargeTypes.SERVICE -> "Service").
    await expect(page.getByText('Service').first()).toBeVisible()
  })
})

// ── Scenario 4: Scope denials ──

test.describe('Warehouse — scope denials', () => {
  test('NURSE cannot issue a transfer (New Transfer action hidden)', async ({ page }) => {
    await setupAuth(page, nurseUser)
    await mockWarehouses(page, [enfermeria])
    await page.route('**/api/v1/warehouse-transfers?*', route => route.fulfill(ok(page0([]))))

    // NURSE has warehouse-transfer:read, so the list is reachable…
    await page.goto('/warehouse-transfers')
    await expect(page.getByRole('heading', { name: /Transfers/i })).toBeVisible()
    // …but lacks warehouse-transfer:create, so cannot issue a transfer out of ADMINISTRACION.
    await expect(page.getByRole('button', { name: /New Transfer/i })).toHaveCount(0)
  })

  test('NURSE cannot reach the warehouse admin screen (redirected away)', async ({ page }) => {
    await setupAuth(page, nurseUser)
    await page.route('**/api/v1/rooms/occupancy', route =>
      route.fulfill(ok({ rooms: [], totalBeds: 0, occupiedBeds: 0, availableBeds: 0, occupancyPercent: 0 }))
    )

    // /warehouses requires warehouse:create — the guard redirects to the dashboard,
    // which in turn routes a nurse to the bed occupancy screen (their default landing).
    await page.goto('/warehouses')
    await expect(page).toHaveURL(/\/bed-occupancy/, { timeout: 10000 })
  })

  test('MAINTENANCE only sees its assigned warehouse', async ({ page }) => {
    await setupAuth(page, maintenanceUser)
    // The backend returns only the bodega(s) assigned to this user.
    await mockWarehouses(page, [mantenimiento1])
    await page.route('**/api/v1/warehouse-transfers?*', route => route.fulfill(ok(page0([]))))

    await page.goto('/warehouse-charges')
    await expect(page.getByText('MANTENIMIENTO_1')).toBeVisible()
    // A non-assigned warehouse must never surface in the maintenance UI.
    await expect(page.getByText('ADMINISTRACION')).toHaveCount(0)
    await expect(page.getByText('ENFERMERIA')).toHaveCount(0)
  })
})
