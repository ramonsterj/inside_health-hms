import { test, expect } from '@playwright/test'

type MockUser = {
  id: number
  username: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
  permissions: string[]
  status: string
  emailVerified: boolean
  createdAt: string
  localePreference: string
}

const mockAdmission = {
  id: 1,
  patient: {
    id: 1,
    firstName: 'Juan',
    lastName: 'Perez',
    age: 45,
    idDocumentNumber: '1234567890101'
  },
  triageCode: null,
  room: null,
  treatingPhysician: { id: 3, firstName: 'Maria', lastName: 'Garcia', salutation: 'DR' },
  admissionDate: '2026-05-09T14:30:00',
  dischargeDate: null,
  status: 'ACTIVE',
  type: 'AMBULATORY',
  hasConsentDocument: false,
  consultingPhysicians: [],
  createdAt: '2026-05-09T14:30:00',
  createdBy: { id: 2, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' },
  updatedAt: '2026-05-09T14:30:00',
  updatedBy: { id: 2, username: 'receptionist', firstName: 'Reception', lastName: 'Staff' }
}

const adminStaffUser = {
  id: 2,
  username: 'receptionist',
  email: 'receptionist@example.com',
  firstName: 'Reception',
  lastName: 'Staff',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: ['admission:read', 'admission:export-pdf'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00',
  localePreference: 'en'
}

const doctorUser = {
  id: 3,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Maria',
  lastName: 'Garcia',
  roles: ['DOCTOR'],
  permissions: ['admission:read'],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00',
  localePreference: 'en'
}

async function setupAuth(page: import('@playwright/test').Page, user: MockUser) {
  await page.addInitScript((userData) => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

async function setupAdmissionMocks(page: import('@playwright/test').Page, user: MockUser) {
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
      body: JSON.stringify({ success: true, data: { accessToken: 'new-token' } })
    })
  })
  await page.route('**/api/v1/admissions/doctors', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{"success":true,"data":[]}' })
  })
  await page.route('**/api/v1/admissions/1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockAdmission })
    })
  })
  await page.route('**/api/v1/admissions/1/**', async (route) => {
    if (route.request().url().endsWith('/export.pdf')) {
      await route.fallback()
      return
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: '{"success":true,"data":null}' })
  })
}

test.describe('Admission PDF export', () => {
  test('administrative staff can download the export', async ({ page }) => {
    await setupAuth(page, adminStaffUser)
    await setupAdmissionMocks(page, adminStaffUser)
    await page.route('**/api/v1/admissions/1/export.pdf', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/pdf',
        headers: { 'Content-Disposition': 'attachment; filename="admission-1-perez-20260511-1430.pdf"' },
        body: Buffer.from('%PDF-1.4\nmock pdf')
      })
    })

    await page.goto('/admissions/1')
    const button = page.getByTestId('admission-export-button')
    await expect(button).toBeVisible()

    const downloadPromise = page.waitForEvent('download')
    await button.click()
    const download = await downloadPromise

    expect(download.suggestedFilename()).toBe('admission-1-perez-20260511-1430.pdf')
  })

  test('doctor does not see the export button', async ({ page }) => {
    await setupAuth(page, doctorUser)
    await setupAdmissionMocks(page, doctorUser)

    await page.goto('/admissions/1')

    await expect(page.getByTestId('admission-export-button')).toHaveCount(0)
  })
})
