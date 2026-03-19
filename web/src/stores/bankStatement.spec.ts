import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useBankStatementStore } from './bankStatement'
import api from '@/services/api'
import {
  BankStatementStatus,
  ExpenseCategory,
  IncomeCategory,
  MatchStatus,
  MatchedEntityType,
  StatementFileType
} from '@/types/treasury'
import type {
  BankStatement,
  BankStatementRow,
  ColumnMapping
} from '@/types/treasury'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

const mockedApi = api as unknown as {
  get: Mock
  post: Mock
  put: Mock
  delete: Mock
}

const apiSuccess = <T>(data: T) => ({
  data: { success: true, data, message: null }
})

const mockStatement: BankStatement = {
  id: 1,
  bankAccountId: 10,
  bankAccountName: 'Banco Industrial',
  fileName: 'statement.xlsx',
  statementDate: '2026-01-15',
  status: BankStatementStatus.IN_PROGRESS,
  totalRows: 5,
  matchedCount: 2,
  unmatchedCount: 2,
  acknowledgedCount: 0,
  suggestedCount: 1,
  createdAt: '2026-01-15T00:00:00',
  updatedAt: '2026-01-15T00:00:00'
}

const mockRow: BankStatementRow = {
  id: 1,
  bankStatementId: 1,
  rowNumber: 1,
  transactionDate: '2026-01-10',
  description: 'Payment to supplier',
  reference: 'REF-001',
  debitAmount: 100.0,
  creditAmount: null,
  balance: null,
  matchStatus: MatchStatus.UNMATCHED,
  matchedEntityType: null,
  matchedEntityId: null,
  matchedEntityDescription: null,
  acknowledgedReason: null
}

const mockMapping: ColumnMapping = {
  id: 1,
  bankAccountId: 10,
  fileType: StatementFileType.XLSX,
  hasHeader: true,
  dateColumn: 'Fecha',
  descriptionColumn: 'Descripcion',
  referenceColumn: null,
  debitColumn: 'Debito',
  creditColumn: 'Credito',
  balanceColumn: null,
  dateFormat: 'dd/MM/yyyy',
  skipRows: 0
}

describe('useBankStatementStore', () => {
  let store: ReturnType<typeof useBankStatementStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useBankStatementStore()
    vi.clearAllMocks()
  })

  it('fetchStatements populates statements list', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockStatement]))

    await store.fetchStatements(10)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/bank-accounts/10/statements')
    expect(store.statements).toEqual([mockStatement])
    expect(store.loading).toBe(false)
  })

  it('fetchStatement returns single statement', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockStatement))

    const result = await store.fetchStatement(10, 1)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/bank-accounts/10/statements/1')
    expect(store.currentStatement).toEqual(mockStatement)
    expect(result).toEqual(mockStatement)
  })

  it('uploadStatement calls POST with FormData', async () => {
    mockedApi.post.mockResolvedValueOnce(apiSuccess(mockStatement))

    const file = new File(['data'], 'test.xlsx')
    const result = await store.uploadStatement(10, file, '2026-01-15')

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    expect(result).toEqual(mockStatement)
  })

  it('deleteStatement calls DELETE', async () => {
    mockedApi.delete.mockResolvedValueOnce(apiSuccess(undefined))

    await store.deleteStatement(10, 1)

    expect(mockedApi.delete).toHaveBeenCalledWith('/v1/treasury/bank-accounts/10/statements/1')
  })

  it('fetchRows populates rows list', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess([mockRow]))

    await store.fetchRows(10, 1)

    expect(mockedApi.get).toHaveBeenCalledWith('/v1/treasury/bank-accounts/10/statements/1/rows')
    expect(store.rows).toEqual([mockRow])
  })

  it('confirmMatch calls POST and updates row', async () => {
    const confirmedRow = { ...mockRow, matchStatus: MatchStatus.MATCHED }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(confirmedRow))
    store.rows = [mockRow]

    const result = await store.confirmMatch(10, 1, 1)

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/1/rows/1/confirm'
    )
    expect(result.matchStatus).toBe(MatchStatus.MATCHED)
    expect(store.rows[0]?.matchStatus).toBe(MatchStatus.MATCHED)
  })

  it('rejectMatch calls POST and updates row', async () => {
    const rejectedRow = { ...mockRow, matchStatus: MatchStatus.UNMATCHED }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(rejectedRow))
    store.rows = [{ ...mockRow, matchStatus: MatchStatus.SUGGESTED }]

    const result = await store.rejectMatch(10, 1, 1)

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/1/rows/1/reject'
    )
    expect(result.matchStatus).toBe(MatchStatus.UNMATCHED)
  })

  it('acknowledgeRow calls POST and updates row', async () => {
    const acknowledgedRow = {
      ...mockRow,
      matchStatus: MatchStatus.ACKNOWLEDGED,
      acknowledgedReason: 'Duplicate'
    }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(acknowledgedRow))
    store.rows = [mockRow]

    const result = await store.acknowledgeRow(10, 1, 1, { reason: 'Duplicate' })

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/1/rows/1/acknowledge',
      { reason: 'Duplicate' }
    )
    expect(result.matchStatus).toBe(MatchStatus.ACKNOWLEDGED)
  })

  it('createExpenseFromRow calls POST and updates row', async () => {
    const matchedRow = {
      ...mockRow,
      matchStatus: MatchStatus.MATCHED,
      matchedEntityType: MatchedEntityType.EXPENSE_PAYMENT,
      matchedEntityId: 50
    }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(matchedRow))
    store.rows = [mockRow]

    const result = await store.createExpenseFromRow(10, 1, 1, {
      supplierName: 'Supplier',
      category: ExpenseCategory.SUPPLIES,
      amount: 100,
      expenseDate: '2026-01-10',
      invoiceNumber: 'INV-001'
    })

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/1/rows/1/create-expense',
      expect.any(Object)
    )
    expect(result.matchStatus).toBe(MatchStatus.MATCHED)
  })

  it('createIncomeFromRow calls POST and updates row', async () => {
    const matchedRow = {
      ...mockRow,
      matchStatus: MatchStatus.MATCHED,
      matchedEntityType: MatchedEntityType.INCOME,
      matchedEntityId: 30
    }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(matchedRow))
    store.rows = [mockRow]

    const result = await store.createIncomeFromRow(10, 1, 1, {
      description: 'Income',
      category: IncomeCategory.PATIENT_PAYMENT,
      amount: 200,
      incomeDate: '2026-01-11'
    })

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/1/rows/1/create-income',
      expect.any(Object)
    )
    expect(result.matchStatus).toBe(MatchStatus.MATCHED)
  })

  it('completeStatement calls POST and updates currentStatement', async () => {
    const completed = { ...mockStatement, status: BankStatementStatus.COMPLETED }
    mockedApi.post.mockResolvedValueOnce(apiSuccess(completed))

    const result = await store.completeStatement(10, 1)

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/1/complete'
    )
    expect(result.status).toBe(BankStatementStatus.COMPLETED)
    expect(store.currentStatement?.status).toBe(BankStatementStatus.COMPLETED)
  })

  it('fetchColumnMapping populates columnMapping', async () => {
    mockedApi.get.mockResolvedValueOnce(apiSuccess(mockMapping))

    await store.fetchColumnMapping(10)

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/column-mapping'
    )
    expect(store.columnMapping).toEqual(mockMapping)
  })

  it('saveColumnMapping calls PUT', async () => {
    mockedApi.put.mockResolvedValueOnce(apiSuccess(mockMapping))

    await store.saveColumnMapping(10, {
      fileType: StatementFileType.XLSX,
      hasHeader: true,
      dateColumn: 'Fecha',
      debitColumn: 'Debito',
      creditColumn: 'Credito',
      dateFormat: 'dd/MM/yyyy',
      skipRows: 0
    })

    expect(mockedApi.put).toHaveBeenCalledWith(
      '/v1/treasury/bank-accounts/10/statements/column-mapping',
      expect.any(Object)
    )
    expect(store.columnMapping).toEqual(mockMapping)
  })

  it('clearState resets all state', () => {
    store.statements = [mockStatement]
    store.currentStatement = mockStatement
    store.rows = [mockRow]
    store.columnMapping = mockMapping

    store.clearState()

    expect(store.statements).toEqual([])
    expect(store.currentStatement).toBeNull()
    expect(store.rows).toEqual([])
    expect(store.columnMapping).toBeNull()
  })
})
