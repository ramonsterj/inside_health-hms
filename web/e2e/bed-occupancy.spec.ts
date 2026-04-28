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
    'admission:create',
    'admission:read',
    'patient:read',
    'room:read',
    'room:occupancy-view'
  ],
  status: 'ACTIVE',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
  localePreference: 'en'
}

// ── Test Data ─────────────────────────────────────────────────────────────────

const occupancyResponse = {
  summary: {
    totalBeds: 7,
    occupiedBeds: 3,
    freeBeds: 4,
    occupancyPercent: 42.9
  },
  rooms: [
    // FEMALE — fully occupied
    {
      id: 1,
      number: '101',
      type: 'PRIVATE',
      gender: 'FEMALE',
      capacity: 1,
      occupiedBeds: 1,
      availableBeds: 0,
      occupants: [
        {
          admissionId: 11,
          patientId: 101,
          patientName: 'Juana Pérez',
          admissionDate: '2026-04-22'
        }
      ]
    },
    // FEMALE — fully free
    {
      id: 2,
      number: '102',
      type: 'PRIVATE',
      gender: 'FEMALE',
      capacity: 1,
      occupiedBeds: 0,
      availableBeds: 1,
      occupants: []
    },
    // MALE — partially occupied (used by search-highlight test)
    {
      id: 3,
      number: '201',
      type: 'SHARED',
      gender: 'MALE',
      capacity: 4,
      occupiedBeds: 2,
      availableBeds: 2,
      occupants: [
        {
          admissionId: 21,
          patientId: 201,
          patientName: 'Pedro García',
          admissionDate: '2026-04-20'
        },
        {
          admissionId: 22,
          patientId: 202,
          patientName: 'Diego Morales',
          admissionDate: '2026-04-21'
        }
      ]
    },
    // MALE — fully free
    {
      id: 4,
      number: '301',
      type: 'PRIVATE',
      gender: 'MALE',
      capacity: 1,
      occupiedBeds: 0,
      availableBeds: 1,
      occupants: []
    }
  ]
}

// ── Page Setup ───────────────────────────────────────────────────────────────

async function setupAuth(page: Page) {
  await page.addInitScript(user => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(user))
  }, mockAdminStaffUser)
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

async function mockOccupancy(page: Page, body: typeof occupancyResponse) {
  await page.route('**/api/v1/rooms/occupancy', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: body })
    })
  })
}

// ── Tests ─────────────────────────────────────────────────────────────────────

test.describe('Bed occupancy view', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear())
  })

  test('renders summary numbers and rooms grouped by gender', async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
    await mockOccupancy(page, occupancyResponse)

    await page.goto('/bed-occupancy')

    await expect(page.getByRole('heading', { name: /Bed Occupancy/i })).toBeVisible()

    // Summary tiles reflect the API payload.
    await expect(page.locator('.summary-card--total .summary-value')).toHaveText('7')
    await expect(page.locator('.summary-card--occupied .summary-value')).toHaveText('3')
    await expect(page.locator('.summary-card--free .summary-value')).toHaveText('4')
    await expect(page.locator('.summary-card--percent .summary-value')).toHaveText('42.9%')

    // Gender groups: FEMALE sorts before MALE alphabetically.
    const groupTitles = page.locator('.gender-group .group-title')
    await expect(groupTitles).toHaveCount(2)
    await expect(groupTitles.nth(0)).toHaveText(/Women/i)
    await expect(groupTitles.nth(1)).toHaveText(/Men/i)

    // One card per room, each sized to its capacity.
    await expect(page.locator('.room-card')).toHaveCount(4)
    const room201 = page.locator('.room-card', { hasText: 'Room 201' })
    await expect(room201.locator('.bed-slot')).toHaveCount(4) // capacity = 4
    await expect(room201.locator('.bed-slot--occupied')).toHaveCount(2)
    await expect(room201.locator('.bed-slot--free')).toHaveCount(2)

    // Occupants render with patient name.
    await expect(page.locator('.patient-link', { hasText: 'Juana Pérez' })).toBeVisible()
    await expect(page.locator('.patient-link', { hasText: 'Pedro García' })).toBeVisible()
  })

  test('"Free only" status filter hides fully-occupied rooms', async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
    await mockOccupancy(page, occupancyResponse)

    await page.goto('/bed-occupancy')
    await expect(page.locator('.room-card')).toHaveCount(4)

    await page.locator('#bed-status-filter').click()
    await page.locator('.p-select-option').filter({ hasText: 'Free only' }).click()

    // Room 101 has 0 free beds → hidden. Other 3 remain.
    await expect(page.locator('.room-card')).toHaveCount(3)
    await expect(page.locator('.room-card', { hasText: 'Room 101' })).toHaveCount(0)
    await expect(page.locator('.room-card', { hasText: 'Room 102' })).toBeVisible()
    await expect(page.locator('.room-card', { hasText: 'Room 201' })).toBeVisible()
    await expect(page.locator('.room-card', { hasText: 'Room 301' })).toBeVisible()
  })

  test('search by patient name filters cards and highlights matching slot', async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
    await mockOccupancy(page, occupancyResponse)

    await page.goto('/bed-occupancy')
    await expect(page.locator('.room-card')).toHaveCount(4)

    await page.locator('#bed-search').fill('Pedro')

    // Only Room 201 (Pedro García's room) survives the filter.
    await expect(page.locator('.room-card')).toHaveCount(1)
    const room201 = page.locator('.room-card', { hasText: 'Room 201' })
    await expect(room201).toBeVisible()

    // Pedro's slot gets the highlight ring; Diego's (same room) does not.
    const pedroSlot = room201.locator('.bed-slot--occupied', { hasText: 'Pedro García' })
    const diegoSlot = room201.locator('.bed-slot--occupied', { hasText: 'Diego Morales' })
    await expect(pedroSlot).toHaveClass(/bed-slot--highlighted/)
    await expect(diegoSlot).not.toHaveClass(/bed-slot--highlighted/)
  })

  test('"Admit here" deep-links to /patients?admitToRoom=<id>', async ({ page }) => {
    await setupAuth(page)
    await setupCommonMocks(page)
    await mockOccupancy(page, occupancyResponse)

    // PatientsView fetches the patient list on load — keep it empty so the
    // landing page is stable.
    await page.route('**/api/patients*', async route => {
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
    })

    await page.goto('/bed-occupancy')

    // Click "Admit here" on Room 102's free slot (first fully-free room).
    const room102 = page.locator('.room-card', { hasText: 'Room 102' })
    await room102.getByRole('button', { name: /Admit here/i }).click()

    // Deep-link forwards the room id chosen from the empty bed slot.
    await expect(page).toHaveURL(/\/patients\?admitToRoom=2$/)
  })
})
