import { test, expect, type Page } from '@playwright/test'

// ── Mock Users ──

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
    'medical-order:read',
    'medication-administration:create',
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en',
}

const mockChiefNurseUser = {
  ...mockNurseUser,
  id: 5,
  username: 'chief_nurse',
  email: 'chief_nurse@example.com',
  firstName: 'Maria',
  lastName: 'Garcia',
  roles: ['CHIEF_NURSE'],
}

const mockDoctorUser = {
  id: 3,
  username: 'doctor',
  email: 'doctor@example.com',
  firstName: 'Carlos',
  lastName: 'Ramirez',
  salutation: 'DR',
  roles: ['DOCTOR'],
  permissions: [
    'admission:read',
    'medical-order:read',
    'medical-order:create',
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en',
}

// ── Mock Kardex Data ──

function createMockKardexSummary(overrides: Record<string, unknown> = {}) {
  return {
    admissionId: 100,
    patientId: 1,
    patientName: 'Juan Pérez García',
    roomNumber: '201-A',
    triageCode: 'II',
    triageColorCode: '#FFA500',
    admissionType: 'HOSPITALIZATION',
    admissionDate: '2026-03-10T08:30:00',
    daysAdmitted: 7,
    treatingPhysicianName: 'Dr. Carlos Ramirez',
    activeMedicationCount: 2,
    medications: [
      {
        orderId: 45,
        medication: 'Haloperidol',
        dosage: '5mg',
        route: 'IM',
        frequency: 'Every 8 hours',
        schedule: '06:00, 14:00, 22:00',
        inventoryItemId: 22,
        inventoryItemName: 'Haloperidol 5mg/ml Amp',
        observations: null,
        lastAdministration: {
          administeredAt: '2026-03-16T06:15:00',
          status: 'GIVEN',
          administeredByName: 'Ana Lopez',
        },
      },
      {
        orderId: 46,
        medication: 'Diazepam',
        dosage: '10mg',
        route: 'ORAL',
        frequency: 'Every 12 hours',
        schedule: '08:00, 20:00',
        inventoryItemId: null,
        inventoryItemName: null,
        observations: null,
        lastAdministration: null,
      },
    ],
    activeCareInstructionCount: 2,
    careInstructions: [
      {
        orderId: 47,
        category: 'DIETA',
        startDate: '2026-03-10',
        observations: 'Dieta blanda, sin restricción de líquidos',
      },
      {
        orderId: 48,
        category: 'RESTRICCIONES_MOVILIDAD',
        startDate: '2026-03-12',
        observations: 'No salidas sin acompañamiento',
      },
    ],
    latestVitalSigns: {
      recordedAt: '2026-03-17T06:00:00',
      systolicBp: 125,
      diastolicBp: 78,
      heartRate: 72,
      respiratoryRate: 16,
      temperature: 36.5,
      oxygenSaturation: 97,
      glucose: null,
      recordedByName: 'Ana Lopez',
    },
    hoursSinceLastVitals: 2.5,
    lastNursingNotePreview: 'Paciente descansó durante la noche sin incidentes.',
    lastNursingNoteAt: '2026-03-17T06:30:00',
    ...overrides,
  }
}

const mockSummary1 = createMockKardexSummary()
const mockSummary2 = createMockKardexSummary({
  admissionId: 101,
  patientId: 2,
  patientName: 'Maria López Hernández',
  roomNumber: '202-B',
  triageCode: null,
  triageColorCode: null,
  admissionType: 'AMBULATORY',
  daysAdmitted: 2,
  activeMedicationCount: 0,
  medications: [],
  activeCareInstructionCount: 0,
  careInstructions: [],
  latestVitalSigns: null,
  hoursSinceLastVitals: null,
  lastNursingNotePreview: null,
  lastNursingNoteAt: null,
})

function mockKardexPageResponse(
  content: unknown[],
  totalElements: number,
  page = 0,
  size = 20,
) {
  return {
    success: true,
    data: {
      content,
      page: {
        totalElements,
        totalPages: Math.ceil(totalElements / size),
        size,
        number: page,
      },
    },
  }
}

// ── Helpers ──

async function setupAuth(page: Page, user: typeof mockNurseUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user,
  )
}

async function setupCommonMocks(page: Page, user: typeof mockNurseUser) {
  await page.route('**/api/users/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: user }),
    })
  })

  await page.route('**/api/auth/refresh', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { accessToken: 'new-mock-token', refreshToken: 'new-mock-refresh' },
      }),
    })
  })
}

async function setupKardexListMock(
  page: Page,
  summaries: unknown[] = [mockSummary1, mockSummary2],
) {
  await page.route('**/api/v1/nursing-kardex?*', async (route) => {
    const url = new URL(route.request().url())
    const typeFilter = url.searchParams.get('type')
    const search = url.searchParams.get('search')

    let filtered = [...summaries] as Array<Record<string, unknown>>
    if (typeFilter) {
      filtered = filtered.filter((s) => s.admissionType === typeFilter)
    }
    if (search) {
      const q = search.toLowerCase()
      filtered = filtered.filter((s) =>
        (s.patientName as string).toLowerCase().includes(q),
      )
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockKardexPageResponse(filtered, filtered.length)),
    })
  })

  // Also handle without query params
  await page.route('**/api/v1/nursing-kardex', async (route) => {
    if (route.request().url().includes('?')) return route.fallback()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(
        mockKardexPageResponse(summaries, summaries.length),
      ),
    })
  })
}

// ── Tests ──

test.describe('Nursing Kardex Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  // ── Role-Based Routing ──

  test.describe('Role-based routing', () => {
    test('nurse login redirects /dashboard to /nursing-kardex', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/dashboard')

      await expect(page).toHaveURL(/\/nursing-kardex/, { timeout: 10000 })
    })

    test('chief nurse login redirects /dashboard to /nursing-kardex', async ({ page }) => {
      await setupAuth(page, mockChiefNurseUser)
      await setupCommonMocks(page, mockChiefNurseUser)
      await setupKardexListMock(page)

      await page.goto('/dashboard')

      await expect(page).toHaveURL(/\/nursing-kardex/, { timeout: 10000 })
    })

    test('doctor is NOT redirected — stays on /dashboard', async ({ page }) => {
      await setupAuth(page, mockDoctorUser)
      await setupCommonMocks(page, mockDoctorUser)

      // Doctor needs a dashboard mock (admissions list)
      await page.route('**/api/v1/admissions*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { content: [], page: { totalElements: 0, totalPages: 0, size: 20, number: 0 } },
          }),
        })
      })

      await page.goto('/dashboard')

      await expect(page).toHaveURL(/\/dashboard/, { timeout: 10000 })
      // Should NOT be at nursing-kardex
      await expect(page).not.toHaveURL(/\/nursing-kardex/)
    })
  })

  // ── Kardex Display ──

  test.describe('Kardex display', () => {
    test('displays active admissions with summary data', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')

      // Page title (use heading role to avoid nav menu match)
      await expect(page.getByRole('heading', { name: 'Nursing Kardex' })).toBeVisible({ timeout: 10000 })

      // Patient 1 summary data
      await expect(page.getByText('Juan Pérez García')).toBeVisible()
      await expect(page.getByText('201-A')).toBeVisible()
      await expect(page.getByText('Dr. Carlos Ramirez').first()).toBeVisible()

      // Patient 2 summary data
      await expect(page.getByText('Maria López Hernández')).toBeVisible()
      await expect(page.getByText('202-B')).toBeVisible()
    })

    test('shows empty state when no active admissions', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page, [])

      await page.goto('/nursing-kardex')

      await expect(page.getByText('No active admissions')).toBeVisible({ timeout: 10000 })
    })

    test('displays triage code badge for patients with triage', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')

      await expect(page.getByText('II')).toBeVisible({ timeout: 10000 })
    })
  })

  // ── Expand / Collapse ──

  test.describe('Card expand and collapse', () => {
    test('expanding a card shows medications, care instructions, and vitals', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Click on the first card header to expand
      await page.getByText('Juan Pérez García').click()
      await page.waitForTimeout(300)

      // Medications section
      await expect(page.getByRole('heading', { name: 'Active Medications' }).first()).toBeVisible()
      await expect(page.getByText('Haloperidol', { exact: true })).toBeVisible()
      await expect(page.getByText('Diazepam')).toBeVisible()
      await expect(page.getByText('5mg', { exact: true })).toBeVisible()

      // Care instructions
      await expect(page.getByRole('heading', { name: 'Care Instructions' }).first()).toBeVisible()
      await expect(page.getByText('Dieta blanda, sin restricción de líquidos')).toBeVisible()
      await expect(page.getByText('No salidas sin acompañamiento')).toBeVisible()

      // Vital signs
      await expect(page.getByRole('heading', { name: 'Latest Vital Signs' }).first()).toBeVisible()
      await expect(page.getByText('125/78')).toBeVisible()
    })

    test('expanding a card with no data shows empty states', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page, [mockSummary2]) // Patient 2 has no meds/vitals/notes

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Maria López Hernández')).toBeVisible({ timeout: 10000 })

      // Expand patient 2
      await page.getByText('Maria López Hernández').click()
      await page.waitForTimeout(300)

      await expect(page.getByText('No active medication orders')).toBeVisible()
      await expect(page.getByText('No active care instructions')).toBeVisible()
      await expect(page.getByText('No vital signs recorded')).toBeVisible()
    })
  })

  // ── Filtering ──

  test.describe('Filtering', () => {
    test('filter by admission type shows only matching admissions', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Select Hospitalization filter via PrimeVue Select
      const typeSelect = page.locator('.type-filter')
      await typeSelect.click()
      await page.getByText('Hospitalization', { exact: false }).last().click()

      // Should show only Juan (HOSPITALIZATION) not Maria (AMBULATORY)
      await expect(page.getByText('Juan Pérez García')).toBeVisible()
      await expect(page.getByText('Maria López Hernández')).not.toBeVisible({ timeout: 5000 })
    })

    test('search by patient name filters results', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Type in search
      const searchInput = page.getByPlaceholder(/Search by patient name/i)
      await searchInput.fill('Maria')

      // Wait for debounce
      await page.waitForTimeout(500)

      await expect(page.getByText('Maria López Hernández')).toBeVisible()
      await expect(page.getByText('Juan Pérez García')).not.toBeVisible({ timeout: 5000 })
    })

    test('search with no results shows empty state', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByRole('heading', { name: 'Nursing Kardex' })).toBeVisible({ timeout: 10000 })

      const searchInput = page.getByPlaceholder(/Search by patient name/i)
      await searchInput.fill('NonExistentPatient')
      await page.waitForTimeout(500)

      await expect(page.getByText('No active admissions')).toBeVisible()
    })
  })

  // ── Quick Actions ──

  test.describe('Quick actions', () => {
    test('quick-administer medication opens MAR dialog', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Expand card
      await page.getByText('Juan Pérez García').click()
      await page.waitForTimeout(300)

      // Click Administer on first medication (Haloperidol)
      const administerBtns = page.getByRole('button', { name: /Administer/i })
      await administerBtns.first().click()

      // MAR dialog should be visible
      await expect(page.locator('.p-dialog')).toBeVisible({ timeout: 5000 })
    })

    test('quick-record vitals opens vital signs dialog', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Expand card
      await page.getByText('Juan Pérez García').click()
      await page.waitForTimeout(300)

      // Click Record Vitals
      await page.getByRole('button', { name: /Record Vitals/i }).click()

      // Vital signs form dialog should be visible
      await expect(page.locator('.p-dialog')).toBeVisible({ timeout: 5000 })
    })

    test('quick-add note opens nursing note dialog', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Expand card
      await page.getByText('Juan Pérez García').click()
      await page.waitForTimeout(300)

      // Click Add Note
      await page.getByRole('button', { name: /Add Note/i }).click()

      // Note dialog should be visible with rich text editor
      await expect(page.locator('.p-dialog')).toBeVisible({ timeout: 5000 })
      await expect(page.locator('.ql-editor')).toBeVisible()
    })

    test('navigate to admission detail from kardex', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      // Mock admission detail endpoint
      await page.route('**/api/v1/admissions/100', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 100,
              patient: { id: 1, firstName: 'Juan', lastName: 'Pérez García', age: 40, sex: 'MALE' },
              type: 'HOSPITALIZATION',
              status: 'ACTIVE',
              admissionDate: '2026-03-10T08:30:00',
              treatingPhysician: { id: 3, salutation: 'DR', firstName: 'Carlos', lastName: 'Ramirez' },
              room: { id: 1, number: '201-A', type: 'PRIVATE' },
              triageCode: { id: 1, code: 'II', color: '#FFA500', description: 'Urgent' },
              consultingPhysicians: [],
              documents: [],
            },
          }),
        })
      })

      // Mock sub-routes for admission detail
      await page.route('**/api/v1/admissions/100/clinical-history', async (route) => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, message: 'Not found' }),
        })
      })

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Expand card
      await page.getByText('Juan Pérez García').click()
      await page.waitForTimeout(300)

      // Click View Detail
      await page.getByRole('button', { name: /View Detail/i }).click()

      await expect(page).toHaveURL(/\/admissions\/100/, { timeout: 10000 })
    })

    test('card refreshes after quick action completion', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      // Track single-refresh calls
      let singleRefreshCalled = false
      await page.route('**/api/v1/nursing-kardex/100', async (route) => {
        singleRefreshCalled = true
        const updated = createMockKardexSummary({
          lastNursingNotePreview: 'New nursing note just added',
        })
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: updated }),
        })
      })

      // Mock nursing note creation (match any method on this URL)
      await page.route('**/api/v1/admissions/100/nursing-notes**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                id: 999,
                admissionId: 100,
                description: 'New nursing note just added',
                createdAt: '2026-03-17T10:00:00Z',
              },
            }),
          })
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: {
                content: [],
                page: { totalElements: 0, totalPages: 0, size: 20, number: 0 },
              },
            }),
          })
        }
      })

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Expand card and open note dialog
      await page.getByText('Juan Pérez García').click()
      await page.waitForTimeout(300)
      await page.getByRole('button', { name: /Add Note/i }).click()
      await expect(page.locator('.p-dialog')).toBeVisible({ timeout: 5000 })

      // Fill note using rich text editor
      const editor = page.locator('.p-dialog .ql-editor')
      await editor.click()
      await editor.fill('New nursing note just added')

      // Submit and wait for the refresh call
      await page.locator('.p-dialog').getByRole('button', { name: /Save/i }).click()
      await page.waitForTimeout(2000)

      // Verify the single-refresh endpoint was called
      expect(singleRefreshCalled).toBe(true)
    })
  })

  // ── Medication Administration Status ──

  test.describe('Medication display', () => {
    test('shows last administration status and "Not yet administered"', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Expand
      await page.getByText('Juan Pérez García').click()
      await page.waitForTimeout(300)

      // Haloperidol has a last administration
      await expect(page.getByText('Ana Lopez').first()).toBeVisible()

      // Diazepam has never been administered
      await expect(page.getByText('Not yet administered')).toBeVisible()
    })
  })

  // ── Vitals Freshness ──

  test.describe('Vitals freshness indicator', () => {
    test('shows freshness label for patients with recent vitals', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page, [mockSummary1])

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })

      // Should show hours-ago label (the exact number depends on current time)
      await expect(page.locator('.freshness-ok, .freshness-warning, .freshness-critical').first()).toBeVisible()
    })

    test('shows critical freshness for patients with no vitals', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page, [mockSummary2]) // No vitals

      await page.goto('/nursing-kardex')
      await expect(page.getByText('Maria López Hernández')).toBeVisible({ timeout: 10000 })

      // Should show critical (red) freshness indicator
      await expect(page.locator('.freshness-critical')).toBeVisible()
    })
  })

  // ── i18n ──

  test.describe('Internationalization', () => {
    test('displays in Spanish when user locale is es', async ({ page }) => {
      const spanishNurse = { ...mockNurseUser, localePreference: 'es' }
      await setupAuth(page, spanishNurse)
      await setupCommonMocks(page, spanishNurse)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')

      await expect(page.getByRole('heading', { name: 'Kardex de Enfermería' })).toBeVisible({ timeout: 10000 })
      await expect(page.getByPlaceholder(/Buscar por nombre de paciente/i)).toBeVisible()
    })

    test('displays in English when user locale is en', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)
      await setupKardexListMock(page)

      await page.goto('/nursing-kardex')

      await expect(page.getByRole('heading', { name: 'Nursing Kardex' })).toBeVisible({ timeout: 10000 })
      await expect(page.getByPlaceholder(/Search by patient name/i)).toBeVisible()
    })
  })

  // ── Refresh ──

  test.describe('Refresh', () => {
    test('manual refresh button reloads data', async ({ page }) => {
      await setupAuth(page, mockNurseUser)
      await setupCommonMocks(page, mockNurseUser)

      let fetchCount = 0
      await page.route('**/api/v1/nursing-kardex*', async (route) => {
        fetchCount++
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(
            mockKardexPageResponse([mockSummary1, mockSummary2], 2),
          ),
        })
      })

      await page.goto('/nursing-kardex')
      await expect(page.getByRole('heading', { name: 'Nursing Kardex' })).toBeVisible({ timeout: 10000 })

      const initialCount = fetchCount

      // Click refresh button
      await page.getByRole('button', { name: /Refresh/i }).click()
      await page.waitForTimeout(500)

      expect(fetchCount).toBeGreaterThan(initialCount)
    })
  })
})
