import { test, expect, type Page } from '@playwright/test'

/**
 * E2E coverage for the AUXILIARY_NURSE role (docs/features/nursing-roles-split.md).
 *
 * The auxiliary nurse sees the same nursing kardex a graduate nurse sees (read context +
 * notes/vitals), but the "Administer" quick action must be hidden — even when a custom role
 * grants the underlying `medication-administration:create` permission, because the backend
 * service guard 403s anyway. A graduate nurse (NURSE) and a stacked NURSE+AUXILIARY_NURSE
 * user are unaffected. The direct-API 403 path (AC-3/4/5) is covered by the backend
 * integration test `AuxiliaryNurseRestrictionTest`; here we exercise the UI gating (AC-7).
 */

// ── Mock Users ──

const auxNurseUser = {
  id: 40,
  username: 'aux_nurse',
  email: 'aux_nurse@example.com',
  firstName: 'Lucia',
  lastName: 'Gomez',
  salutation: 'SRTA',
  roles: ['AUXILIARY_NURSE'],
  permissions: [
    'admission:read',
    'patient:read',
    'clinical-history:read',
    'nursing-note:read',
    'nursing-note:create',
    'vital-sign:read',
    'vital-sign:create',
    'medical-order:read',
    'medication-administration:read',
    'progress-note:read',
    'room:occupancy-view',
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en',
}

// Auxiliary nurse whose extra custom role accidentally grants medication-administration:create.
// The button must STILL hide because isAuxiliaryNurseOnly is true (no graduate-or-better role).
const auxNurseWithAdministerPerm = {
  ...auxNurseUser,
  id: 41,
  username: 'aux_with_perms',
  email: 'aux_with_perms@example.com',
  roles: ['AUXILIARY_NURSE', 'CUSTOM_NURSE_PERMS'],
  permissions: [...auxNurseUser.permissions, 'medication-administration:create'],
}

const graduateNurseUser = {
  ...auxNurseUser,
  id: 42,
  username: 'nurse',
  email: 'nurse@example.com',
  firstName: 'Ana',
  lastName: 'Lopez',
  roles: ['NURSE'],
  permissions: [...auxNurseUser.permissions, 'medication-administration:create'],
}

// Graduate nurse covering an auxiliary shift — holds NURSE, so the restriction does not apply.
const stackedNurseAuxUser = {
  ...graduateNurseUser,
  id: 43,
  username: 'graduate_covering_aux',
  email: 'graduate_covering_aux@example.com',
  roles: ['NURSE', 'AUXILIARY_NURSE'],
}

// ── Mock Kardex Data ──

const mockSummary = {
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
      nextLotExpirationDate: null,
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
      nextLotExpirationDate: null,
      observations: null,
      lastAdministration: null,
    },
  ],
  activeCareInstructionCount: 1,
  careInstructions: [
    {
      orderId: 47,
      category: 'DIETA',
      startDate: '2026-03-10',
      observations: 'Dieta blanda, sin restricción de líquidos',
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
}

function kardexPageResponse(content: unknown[]) {
  return {
    success: true,
    data: {
      content,
      page: { totalElements: content.length, totalPages: 1, size: 20, number: 0 },
    },
  }
}

// ── Helpers ──

async function setupAuth(page: Page, user: typeof auxNurseUser) {
  await page.addInitScript(
    (userData) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(userData))
    },
    user,
  )

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

  await page.route('**/api/v1/nursing-kardex**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(kardexPageResponse([mockSummary])),
    })
  })
}

/** Open the kardex and expand the first patient card so the medication list renders. */
async function openExpandedKardex(page: Page) {
  await page.goto('/nursing-kardex')
  await expect(page.getByText('Juan Pérez García')).toBeVisible({ timeout: 10000 })
  await page.getByText('Juan Pérez García').click()
  await page.waitForTimeout(300)
  await expect(page.getByText('Haloperidol', { exact: true })).toBeVisible()
}

// ── Tests ──

test.describe('Auxiliary nurse role', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('auxiliary nurse login redirects /dashboard to /bed-occupancy', async ({ page }) => {
    await setupAuth(page, auxNurseUser)
    await page.route('**/api/v1/rooms/occupancy', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            summary: { totalBeds: 0, occupiedBeds: 0, freeBeds: 0, occupancyPercent: 0 },
            rooms: [],
          },
        }),
      })
    })

    await page.goto('/dashboard')

    await expect(page).toHaveURL(/\/bed-occupancy/, { timeout: 10000 })
  })

  test('auxiliary nurse sees the kardex but NO Administer button', async ({ page }) => {
    await setupAuth(page, auxNurseUser)

    await openExpandedKardex(page)

    // The medication list is visible (read context) but the quick action is hidden.
    await expect(page.getByRole('button', { name: /Administer/i })).toHaveCount(0)
    // The note/vital quick actions the auxiliary IS allowed remain available.
    await expect(page.getByRole('button', { name: /Record Vitals/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /Add Note/i })).toBeVisible()
  })

  test('Administer stays hidden even when a custom role grants the permission', async ({ page }) => {
    await setupAuth(page, auxNurseWithAdministerPerm)

    await openExpandedKardex(page)

    await expect(page.getByRole('button', { name: /Administer/i })).toHaveCount(0)
  })

  test('graduate nurse (NURSE) sees the Administer button — regression', async ({ page }) => {
    await setupAuth(page, graduateNurseUser)

    await openExpandedKardex(page)

    // One Administer button per active medication (two in the mock).
    await expect(page.getByRole('button', { name: /Administer/i })).toHaveCount(2)
  })

  test('nurse stacked with auxiliary role still sees the Administer button', async ({ page }) => {
    await setupAuth(page, stackedNurseAuxUser)

    await openExpandedKardex(page)

    await expect(page.getByRole('button', { name: /Administer/i })).toHaveCount(2)
  })
})
