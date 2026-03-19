package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.AcknowledgeRowRequest
import com.insidehealthgt.hms.dto.request.CreateExpenseFromRowRequest
import com.insidehealthgt.hms.dto.request.CreateIncomeFromRowRequest
import com.insidehealthgt.hms.dto.request.MatchRowRequest
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.BankStatement
import com.insidehealthgt.hms.entity.BankStatementRow
import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpensePayment
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.entity.Income
import com.insidehealthgt.hms.entity.IncomeCategory
import com.insidehealthgt.hms.entity.MatchStatus
import com.insidehealthgt.hms.entity.MatchedEntityType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.BankStatementRepository
import com.insidehealthgt.hms.repository.BankStatementRowRepository
import com.insidehealthgt.hms.repository.ExpensePaymentRepository
import com.insidehealthgt.hms.repository.IncomeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals

class ReconciliationServiceTest {

    private lateinit var bankStatementRepository: BankStatementRepository
    private lateinit var bankStatementRowRepository: BankStatementRowRepository
    private lateinit var expensePaymentRepository: ExpensePaymentRepository
    private lateinit var incomeRepository: IncomeRepository
    private lateinit var expenseService: ExpenseService
    private lateinit var incomeService: IncomeService
    private lateinit var bankAccountService: BankAccountService
    private lateinit var service: ReconciliationService

    private val bankAccount = makeBankAccount()

    private fun makeBankAccount(id: Long = 1L): BankAccount {
        val account = BankAccount(
            name = "Test Bank",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
            openingBalance = BigDecimal("5000.00"),
        )
        account.id = id
        return account
    }

    private fun makeStatement(id: Long = 1L): BankStatement {
        val statement = BankStatement(
            bankAccount = bankAccount,
            fileName = "test.xlsx",
            filePath = "/path/test.xlsx",
            statementDate = LocalDate.of(2026, 1, 15),
        )
        statement.id = id
        statement.totalRows = 5
        return statement
    }

    private fun makeRow(
        statement: BankStatement,
        matchStatus: MatchStatus = MatchStatus.UNMATCHED,
        debitAmount: BigDecimal? = BigDecimal("100.00"),
        creditAmount: BigDecimal? = null,
        id: Long = 1L,
        transactionDate: LocalDate = LocalDate.of(2026, 1, 10),
    ): BankStatementRow {
        val row = BankStatementRow(
            bankStatement = statement,
            rowNumber = 1,
            transactionDate = transactionDate,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
        )
        row.id = id
        row.matchStatus = matchStatus
        return row
    }

    private fun makeExpensePayment(
        id: Long = 10L,
        amount: BigDecimal = BigDecimal("100.00"),
        paymentDate: LocalDate = LocalDate.of(2026, 1, 10),
    ): ExpensePayment {
        val expense = Expense(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = amount,
            expenseDate = paymentDate,
            invoiceNumber = "INV-001",
            status = ExpenseStatus.PAID,
            paidAmount = amount,
        )
        expense.id = 100L
        val payment = ExpensePayment(
            expense = expense,
            bankAccount = bankAccount,
            amount = amount,
            paymentDate = paymentDate,
        )
        payment.id = id
        return payment
    }

    private fun makeIncome(
        id: Long = 20L,
        amount: BigDecimal = BigDecimal("200.00"),
        incomeDate: LocalDate = LocalDate.of(2026, 1, 11),
    ): Income {
        val income = Income(
            description = "Test Income",
            category = IncomeCategory.PATIENT_PAYMENT,
            amount = amount,
            incomeDate = incomeDate,
            bankAccount = bankAccount,
        )
        income.id = id
        return income
    }

    @BeforeEach
    fun setUp() {
        bankStatementRepository = mock()
        bankStatementRowRepository = mock()
        expensePaymentRepository = mock()
        incomeRepository = mock()
        expenseService = mock()
        incomeService = mock()
        bankAccountService = mock()

        service = ReconciliationService(
            bankStatementRepository,
            bankStatementRowRepository,
            expensePaymentRepository,
            incomeRepository,
            expenseService,
            incomeService,
            bankAccountService,
        )
    }

    private fun stubCounters(statementId: Long = 1L) {
        whenever(bankStatementRepository.findById(statementId)).thenReturn(Optional.of(makeStatement(statementId)))
        whenever(bankStatementRowRepository.countGroupedByMatchStatus(statementId))
            .thenReturn(emptyList())
        whenever(bankStatementRepository.save(any())).thenAnswer { it.getArgument(0) }
    }

    @Test
    fun `confirmMatch confirms SUGGESTED row to MATCHED`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.SUGGESTED, id = 5L)
        row.matchedEntityType = MatchedEntityType.EXPENSE_PAYMENT
        row.matchedEntityId = 10L

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(
            bankStatementRowRepository.existsByMatchedEntityTypeAndMatchedEntityIdAndMatchStatusAndIdNot(
                MatchedEntityType.EXPENSE_PAYMENT,
                10L,
                MatchStatus.MATCHED,
                5L,
            ),
        ).thenReturn(false)
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(expensePaymentRepository.findById(10L)).thenReturn(Optional.of(makeExpensePayment()))
        stubCounters()

        val result = service.confirmMatch(1L, 1L, 5L)

        assertEquals(MatchStatus.MATCHED, row.matchStatus)
    }

    @Test
    fun `confirmMatch rejects non-SUGGESTED rows`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.UNMATCHED, id = 5L)

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))

        assertThrows<BadRequestException> {
            service.confirmMatch(1L, 1L, 5L)
        }
    }

    @Test
    fun `confirmMatch rejects if entity already matched to another row`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.SUGGESTED, id = 5L)
        row.matchedEntityType = MatchedEntityType.EXPENSE_PAYMENT
        row.matchedEntityId = 10L

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(
            bankStatementRowRepository.existsByMatchedEntityTypeAndMatchedEntityIdAndMatchStatusAndIdNot(
                MatchedEntityType.EXPENSE_PAYMENT,
                10L,
                MatchStatus.MATCHED,
                5L,
            ),
        ).thenReturn(true)

        assertThrows<BadRequestException> {
            service.confirmMatch(1L, 1L, 5L)
        }
    }

    @Test
    fun `rejectMatch rejects SUGGESTED match back to UNMATCHED`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.SUGGESTED, id = 5L)
        row.matchedEntityType = MatchedEntityType.EXPENSE_PAYMENT
        row.matchedEntityId = 10L

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        stubCounters()

        val result = service.rejectMatch(1L, 1L, 5L)

        assertEquals(MatchStatus.UNMATCHED, row.matchStatus)
        assertEquals(null, row.matchedEntityType)
        assertEquals(null, row.matchedEntityId)
    }

    @Test
    fun `rejectMatch rejects non-SUGGESTED rows`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.MATCHED, id = 5L)

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))

        assertThrows<BadRequestException> {
            service.rejectMatch(1L, 1L, 5L)
        }
    }

    @Test
    fun `manualMatch matches UNMATCHED row`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.UNMATCHED, id = 5L)
        val payment = makeExpensePayment()
        val request = MatchRowRequest(
            matchedEntityType = MatchedEntityType.EXPENSE_PAYMENT,
            matchedEntityId = 10L,
        )

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(expensePaymentRepository.findById(10L)).thenReturn(Optional.of(payment))
        whenever(
            bankStatementRowRepository.existsByMatchedEntityTypeAndMatchedEntityIdAndMatchStatusAndIdNot(
                MatchedEntityType.EXPENSE_PAYMENT,
                10L,
                MatchStatus.MATCHED,
                5L,
            ),
        ).thenReturn(false)
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        stubCounters()

        val result = service.manualMatch(1L, 1L, 5L, request)

        assertEquals(MatchStatus.MATCHED, row.matchStatus)
        assertEquals(MatchedEntityType.EXPENSE_PAYMENT, row.matchedEntityType)
    }

    @Test
    fun `manualMatch rejects non-UNMATCHED rows`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.SUGGESTED, id = 5L)
        val request = MatchRowRequest(
            matchedEntityType = MatchedEntityType.EXPENSE_PAYMENT,
            matchedEntityId = 10L,
        )

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))

        assertThrows<BadRequestException> {
            service.manualMatch(1L, 1L, 5L, request)
        }
    }

    @Test
    fun `manualMatch validates entity exists`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.UNMATCHED, id = 5L)
        val request = MatchRowRequest(
            matchedEntityType = MatchedEntityType.EXPENSE_PAYMENT,
            matchedEntityId = 999L,
        )

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(expensePaymentRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.manualMatch(1L, 1L, 5L, request)
        }
    }

    @Test
    fun `acknowledgeRow acknowledges UNMATCHED row`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.UNMATCHED, id = 5L)
        val request = AcknowledgeRowRequest(reason = "Duplicate import")

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        stubCounters()

        val result = service.acknowledgeRow(1L, 1L, 5L, request)

        assertEquals(MatchStatus.ACKNOWLEDGED, row.matchStatus)
        assertEquals("Duplicate import", row.acknowledgedReason)
    }

    @Test
    fun `acknowledgeRow rejects non-UNMATCHED rows`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.MATCHED, id = 5L)
        val request = AcknowledgeRowRequest(reason = "Test")

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))

        assertThrows<BadRequestException> {
            service.acknowledgeRow(1L, 1L, 5L, request)
        }
    }

    @Test
    fun `autoMatch matches debit row to single expense payment`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.UNMATCHED, debitAmount = BigDecimal("100.00"))
        val payment = makeExpensePayment(amount = BigDecimal("100.00"))

        whenever(bankStatementRepository.findById(1L)).thenReturn(Optional.of(statement))
        whenever(bankStatementRowRepository.findAllByBankStatementIdOrderByRowNumberAsc(1L))
            .thenReturn(listOf(row))
        whenever(expensePaymentRepository.findAllByBankAccountIdAndPaymentDateBetween(any(), any(), any()))
            .thenReturn(listOf(payment))
        whenever(incomeRepository.findAllByBankAccountIdAndIncomeDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(bankStatementRowRepository.countGroupedByMatchStatus(1L)).thenReturn(emptyList())
        whenever(bankStatementRepository.save(any())).thenAnswer { it.getArgument(0) }

        service.autoMatch(1L)

        assertEquals(MatchStatus.SUGGESTED, row.matchStatus)
        assertEquals(MatchedEntityType.EXPENSE_PAYMENT, row.matchedEntityType)
        assertEquals(payment.id, row.matchedEntityId)
    }

    @Test
    fun `autoMatch does not match when multiple candidates exist`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.UNMATCHED, debitAmount = BigDecimal("100.00"))
        val payment1 = makeExpensePayment(id = 10L, amount = BigDecimal("100.00"))
        val payment2 = makeExpensePayment(id = 11L, amount = BigDecimal("100.00"))

        whenever(bankStatementRepository.findById(1L)).thenReturn(Optional.of(statement))
        whenever(bankStatementRowRepository.findAllByBankStatementIdOrderByRowNumberAsc(1L))
            .thenReturn(listOf(row))
        whenever(expensePaymentRepository.findAllByBankAccountIdAndPaymentDateBetween(any(), any(), any()))
            .thenReturn(listOf(payment1, payment2))
        whenever(incomeRepository.findAllByBankAccountIdAndIncomeDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(bankStatementRowRepository.countGroupedByMatchStatus(1L)).thenReturn(emptyList())
        whenever(bankStatementRepository.save(any())).thenAnswer { it.getArgument(0) }

        service.autoMatch(1L)

        assertEquals(MatchStatus.UNMATCHED, row.matchStatus)
    }

    @Test
    fun `autoMatch matches credit row to single income record`() {
        val statement = makeStatement()
        val row = makeRow(
            statement,
            MatchStatus.UNMATCHED,
            debitAmount = null,
            creditAmount = BigDecimal("200.00"),
        )
        val income = makeIncome(amount = BigDecimal("200.00"), incomeDate = LocalDate.of(2026, 1, 10))

        whenever(bankStatementRepository.findById(1L)).thenReturn(Optional.of(statement))
        whenever(bankStatementRowRepository.findAllByBankStatementIdOrderByRowNumberAsc(1L))
            .thenReturn(listOf(row))
        whenever(expensePaymentRepository.findAllByBankAccountIdAndPaymentDateBetween(any(), any(), any()))
            .thenReturn(emptyList())
        whenever(incomeRepository.findAllByBankAccountIdAndIncomeDateBetween(any(), any(), any()))
            .thenReturn(listOf(income))
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(bankStatementRowRepository.countGroupedByMatchStatus(1L)).thenReturn(emptyList())
        whenever(bankStatementRepository.save(any())).thenAnswer { it.getArgument(0) }

        service.autoMatch(1L)

        assertEquals(MatchStatus.SUGGESTED, row.matchStatus)
        assertEquals(MatchedEntityType.INCOME, row.matchedEntityType)
        assertEquals(income.id, row.matchedEntityId)
    }

    @Test
    fun `createExpenseAndMatch creates expense and matches row`() {
        val statement = makeStatement()
        val row = makeRow(statement, MatchStatus.UNMATCHED, id = 5L)
        val request = CreateExpenseFromRowRequest(
            supplierName = "Test Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("100.00"),
            expenseDate = LocalDate.of(2026, 1, 10),
            invoiceNumber = "INV-NEW",
        )

        val expense = Expense(
            supplierName = "Test Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("100.00"),
            expenseDate = LocalDate.of(2026, 1, 10),
            invoiceNumber = "INV-NEW",
            status = ExpenseStatus.PAID,
            paidAmount = BigDecimal("100.00"),
        )
        expense.id = 50L

        val payment = makeExpensePayment(id = 60L)

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(expenseService.createPaidExpense(any())).thenReturn(expense)
        whenever(expensePaymentRepository.findAllByExpenseIdOrderByPaymentDateAsc(50L)).thenReturn(listOf(payment))
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        whenever(expensePaymentRepository.findById(60L)).thenReturn(Optional.of(payment))
        stubCounters()

        val result = service.createExpenseAndMatch(1L, 1L, 5L, request)

        assertEquals(MatchStatus.MATCHED, row.matchStatus)
        assertEquals(MatchedEntityType.EXPENSE_PAYMENT, row.matchedEntityType)
        assertEquals(60L, row.matchedEntityId)
    }

    @Test
    fun `createIncomeAndMatch creates income and matches row`() {
        val statement = makeStatement()
        val row = makeRow(
            statement,
            MatchStatus.UNMATCHED,
            id = 5L,
            debitAmount = null,
            creditAmount = BigDecimal("200.00"),
        )
        val request = CreateIncomeFromRowRequest(
            description = "Test Income",
            category = IncomeCategory.PATIENT_PAYMENT,
            amount = BigDecimal("200.00"),
            incomeDate = LocalDate.of(2026, 1, 11),
        )

        val incomeResponse = com.insidehealthgt.hms.dto.response.IncomeResponse(
            id = 30L,
            description = "Test Income",
            category = IncomeCategory.PATIENT_PAYMENT,
            amount = BigDecimal("200.00"),
            incomeDate = LocalDate.of(2026, 1, 11),
            bankAccountId = 1L,
            bankAccountName = "Test Bank",
            reference = null,
            invoiceId = null,
            invoiceNumber = null,
            notes = null,
            createdAt = null,
            updatedAt = null,
            createdBy = null,
            updatedBy = null,
        )

        whenever(bankStatementRowRepository.findById(5L)).thenReturn(Optional.of(row))
        whenever(incomeService.create(any())).thenReturn(incomeResponse)
        whenever(bankStatementRowRepository.save(any())).thenAnswer { it.getArgument(0) }
        val income = makeIncome(id = 30L)
        whenever(incomeRepository.findById(30L)).thenReturn(Optional.of(income))
        stubCounters()

        val result = service.createIncomeAndMatch(1L, 1L, 5L, request)

        assertEquals(MatchStatus.MATCHED, row.matchStatus)
        assertEquals(MatchedEntityType.INCOME, row.matchedEntityType)
        assertEquals(30L, row.matchedEntityId)
    }
}
