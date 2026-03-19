import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'
import type {
  BankStatement,
  BankStatementRow,
  ColumnMapping,
  SaveColumnMappingRequest,
  MatchRowRequest,
  AcknowledgeRowRequest,
  CreateExpenseFromRowRequest,
  CreateIncomeFromRowRequest
} from '@/types/treasury'
import type { ApiResponse } from '@/types'

export const useBankStatementStore = defineStore('bankStatement', () => {
  const statements = ref<BankStatement[]>([])
  const currentStatement = ref<BankStatement | null>(null)
  const rows = ref<BankStatementRow[]>([])
  const columnMapping = ref<ColumnMapping | null>(null)
  const loading = ref(false)

  const baseUrl = (bankAccountId: number) =>
    `/v1/treasury/bank-accounts/${bankAccountId}/statements`

  async function fetchStatements(bankAccountId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<BankStatement[]>>(baseUrl(bankAccountId))
      if (response.data.success && response.data.data) {
        statements.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchStatement(bankAccountId: number, statementId: number): Promise<BankStatement> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<BankStatement>>(
        `${baseUrl(bankAccountId)}/${statementId}`
      )
      if (response.data.success && response.data.data) {
        currentStatement.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Statement not found')
    } finally {
      loading.value = false
    }
  }

  async function uploadStatement(
    bankAccountId: number,
    file: File,
    statementDate: string
  ): Promise<BankStatement> {
    loading.value = true
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('statementDate', statementDate)
      const response = await api.post<ApiResponse<BankStatement>>(
        baseUrl(bankAccountId),
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      )
      if (response.data.success && response.data.data) {
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to upload statement')
    } finally {
      loading.value = false
    }
  }

  async function deleteStatement(bankAccountId: number, statementId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.delete<ApiResponse<void>>(
        `${baseUrl(bankAccountId)}/${statementId}`
      )
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to delete statement')
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchRows(bankAccountId: number, statementId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<BankStatementRow[]>>(
        `${baseUrl(bankAccountId)}/${statementId}/rows`
      )
      if (response.data.success && response.data.data) {
        rows.value = response.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function confirmMatch(
    bankAccountId: number,
    statementId: number,
    rowId: number
  ): Promise<BankStatementRow> {
    const response = await api.post<ApiResponse<BankStatementRow>>(
      `${baseUrl(bankAccountId)}/${statementId}/rows/${rowId}/confirm`
    )
    if (response.data.success && response.data.data) {
      updateRowInList(response.data.data)
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to confirm match')
  }

  async function rejectMatch(
    bankAccountId: number,
    statementId: number,
    rowId: number
  ): Promise<BankStatementRow> {
    const response = await api.post<ApiResponse<BankStatementRow>>(
      `${baseUrl(bankAccountId)}/${statementId}/rows/${rowId}/reject`
    )
    if (response.data.success && response.data.data) {
      updateRowInList(response.data.data)
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to reject match')
  }

  async function manualMatch(
    bankAccountId: number,
    statementId: number,
    rowId: number,
    request: MatchRowRequest
  ): Promise<BankStatementRow> {
    const response = await api.post<ApiResponse<BankStatementRow>>(
      `${baseUrl(bankAccountId)}/${statementId}/rows/${rowId}/match`,
      request
    )
    if (response.data.success && response.data.data) {
      updateRowInList(response.data.data)
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to match row')
  }

  async function acknowledgeRow(
    bankAccountId: number,
    statementId: number,
    rowId: number,
    request: AcknowledgeRowRequest
  ): Promise<BankStatementRow> {
    const response = await api.post<ApiResponse<BankStatementRow>>(
      `${baseUrl(bankAccountId)}/${statementId}/rows/${rowId}/acknowledge`,
      request
    )
    if (response.data.success && response.data.data) {
      updateRowInList(response.data.data)
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to acknowledge row')
  }

  async function createExpenseFromRow(
    bankAccountId: number,
    statementId: number,
    rowId: number,
    request: CreateExpenseFromRowRequest
  ): Promise<BankStatementRow> {
    const response = await api.post<ApiResponse<BankStatementRow>>(
      `${baseUrl(bankAccountId)}/${statementId}/rows/${rowId}/create-expense`,
      request
    )
    if (response.data.success && response.data.data) {
      updateRowInList(response.data.data)
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to create expense')
  }

  async function createIncomeFromRow(
    bankAccountId: number,
    statementId: number,
    rowId: number,
    request: CreateIncomeFromRowRequest
  ): Promise<BankStatementRow> {
    const response = await api.post<ApiResponse<BankStatementRow>>(
      `${baseUrl(bankAccountId)}/${statementId}/rows/${rowId}/create-income`,
      request
    )
    if (response.data.success && response.data.data) {
      updateRowInList(response.data.data)
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to create income')
  }

  async function completeStatement(
    bankAccountId: number,
    statementId: number
  ): Promise<BankStatement> {
    const response = await api.post<ApiResponse<BankStatement>>(
      `${baseUrl(bankAccountId)}/${statementId}/complete`
    )
    if (response.data.success && response.data.data) {
      currentStatement.value = response.data.data
      return response.data.data
    }
    throw new Error(response.data.message || 'Failed to complete statement')
  }

  async function fetchColumnMapping(bankAccountId: number): Promise<void> {
    loading.value = true
    try {
      const response = await api.get<ApiResponse<ColumnMapping | null>>(
        `${baseUrl(bankAccountId)}/column-mapping`
      )
      if (response.data.success) {
        columnMapping.value = response.data.data ?? null
      }
    } finally {
      loading.value = false
    }
  }

  async function saveColumnMapping(
    bankAccountId: number,
    request: SaveColumnMappingRequest
  ): Promise<ColumnMapping> {
    loading.value = true
    try {
      const response = await api.put<ApiResponse<ColumnMapping>>(
        `${baseUrl(bankAccountId)}/column-mapping`,
        request
      )
      if (response.data.success && response.data.data) {
        columnMapping.value = response.data.data
        return response.data.data
      }
      throw new Error(response.data.message || 'Failed to save column mapping')
    } finally {
      loading.value = false
    }
  }

  function updateRowInList(updatedRow: BankStatementRow) {
    const idx = rows.value.findIndex((r) => r.id === updatedRow.id)
    if (idx !== -1) {
      // eslint-disable-next-line security/detect-object-injection -- idx is from findIndex, not user input
      rows.value[idx] = updatedRow
    }
  }

  function clearState() {
    statements.value = []
    currentStatement.value = null
    rows.value = []
    columnMapping.value = null
  }

  return {
    statements,
    currentStatement,
    rows,
    columnMapping,
    loading,
    fetchStatements,
    fetchStatement,
    uploadStatement,
    deleteStatement,
    fetchRows,
    confirmMatch,
    rejectMatch,
    manualMatch,
    acknowledgeRow,
    createExpenseFromRow,
    createIncomeFromRow,
    completeStatement,
    fetchColumnMapping,
    saveColumnMapping,
    clearState
  }
})
