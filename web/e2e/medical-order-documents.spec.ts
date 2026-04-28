import { test, expect } from '@playwright/test'
import {
  waitForOverlaysToClear,
  waitForMedicalRecordTabs,
  selectMedicalRecordTab,
  expandAccordionPanel
} from './utils/test-helpers'

// Mock users with different permission levels
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
    'clinical-history:read',
    'progress-note:read',
    'medical-order:create',
    'medical-order:read',
    'medical-order:discontinue',
    'medical-order:upload-document'
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
    'progress-note:read',
    'medical-order:read',
    'medical-order:upload-document'
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
  roles: ['ADMIN'],
  permissions: [
    'admission:read',
    'clinical-history:read',
    'progress-note:read',
    'medical-order:create',
    'medical-order:read',
    'medical-order:update',
    'medical-order:discontinue',
    'medical-order:upload-document',
    'medical-order:delete-document'
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

const mockLabOrder = {
  id: 10,
  admissionId: 1,
  category: 'LABORATORIOS',
  startDate: '2026-01-24',
  endDate: null,
  medication: null,
  dosage: null,
  route: null,
  frequency: null,
  schedule: null,
  observations: 'Complete blood count',
  status: 'AUTORIZADO',
  discontinuedAt: null,
  discontinuedBy: null,
  inventoryItemId: null,
  inventoryItemName: null,
  documentCount: 0,
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

const mockLabOrderWithDocs = {
  ...mockLabOrder,
  documentCount: 2
}

const mockDocuments = [
  {
    id: 1,
    displayName: 'Blood Work Results',
    fileName: 'lab-result.pdf',
    contentType: 'application/pdf',
    fileSize: 102400,
    hasThumbnail: false,
    thumbnailUrl: null,
    downloadUrl: '/v1/admissions/1/medical-orders/10/documents/1/file',
    createdAt: '2026-02-10T14:30:00Z',
    createdBy: {
      id: 2,
      salutation: 'Dra.',
      firstName: 'Maria',
      lastName: 'Garcia',
      roles: ['DOCTOR']
    }
  },
  {
    id: 2,
    displayName: 'X-Ray Scan',
    fileName: 'xray.jpg',
    contentType: 'image/jpeg',
    fileSize: 204800,
    hasThumbnail: true,
    thumbnailUrl: '/v1/admissions/1/medical-orders/10/documents/2/thumbnail',
    downloadUrl: '/v1/admissions/1/medical-orders/10/documents/2/file',
    createdAt: '2026-02-10T15:00:00Z',
    createdBy: {
      id: 3,
      salutation: 'Lic.',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    }
  }
]

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
}

// Create grouped orders response
function createGroupedOrders(orders: typeof mockLabOrder[]) {
  const grouped: Record<string, typeof mockLabOrder[]> = {
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

test.describe('Medical Order Documents', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('doctor sees attach button on lab order', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([mockLabOrder]) })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Expand lab orders category
    await expandAccordionPanel(page, /Lab|Laboratorios/i)

    // Should see attach button
    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await expect(orderCard.getByRole('button', { name: /Attach|Adjuntar/i })).toBeVisible()
  })

  test('document count badge visible when documents exist', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockLabOrderWithDocs])
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Expand lab orders category
    await expandAccordionPanel(page, /Lab|Laboratorios/i)

    // Should see document count badge
    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await expect(orderCard.locator('.p-badge')).toBeVisible()
  })

  test('expanding attachments loads and displays documents', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockLabOrderWithDocs])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/10/documents', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDocuments })
        })
      }
    })

    // Mock thumbnail endpoint
    await page.route('**/api/v1/admissions/1/medical-orders/10/documents/*/thumbnail', async route => {
      // Return a tiny 1x1 PNG
      const pngBytes = Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==',
        'base64'
      )
      await route.fulfill({
        status: 200,
        contentType: 'image/png',
        body: pngBytes
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Expand lab orders category
    await expandAccordionPanel(page, /Lab|Laboratorios/i)

    // Click the attachments button to expand documents
    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await waitForOverlaysToClear(page)

    const attachmentsButton = orderCard.getByRole('button', { name: /2/i }).filter({ has: page.locator('.pi-images') })
    await attachmentsButton.click()

    // Should see the document thumbnails
    await expect(orderCard.locator('.attachments-section')).toBeVisible()
    await expect(orderCard.locator('.document-thumbnail')).toHaveCount(2)
  })

  test('nurse without delete permission cannot see delete button', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockLabOrderWithDocs])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/10/documents', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocuments })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/10/documents/*/thumbnail', async route => {
      await route.fulfill({ status: 404 })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    await expandAccordionPanel(page, /Lab|Laboratorios/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await waitForOverlaysToClear(page)

    // Expand attachments
    const attachmentsButton = orderCard.getByRole('button', { name: /2/i }).filter({ has: page.locator('.pi-images') })
    await attachmentsButton.click()

    await expect(orderCard.locator('.attachments-section')).toBeVisible()

    // Nurse should NOT see delete button (no medical-order:delete-document permission)
    await expect(orderCard.locator('.thumbnail-actions .pi-trash')).not.toBeVisible()
  })

  test('admin sees delete button on documents', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: createGroupedOrders([mockLabOrderWithDocs])
        })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/10/documents', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockDocuments })
      })
    })

    await page.route('**/api/v1/admissions/1/medical-orders/10/documents/*/thumbnail', async route => {
      await route.fulfill({ status: 404 })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    await expandAccordionPanel(page, /Lab|Laboratorios/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await waitForOverlaysToClear(page)

    // Expand attachments
    const attachmentsButton = orderCard.getByRole('button', { name: /2/i }).filter({ has: page.locator('.pi-images') })
    await attachmentsButton.click()

    await expect(orderCard.locator('.attachments-section')).toBeVisible()

    // Admin SHOULD see delete button
    await expect(orderCard.locator('.thumbnail-actions .pi-trash').first()).toBeVisible()
  })

  test('attach button opens upload dialog', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([mockLabOrder]) })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    await expandAccordionPanel(page, /Lab|Laboratorios/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()
    await waitForOverlaysToClear(page)

    // Click attach button
    await orderCard.getByRole('button', { name: /Attach|Adjuntar/i }).click()

    // Upload dialog should open
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/Attach Document|Adjuntar Documento/i)).toBeVisible()
  })

  test('no attach button when user lacks upload permission', async ({ page }) => {
    const userWithoutUpload = {
      ...mockNurseUser,
      permissions: mockNurseUser.permissions.filter(p => p !== 'medical-order:upload-document')
    }

    await setupAuth(page, userWithoutUpload)
    await setupUserMock(page, userWithoutUpload)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: createGroupedOrders([mockLabOrder]) })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    await expandAccordionPanel(page, /Lab|Laboratorios/i)

    const orderCard = page.locator('.medical-order-card').first()
    await expect(orderCard).toBeVisible()

    // Should NOT see attach button
    await expect(orderCard.getByRole('button', { name: /Attach|Adjuntar/i })).not.toBeVisible()
  })
})
