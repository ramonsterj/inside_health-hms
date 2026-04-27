import { test, expect, type Page } from '@playwright/test'

// ── Mock User ─────────────────────────────────────────────────────────────────

const mockAdminStaffUser = {
  id: 2,
  username: 'receptionist',
  email: 'receptionist@example.com',
  firstName: 'Reception',
  lastName: 'Staff',
  roles: ['ADMINISTRATIVE_STAFF'],
  permissions: [
    'admission:read',
    'admission:create',
    'admission:update',
    'patient:read',
    'room:read',
    'triage-code:read'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// User-scoped localStorage key used by useAdmissionsListPreferencesStore.
const PREFS_KEY = `hms.admissionsListView.v1.user.${mockAdminStaffUser.id}`

// ── Test Data Helpers ────────────────────────────────────────────────────────

type Sex = 'MALE' | 'FEMALE' | 'OTHER'

function makeAdmission({
  id,
  firstName,
  lastName,
  sex,
  triage
}: {
  id: number
  firstName: string
  lastName: string
  sex: Sex
  triage: { id: number; code: string; color: string; description: string | null } | null
}) {
  return {
    id,
    patient: {
      id: id + 100,
      firstName,
      lastName,
      age: 35,
      sex,
      idDocumentNumber: null,
      hasIdDocument: false,
      hasActiveAdmission: true
    },
    triageCode: triage,
    room: { id: 1, number: '101', type: 'PRIVATE', gender: sex },
    treatingPhysician: {
      id: 9,
      firstName: 'Carlos',
      lastName: 'Ramirez',
      salutation: 'DR',
      username: 'carlos.ramirez'
    },
    admissionDate: '2026-03-10T08:30:00',
    dischargeDate: null,
    status: 'ACTIVE',
    type: 'HOSPITALIZATION',
    hasConsentDocument: false,
    createdAt: '2026-03-10T08:35:00'
  }
}

const triageA = {
  id: 1,
  code: 'A',
  color: '#FF0000',
  description: 'Critical - Immediate attention required'
}
const triageB = {
  id: 2,
  code: 'B',
  color: '#FFA500',
  description: 'Urgent - Requires prompt attention'
}

// ── Page Setup ───────────────────────────────────────────────────────────────

async function setupAuth(page: Page, prefs?: { viewMode?: string; groupBy?: string }) {
  await page.addInitScript(
    ({ user, prefsKey, prefs }) => {
      localStorage.setItem('access_token', 'mock-access-token')
      localStorage.setItem('refresh_token', 'mock-refresh-token')
      localStorage.setItem('mock_user', JSON.stringify(user))
      if (prefs) {
        localStorage.setItem(
          prefsKey,
          JSON.stringify({
            viewMode: prefs.viewMode ?? 'cards',
            groupBy: prefs.groupBy ?? 'gender'
          })
        )
      }
    },
    { user: mockAdminStaffUser, prefsKey: PREFS_KEY, prefs }
  )
}

async function setupCommonMocks(page: Page) {
  await page.route('**/api/users/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: mockAdminStaffUser })
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

async function mockAdmissionsList(page: Page, content: ReturnType<typeof makeAdmission>[]) {
  await page.route('**/api/v1/admissions*', async route => {
    if (route.request().method() === 'GET' && !route.request().url().includes('/admissions/')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content,
            page: { totalElements: content.length, totalPages: 1, size: 20, number: 0 }
          }
        })
      })
    } else {
      await route.continue()
    }
  })
}

async function mockPagedAdmissionsList(
  page: Page,
  pages: Record<number, ReturnType<typeof makeAdmission>[]>,
  totalElements: number
) {
  await page.route('**/api/v1/admissions*', async route => {
    if (route.request().method() === 'GET' && !route.request().url().includes('/admissions/')) {
      const url = new URL(route.request().url())
      const pageNumber = Number(url.searchParams.get('page') ?? 0)
      const size = Number(url.searchParams.get('size') ?? 20)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: pages[pageNumber] ?? [],
            page: {
              totalElements,
              totalPages: Math.ceil(totalElements / size),
              size,
              number: pageNumber
            }
          }
        })
      })
    } else {
      await route.continue()
    }
  })
}

// ── Tests ─────────────────────────────────────────────────────────────────────

test.describe('Admissions list view — cards, grouping, and card click', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('first visit renders cards grouped by gender (no DataTable)', async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
    await mockAdmissionsList(page, [
      makeAdmission({
        id: 1,
        firstName: 'Juana',
        lastName: 'Pérez',
        sex: 'FEMALE',
        triage: triageA
      }),
      makeAdmission({
        id: 2,
        firstName: 'Pedro',
        lastName: 'García',
        sex: 'MALE',
        triage: triageB
      })
    ])

    await page.goto('/admissions')

    // Cards are visible.
    await expect(page.locator('.admission-card').first()).toBeVisible()
    await expect(page.locator('.admission-card')).toHaveCount(2)

    // Group headers for both genders.
    const headers = page.locator('.p-panel-header')
    await expect(headers.filter({ hasText: /Female|Femenino/ })).toBeVisible()
    await expect(headers.filter({ hasText: /^Male|Masculino/ })).toBeVisible()

    // Triage row inside the card shows the short label inside a colored pill,
    // not the long description and not the code letter.
    const juanaCard = page.locator('.admission-card', { hasText: 'Juana Pérez' })
    await expect(juanaCard.locator('.triage-pill')).toHaveText('Critical')
    await expect(juanaCard.locator('.triage-pill')).not.toContainText(
      'Immediate attention required'
    )

    // No DataTable rows (table view is not active).
    await expect(page.locator('table tbody tr')).toHaveCount(0)
  })

  test('clicking anywhere on a card navigates to admission detail', async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
    const admission = makeAdmission({
      id: 7,
      firstName: 'Juana',
      lastName: 'Pérez',
      sex: 'FEMALE',
      triage: triageA
    })
    await mockAdmissionsList(page, [admission])

    // Mock the admission detail endpoint we'll navigate to.
    await page.route('**/api/v1/admissions/7', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { ...admission, consultingPhysicians: [] }
          })
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/admissions')
    const card = page.locator('.admission-card').first()
    await expect(card).toBeVisible()

    // Click on a non-button area inside the card body to verify the whole card is clickable.
    await card.getByText(/Doctor|Médico/).click()
    await expect(page).toHaveURL(/\/admissions\/7$/)
  })

  test('view-mode preference persists across Dashboard ↔ Admissions screens', async ({ page }) => {
    await setupAuth(page, { viewMode: 'table', groupBy: 'none' })
    await setupCommonMocks(page)
    await mockAdmissionsList(page, [
      makeAdmission({
        id: 1,
        firstName: 'Juana',
        lastName: 'Pérez',
        sex: 'FEMALE',
        triage: triageA
      })
    ])

    // Dashboard opens in Table mode (no card grid).
    await page.goto('/dashboard')
    await expect(page.locator('table tbody tr').first()).toBeVisible()
    await expect(page.locator('.admission-card')).toHaveCount(0)

    // Navigating to Admissions keeps Table mode (preference is shared).
    await page.goto('/admissions')
    await expect(page.locator('table tbody tr').first()).toBeVisible()
    await expect(page.locator('.admission-card')).toHaveCount(0)
  })

  test('group-by Triage renders groups in code order with untriaged last', async ({ page }) => {
    await setupAuth(page, { viewMode: 'cards', groupBy: 'triage' })
    await setupCommonMocks(page)
    await mockAdmissionsList(page, [
      makeAdmission({
        id: 1,
        firstName: 'Bea',
        lastName: 'Bee',
        sex: 'FEMALE',
        triage: triageB
      }),
      makeAdmission({
        id: 2,
        firstName: 'Adam',
        lastName: 'Apple',
        sex: 'MALE',
        triage: triageA
      }),
      makeAdmission({
        id: 3,
        firstName: 'Carla',
        lastName: 'Cero',
        sex: 'FEMALE',
        triage: null
      })
    ])

    await page.goto('/admissions')
    const headers = page.locator('.p-panel-header')
    await expect(headers).toHaveCount(3)
    // Order: A, B, then untriaged.
    await expect(headers.nth(0)).toContainText('A')
    await expect(headers.nth(0)).toContainText(/Critical/)
    await expect(headers.nth(1)).toContainText('B')
    await expect(headers.nth(1)).toContainText(/Urgent/)
    await expect(headers.nth(2)).toContainText(/No triage|Sin triage/i)
  })

  test('cards are sorted by triage code within an ungrouped list', async ({ page }) => {
    await setupAuth(page, { viewMode: 'cards', groupBy: 'none' })
    await setupCommonMocks(page)
    // Server returns the admissions in deliberately wrong order:
    //   B, untriaged, A — the UI must reorder to A, B, untriaged.
    await mockAdmissionsList(page, [
      makeAdmission({
        id: 1,
        firstName: 'Bea',
        lastName: 'Bee',
        sex: 'FEMALE',
        triage: triageB
      }),
      makeAdmission({
        id: 2,
        firstName: 'Carla',
        lastName: 'Cero',
        sex: 'FEMALE',
        triage: null
      }),
      makeAdmission({
        id: 3,
        firstName: 'Adam',
        lastName: 'Apple',
        sex: 'MALE',
        triage: triageA
      })
    ])

    await page.goto('/admissions')
    const cards = page.locator('.admission-card')
    await expect(cards).toHaveCount(3)
    // First card: triage A (Adam Apple); then B (Bea Bee); then untriaged (Carla Cero).
    await expect(cards.nth(0)).toContainText('Adam Apple')
    await expect(cards.nth(1)).toContainText('Bea Bee')
    await expect(cards.nth(2)).toContainText('Carla Cero')
  })

  test('card view exposes pagination for result sets larger than one page', async ({ page }) => {
    await setupAuth(page, { viewMode: 'cards', groupBy: 'none' })
    await setupCommonMocks(page)
    await mockPagedAdmissionsList(
      page,
      {
        0: Array.from({ length: 20 }, (_, index) =>
          makeAdmission({
            id: index + 1,
            firstName: `PageOne${index + 1}`,
            lastName: 'Patient',
            sex: index % 2 === 0 ? 'FEMALE' : 'MALE',
            triage: triageB
          })
        ),
        1: [
          makeAdmission({
            id: 21,
            firstName: 'LastPage',
            lastName: 'Patient',
            sex: 'FEMALE',
            triage: triageA
          })
        ]
      },
      21
    )

    await page.goto('/admissions')
    await expect(page.locator('.admission-card')).toHaveCount(20)
    await expect(page.locator('.p-paginator')).toBeVisible()

    await page.locator('.p-paginator-next').click()
    await expect(page.locator('.admission-card')).toHaveCount(1)
    await expect(page.locator('.admission-card').first()).toContainText('LastPage Patient')
  })
})
