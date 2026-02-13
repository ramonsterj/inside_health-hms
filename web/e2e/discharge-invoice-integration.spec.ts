import { test, expect } from '@playwright/test'
import { confirmDialogAccept, waitForOverlaysToClear } from './utils/test-helpers'

const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  firstName: 'Admin',
  lastName: 'User',
  roles: ['ADMIN'],
  permissions: [
    'admission:read',
    'admission:update',
    'billing:read',
    'billing:create',
    'invoice:read',
    'invoice:create'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockReadOnlyUser = {
  id: 2,
  username: 'viewer',
  email: 'viewer@example.com',
  firstName: 'View',
  lastName: 'Only',
  roles: ['USER'],
  permissions: ['admission:read', 'billing:read', 'invoice:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockNoBillingUser = {
  id: 3,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Nurse',
  lastName: 'User',
  roles: ['NURSE'],
  permissions: ['admission:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockPatient = {
  id: 1,
  firstName: 'Carlos',
  lastName: 'Martinez',
  age: 55,
  idDocumentNumber: '9876543210101'
}

const mockActiveAdmission = {
  id: 10,
  patient: mockPatient,
  triageCode: { id: 1, code: 'B', color: '#FFA500', description: 'Urgent', displayOrder: 2 },
  room: { id: 1, number: '201', type: 'SHARED', capacity: 4, availableBeds: 2 },
  treatingPhysician: { id: 4, firstName: 'Carlos', lastName: 'Lopez', salutation: 'Dr.' },
  admissionDate: '2026-02-01T08:00:00',
  dischargeDate: null,
  status: 'ACTIVE',
  type: 'HOSPITALIZATION',
  hasConsentDocument: false,
  consultingPhysicians: [],
  createdAt: '2026-02-01T08:05:00',
  createdBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' },
  updatedAt: '2026-02-01T08:05:00',
  updatedBy: { id: 1, username: 'admin', firstName: 'Admin', lastName: 'User' }
}

const mockDischargedAdmission = {
  ...mockActiveAdmission,
  status: 'DISCHARGED',
  dischargeDate: '2026-02-07T15:00:00'
}

const mockInvoice = {
  id: 1,
  invoiceNumber: 'INV-2026-0010',
  admissionId: 10,
  patientName: 'Carlos Martinez',
  admissionDate: '2026-02-01',
  dischargeDate: '2026-02-07',
  totalAmount: 1250.0,
  chargeCount: 5,
  chargeSummary: [
    { chargeType: 'ROOM', count: 6, subtotal: 600.0 },
    { chargeType: 'MEDICATION', count: 3, subtotal: 150.0 },
    { chargeType: 'SERVICE', count: 2, subtotal: 500.0 }
  ],
  generatedAt: '2026-02-07T15:05:00',
  generatedByName: 'Admin User'
}

async function setupAuth(
  page: import('@playwright/test').Page,
  user: typeof mockAdminUser
) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user
  )
}

async function setupCommonMocks(
  page: import('@playwright/test').Page,
  user: typeof mockAdminUser
) {
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

async function setupAdmissionStubs(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/admissions/10/documents', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [] })
    })
  })

  await page.route('**/api/v1/admissions/10/clinical-history', async (route) => {
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, message: 'Not found' })
    })
  })

  await page.route('**/api/v1/admissions/10/progress-notes*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          content: [],
          page: { totalElements: 0, totalPages: 0, size: 10, number: 0 }
        }
      })
    })
  })

  await page.route('**/api/v1/admissions/10/medical-orders', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          orders: {
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
        }
      })
    })
  })
}

test.describe('Discharge-to-Invoice Integration', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('discharges patient then views auto-generated invoice', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)
    await setupAdmissionStubs(page)

    let discharged = false

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: discharged ? mockDischargedAdmission : mockActiveAdmission
          })
        })
      } else {
        await route.continue()
      }
    })

    await page.route('**/api/v1/admissions/10/discharge', async (route) => {
      if (route.request().method() === 'POST') {
        discharged = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDischargedAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/doctors', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.goto('/admissions/10')

    await expect(page.getByText('Carlos Martinez')).toBeVisible()
    await waitForOverlaysToClear(page)

    await page.getByRole('button', { name: /Discharge|Alta/i }).click()
    await confirmDialogAccept(page)

    await expect(page.getByText(/discharged|dado de alta/i).first()).toBeVisible({
      timeout: 10000
    })

    await page.route('**/api/v1/admissions/10/invoice', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockInvoice })
        })
      }
    })

    await page.goto('/admissions/10/invoice')

    await expect(page.getByText('INV-2026-0010')).toBeVisible()
  })

  test('generate invoice button disabled when admission is still active', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/10/invoice', async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          error: { code: 'NOT_FOUND', message: 'Invoice not found' }
        })
      })
    })

    await page.goto('/admissions/10/invoice')

    await expect(
      page.getByText('No invoice has been generated for this admission yet.')
    ).toBeVisible()

    const generateBtn = page.getByRole('button', { name: 'Generate Invoice' })
    await expect(generateBtn).toBeVisible()
    await expect(generateBtn).toBeDisabled()
  })

  test('generate invoice button enabled after discharge and clicking generates invoice', async ({
    page
  }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDischargedAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/10/invoice', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockInvoice })
        })
      } else {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            error: { code: 'NOT_FOUND', message: 'Invoice not found' }
          })
        })
      }
    })

    await page.goto('/admissions/10/invoice')

    const generateBtn = page.getByRole('button', { name: 'Generate Invoice' })
    await expect(generateBtn).toBeVisible()
    await expect(generateBtn).toBeEnabled()

    await generateBtn.click()

    const dialog = page.locator('.p-dialog')
    await expect(dialog).toBeVisible({ timeout: 5000 })

    await dialog.getByRole('button', { name: 'Generate' }).click()

    const toast = page.locator('.p-toast')
    await expect(toast).toBeVisible({ timeout: 10000 })
  })

  test('billing section visible with correct permissions', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/doctors', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.goto('/admissions/10')

    await expect(page.getByText('Carlos Martinez')).toBeVisible()

    await expect(page.getByText(/Billing|Facturación/i)).toBeVisible()
    await expect(page.getByText(/Charges|Cargos/i)).toBeVisible()
    await expect(page.getByText(/Balance|Saldo/i)).toBeVisible()
    await expect(page.getByText(/Invoice|Factura/i)).toBeVisible()
  })

  test('billing section hidden for user without billing:read', async ({ page }) => {
    await setupAuth(page, mockNoBillingUser)
    await setupCommonMocks(page, mockNoBillingUser)

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/doctors', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.goto('/admissions/10')

    await expect(page.getByText('Carlos Martinez')).toBeVisible()

    await expect(page.getByText(/Billing|Facturación/i)).not.toBeVisible()
  })

  test('invoice button hidden for user without invoice:read', async ({ page }) => {
    const billingOnlyUser = {
      ...mockAdminUser,
      id: 4,
      username: 'billing-only',
      email: 'billing@example.com',
      firstName: 'Billing',
      lastName: 'Staff',
      roles: ['USER'],
      permissions: ['admission:read', 'billing:read']
    }

    await setupAuth(page, billingOnlyUser)
    await setupCommonMocks(page, billingOnlyUser)

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/doctors', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.goto('/admissions/10')

    await expect(page.getByText('Carlos Martinez')).toBeVisible()

    await expect(page.getByText(/Charges|Cargos/i)).toBeVisible()
    await expect(page.getByText(/Invoice|Factura/i)).not.toBeVisible()
  })

  test('generate invoice button hidden for user without invoice:create', async ({ page }) => {
    await setupAuth(page, mockReadOnlyUser)
    await setupCommonMocks(page, mockReadOnlyUser)

    await page.route('**/api/v1/admissions/10/invoice', async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          error: { code: 'NOT_FOUND', message: 'Invoice not found' }
        })
      })
    })

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      }
    })

    await page.goto('/admissions/10/invoice')

    await expect(
      page.getByText('No invoice has been generated for this admission yet.')
    ).toBeVisible()

    await expect(page.getByRole('button', { name: 'Generate Invoice' })).not.toBeVisible()
  })

  test('full workflow - discharge then charges then balance then invoice navigation', async ({
    page
  }) => {
    await setupAuth(page, mockAdminUser)
    await setupCommonMocks(page, mockAdminUser)
    await setupAdmissionStubs(page)

    let discharged = false

    const mockCharges = [
      {
        id: 1,
        admissionId: 10,
        chargeType: 'ROOM',
        description: 'Room 201 - Day 1',
        quantity: 1,
        unitPrice: 100.0,
        totalAmount: 100.0,
        invoiced: false,
        chargeDate: '2026-02-01',
        createdAt: '2026-02-01T08:00:00',
        createdByName: 'System'
      }
    ]

    const mockBalance = {
      admissionId: 10,
      patientName: 'Carlos Martinez',
      admissionDate: '2026-02-01',
      totalBalance: 100.0,
      dailyBreakdown: [
        {
          date: '2026-02-01',
          charges: [
            {
              id: 1,
              chargeType: 'ROOM',
              description: 'Room 201',
              quantity: 1,
              unitPrice: 100.0,
              totalAmount: 100.0
            }
          ],
          dailyTotal: 100.0,
          cumulativeTotal: 100.0
        }
      ]
    }

    await page.route('**/api/v1/admissions/10', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: discharged ? mockDischargedAdmission : mockActiveAdmission
          })
        })
      } else {
        await route.continue()
      }
    })

    await page.route('**/api/v1/admissions/10/discharge', async (route) => {
      if (route.request().method() === 'POST') {
        discharged = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDischargedAdmission })
        })
      }
    })

    await page.route('**/api/v1/admissions/doctors', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] })
      })
    })

    await page.route('**/api/v1/admissions/10/charges', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockCharges })
      })
    })

    await page.route('**/api/v1/admissions/10/balance', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockBalance })
      })
    })

    await page.route('**/api/v1/admissions/10/invoice', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockInvoice })
        })
      } else if (route.request().method() === 'GET') {
        if (discharged) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ success: true, data: mockInvoice })
          })
        } else {
          await route.fulfill({
            status: 404,
            contentType: 'application/json',
            body: JSON.stringify({
              error: { code: 'NOT_FOUND', message: 'Invoice not found' }
            })
          })
        }
      }
    })

    await page.goto('/admissions/10')

    await expect(page.getByText('Carlos Martinez')).toBeVisible()
    await waitForOverlaysToClear(page)

    await page.getByRole('button', { name: /Discharge|Alta/i }).click()
    await confirmDialogAccept(page)

    await expect(page.getByText(/discharged|dado de alta/i).first()).toBeVisible({
      timeout: 10000
    })

    await page.goto('/admissions/10/charges')
    await expect(page.getByText('Room 201 - Day 1')).toBeVisible()

    await page.goto('/admissions/10/balance')
    await expect(page.getByText('Carlos Martinez')).toBeVisible()

    await page.goto('/admissions/10/invoice')
    await expect(page.getByText('INV-2026-0010')).toBeVisible()
  })
})
