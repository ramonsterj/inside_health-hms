import { test, expect } from '@playwright/test'
import { waitForMedicalRecordTabs, selectMedicalRecordTab } from './utils/test-helpers'

// Mock user data
//
// Per spec v1.3 + v1.4 (nursing-module.md), only ADMIN holds `nursing-note:update`
// (after V096) and `vital-sign:update` (after V097). DOCTOR / NURSE / CHIEF_NURSE
// can create and read both record types but cannot edit.
const mockNurseUser = {
  id: 4,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Ana',
  lastName: 'Lopez',
  salutation: 'LICDA',
  roles: ['NURSE'],
  permissions: [
    'admission:read',
    'nursing-note:read',
    'nursing-note:create',
    'vital-sign:read',
    'vital-sign:create',
    'clinical-history:read',
    'psychotherapy-activity:read'
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
    'nursing-note:read',
    'nursing-note:create',
    'nursing-note:update',
    'vital-sign:read',
    'vital-sign:create',
    'vital-sign:update',
    'clinical-history:read',
    'clinical-history:update'
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
  salutation: 'DRA',
  roles: ['DOCTOR'],
  permissions: [
    'admission:read',
    'nursing-note:read',
    'nursing-note:create',
    'vital-sign:read',
    'vital-sign:create',
    'clinical-history:read'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockChiefNurseUser = {
  id: 5,
  username: 'chiefnurse',
  email: 'chiefnurse@example.com',
  firstName: 'Carmen',
  lastName: 'Flores',
  salutation: 'SRA',
  roles: ['CHIEF_NURSE'],
  permissions: [
    'admission:read',
    'nursing-note:read',
    'nursing-note:create',
    'vital-sign:read',
    'vital-sign:create',
    'clinical-history:read'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

const mockActiveAdmission = {
  id: 100,
  patient: {
    id: 1,
    firstName: 'John',
    lastName: 'Doe',
    age: 35,
    sex: 'MALE'
  },
  type: 'HOSPITALIZATION',
  status: 'ACTIVE',
  admissionDate: '2026-02-01T10:00:00Z',
  treatingPhysician: {
    id: 3,
    salutation: 'DR',
    firstName: 'Maria',
    lastName: 'Garcia'
  },
  room: { id: 1, number: '101', type: 'PRIVATE' },
  triageCode: { id: 1, code: 'B', color: '#FFA500', description: 'Urgent' },
  consultingPhysicians: [],
  documents: []
}

const mockDischargedAdmission = {
  ...mockActiveAdmission,
  status: 'DISCHARGED',
  dischargeDate: '2026-02-05T16:00:00Z'
}

// Default `canEdit: false` reflects what a non-admin viewer sees under the
// admin-only update policy (spec v1.3). Admin tests override this per-test.
const mockNursingNotes = [
  {
    id: 2,
    admissionId: 100,
    description: 'Afternoon check: Medication administered as prescribed.',
    createdAt: '2026-02-05T14:00:00Z',
    updatedAt: '2026-02-05T14:00:00Z',
    createdBy: {
      id: 4,
      salutation: 'LICDA',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    },
    updatedBy: null,
    canEdit: false
  },
  {
    id: 1,
    admissionId: 100,
    description: 'Patient resting comfortably. Vital signs stable.',
    createdAt: '2026-02-05T10:30:00Z',
    updatedAt: '2026-02-05T10:30:00Z',
    createdBy: {
      id: 4,
      salutation: 'LICDA',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    },
    updatedBy: null,
    canEdit: false
  }
]

// Default `canEdit: false` reflects what a non-admin viewer sees under the
// admin-only update policy (spec v1.4, V097). Admin tests override this per-test.
const mockVitalSigns = [
  {
    id: 2,
    admissionId: 100,
    recordedAt: '2026-02-05T14:00:00Z',
    systolicBp: 125,
    diastolicBp: 82,
    heartRate: 75,
    respiratoryRate: 18,
    temperature: 36.8,
    oxygenSaturation: 97,
    glucose: null,
    other: null,
    createdAt: '2026-02-05T14:00:00Z',
    updatedAt: '2026-02-05T14:00:00Z',
    createdBy: {
      id: 4,
      salutation: 'LICDA',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    },
    updatedBy: null,
    canEdit: false
  },
  {
    id: 1,
    admissionId: 100,
    recordedAt: '2026-02-05T10:30:00Z',
    systolicBp: 120,
    diastolicBp: 80,
    heartRate: 72,
    respiratoryRate: 16,
    temperature: 36.5,
    oxygenSaturation: 98,
    glucose: null,
    other: null,
    createdAt: '2026-02-05T10:30:00Z',
    updatedAt: '2026-02-05T10:30:00Z',
    createdBy: {
      id: 4,
      salutation: 'LICDA',
      firstName: 'Ana',
      lastName: 'Lopez',
      roles: ['NURSE']
    },
    updatedBy: null,
    canEdit: false
  }
]

// Helper function to setup authenticated state
async function setupAuth(page: import('@playwright/test').Page, user: typeof mockNurseUser) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

async function setupCommonMocks(page: import('@playwright/test').Page, user: typeof mockNurseUser) {
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

  await page.route('**/api/v1/users/doctors', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [] })
    })
  })

  await page.route('**/api/v1/psychotherapy-categories', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [] })
    })
  })
}

test.describe('Nursing Module', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  // ============ NURSING NOTES TESTS ============

  test.describe('Nursing Notes', () => {
    test('displays empty state when no notes', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      await expect(page.getByText(/No nursing notes recorded/i)).toBeVisible()
    })

    test('displays nursing notes list', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: mockNursingNotes,
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      await expect(
        page.getByText('Afternoon check: Medication administered as prescribed.')
      ).toBeVisible()
      await expect(page.getByText('Patient resting comfortably. Vital signs stable.')).toBeVisible()
    })

    test('can open create nursing note dialog', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: mockNursingNotes,
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      // Click Add Note
      await page.getByRole('button', { name: /Add Note/i }).click()

      // Dialog should open with Add Note title and rich text editor
      await expect(page.getByText(/Add Note/i).first()).toBeVisible()
      await expect(page.locator('.ql-editor')).toBeVisible()
      await expect(page.getByRole('button', { name: /Save/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /Cancel/i })).toBeVisible()
    })

    test('admin can open edit nursing note dialog with existing content', async ({ page }) => {
      // Per spec v1.3, only ADMIN can edit nursing notes — so the edit-flow test
      // runs as ADMIN. The server returns `canEdit: true` only for ADMIN viewers.
      await setupAuth(page, mockAdminUser)
      await setupCommonMocks(page, mockAdminUser)

      const adminVisibleNotes = mockNursingNotes.map(n => ({ ...n, canEdit: true }))

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: adminVisibleNotes,
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      // Admin clicks Edit on the first nursing note (canEdit is true for admin)
      await page.locator('.nursing-note-card').first().getByRole('button', { name: /Edit/i }).click()

      // Dialog should open with Edit Note title and pre-populated content
      await expect(page.getByText(/Edit Note/i).first()).toBeVisible()
      await expect(page.locator('.ql-editor')).toBeVisible()
      await expect(page.locator('.ql-editor')).toContainText('Afternoon check')
    })

    test('nurse cannot edit own nursing notes (no edit button)', async ({ page }) => {
      // Even though the nurse authored the notes, the spec v1.3 rule is admin-only update.
      // The server returns `canEdit: false` for the nurse on every note.
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: mockNursingNotes,
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      await expect(
        page.getByText('Afternoon check: Medication administered as prescribed.')
      ).toBeVisible()
      // No edit button on any nursing-note card for the nurse
      await expect(
        page.locator('.nursing-note-card').getByRole('button', { name: /Edit/i })
      ).toHaveCount(0)
    })

    test('doctor cannot edit nursing notes (no edit button)', async ({ page }) => {
      await setupAuth(page, mockDoctorUser)
      await setupCommonMocks(page, mockDoctorUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: mockNursingNotes,
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      await expect(
        page.locator('.nursing-note-card').getByRole('button', { name: /Edit/i })
      ).toHaveCount(0)
    })

    test('chief nurse can create but not edit nursing notes', async ({ page }) => {
      await setupAuth(page, mockChiefNurseUser)
      await setupCommonMocks(page, mockChiefNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: mockNursingNotes,
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      // Chief nurse holds nursing-note:create — Add Note button is visible.
      await expect(page.getByRole('button', { name: /Add Note/i })).toBeVisible()
      // …but no edit buttons; canEdit is false.
      await expect(
        page.locator('.nursing-note-card').getByRole('button', { name: /Edit/i })
      ).toHaveCount(0)
    })

    test('admin edit button is hidden for discharged admissions', async ({ page }) => {
      // Admission discharged — server returns `canEdit: false` even for ADMIN.
      await setupAuth(page, mockAdminUser)
      await setupCommonMocks(page, mockAdminUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDischargedAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: mockNursingNotes.map(n => ({ ...n, canEdit: false })),
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      // Admin sees no edit button because the admission is discharged.
      await expect(
        page.locator('.nursing-note-card').getByRole('button', { name: /Edit/i })
      ).toHaveCount(0)
    })

    test('hides edit button when canEdit is false', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      const nonEditableNotes = mockNursingNotes.map(n => ({ ...n, canEdit: false }))

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: nonEditableNotes,
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      // Notes should be visible but Edit buttons should not
      await expect(
        page.getByText('Afternoon check: Medication administered as prescribed.')
      ).toBeVisible()
      await expect(page.getByRole('button', { name: /Edit/i })).not.toBeVisible()
    })

    test('hides add button for discharged admissions', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockDischargedAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/nursing-notes*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: mockNursingNotes.map(n => ({ ...n, canEdit: false })),
              page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
            }
          })
        })
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'nursingNotes')

      // Add Note button should not be visible for discharged admissions
      await expect(page.getByRole('button', { name: /Add Note/i })).not.toBeVisible()
    })
  })

  // ============ VITAL SIGNS TESTS ============

  test.describe('Vital Signs - Table View', () => {
    test('displays vital signs in table format', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs/chart*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockVitalSigns })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs*', async route => {
        if (!route.request().url().includes('/chart')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: mockVitalSigns,
                page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
              }
            })
          })
        }
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Should see vital signs data in table
      await expect(page.getByText('120/80')).toBeVisible()
      await expect(page.getByText('125/82')).toBeVisible()
    })

    test('displays blood pressure as systolic/diastolic', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs/chart*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockVitalSigns })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs*', async route => {
        if (!route.request().url().includes('/chart')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: mockVitalSigns,
                page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
              }
            })
          })
        }
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // BP should be formatted as systolic/diastolic
      await expect(page.getByText('120/80')).toBeVisible()
    })

    test('can create vital signs', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs/chart*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [] })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs*', async route => {
        if (route.request().url().includes('/chart')) {
          await route.fallback()
          return
        }
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: mockVitalSigns[0]
            })
          })
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: [],
                page: { totalElements: 0, totalPages: 0, size: 20, number: 0 }
              }
            })
          })
        }
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Click Record Vital Signs
      await page.getByRole('button', { name: 'Record Vital Signs' }).click()

      // Dialog should open with vital sign form fields
      await expect(page.getByText(/Record Vital Signs/i).first()).toBeVisible()
      await expect(page.locator('#systolicBp')).toBeVisible()
      await expect(page.locator('#diastolicBp')).toBeVisible()
      await expect(page.locator('#heartRate')).toBeVisible()
      await expect(page.locator('#respiratoryRate')).toBeVisible()
      await expect(page.locator('#temperature')).toBeVisible()
      await expect(page.locator('#oxygenSaturation')).toBeVisible()
      await expect(page.locator('#glucose')).toBeVisible()
      await expect(page.getByRole('button', { name: /Save/i })).toBeVisible()
    })
  })

  // Per spec v1.4 (nursing-module.md, V097), only ADMIN can update vital signs.
  // Doctors, nurses, and chief nurses are append-only — they can record new
  // readings but cannot edit existing ones. The frontend gates the pencil
  // edit button on `vital-sign:update` permission AND the server-side `canEdit`
  // flag (which is `isAdmin && admission.isActive`).
  test.describe('Vital Signs - Edit Permissions (admin-only)', () => {
    async function setupVitalSignsView(
      page: import('@playwright/test').Page,
      user: typeof mockNurseUser,
      options: { admission?: typeof mockActiveAdmission; canEdit?: boolean } = {}
    ) {
      const admission = options.admission ?? mockActiveAdmission
      const canEdit = options.canEdit ?? false
      const vitalSigns = mockVitalSigns.map(vs => ({ ...vs, canEdit }))

      await setupAuth(page, user)
      await setupCommonMocks(page, user)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: admission })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs/chart*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: vitalSigns })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs*', async route => {
        if (!route.request().url().includes('/chart')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: vitalSigns,
                page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
              }
            })
          })
        }
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })
    }

    test('nurse cannot edit own vital signs (no edit button)', async ({ page }) => {
      // Nurse authored the readings, but the spec v1.4 rule is admin-only update.
      // The server returns canEdit=false for the nurse on every record.
      await setupVitalSignsView(page, mockNurseUser, { canEdit: false })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Vital signs render…
      await expect(page.getByText('120/80')).toBeVisible()
      // …but the pencil edit button is hidden on every row.
      await expect(
        page.locator('.vital-sign-table .p-datatable-tbody button.p-button-icon-only')
      ).toHaveCount(0)
    })

    test('doctor cannot edit vital signs (no edit button)', async ({ page }) => {
      await setupVitalSignsView(page, mockDoctorUser, { canEdit: false })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      await expect(page.getByText('120/80')).toBeVisible()
      await expect(
        page.locator('.vital-sign-table .p-datatable-tbody button.p-button-icon-only')
      ).toHaveCount(0)
    })

    test('chief nurse can record but not edit vital signs', async ({ page }) => {
      await setupVitalSignsView(page, mockChiefNurseUser, { canEdit: false })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Chief nurse holds vital-sign:create — Record button is visible.
      await expect(page.getByRole('button', { name: 'Record Vital Signs' })).toBeVisible()
      // …but no edit pencil; canEdit is false.
      await expect(
        page.locator('.vital-sign-table .p-datatable-tbody button.p-button-icon-only')
      ).toHaveCount(0)
    })

    test('admin can open edit dialog for vital signs on active admission', async ({ page }) => {
      // Server returns canEdit=true only for ADMIN viewers on active admissions.
      await setupVitalSignsView(page, mockAdminUser, { canEdit: true })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Edit pencil is rendered on each data row.
      const editButtons = page.locator(
        '.vital-sign-table .p-datatable-tbody button.p-button-icon-only'
      )
      await expect(editButtons.first()).toBeVisible()
      await editButtons.first().click()

      // The form dialog opens in edit mode and is pre-populated from the row.
      // PrimeVue InputNumber wraps the actual <input> inside a span, so target
      // the inner input element rather than the wrapper.
      await expect(page.getByText(/Edit Vital Signs/i).first()).toBeVisible()
      await expect(page.locator('#systolicBp input')).toHaveValue('125')
      await expect(page.locator('#diastolicBp input')).toHaveValue('82')
    })

    test('admin edit button is hidden for discharged admissions', async ({ page }) => {
      // Discharge protection blocks all writes — server returns canEdit=false even for ADMIN.
      await setupVitalSignsView(page, mockAdminUser, {
        admission: mockDischargedAdmission,
        canEdit: false
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      await expect(page.getByText('120/80')).toBeVisible()
      await expect(
        page.locator('.vital-sign-table .p-datatable-tbody button.p-button-icon-only')
      ).toHaveCount(0)
      // Record button is also hidden for discharged admissions.
      await expect(page.getByRole('button', { name: 'Record Vital Signs' })).not.toBeVisible()
    })
  })

  test.describe('Vital Signs - Charts', () => {
    test('can switch between table and chart view', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs/chart*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockVitalSigns })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs*', async route => {
        if (!route.request().url().includes('/chart')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: mockVitalSigns,
                page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
              }
            })
          })
        }
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Should see Table view by default
      await expect(page.getByText(/Table/i).first()).toBeVisible()
      await expect(page.getByText(/Charts/i).first()).toBeVisible()

      // Click Charts to switch view
      await page
        .getByText(/Charts/i)
        .first()
        .click()
      await page.waitForTimeout(500)

      // Should see chart labels (e.g. Blood Pressure, Heart Rate)
      await expect(page.getByText(/Blood Pressure/i).first()).toBeVisible()
    })

    test('shows empty state for charts when no data', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs/chart*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [] })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs*', async route => {
        if (!route.request().url().includes('/chart')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: [],
                page: { totalElements: 0, totalPages: 0, size: 20, number: 0 }
              }
            })
          })
        }
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Switch to charts
      await page
        .getByText(/Charts/i)
        .first()
        .click()
      await page.waitForTimeout(500)

      // Should show empty chart state
      await expect(page.getByText(/No data available for charts/i)).toBeVisible()
    })
  })

  test.describe('Vital Signs - Date Filter', () => {
    test('date filter controls are visible', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      await page.route('**/api/v1/admissions/100', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockActiveAdmission })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs/chart*', async route => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockVitalSigns })
        })
      })

      await page.route('**/api/v1/admissions/100/vital-signs*', async route => {
        if (!route.request().url().includes('/chart')) {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: mockVitalSigns,
                page: { totalElements: 2, totalPages: 1, size: 20, number: 0 }
              }
            })
          })
        }
      })

      await page.route('**/api/v1/admissions/100/clinical-history', async route => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' })
        })
      })

      await page.goto('/admissions/100')
      await waitForMedicalRecordTabs(page)
      await selectMedicalRecordTab(page, 'vitalSigns')

      // Date filter preset buttons should be visible
      await expect(page.getByText(/Last 7 Days/i)).toBeVisible()
      await expect(page.getByText(/Last 30 Days/i)).toBeVisible()
      await expect(page.getByText(/All Time/i)).toBeVisible()
    })
  })
})
