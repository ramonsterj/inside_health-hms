import { test, expect, Page } from '@playwright/test'
import {
  waitForMedicalRecordTabs,
  selectMedicalRecordTab,
  expandAccordionPanel
} from './utils/test-helpers'

/**
 * Regression suite for spec v1.5 of `medical-psychiatric-record.md`:
 *
 *  1. Progress note SOAP fields render saved HTML (sanitized) — not as a single
 *     running line of literal `<p>` / `<ul>` text.
 *  2. Medical order `observations` renders saved HTML (sanitized) — same fix.
 *  3. `RichTextEditor` strips inline `style` attributes, `<span>` wrappers, and
 *     unsupported tags on paste so pasted content from Word / Google Docs /
 *     Chrome no longer pollutes the saved payload.
 *  4. The medical-order form field labelled "Insumo/servicio a facturar" (es)
 *     / "Billable supply / service" (en) is the one that links a medical order
 *     to an inventory item.
 *
 * These tests are intentionally written against the spec, not the current
 * buggy implementation — they will fail until ProgressNoteCard.vue and
 * MedicalOrderCard.vue switch from `{{ text }}` interpolation to sanitized
 * `v-html`, and until RichTextEditor registers a Quill clipboard matcher.
 */

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

// Server returns the same HTML the editor produced — the rendering bug lives
// on the display side, so we ship realistic HTML and assert it renders as DOM.
const formattedProgressNote = {
  id: 1,
  admissionId: 1,
  subjectiveData:
    '<p>Patient reports feeling <strong>much better</strong> today.</p>' +
    '<ul><li>Sleep improved</li><li>Appetite normal</li></ul>',
  objectiveData: '<p>BP 120/80. HR 72. <em>Calm</em> demeanor.</p>',
  analysis: '<p>Responding well to current regimen.</p>',
  actionPlans: '<ol><li>Continue sertraline 50mg</li><li>Schedule therapy session</li></ol>',
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

const formattedMedicationOrder = {
  id: 1,
  admissionId: 1,
  category: 'MEDICAMENTOS',
  startDate: '2026-01-24',
  endDate: null,
  medication: 'Sertraline',
  dosage: '50mg',
  route: 'ORAL',
  frequency: 'Once daily',
  schedule: 'Morning with breakfast',
  observations:
    '<p>Administer <strong>with food</strong>.</p>' +
    '<ul><li>Watch for nausea</li><li>Watch for dizziness</li></ul>',
  status: 'AUTORIZADO',
  discontinuedAt: null,
  discontinuedBy: null,
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

async function setupAuth(page: Page, user: typeof mockDoctorUser) {
  await page.addInitScript(userData => {
    localStorage.setItem('access_token', 'mock-access-token')
    localStorage.setItem('refresh_token', 'mock-refresh-token')
    localStorage.setItem('mock_user', JSON.stringify(userData))
  }, user)
}

async function setupUserMock(page: Page, user: typeof mockDoctorUser) {
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

async function setupAdmissionBaseMocks(page: Page) {
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
}

test.describe('Medical Record - rich-text formatting (spec v1.5)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.clear()
    })
  })

  test('progress note renders HTML formatting as DOM, not literal text', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionBaseMocks(page)

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
            content: [formattedProgressNote],
            page: { totalElements: 1, totalPages: 1, size: 10, number: 0 }
          }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'progressNotes')

    // Expand the card so all content is visible (collapsed view uses CSS
    // truncation but still renders HTML — both views preserve formatting).
    const card = page.locator('.progress-note-card').first()
    await expect(card).toBeVisible()
    await card.getByRole('button', { name: /Expand|Expandir/i }).click()

    // 1. Bold should render as a <strong> (or <b>) element with the right text,
    //    NOT as the literal string "<strong>much better</strong>".
    const subjectiveSection = card.locator('.soap-section').first()
    await expect(subjectiveSection.locator('strong, b').first()).toHaveText(/much better/i)

    // 2. The bullet list should render as <ul><li>, with each item on its own
    //    line. The visible text must not contain literal "<ul>" or "<li>".
    const subjectiveText = await subjectiveSection.innerText()
    expect(subjectiveText).not.toContain('<ul>')
    expect(subjectiveText).not.toContain('<li>')
    expect(subjectiveText).not.toContain('<p>')
    await expect(subjectiveSection.locator('ul li')).toHaveCount(2)
    await expect(subjectiveSection.locator('ul li').first()).toHaveText(/Sleep improved/)
    await expect(subjectiveSection.locator('ul li').nth(1)).toHaveText(/Appetite normal/)

    // 3. The action-plans field uses an ordered list — make sure <ol><li>
    //    renders and isn't shown as literal text.
    const planSection = card.locator('.soap-section').nth(3)
    await expect(planSection.locator('ol li')).toHaveCount(2)
    await expect(planSection.locator('ol li').first()).toHaveText(/sertraline/i)
  })

  test('medical order observations render HTML formatting as DOM', async ({ page }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionBaseMocks(page)

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

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              orders: {
                MEDICAMENTOS: [formattedMedicationOrder]
              }
            }
          })
        })
      }
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    // Orders are listed inside a category accordion — expand MEDICAMENTOS first
    await expandAccordionPanel(page, /Medications|Medicamentos/i)
    await expect(page.locator('.medical-order-card')).toBeVisible()

    const observations = page.locator('.observations').first()
    await expect(observations).toBeVisible()

    // Bold inside the first paragraph.
    await expect(observations.locator('strong, b').first()).toHaveText(/with food/i)

    // Bullet list with the two warning items, rendered as DOM.
    const items = observations.locator('ul li')
    await expect(items).toHaveCount(2)
    await expect(items.first()).toHaveText(/Watch for nausea/)
    await expect(items.nth(1)).toHaveText(/Watch for dizziness/)

    // The rendered text must NOT contain literal tags.
    const text = await observations.innerText()
    expect(text).not.toContain('<p>')
    expect(text).not.toContain('<ul>')
    expect(text).not.toContain('<li>')
    expect(text).not.toContain('<strong>')
  })

  test('RichTextEditor strips inline style attributes and unsupported tags on paste', async ({
    page
  }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionBaseMocks(page)

    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      const method = route.request().method()
      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { orders: {} } })
        })
      } else if (method === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: formattedMedicationOrder })
        })
      }
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

    await page.route('**/api/v1/inventory-items*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [], page: { totalElements: 0, totalPages: 0, size: 10, number: 0 } }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    await page.getByRole('button', { name: /Add First Order|Agregar Primera Orden/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Fill the minimum required fields. The form has a category Select and a
    // start-date DatePicker — this test focuses on the observations editor.
    // Other fields can rely on existing tests for the full create flow.

    // Find the observations editor (Quill .ql-editor) and dispatch a paste
    // event with rich HTML straight off a Google Docs / Word copy.
    const editor = page.locator('.rich-text-editor .ql-editor').last()
    await editor.click()

    const pastedHtml =
      '<p><span style="background-color: transparent; color: rgb(0, 0, 0);">Administer with food.</span></p>' +
      '<ul><li style="font-family: Arial; color: red;">Watch for nausea</li><li>Watch for dizziness</li></ul>'

    await page.evaluate(html => {
      const target = document.querySelector('.rich-text-editor .ql-editor') as HTMLElement | null
      if (!target) throw new Error('No .ql-editor target found')
      target.focus()
      const dt = new DataTransfer()
      dt.setData('text/html', html)
      dt.setData('text/plain', 'Administer with food.\nWatch for nausea\nWatch for dizziness')
      const evt = new ClipboardEvent('paste', {
        clipboardData: dt,
        bubbles: true,
        cancelable: true
      })
      target.dispatchEvent(evt)
    }, pastedHtml)

    // The editor must show the pasted text but none of the styling noise the
    // customer reported (inline styles, color/background, font-family from
    // Word / Google Docs / Chrome pastes). The customer specifically
    // complained about `<span style="background-color: transparent; color:
    // rgb(0,0,0);">` and similar artifacts surfacing as literal text on
    // cards — those are what we have to keep out of the editor.
    const editorHtml = await editor.innerHTML()
    expect(editorHtml).toContain('Administer with food')
    expect(editorHtml).toContain('Watch for nausea')
    expect(editorHtml).toContain('Watch for dizziness')

    expect(editorHtml).not.toMatch(/style\s*=/i)
    expect(editorHtml).not.toMatch(/<span\s+style/i)
    expect(editorHtml).not.toMatch(/background-color/i)
    expect(editorHtml).not.toMatch(/font-family/i)
    expect(editorHtml).not.toMatch(/color:\s*red/i)
    expect(editorHtml).not.toMatch(/color:\s*rgb/i)

    // Visible <span> elements that wrap *user content* must be gone (Quill
    // 2.x emits empty `<span class="ql-ui" contenteditable="false">` markers
    // inside <li> for bullet rendering — those are internal layout, not
    // pasted content, and carry no styles, so we filter them out before
    // asserting).
    const contentSpans = await editor
      .locator('span:not(.ql-ui)')
      .evaluateAll(els =>
        els.filter(e => e.textContent && e.textContent.trim().length > 0).map(e => e.outerHTML)
      )
    expect(contentSpans).toEqual([])

    // The bullet structure must be preserved. Quill normalizes `<ul><li>` to
    // `<ol><li data-list="bullet">` internally — both forms render as a
    // bulleted list, so accept either.
    await expect(editor.locator('li')).toHaveCount(2)

    // We don't assert the persisted payload here because the form requires
    // additional fields to validate; the paste-sanitization invariant we
    // care about is that the editor itself does not retain the noise. A
    // companion unit test on `sanitizeHtml` plus the display test above
    // closes the round trip.
  })

  test('medical-order form labels inventory link as "Billable supply / service" / "Insumo a facturar"', async ({
    page
  }) => {
    await setupAuth(page, mockDoctorUser)
    await setupUserMock(page, mockDoctorUser)
    await setupAdmissionBaseMocks(page)

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
    await page.route('**/api/v1/admissions/1/medical-orders', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { orders: {} } })
        })
      }
    })
    await page.route('**/api/v1/inventory-items*', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: [], page: { totalElements: 0, totalPages: 0, size: 10, number: 0 } }
        })
      })
    })

    await page.goto('/admissions/1')
    await waitForMedicalRecordTabs(page)
    await selectMedicalRecordTab(page, 'medicalOrders')

    await page.getByRole('button', { name: /Add First Order|Agregar Primera Orden/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Pick a billable category (MEDICAMENTOS) so the inventory-link field is
    // rendered, then assert its label.
    const categorySelect = page.getByRole('dialog').locator('.p-select').first()
    await categorySelect.click()
    await page.getByRole('option', { name: /Medicamentos|Medications/i }).click()

    // The renamed label — accept both locales because the test runs against
    // whatever the mock user's localePreference resolves to in CI.
    const label = page
      .getByRole('dialog')
      .getByText(/Billable supply\s*\/\s*service|Insumo\s*\/\s*servicio a facturar/i)
    await expect(label).toBeVisible()

    // Make sure the legacy strings are no longer in the DOM.
    await expect(page.getByRole('dialog').getByText(/Servicio\/Artículo vinculado/i)).toHaveCount(0)
    await expect(page.getByRole('dialog').getByText(/^Linked Service\/Item$/i)).toHaveCount(0)
  })
})
