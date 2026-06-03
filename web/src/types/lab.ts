// Lab catalog types (providers, canonical tests, per-provider priced tests, panels)
// and panel resolution. See docs/features/laboratory-orders-with-providers.md.

export interface LabProvider {
  id: number
  name: string
  code: string | null
  active: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

export interface LabTest {
  id: number
  name: string
  active: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

export interface LabProviderTest {
  id: number
  providerId: number
  labTestId: number
  labTestName: string
  displayName: string
  cost: number
  salesPrice: number
  active: boolean
}

export interface LabPanelItem {
  labTestId: number
  labTestName: string
}

export interface LabPanel {
  id: number
  name: string
  active: boolean
  items: LabPanelItem[]
}

export interface ResolvedPanelTest {
  labProviderTestId: number
  labTestId: number
  displayName: string
  salesPrice: number
}

export interface UnmatchedPanelTest {
  labTestId: number
  name: string
}

export interface PanelResolution {
  panelId: number
  providerId: number
  matched: ResolvedPanelTest[]
  unmatchedTests: UnmatchedPanelTest[]
}

// ===== Request DTOs =====

export interface CreateLabProviderRequest {
  name: string
  code?: string | null
  active: boolean
}

export type UpdateLabProviderRequest = CreateLabProviderRequest

export interface CreateLabTestRequest {
  name: string
  active: boolean
}

export type UpdateLabTestRequest = CreateLabTestRequest

export interface CreateLabProviderTestRequest {
  labTestId: number
  displayName?: string | null
  cost: number
  salesPrice: number
  active: boolean
}

export interface UpdateLabProviderTestRequest {
  displayName?: string | null
  cost: number
  salesPrice: number
  active: boolean
}

export interface CreateLabPanelRequest {
  name: string
  active: boolean
  labTestIds: number[]
}

export type UpdateLabPanelRequest = CreateLabPanelRequest
