import { test, expect } from '@playwright/test'
import {
  waitForMedicalRecordTabs,
  selectMedicalRecordTab
} from './utils/test-helpers'

// Mock user data
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
    'nursing-note:update',
    'vital-sign:read',
    'vital-sign:create',
    'vital-sign:update',
    'clinical-history:read',
    'psychotherapy-activity:read'
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
    canEdit: true
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
    canEdit: true
  }
]

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
    canEdit: true
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
    canEdit: true
  }
]

// Helper function to setup authenticated state
async function setupAuth(
  page: import('@playwright/test').Page,
  user: typeof mockNurseUser
) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

async function setupCommonMocks(
  page: import('@playwright/test').Page,
  user: typeof mockNurseUser
) {
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

      await expect(page.getByText('Afternoon check: Medication administered as prescribed.')).toBeVisible()
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

    test('can open edit nursing note dialog with existing content', async ({ page }) => {
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

      // Click Edit on first note (canEdit is true)
      await page.getByRole('button', { name: /Edit/i }).first().click()

      // Dialog should open with Edit Note title and pre-populated content
      await expect(page.getByText(/Edit Note/i).first()).toBeVisible()
      await expect(page.locator('.ql-editor')).toBeVisible()
      await expect(page.locator('.ql-editor')).toContainText('Afternoon check')
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
      await expect(page.getByText('Afternoon check: Medication administered as prescribed.')).toBeVisible()
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
      await expect(page.getByRole('button', { name: /Save/i })).toBeVisible()
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
      await page.getByText(/Charts/i).first().click()
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
      await page.getByText(/Charts/i).first().click()
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
