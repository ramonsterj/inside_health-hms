import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import PrimeVue from 'primevue/config'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import { MedicalOrderCategory, MedicalOrderStatus } from '@/types/medicalRecord'
import type { LabPanel, LabProvider, LabProviderTest } from '@/types/lab'
import MedicalOrderFormDialog from './MedicalOrderFormDialog.vue'

vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({ showError: vi.fn(), showSuccess: vi.fn() })
}))

const providers: LabProvider[] = [{ id: 1, name: 'CLONY', code: 'CLONY', active: true }]
const panels: LabPanel[] = [
  {
    id: 5,
    name: 'Laboratorios de ingreso',
    active: true,
    items: [{ labTestId: 3, labTestName: 'Hematología' }]
  }
]
const clonyTests: LabProviderTest[] = [
  {
    id: 11,
    providerId: 1,
    labTestId: 3,
    labTestName: 'Hematología',
    displayName: 'Hemograma completo',
    cost: 40,
    salesPrice: 75,
    active: true
  }
]

const resolvePanelMock = vi.fn()
const fetchProviderTestsMock = vi.fn().mockResolvedValue(clonyTests)

vi.mock('@/stores/labCatalog', () => ({
  useLabCatalogStore: () => ({
    providers,
    panels,
    fetchProviders: vi.fn().mockResolvedValue(undefined),
    fetchPanels: vi.fn().mockResolvedValue(undefined),
    fetchProviderTests: fetchProviderTestsMock,
    getProviderTests: (id: number | null | undefined) => (id === 1 ? clonyTests : []),
    resolvePanel: resolvePanelMock
  })
}))

vi.mock('@/stores/medicalOrder', () => ({
  useMedicalOrderStore: () => ({
    createMedicalOrder: vi.fn().mockResolvedValue(undefined),
    updateMedicalOrder: vi.fn().mockResolvedValue(undefined)
  })
}))

vi.mock('@/stores/inventoryItem', () => ({
  useInventoryItemStore: () => ({ items: [], fetchItems: vi.fn().mockResolvedValue(undefined) })
}))

vi.mock('@/stores/pharmacy', () => ({
  usePharmacyStore: () => ({ items: [], fetchMedications: vi.fn().mockResolvedValue(undefined) })
}))

function i18n() {
  return createI18n({ legacy: false, locale: 'en', fallbackLocale: 'en', messages: { en, es } })
}

function mountDialog() {
  return mount(MedicalOrderFormDialog, {
    props: { visible: true, admissionId: 1, order: null },
    global: {
      plugins: [PrimeVue, i18n()],
      directives: { tooltip: {} }
    }
  })
}

describe('MedicalOrderFormDialog lab branch', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  it('shows the lab provider field and hides the inventory picker for LABORATORIOS', async () => {
    const wrapper = mountDialog()
    await flushPromises()
    // Switch to LABORATORIOS via the exposed form value.
    const vm = wrapper.vm as unknown as { category: MedicalOrderCategory }
    vm.category = MedicalOrderCategory.LABORATORIOS
    await flushPromises()

    const body = document.body.textContent ?? ''
    expect(body).toContain('Lab provider')
    expect(body).not.toContain('Billable supply / service')
  })

  it('applies a panel: resolves and surfaces the unmatched notice', async () => {
    resolvePanelMock.mockResolvedValue({
      panelId: 5,
      providerId: 1,
      matched: [
        { labProviderTestId: 11, labTestId: 3, displayName: 'Hemograma completo', salesPrice: 75 }
      ],
      unmatchedTests: [{ labTestId: 15, name: 'Panel de drogas en sangre' }]
    })

    const wrapper = mountDialog()
    await flushPromises()
    const vm = wrapper.vm as unknown as {
      category: MedicalOrderCategory
      labProviderId: number | null
      selectedPanelId: number | null
      applyPanel: () => Promise<void>
      labProviderTestIds: number[]
      panelNotice: { provider: string; tests: string } | null
    }
    vm.category = MedicalOrderCategory.LABORATORIOS
    await flushPromises()
    vm.labProviderId = 1
    await flushPromises()
    vm.selectedPanelId = 5
    await vm.applyPanel()
    await flushPromises()

    expect(resolvePanelMock).toHaveBeenCalledWith(5, 1)
    expect(vm.labProviderTestIds).toContain(11)
    expect(vm.panelNotice?.tests).toContain('Panel de drogas en sangre')
  })

  it('locks the category select and shows the locked-on-edit hint in edit mode', async () => {
    const wrapper = mount(MedicalOrderFormDialog, {
      props: {
        visible: true,
        admissionId: 1,
        order: {
          id: 9,
          admissionId: 1,
          category: MedicalOrderCategory.ORDENES_MEDICAS,
          status: MedicalOrderStatus.ACTIVA,
          labProvider: null,
          labTests: [],
          labTotal: null,
          startDate: '2026-06-03',
          endDate: null,
          medication: null,
          dosage: null,
          route: null,
          frequency: null,
          schedule: null,
          observations: null,
          authorizedAt: null,
          authorizedBy: null,
          inProgressAt: null,
          inProgressBy: null,
          resultsReceivedAt: null,
          resultsReceivedBy: null,
          rejectedAt: null,
          rejectedBy: null,
          rejectionReason: null,
          emergencyAuthorized: false,
          emergencyReason: null,
          emergencyReasonNote: null,
          emergencyAt: null,
          emergencyBy: null,
          discontinuedAt: null,
          discontinuedBy: null,
          inventoryItemId: null,
          inventoryItemName: null,
          documentCount: 0,
          createdAt: null,
          updatedAt: null,
          createdBy: null,
          updatedBy: null
        }
      },
      global: { plugins: [PrimeVue, i18n()], directives: { tooltip: {} } }
    })
    await flushPromises()

    // The Dialog teleports its content to document.body.
    expect((wrapper.vm as unknown as { isEditMode: boolean }).isEditMode).toBe(true)
    const category = document.body.querySelector('#category')
    expect(category?.classList.contains('p-disabled')).toBe(true)
    expect(document.body.textContent).toContain("Category can't be changed when editing.")
  })

  it('does not show the category lock hint when creating', async () => {
    mountDialog()
    await flushPromises()
    expect(document.body.textContent).not.toContain("Category can't be changed when editing.")
  })

  it('locks lab fields when editing an already-authorized order (AC14)', async () => {
    const wrapper = mount(MedicalOrderFormDialog, {
      props: {
        visible: true,
        admissionId: 1,
        order: {
          id: 9,
          admissionId: 1,
          category: MedicalOrderCategory.LABORATORIOS,
          status: MedicalOrderStatus.AUTORIZADO,
          labProvider: { id: 1, name: 'CLONY' },
          labTests: [],
          labTotal: 75,
          startDate: '2026-06-03',
          endDate: null,
          medication: null,
          dosage: null,
          route: null,
          frequency: null,
          schedule: null,
          observations: null,
          authorizedAt: null,
          authorizedBy: null,
          inProgressAt: null,
          inProgressBy: null,
          resultsReceivedAt: null,
          resultsReceivedBy: null,
          rejectedAt: null,
          rejectedBy: null,
          rejectionReason: null,
          emergencyAuthorized: false,
          emergencyReason: null,
          emergencyReasonNote: null,
          emergencyAt: null,
          emergencyBy: null,
          discontinuedAt: null,
          discontinuedBy: null,
          inventoryItemId: null,
          inventoryItemName: null,
          documentCount: 0,
          createdAt: null,
          updatedAt: null,
          createdBy: null,
          updatedBy: null
        }
      },
      global: { plugins: [PrimeVue, i18n()], directives: { tooltip: {} } }
    })
    await flushPromises()
    const vm = wrapper.vm as unknown as { labFieldsEditable: boolean }
    expect(vm.labFieldsEditable).toBe(false)
  })
})
