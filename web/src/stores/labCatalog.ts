import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type { ApiResponse } from '@/types'
import type {
  CreateLabPanelRequest,
  CreateLabProviderRequest,
  CreateLabProviderTestRequest,
  CreateLabTestRequest,
  LabPanel,
  LabProvider,
  LabProviderTest,
  LabTest,
  PanelResolution,
  UpdateLabPanelRequest,
  UpdateLabProviderRequest,
  UpdateLabProviderTestRequest,
  UpdateLabTestRequest
} from '@/types/lab'

export const useLabCatalogStore = defineStore('labCatalog', () => {
  const providers = ref<LabProvider[]>([])
  const tests = ref<LabTest[]>([])
  const panels = ref<LabPanel[]>([])
  // Per-provider catalog cache, keyed by provider id.
  const providerTests = ref<Map<number, LabProviderTest[]>>(new Map())
  const loading = ref(false)

  function unwrap<T>(response: { data: ApiResponse<T> }, fallback: string): T {
    if (response.data.success && response.data.data !== null && response.data.data !== undefined) {
      return response.data.data
    }
    throw new Error(response.data.message || fallback)
  }

  // ===================== Providers =====================

  async function fetchProviders(activeOnly = false): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<LabProvider[]>>('/v1/lab/providers', {
        params: { activeOnly }
      })
      providers.value = unwrap(response, 'Failed to load providers')
    } finally {
      loading.value = false
    }
  }

  async function createProvider(data: CreateLabProviderRequest): Promise<LabProvider> {
    const response = await api.post<ApiResponse<LabProvider>>('/v1/lab/providers', data)
    return unwrap(response, 'Create provider failed')
  }

  async function updateProvider(id: number, data: UpdateLabProviderRequest): Promise<LabProvider> {
    const response = await api.put<ApiResponse<LabProvider>>(`/v1/lab/providers/${id}`, data)
    return unwrap(response, 'Update provider failed')
  }

  async function deleteProvider(id: number): Promise<void> {
    const response = await api.delete<ApiResponse<void>>(`/v1/lab/providers/${id}`)
    if (!response.data.success) throw new Error(response.data.message || 'Delete provider failed')
  }

  // ===================== Canonical tests =====================

  async function fetchTests(activeOnly = false): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<LabTest[]>>('/v1/lab/tests', {
        params: { activeOnly }
      })
      tests.value = unwrap(response, 'Failed to load tests')
    } finally {
      loading.value = false
    }
  }

  async function createTest(data: CreateLabTestRequest): Promise<LabTest> {
    const response = await api.post<ApiResponse<LabTest>>('/v1/lab/tests', data)
    return unwrap(response, 'Create test failed')
  }

  async function updateTest(id: number, data: UpdateLabTestRequest): Promise<LabTest> {
    const response = await api.put<ApiResponse<LabTest>>(`/v1/lab/tests/${id}`, data)
    return unwrap(response, 'Update test failed')
  }

  async function deleteTest(id: number): Promise<void> {
    const response = await api.delete<ApiResponse<void>>(`/v1/lab/tests/${id}`)
    if (!response.data.success) throw new Error(response.data.message || 'Delete test failed')
  }

  // ===================== Provider tests =====================

  async function fetchProviderTests(
    providerId: number,
    activeOnly = false
  ): Promise<LabProviderTest[]> {
    const response = await api.get<ApiResponse<LabProviderTest[]>>(
      `/v1/lab/providers/${providerId}/tests`,
      { params: { activeOnly } }
    )
    const rows = unwrap(response, 'Failed to load provider tests')
    providerTests.value.set(providerId, rows)
    return rows
  }

  function getProviderTests(providerId: number | null | undefined): LabProviderTest[] {
    if (providerId == null) return []
    return providerTests.value.get(providerId) ?? []
  }

  async function createProviderTest(
    providerId: number,
    data: CreateLabProviderTestRequest
  ): Promise<LabProviderTest> {
    const response = await api.post<ApiResponse<LabProviderTest>>(
      `/v1/lab/providers/${providerId}/tests`,
      data
    )
    return unwrap(response, 'Create provider test failed')
  }

  async function updateProviderTest(
    id: number,
    data: UpdateLabProviderTestRequest
  ): Promise<LabProviderTest> {
    const response = await api.put<ApiResponse<LabProviderTest>>(
      `/v1/lab/provider-tests/${id}`,
      data
    )
    return unwrap(response, 'Update provider test failed')
  }

  async function deleteProviderTest(id: number): Promise<void> {
    const response = await api.delete<ApiResponse<void>>(`/v1/lab/provider-tests/${id}`)
    if (!response.data.success)
      throw new Error(response.data.message || 'Delete provider test failed')
  }

  // ===================== Panels =====================

  async function fetchPanels(): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<LabPanel[]>>('/v1/lab/panels')
      panels.value = unwrap(response, 'Failed to load panels')
    } finally {
      loading.value = false
    }
  }

  async function createPanel(data: CreateLabPanelRequest): Promise<LabPanel> {
    const response = await api.post<ApiResponse<LabPanel>>('/v1/lab/panels', data)
    return unwrap(response, 'Create panel failed')
  }

  async function updatePanel(id: number, data: UpdateLabPanelRequest): Promise<LabPanel> {
    const response = await api.put<ApiResponse<LabPanel>>(`/v1/lab/panels/${id}`, data)
    return unwrap(response, 'Update panel failed')
  }

  async function deletePanel(id: number): Promise<void> {
    const response = await api.delete<ApiResponse<void>>(`/v1/lab/panels/${id}`)
    if (!response.data.success) throw new Error(response.data.message || 'Delete panel failed')
  }

  async function resolvePanel(panelId: number, providerId: number): Promise<PanelResolution> {
    const response = await api.get<ApiResponse<PanelResolution>>(
      `/v1/lab/panels/${panelId}/resolve`,
      { params: { providerId } }
    )
    return unwrap(response, 'Resolve panel failed')
  }

  return {
    providers,
    tests,
    panels,
    providerTests,
    loading,
    fetchProviders,
    createProvider,
    updateProvider,
    deleteProvider,
    fetchTests,
    createTest,
    updateTest,
    deleteTest,
    fetchProviderTests,
    getProviderTests,
    createProviderTest,
    updateProviderTest,
    deleteProviderTest,
    fetchPanels,
    createPanel,
    updatePanel,
    deletePanel,
    resolvePanel
  }
})
