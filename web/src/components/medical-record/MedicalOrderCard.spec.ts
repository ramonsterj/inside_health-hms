import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en.json'
import es from '@/i18n/locales/es.json'
import {
  MedicalOrderCategory,
  MedicalOrderStatus,
  type MedicalOrderResponse
} from '@/types/medicalRecord'
import MedicalOrderCard from './MedicalOrderCard.vue'

vi.mock('@/composables/useErrorHandler', () => ({
  useErrorHandler: () => ({ showError: vi.fn(), showSuccess: vi.fn() })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: vi.fn() })
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    hasPermission: () => true,
    isAuxiliaryNurseOnly: false,
    user: { roles: ['ADMINISTRADOR'] }
  })
}))

vi.mock('@/stores/medicalOrderDocument', () => ({
  useMedicalOrderDocumentStore: () => ({
    getDocuments: () => [],
    loading: false,
    fetchDocuments: vi.fn().mockResolvedValue(undefined)
  })
}))

vi.mock('@/stores/medicalOrder', () => ({
  useMedicalOrderStore: () => ({
    authorizeMedicalOrder: vi.fn().mockResolvedValue(undefined),
    markInProgress: vi.fn().mockResolvedValue(undefined)
  })
}))

const STAFF = { id: 7, salutation: null, firstName: 'Ana', lastName: 'López', roles: ['MEDICO'] }

function makeOrder(overrides: Partial<MedicalOrderResponse> = {}): MedicalOrderResponse {
  return {
    id: 1,
    admissionId: 10,
    category: MedicalOrderCategory.LABORATORIOS,
    startDate: '2026-06-12',
    endDate: null,
    medication: null,
    dosage: null,
    route: null,
    frequency: null,
    schedule: null,
    observations: null,
    status: MedicalOrderStatus.SOLICITADO,
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
    labProvider: { id: 1, name: 'CLONY' },
    labTests: [
      {
        id: 1,
        labProviderTestId: 11,
        labTestId: 3,
        displayName: 'Hemograma',
        salesPrice: 75,
        cost: 40
      }
    ],
    labTotal: 75,
    documentCount: 0,
    createdAt: '2026-06-12T08:00:00',
    updatedAt: null,
    createdBy: STAFF,
    updatedBy: null,
    ...overrides
  }
}

function mountCard(order: MedicalOrderResponse, admissionActive = true, canEdit = false) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: { en, es }
  })
  return mount(MedicalOrderCard, {
    props: { order, canEdit, admissionActive },
    global: {
      plugins: [i18n],
      stubs: {
        Card: { template: '<div class="p-card"><slot name="content" /></div>' },
        Button: { props: ['label'], template: '<button>{{ label }}</button>' },
        Badge: true,
        MedicalOrderStateBadge: true,
        MedicationAdministrationDialog: true,
        MedicationAdministrationHistory: true,
        MedicalOrderDocumentUploadDialog: true,
        MedicalOrderDocumentViewer: true,
        MedicalOrderRejectDialog: true,
        MedicalOrderEmergencyAuthorizeDialog: true,
        DocumentThumbnail: true
      }
    }
  })
}

function buttonLabels(wrapper: ReturnType<typeof mountCard>): string[] {
  return wrapper.findAll('button').map(b => b.text())
}

describe('MedicalOrderCard authorization-status presentation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the pending-authorization indicator with a neutral (non-green) accent for SOLICITADO', () => {
    const wrapper = mountCard(makeOrder({ status: MedicalOrderStatus.SOLICITADO }))

    expect(wrapper.find('.pending-auth-info').exists()).toBe(true)
    expect(wrapper.text()).toContain('Pending authorization')
    expect(wrapper.text()).not.toContain('Authorized by')

    const card = wrapper.find('.medical-order-card')
    expect(card.classes()).toContain('accent-info')
    expect(card.classes()).not.toContain('accent-success')
  })

  it('exposes authorize/reject actions only for SOLICITADO', () => {
    const pending = mountCard(makeOrder({ status: MedicalOrderStatus.SOLICITADO }))
    expect(buttonLabels(pending)).toContain('Authorize')
    expect(buttonLabels(pending)).toContain('Reject')
  })

  it('renders the rejection block with a danger accent and no "Authorized by" for NO_AUTORIZADO', () => {
    const wrapper = mountCard(
      makeOrder({
        status: MedicalOrderStatus.NO_AUTORIZADO,
        rejectionReason: 'Sin cobertura',
        rejectedAt: '2026-06-12T09:00:00',
        rejectedBy: STAFF
      })
    )

    expect(wrapper.find('.rejection-info').exists()).toBe(true)
    expect(wrapper.text()).toContain('Sin cobertura')
    expect(wrapper.find('.pending-auth-info').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Authorized by')
    expect(wrapper.find('.medical-order-card').classes()).toContain('accent-danger')
    expect(buttonLabels(wrapper)).not.toContain('Authorize')
  })

  it('renders "Authorized by" with a green accent for AUTORIZADO', () => {
    const wrapper = mountCard(
      makeOrder({
        status: MedicalOrderStatus.AUTORIZADO,
        authorizedAt: '2026-06-12T09:00:00',
        authorizedBy: STAFF
      })
    )

    expect(wrapper.text()).toContain('Authorized by')
    expect(wrapper.find('.pending-auth-info').exists()).toBe(false)
    expect(wrapper.find('.medical-order-card').classes()).toContain('accent-success')
    expect(buttonLabels(wrapper)).not.toContain('Authorize')
  })

  it('keeps the authorization audit row after a results-bearing order is marked in progress', () => {
    const wrapper = mountCard(
      makeOrder({
        status: MedicalOrderStatus.EN_PROCESO,
        authorizedAt: '2026-06-12T09:00:00',
        authorizedBy: STAFF,
        inProgressAt: '2026-06-12T09:30:00',
        inProgressBy: STAFF
      })
    )

    expect(wrapper.text()).toContain('Authorized by')
    expect(wrapper.text()).toContain('Marked in progress by')
    expect(wrapper.find('.medical-order-card').classes()).toContain('accent-warn')
  })

  it('renders the Edit action only when canEdit is true', () => {
    // The list passes canEdit=false for terminal orders (NO_AUTORIZADO / RESULTADOS_RECIBIDOS /
    // DESCONTINUADO) so the misleading edit dialog is unreachable for them.
    const rejected = makeOrder({ status: MedicalOrderStatus.NO_AUTORIZADO })
    expect(buttonLabels(mountCard(rejected, true, false))).not.toContain('Edit')
    expect(buttonLabels(mountCard(rejected, true, true))).toContain('Edit')
  })

  it('does not present a DESCONTINUADO-after-authorize order as currently authorized', () => {
    const wrapper = mountCard(
      makeOrder({
        status: MedicalOrderStatus.DESCONTINUADO,
        authorizedAt: '2026-06-12T09:00:00',
        authorizedBy: STAFF,
        discontinuedAt: '2026-06-12T10:00:00',
        discontinuedBy: STAFF
      })
    )

    // The "Authorized by" line is suppressed for discontinued-after-authorize orders.
    expect(wrapper.text()).not.toContain('Authorized by')
    expect(wrapper.find('.discontinued-info').exists()).toBe(true)
    expect(wrapper.find('.medical-order-card').classes()).toContain('accent-secondary')
  })
})
