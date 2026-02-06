import { Page, expect } from '@playwright/test'

/**
 * Wait for PrimeVue overlay masks to clear (dialog animations)
 */
export async function waitForOverlaysToClear(page: Page) {
  const mask = page.locator('.p-dialog-mask, .p-overlay-mask')
  const count = await mask.count()
  if (count > 0) {
    await expect(mask.first()).not.toBeVisible({ timeout: 5000 }).catch(() => {})
  }
}

/**
 * Standardized confirm dialog interaction
 * Handles PrimeVue ConfirmDialog with animation-safe clicking
 */
export async function confirmDialogAccept(page: Page) {
  const confirmDialog = page.locator('.p-confirmdialog').first()
  await expect(confirmDialog).toBeVisible({ timeout: 5000 })

  // Wait for animation to settle (PrimeVue overlay has enter animations)
  await page.waitForTimeout(300)

  // Get the current dialog count before clicking
  const initialCount = await page.locator('.p-confirmdialog').count()

  // Use JavaScript to click the button to ensure Vue event handlers are triggered
  await page.evaluate(() => {
    const btn = document.querySelector('.p-confirmdialog .p-confirmdialog-accept-button') as HTMLButtonElement
    if (btn) btn.click()
  })

  // Wait for dialog count to decrease (meaning our dialog closed)
  // This handles the case where another dialog might appear
  await page.waitForFunction(
    (count) => document.querySelectorAll('.p-confirmdialog').length < count,
    initialCount,
    { timeout: 10000 }
  )
  await waitForOverlaysToClear(page)
}

/**
 * Standardized confirm dialog cancel
 */
export async function confirmDialogCancel(page: Page) {
  const confirmDialog = page.locator('.p-confirmdialog').first()
  await expect(confirmDialog).toBeVisible({ timeout: 5000 })

  // Wait for animation to settle
  await page.waitForTimeout(300)

  // Use JavaScript to click the button to ensure Vue event handlers are triggered
  await page.evaluate(() => {
    const btn = document.querySelector('.p-confirmdialog .p-confirmdialog-reject-button') as HTMLButtonElement
    if (btn) btn.click()
  })

  await expect(confirmDialog).not.toBeVisible({ timeout: 5000 })
  await waitForOverlaysToClear(page)
}

/**
 * Wait for table data to load (no "No data" message)
 */
export async function waitForTableData(page: Page, expectedText?: string) {
  if (expectedText) {
    await expect(page.getByText(expectedText)).toBeVisible({ timeout: 10000 })
  }
  // Ensure "No data" or loading states are gone
  await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10000 })
}

/**
 * Fill a RichTextEditor (PrimeVue Editor/Quill) with content
 * Targets the .ql-editor contenteditable inside the editor container
 */
export async function fillRichTextEditor(
  page: Page,
  editorContainer: string,
  content: string
) {
  const editor = page.locator(`${editorContainer} .ql-editor`)
  await editor.click()
  await editor.fill(content)
}

/**
 * Select a tab in the Medical Record tabs component
 */
export async function selectMedicalRecordTab(
  page: Page,
  tabKey: 'clinicalHistory' | 'progressNotes' | 'medicalOrders' | 'psychotherapyActivities' | 'nursingNotes' | 'vitalSigns'
) {
  const tabLabels: Record<string, RegExp> = {
    clinicalHistory: /Clinical History|Historia Clínica/i,
    progressNotes: /Progress Notes|Notas de Evolución/i,
    medicalOrders: /Medical Orders|Órdenes Médicas/i,
    psychotherapyActivities: /Psychotherapeutic Activities|Actividades Psicoterapéuticas/i,
    nursingNotes: /Nursing Notes|Notas de Enfermería/i,
    vitalSigns: /Vital Signs|Signos Vitales/i
  }
  const tab = page.locator('.p-tablist').getByText(tabLabels[tabKey])
  await tab.click()
  // Wait for animation
  await page.waitForTimeout(300)
}

/**
 * Wait for Medical Record tabs to be visible
 */
export async function waitForMedicalRecordTabs(page: Page) {
  await expect(page.getByText(/Medical Record|Expediente Médico/i).first()).toBeVisible({
    timeout: 10000
  })
}

/**
 * Expand an accordion panel by header text
 */
export async function expandAccordionPanel(page: Page, headerText: string | RegExp) {
  const panel = page.locator('.p-accordionheader').filter({ hasText: headerText })
  const isExpanded = (await panel.getAttribute('aria-expanded')) === 'true'
  if (!isExpanded) {
    await panel.click()
    await page.waitForTimeout(300)
  }
}
