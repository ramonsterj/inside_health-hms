import { test, expect } from '@playwright/test'
import {
  waitForOverlaysToClear,
  waitForMedicalRecordTabs,
  selectMedicalRecordTab,
  fillRichTextEditor
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

// Per spec v1.4 (medical-psychiatric-record.md), CHIEF_NURSE can create + read
// progress notes but cannot update them. The grant for `progress-note:create`
// for chief nurse comes from V096; the legacy `progress-note:update` seed grant
// is revoked in the same migration.
const mockChiefNurseUser = {
  id: 4,
  username: 'chiefnurse',
  email: 'chiefnurse@example.com',
  firstName: 'Carmen',
  lastName: 'Flores',
  salutation: 'SRA',
  roles: ['CHIEF_NURSE'],
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

// `canEdit` defaults to false (the value the server returns for any non-admin viewer).
// Tests that act as ADMIN override it to `true` per-test via spread.
const mockProgressNote = {
  id: 1,
  admissionId: 1,
  subjectiveData: 'Patient reports feeling better today. Less anxious.',
  objectiveData: 'BP 130/85, HR 78. Calm demeanor, good eye contact.',
  analysis: 'Improvement noted. Medication appears effective.',
  actionPlans: 'Continue current medication. Schedule therapy session.',
  createdAt: '2026-01-24T09:00:00',
  createdBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  },
  updatedAt: '2026-01-24T09:00:00',
  updatedBy: {
    id: 2,
    salutation: 'Dra.',
    firstName: 'Maria',
    lastName: 'Garcia',
    roles: ['DOCTOR']
  },
  canEdit: false
}

const mockEditedProgressNote = {
  ...mockProgressNote,
  id: 2,
  subjectiveData: 'Patient had a difficult night.',
  updatedAt: '2026-01-24T15:00:00',
  updatedBy: {
    id: 1,
    salutation: null,
    firstName: 'Admin',
    lastName: 'User',
    roles: ['ADMIN']
  },
  canEdit: true
}

// Helper function to setup authenticated state
async function setupAuth(page: import('@playwright/test').Page, user: typeof mockAdminUser) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
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

  // Mock medical orders
  await page.route('**/api/v1/admissions/1/medical-orders', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { orders: {} } })
      })
    }
  })
}

test.describe('Medical Record - Progress Notes', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('doctor can create progress note', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    const notes: (typeof mockProgressNote)[] = []

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      const url = new URL(route.request().url())

      if (route.request().method() === 'GET' && !url.pathname.includes('/progress-notes/')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: notes,
              page: { totalElements: notes.length, totalPages: 1, size: 10, number: 0 }
            }
          })
        })
      } else if (route.request().method() === 'POST') {
        const newNote = { ...mockProgressNote, id: notes.length + 1 }
        notes.push(newNote)
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: newNote })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Should see add button (doctor has progress-note:create)
    const addButton = page.getByRole('button', { name: /Add Note|Agregar Nota/i })
    await expect(addButton).toBeVisible()

    // Click add button
    await addButton.click()

    // Dialog should open
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/SOAP/i)).toBeVisible()

    // Fill the SOAP fields using RichTextEditor
    await fillRichTextEditor(
      page,
      '.progress-note-form .form-field:nth-child(2)',
      'Patient feels better'
    )
    await fillRichTextEditor(page, '.progress-note-form .form-field:nth-child(3)', 'Vitals normal')
    await fillRichTextEditor(page, '.progress-note-form .form-field:nth-child(4)', 'Improving')
    await fillRichTextEditor(
      page,
      '.progress-note-form .form-field:nth-child(5)',
      'Continue treatment'
    )

    // Submit
    await page
      .getByRole('dialog')
      .getByRole('button', { name: /Save|Guardar/i })
      .click()

    // Should see success message
    await expect(page.getByText(/created successfully|creado exitosamente/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('doctor cannot edit progress notes', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [mockProgressNote],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Wait for note card to appear
    await expect(page.locator('.progress-note-card')).toBeVisible()

    // Doctor should NOT see edit button (no progress-note:update permission)
    await expect(
      page.locator('.progress-note-card').getByRole('button', { name: /Edit|Editar/i })
    ).not.toBeVisible()
  })

  test('nurse can create progress note', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      if (route.request().method() === 'GET') {
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
      } else if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockProgressNote })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Nurse should see add button (has progress-note:create)
    await expect(
      page.getByRole('button', { name: /Add First Note|Agregar Primera Nota/i })
    ).toBeVisible()
  })

  test('nurse cannot edit progress notes', async ({ page }) => {
    await setupAuth(page, mockNurseUser)
    await setupUserMock(page, mockNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [mockProgressNote],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Wait for note card
    await expect(page.locator('.progress-note-card')).toBeVisible()

    // Nurse should NOT see edit button
    await expect(
      page.locator('.progress-note-card').getByRole('button', { name: /Edit|Editar/i })
    ).not.toBeVisible()
  })

  test('admin can edit progress note', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)
    await setupAdmissionMock(page)

    // Server returns `canEdit: true` for the admin viewer on an active admission.
    const adminVisibleNote = { ...mockProgressNote, canEdit: true }

    // Mock progress notes endpoints
    await page.route('**/api/v1/admissions/1/progress-notes**', async route => {
      const url = new URL(route.request().url())
      const method = route.request().method()

      // PUT to /progress-notes/{id}
      if (method === 'PUT' && url.pathname.match(/\/progress-notes\/\d+$/)) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: mockEditedProgressNote })
        })
      }
      // GET list /progress-notes or /progress-notes?params
      else if (method === 'GET' && !url.pathname.match(/\/progress-notes\/\d+$/)) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              content: [adminVisibleNote],
              page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
            }
          })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')
    await waitForOverlaysToClear(page)

    // Wait for note card
    await expect(page.locator('.progress-note-card')).toBeVisible()

    // Admin should see edit button
    const editButton = page
      .locator('.progress-note-card')
      .getByRole('button', { name: /Edit|Editar/i })
    await expect(editButton).toBeVisible()

    // Click edit
    await editButton.click()

    // Dialog should open with Edit title
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/Edit Progress Note|Editar Nota/i)).toBeVisible()

    // Submit
    await page
      .getByRole('dialog')
      .getByRole('button', { name: /Save|Guardar/i })
      .click()

    // Dialog should close and success message should appear
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 10000 })
    await expect(page.getByText(/updated successfully|actualizada exitosamente/i)).toBeVisible({
      timeout: 10000
    })
  })

  test('edited notes show edited badge', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [mockEditedProgressNote],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Should see the edited badge
    await expect(page.getByText(/\(edited\)|\(editado\)/i)).toBeVisible()
  })

  test('pagination works correctly', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    // Create 15 notes
    const manyNotes = Array.from({ length: 15 }, (_, i) => ({
      ...mockProgressNote,
      id: i + 1,
      subjectiveData: `Note ${i + 1} subjective data`
    }))

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      const url = new URL(route.request().url())
      const pageNum = parseInt(url.searchParams.get('page') || '0')
      const pageSize = parseInt(url.searchParams.get('size') || '10')

      const start = pageNum * pageSize
      const end = start + pageSize
      const pageNotes = manyNotes.slice(start, end)

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: pageNotes,
            page: {
              totalElements: manyNotes.length,
              totalPages: Math.ceil(manyNotes.length / pageSize),
              size: pageSize,
              number: pageNum
            }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Should see first page notes and total count
    await expect(page.getByText(/15 notes|15 notas/i)).toBeVisible()

    // Should see paginator
    await expect(page.locator('.p-paginator')).toBeVisible()

    // Click next page
    await page.locator('.p-paginator-next').click()

    // Should now see page 2 notes (Note 11-15)
    await expect(page.getByText('Note 11 subjective data')).toBeVisible()
  })

  test('sort by newest/oldest first', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    let lastSortParam = 'DESC'

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      const url = new URL(route.request().url())
      const sortParam = url.searchParams.get('sort')

      if (sortParam?.includes('ASC')) {
        lastSortParam = 'ASC'
      } else if (sortParam?.includes('DESC')) {
        lastSortParam = 'DESC'
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [mockProgressNote],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Default should be newest first (DESC)
    expect(lastSortParam).toBe('DESC')

    // Click oldest first button
    await page.getByText(/Oldest first|Más antiguo primero/i).click()

    // Wait for API call
    await page.waitForTimeout(500)

    // Should have made request with ASC sort
    expect(lastSortParam).toBe('ASC')
  })

  test('expand/collapse note cards', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    // Note with long content that will be truncated
    const longNote = {
      ...mockProgressNote,
      subjectiveData:
        'This is a very long subjective data text that should be truncated when the card is collapsed. It contains more than 150 characters to ensure truncation happens properly in the UI. The patient reported many symptoms including headaches and fatigue.'
    }

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [longNote],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // By default, content is collapsed — `.truncated` class clips height via CSS.
    await expect(page.locator('.soap-content').first()).toHaveClass(/truncated/)

    // Click expand button
    await page.getByRole('button', { name: /Expand|Expandir/i }).click()

    // Now the `.truncated` class is gone and the full text is visible.
    await expect(page.locator('.soap-content').first()).not.toHaveClass(/truncated/)
    await expect(page.locator('.soap-content').first()).toContainText('headaches and fatigue')

    // Click collapse
    await page.getByRole('button', { name: /Collapse|Contraer/i }).click()

    // Should be truncated again
    await expect(page.locator('.soap-content').first()).toHaveClass(/truncated/)
  })

  test('empty state shows add button', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
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

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Should see empty state message
    await expect(page.getByText(/No progress notes recorded/i)).toBeVisible()

    // Should see add button in empty state
    await expect(
      page.getByRole('button', { name: /Add First Note|Agregar Primera Nota/i })
    ).toBeVisible()
  })

  // ============ ADMIN-ONLY UPDATE ENFORCEMENT (spec v1.4) ============

  test('chief nurse can create progress note', async ({ page }) => {
    await setupAuth(page, mockChiefNurseUser)
    await setupUserMock(page, mockChiefNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      if (route.request().method() === 'GET') {
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
      } else if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { ...mockProgressNote, canEdit: false } })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Chief nurse holds progress-note:create after V096 — Add button is visible.
    await expect(
      page.getByRole('button', { name: /Add First Note|Agregar Primera Nota/i })
    ).toBeVisible()
  })

  test('chief nurse cannot edit progress notes', async ({ page }) => {
    await setupAuth(page, mockChiefNurseUser)
    await setupUserMock(page, mockChiefNurseUser)
    await setupAdmissionMock(page)

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [mockProgressNote],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    await expect(page.locator('.progress-note-card')).toBeVisible()

    // Chief nurse: progress-note:update is revoked by V096; canEdit is false; no edit button.
    await expect(
      page.locator('.progress-note-card').getByRole('button', { name: /Edit|Editar/i })
    ).not.toBeVisible()
  })

  test('admin edit button is hidden for discharged admissions', async ({ page }) => {
    await setupAuth(page, mockAdminUser)
    await setupUserMock(page, mockAdminUser)

    // Admission is DISCHARGED; server returns `canEdit: false` even for ADMIN.
    const dischargedAdmission = {
      ...mockAdmission,
      status: 'DISCHARGED',
      dischargeDate: '2026-01-24T18:00:00'
    }

    await page.route('**/api/v1/admissions/1', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: dischargedAdmission })
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
    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { orders: {} } })
        })
      }
    })

    await page.route('**/api/v1/admissions/1/progress-notes*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            // canEdit: false because admission is discharged, even though viewer is ADMIN.
            content: [{ ...mockProgressNote, canEdit: false }],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    await expect(page.locator('.progress-note-card')).toBeVisible()

    // Discharge protection means even admin sees no edit button.
    await expect(
      page.locator('.progress-note-card').getByRole('button', { name: /Edit|Editar/i })
    ).not.toBeVisible()
  })
})
