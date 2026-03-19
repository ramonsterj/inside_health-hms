package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.SaveColumnMappingRequest
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountColumnMapping
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.BankStatement
import com.insidehealthgt.hms.entity.BankStatementRow
import com.insidehealthgt.hms.entity.BankStatementStatus
import com.insidehealthgt.hms.entity.MatchStatus
import com.insidehealthgt.hms.entity.StatementFileType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.BankAccountColumnMappingRepository
import com.insidehealthgt.hms.repository.BankStatementRepository
import com.insidehealthgt.hms.repository.BankStatementRowRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BankStatementServiceTest {

    private lateinit var bankAccountService: BankAccountService
    private lateinit var columnMappingRepository: BankAccountColumnMappingRepository
    private lateinit var bankStatementRepository: BankStatementRepository
    private lateinit var bankStatementRowRepository: BankStatementRowRepository
    private lateinit var fileStorageService: FileStorageService
    private lateinit var reconciliationService: ReconciliationService
    private lateinit var service: BankStatementService

    private fun makeBankAccount(id: Long = 1L, isPettyCash: Boolean = false): BankAccount {
        val account = BankAccount(
            name = "Test Bank",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
            openingBalance = BigDecimal("5000.00"),
            isPettyCash = isPettyCash,
        )
        account.id = id
        return account
    }

    private fun makeColumnMapping(bankAccount: BankAccount): BankAccountColumnMapping {
        val mapping = BankAccountColumnMapping(
            bankAccount = bankAccount,
            dateColumn = "Fecha",
            debitColumn = "Debito",
            creditColumn = "Credito",
        )
        mapping.id = 1L
        return mapping
    }

    private fun makeStatement(bankAccount: BankAccount, id: Long = 1L): BankStatement {
        val statement = BankStatement(
            bankAccount = bankAccount,
            fileName = "test.xlsx",
            filePath = "/path/test.xlsx",
            statementDate = LocalDate.of(2026, 1, 15),
        )
        statement.id = id
        statement.totalRows = 5
        statement.matchedCount = 3
        statement.unmatchedCount = 1
        statement.suggestedCount = 1
        return statement
    }

    private fun makeRow(statement: BankStatement, rowNum: Int = 1, id: Long = 1L): BankStatementRow {
        val row = BankStatementRow(
            bankStatement = statement,
            rowNumber = rowNum,
            transactionDate = LocalDate.of(2026, 1, 10),
            debitAmount = BigDecimal("100.00"),
        )
        row.id = id
        return row
    }

    @BeforeEach
    fun setUp() {
        bankAccountService = mock()
        columnMappingRepository = mock()
        bankStatementRepository = mock()
        bankStatementRowRepository = mock()
        fileStorageService = mock()
        reconciliationService = mock()

        service = BankStatementService(
            bankAccountService,
            columnMappingRepository,
            bankStatementRepository,
            bankStatementRowRepository,
            fileStorageService,
            reconciliationService,
        )
    }

    @Test
    fun `saveColumnMapping creates new mapping when none exists`() {
        val bankAccount = makeBankAccount()
        val request = SaveColumnMappingRequest(
            fileType = StatementFileType.XLSX,
            hasHeader = true,
            dateColumn = "Fecha",
            debitColumn = "Debito",
            creditColumn = "Credito",
        )

        whenever(bankAccountService.findEntityById(1L)).thenReturn(bankAccount)
        whenever(columnMappingRepository.findByBankAccountId(1L)).thenReturn(null)
        whenever(columnMappingRepository.save(any())).thenAnswer { invocation ->
            val mapping = invocation.getArgument<BankAccountColumnMapping>(0)
            mapping.id = 1L
            mapping
        }

        val result = service.saveColumnMapping(1L, request)

        assertNotNull(result)
        assertEquals("Fecha", result.dateColumn)
        verify(columnMappingRepository).save(any())
    }

    @Test
    fun `saveColumnMapping updates existing mapping`() {
        val bankAccount = makeBankAccount()
        val existing = makeColumnMapping(bankAccount)
        val request = SaveColumnMappingRequest(
            fileType = StatementFileType.CSV,
            hasHeader = false,
            dateColumn = "0",
            debitColumn = "1",
            creditColumn = "2",
        )

        whenever(bankAccountService.findEntityById(1L)).thenReturn(bankAccount)
        whenever(columnMappingRepository.findByBankAccountId(1L)).thenReturn(existing)
        whenever(columnMappingRepository.save(any())).thenReturn(existing)

        val result = service.saveColumnMapping(1L, request)

        assertEquals(StatementFileType.CSV, result.fileType)
        verify(columnMappingRepository).save(existing)
    }

    @Test
    fun `getColumnMapping returns mapping when exists`() {
        val bankAccount = makeBankAccount()
        val mapping = makeColumnMapping(bankAccount)

        whenever(bankAccountService.findEntityById(1L)).thenReturn(bankAccount)
        whenever(columnMappingRepository.findByBankAccountId(1L)).thenReturn(mapping)

        val result = service.getColumnMapping(1L)

        assertNotNull(result)
        assertEquals("Fecha", result.dateColumn)
    }

    @Test
    fun `getColumnMapping returns null when not found`() {
        val bankAccount = makeBankAccount()

        whenever(bankAccountService.findEntityById(1L)).thenReturn(bankAccount)
        whenever(columnMappingRepository.findByBankAccountId(1L)).thenReturn(null)

        val result = service.getColumnMapping(1L)

        assertNull(result)
    }

    @Test
    fun `uploadStatement rejects petty cash accounts`() {
        val pettyCash = makeBankAccount(isPettyCash = true)
        whenever(bankAccountService.findEntityById(1L)).thenReturn(pettyCash)

        val file: org.springframework.web.multipart.MultipartFile = mock()
        assertThrows<BadRequestException> {
            service.uploadStatement(1L, file, LocalDate.now())
        }
    }

    @Test
    fun `uploadStatement rejects when no column mapping configured`() {
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(1L)).thenReturn(bankAccount)
        whenever(columnMappingRepository.findByBankAccountId(1L)).thenReturn(null)

        val file: org.springframework.web.multipart.MultipartFile = mock()
        whenever(file.originalFilename).thenReturn("test.xlsx")
        assertThrows<BadRequestException> {
            service.uploadStatement(1L, file, LocalDate.now())
        }
    }

    @Test
    fun `uploadStatement rejects wrong file extension`() {
        val bankAccount = makeBankAccount()
        val mapping = makeColumnMapping(bankAccount)
        mapping.fileType = StatementFileType.XLSX

        whenever(bankAccountService.findEntityById(1L)).thenReturn(bankAccount)
        whenever(columnMappingRepository.findByBankAccountId(1L)).thenReturn(mapping)

        val file: org.springframework.web.multipart.MultipartFile = mock()
        whenever(file.originalFilename).thenReturn("test.csv")
        assertThrows<BadRequestException> {
            service.uploadStatement(1L, file, LocalDate.now())
        }
    }

    @Test
    fun `getStatements returns list of statements`() {
        val bankAccount = makeBankAccount()
        val statement = makeStatement(bankAccount)

        whenever(bankAccountService.findEntityById(1L)).thenReturn(bankAccount)
        whenever(bankStatementRepository.findAllByBankAccountIdOrderByStatementDateDesc(1L))
            .thenReturn(listOf(statement))

        val result = service.getStatements(1L)

        assertEquals(1, result.size)
        assertEquals("test.xlsx", result[0].fileName)
    }

    @Test
    fun `getStatement returns single statement`() {
        val bankAccount = makeBankAccount()
        val statement = makeStatement(bankAccount)

        whenever(bankStatementRepository.findByIdAndBankAccountId(1L, 1L)).thenReturn(statement)

        val result = service.getStatement(1L, 1L)

        assertEquals("test.xlsx", result.fileName)
    }

    @Test
    fun `getStatement throws when not found`() {
        whenever(bankStatementRepository.findByIdAndBankAccountId(1L, 1L)).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            service.getStatement(1L, 1L)
        }
    }

    @Test
    fun `deleteStatement soft deletes statement and rows`() {
        val bankAccount = makeBankAccount()
        val statement = makeStatement(bankAccount)
        val rows = listOf(makeRow(statement))

        whenever(bankStatementRepository.findByIdAndBankAccountId(1L, 1L)).thenReturn(statement)
        whenever(bankStatementRowRepository.findAllByBankStatementIdOrderByRowNumberAsc(1L))
            .thenReturn(rows)
        whenever(bankStatementRowRepository.saveAll(any<List<BankStatementRow>>())).thenReturn(rows)
        whenever(bankStatementRepository.save(any())).thenReturn(statement)

        service.deleteStatement(1L, 1L)

        assertNotNull(statement.deletedAt)
        assertNotNull(rows[0].deletedAt)
        verify(bankStatementRepository).save(statement)
    }

    @Test
    fun `completeStatement succeeds when no unmatched or suggested rows`() {
        val bankAccount = makeBankAccount()
        val statement = makeStatement(bankAccount)
        statement.unmatchedCount = 0
        statement.suggestedCount = 0

        whenever(bankStatementRepository.findByIdAndBankAccountId(1L, 1L)).thenReturn(statement)
        whenever(bankStatementRowRepository.countByBankStatementIdAndMatchStatus(1L, MatchStatus.UNMATCHED))
            .thenReturn(0)
        whenever(bankStatementRowRepository.countByBankStatementIdAndMatchStatus(1L, MatchStatus.SUGGESTED))
            .thenReturn(0)
        whenever(bankStatementRepository.save(any())).thenReturn(statement)

        val result = service.completeStatement(1L, 1L)

        assertEquals(BankStatementStatus.COMPLETED, statement.status)
    }

    @Test
    fun `completeStatement fails when unmatched rows exist`() {
        val bankAccount = makeBankAccount()
        val statement = makeStatement(bankAccount)

        whenever(bankStatementRepository.findByIdAndBankAccountId(1L, 1L)).thenReturn(statement)
        whenever(bankStatementRowRepository.countByBankStatementIdAndMatchStatus(1L, MatchStatus.UNMATCHED))
            .thenReturn(2)
        whenever(bankStatementRowRepository.countByBankStatementIdAndMatchStatus(1L, MatchStatus.SUGGESTED))
            .thenReturn(0)

        assertThrows<BadRequestException> {
            service.completeStatement(1L, 1L)
        }
    }

    @Test
    fun `completeStatement fails when suggested rows exist`() {
        val bankAccount = makeBankAccount()
        val statement = makeStatement(bankAccount)

        whenever(bankStatementRepository.findByIdAndBankAccountId(1L, 1L)).thenReturn(statement)
        whenever(bankStatementRowRepository.countByBankStatementIdAndMatchStatus(1L, MatchStatus.UNMATCHED))
            .thenReturn(0)
        whenever(bankStatementRowRepository.countByBankStatementIdAndMatchStatus(1L, MatchStatus.SUGGESTED))
            .thenReturn(1)

        assertThrows<BadRequestException> {
            service.completeStatement(1L, 1L)
        }
    }
}
