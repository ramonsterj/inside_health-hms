import { test, expect } from '@playwright/test'
import {
  confirmDialogAccept,
  waitForOverlaysToClear,
  waitForMedicalRecordTabs,
  fillRichTextEditor,
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

const mockClinicalHistory = {
  id: 1,
  admissionId: 1,
  reasonForAdmission: '<p>Patient presents with severe anxiety and depression</p>',
  historyOfPresentIllness: '<p>Symptoms began 3 weeks ago after job loss</p>',
  psychiatricHistory: '<p>Previous episode of depression in 2020</p>',
  medicalHistory: '<p>Hypertension, controlled with medication</p>',
  familyHistory: '<p>Mother with depression, father with alcohol use disorder</p>',
  personalHistory: null,
  substanceUseHistory: '<p>Social alcohol use, no illicit substances</p>',
  legalHistory: null,
  socialHistory: '<p>Married, two children, recently unemployed</p>',
  developmentalHistory: null,
  educationalOccupationalHistory: '<p>College graduate, worked as accountant for 15 years</p>',
  sexualHistory: null,
  religiousSpiritualHistory: null,
  mentalStatusExam: '<p>Alert, oriented x3, anxious mood, congruent affect</p>',
  physicalExam: '<p>BP 140/90, HR 88, otherwise unremarkable</p>',
  diagnosticImpression: '<p>Major Depressive Disorder, moderate severity</p>',
  treatmentPlan: '<p>1. Start SSRI medication\n2. Individual therapy twice weekly</p>',
  riskAssessment: '<p>Low suicide risk, no active ideation</p>',
  prognosis: '<p>Good with treatment compliance</p>',
  informedConsentNotes: null,
  additionalNotes: null,
  createdAt: '2026-01-23T11:00:00',
  createdBy: { id: 2, salutation: 'Dra.', firstName: 'Maria', lastName: 'Garcia', roles: ['DOCTOR'] },
  updatedAt: '2026-01-23T11:00:00',
  updatedBy: { id: 2, salutation: 'Dra.', firstName: 'Maria', lastName: 'Garcia', roles: ['DOCTOR'] }
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

  // Mock progress notes for tabs
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

  // Mock medical orders for tabs
  await page.route('**/api/v1/admissions/1/medical-orders', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { orders: {} } })
    })
  })
}

test.describe('Medical Record - Clinical History', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('doctor can create clinical history', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    // Track created state
    let clinicalHistoryCreated = false

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      if (route.request().method() === 'GET') {
        if (clinicalHistoryCreated) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ success: true, data: mockClinicalHistory })
          })
        } else {
          await route.fulfill({
            status: 404,
            contentType: 'application/json',
            body: JSON.stringify({ success: false, message: 'Clinical history not found' })
          })
        }
      } else if (route.request().method() === 'POST') {
        clinicalHistoryCreated = true
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockClinicalHistory })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)

    // Should see the create button in empty state
    await expect(page.getByRole('button', { name: /Create Clinical History/i })).toBeVisible()

    // Click create button
    await page.getByRole('button', { name: /Create Clinical History/i }).click()

    // Fill in some data - expand the Presentation section
    await expandAccordionPanel(page, /Presentation|Presentación/i)

    // Fill the reason for admission field
    const reasonEditor = page.locator('.section-fields').first().locator('.ql-editor').first()
    await reasonEditor.click()
    await reasonEditor.fill('Patient presents with severe anxiety')

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should see success message
    await expect(page.getByText(/created successfully|creado exitosamente/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('doctor can view clinical history', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockClinicalHistory })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)

    // Should see the clinical history title
    await expect(page.getByRole('heading', { name: /Clinical History/i })).toBeVisible()

    // Should see the content - expand a section
    await expandAccordionPanel(page, /Presentation|Presentación/i)

    // Should see the field content
    await expect(page.getByText(/severe anxiety and depression/i)).toBeVisible()
  })

  test('doctor cannot edit clinical history', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockClinicalHistory })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)

    // Should NOT see edit button (doctor has clinical-history:read but not clinical-history:update)
    await expect(
      page.locator('.clinical-history-view').getByRole('button', { name: /Edit|Editar/i })
    ).not.toBeVisible()
  })

  test('nurse cannot create clinical history', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Clinical history not found' })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)

    // Should see empty state but no create button (nurse only has read permission)
    await expect(page.getByText(/No clinical history has been created/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /Create Clinical History/i })).not.toBeVisible()
  })

  test('nurse can view clinical history', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: mockClinicalHistory })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)

    // Should see the clinical history
    await expect(page.getByRole('heading', { name: /Clinical History/i })).toBeVisible()

    // Should NOT see edit button
    await expect(
      page.locator('.clinical-history-view').getByRole('button', { name: /Edit|Editar/i })
    ).not.toBeVisible()
  })

  test('admin can edit clinical history', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    const updatedClinicalHistory = {
      ...mockClinicalHistory,
      reasonForAdmission: '<p>Updated reason for admission</p>',
      updatedAt: '2026-01-24T10:00:00'
    }

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockClinicalHistory })
        })
      } else if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: updatedClinicalHistory })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await waitForOverlaysToClear(page)

    // Should see edit button (admin has clinical-history:update)
    const editButton = page
      .locator('.clinical-history-view')
      .getByRole('button', { name: /Edit|Editar/i })
    await expect(editButton).toBeVisible()

    // Click edit
    await editButton.click()

    // Should see the form with existing data
    await expect(page.getByRole('heading', { name: /Edit Clinical History/i })).toBeVisible()

    // Submit the form
    await page.getByRole('button', { name: /Save|Guardar/i }).click()

    // Should see success message
    await expect(page.getByText(/updated successfully|actualizado exitosamente/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('empty state shows create button for doctors only', async ({ page }) => {
    // Test with doctor - should see button
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, message: 'Clinical history not found' })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)

    // Doctor should see create button
    await expect(page.getByRole('button', { name: /Create Clinical History/i })).toBeVisible()
  })

  test('rich text formatting is preserved', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    // Clinical history with HTML formatting
    const formattedHistory = {
      ...mockClinicalHistory,
      reasonForAdmission: '<p><strong>Bold text</strong> and <em>italic text</em></p>'
    }

    await page.route('**/api/v1/admissions/1/clinical-history', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: formattedHistory })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)

    // Expand section
    await expandAccordionPanel(page, /Presentation|Presentación/i)

    // Check that formatted content is displayed (HTML is rendered)
    const fieldValue = page.locator('.field-value').first()
    await expect(fieldValue.locator('strong')).toHaveText('Bold text')
    await expect(fieldValue.locator('em')).toHaveText('italic text')
  })
})
