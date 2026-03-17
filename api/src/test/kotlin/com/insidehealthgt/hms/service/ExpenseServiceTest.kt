package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateExpenseRequest
import com.insidehealthgt.hms.dto.request.RecordExpensePaymentRequest
import com.insidehealthgt.hms.dto.request.UpdateExpenseRequest
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.Expense
import com.insidehealthgt.hms.entity.ExpenseCategory
import com.insidehealthgt.hms.entity.ExpensePayment
import com.insidehealthgt.hms.entity.ExpenseStatus
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.ExpensePaymentRepository
import com.insidehealthgt.hms.repository.ExpenseRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExpenseServiceTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var expensePaymentRepository: ExpensePaymentRepository
    private lateinit var bankAccountService: BankAccountService
    private lateinit var fileStorageService: FileStorageService
    private lateinit var userRepository: UserRepository
    private lateinit var expenseService: ExpenseService

    private fun makeExpense(
        amount: BigDecimal = BigDecimal("500.00"),
        paidAmount: BigDecimal = BigDecimal.ZERO,
        status: ExpenseStatus = ExpenseStatus.PENDING,
        dueDate: LocalDate? = LocalDate.now().plusDays(30),
        id: Long = 1L,
    ): Expense {
        val expense = Expense(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = amount,
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
            status = status,
            dueDate = dueDate,
            paidAmount = paidAmount,
        )
        expense.id = id
        return expense
    }

    private fun makeBankAccount(id: Long = 10L): BankAccount {
        val account = BankAccount(
            name = "Test Bank",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
            openingBalance = BigDecimal("5000.00"),
        )
        account.id = id
        return account
    }

    @BeforeEach
    fun setUp() {
        expenseRepository = mock()
        expensePaymentRepository = mock()
        bankAccountService = mock()
        fileStorageService = mock()
        userRepository = mock()

        expenseService = ExpenseService(
            expenseRepository,
            expensePaymentRepository,
            bankAccountService,
            fileStorageService,
            userRepository,
        )

        whenever(userRepository.findAllById(any())).thenReturn(emptyList())
    }

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    fun `create throws BadRequestException when not paid and dueDate is null`() {
        val request = CreateExpenseRequest(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("100.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
            dueDate = null,
            isPaid = false,
        )

        assertThrows<BadRequestException> { expenseService.create(request) }
        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `create throws BadRequestException when paid but paymentDate is null`() {
        val request = CreateExpenseRequest(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("100.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
            isPaid = true,
            paymentDate = null,
            bankAccountId = 10L,
        )

        assertThrows<BadRequestException> { expenseService.create(request) }
    }

    @Test
    fun `create throws BadRequestException when paid but bankAccountId is null`() {
        val request = CreateExpenseRequest(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("100.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
            isPaid = true,
            paymentDate = LocalDate.now(),
            bankAccountId = null,
        )

        assertThrows<BadRequestException> { expenseService.create(request) }
    }

    @Test
    fun `create unpaid expense sets PENDING status and no payment record`() {
        val request = CreateExpenseRequest(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("200.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
            dueDate = LocalDate.now().plusDays(15),
            isPaid = false,
        )
        val saved = makeExpense(status = ExpenseStatus.PENDING)
        whenever(expenseRepository.save(any<Expense>())).thenReturn(saved)

        expenseService.create(request)

        verify(expensePaymentRepository, never()).save(any())
    }

    @Test
    fun `create paid expense sets PAID status and creates payment record`() {
        val request = CreateExpenseRequest(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("200.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
            isPaid = true,
            paymentDate = LocalDate.now(),
            bankAccountId = 10L,
        )
        val bankAccount = makeBankAccount()
        val saved = makeExpense(
            amount = BigDecimal("200.00"),
            paidAmount = BigDecimal("200.00"),
            status = ExpenseStatus.PAID,
        )
        whenever(expenseRepository.save(any<Expense>())).thenReturn(saved)
        whenever(bankAccountService.findEntityById(10L)).thenReturn(bankAccount)
        whenever(expensePaymentRepository.save(any<ExpensePayment>())).thenAnswer { it.arguments[0] }

        val result = expenseService.create(request)

        verify(expensePaymentRepository).save(any())
        assertEquals(ExpenseStatus.PAID, result.status)
    }

    // ─── recordPayment ─────────────────────────────────────────────────────────

    @Test
    fun `recordPayment throws BadRequestException when expense already fully paid`() {
        val paidExpense = makeExpense(
            amount = BigDecimal("500.00"),
            paidAmount = BigDecimal("500.00"),
            status = ExpenseStatus.PAID,
        )
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(paidExpense))

        val request = RecordExpensePaymentRequest(
            amount = BigDecimal("100.00"),
            paymentDate = LocalDate.now(),
            bankAccountId = 10L,
        )

        assertThrows<BadRequestException> { expenseService.recordPayment(1L, request) }
    }

    @Test
    fun `recordPayment throws BadRequestException when expense is cancelled`() {
        val cancelled = makeExpense(status = ExpenseStatus.CANCELLED)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(cancelled))

        val request = RecordExpensePaymentRequest(
            amount = BigDecimal("100.00"),
            paymentDate = LocalDate.now(),
            bankAccountId = 10L,
        )

        assertThrows<BadRequestException> { expenseService.recordPayment(1L, request) }
    }

    @Test
    fun `recordPayment throws BadRequestException when payment exceeds remaining balance`() {
        val expense = makeExpense(
            amount = BigDecimal("500.00"),
            paidAmount = BigDecimal("400.00"),
            status = ExpenseStatus.PARTIALLY_PAID,
        )
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))

        val request = RecordExpensePaymentRequest(
            amount = BigDecimal("200.00"), // remaining is only 100
            paymentDate = LocalDate.now(),
            bankAccountId = 10L,
        )

        assertThrows<BadRequestException> { expenseService.recordPayment(1L, request) }
    }

    @Test
    fun `recordPayment creates payment and transitions status to PARTIALLY_PAID`() {
        val expense = makeExpense(
            amount = BigDecimal("500.00"),
            paidAmount = BigDecimal.ZERO,
            status = ExpenseStatus.PENDING,
        )
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(10L)).thenReturn(bankAccount)
        whenever(expensePaymentRepository.save(any<ExpensePayment>())).thenAnswer { invocation ->
            val p = invocation.getArgument<ExpensePayment>(0)
            p.id = 100L
            p
        }
        whenever(expenseRepository.save(any<Expense>())).thenAnswer { it.arguments[0] }

        val payment = expenseService.recordPayment(
            1L,
            RecordExpensePaymentRequest(
                amount = BigDecimal("200.00"),
                paymentDate = LocalDate.now(),
                bankAccountId = 10L,
            ),
        )

        assertEquals(BigDecimal("200.00"), expense.paidAmount)
        assertEquals(ExpenseStatus.PARTIALLY_PAID, expense.status)
    }

    @Test
    fun `recordPayment creates payment and transitions status to PAID when full amount paid`() {
        val expense = makeExpense(
            amount = BigDecimal("500.00"),
            paidAmount = BigDecimal.ZERO,
            status = ExpenseStatus.PENDING,
        )
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(10L)).thenReturn(bankAccount)
        whenever(expensePaymentRepository.save(any<ExpensePayment>())).thenAnswer { invocation ->
            val p = invocation.getArgument<ExpensePayment>(0)
            p.id = 101L
            p
        }
        whenever(expenseRepository.save(any<Expense>())).thenAnswer { it.arguments[0] }

        expenseService.recordPayment(
            1L,
            RecordExpensePaymentRequest(
                amount = BigDecimal("500.00"),
                paymentDate = LocalDate.now(),
                bankAccountId = 10L,
            ),
        )

        assertEquals(BigDecimal("500.00"), expense.paidAmount)
        assertEquals(ExpenseStatus.PAID, expense.status)
    }

    // ─── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `delete throws BadRequestException when expense has recorded payments`() {
        val expenseWithPayments = makeExpense(paidAmount = BigDecimal("100.00"), status = ExpenseStatus.PARTIALLY_PAID)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expenseWithPayments))

        assertThrows<BadRequestException> { expenseService.delete(1L) }
        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `delete soft-deletes expense with no payments`() {
        val expense = makeExpense()
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))
        whenever(expenseRepository.save(any<Expense>())).thenAnswer { it.arguments[0] }

        expenseService.delete(1L)

        assertNotNull(expense.deletedAt)
        verify(expenseRepository).save(expense)
    }

    @Test
    fun `delete throws ResourceNotFoundException when expense not found`() {
        whenever(expenseRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { expenseService.delete(99L) }
    }

    // ─── update ────────────────────────────────────────────────────────────────

    @Test
    fun `update throws BadRequestException when expense is cancelled`() {
        val cancelled = makeExpense(status = ExpenseStatus.CANCELLED)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(cancelled))

        val request = UpdateExpenseRequest(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("500.00"),
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
        )

        assertThrows<BadRequestException> { expenseService.update(1L, request) }
    }

    @Test
    fun `update throws BadRequestException when new amount is less than already paid`() {
        val expense = makeExpense(
            amount = BigDecimal("500.00"),
            paidAmount = BigDecimal("300.00"),
            status = ExpenseStatus.PARTIALLY_PAID,
        )
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))

        val request = UpdateExpenseRequest(
            supplierName = "Supplier",
            category = ExpenseCategory.SUPPLIES,
            amount = BigDecimal("200.00"), // less than paid amount of 300
            expenseDate = LocalDate.now(),
            invoiceNumber = "INV-001",
        )

        assertThrows<BadRequestException> { expenseService.update(1L, request) }
    }

    // ─── OVERDUE logic in response ─────────────────────────────────────────────

    @Test
    fun `getById marks expense as overdue when due date is in the past and not paid`() {
        val overdueExpense = makeExpense(
            dueDate = LocalDate.now().minusDays(1),
            status = ExpenseStatus.PENDING,
        )
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(overdueExpense))

        val result = expenseService.getById(1L)

        assertEquals(true, result.isOverdue)
    }

    @Test
    fun `getById does not mark paid expense as overdue`() {
        val paidExpense = makeExpense(
            amount = BigDecimal("500.00"),
            paidAmount = BigDecimal("500.00"),
            status = ExpenseStatus.PAID,
            dueDate = LocalDate.now().minusDays(5),
        )
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(paidExpense))

        val result = expenseService.getById(1L)

        assertEquals(false, result.isOverdue)
    }

    @Test
    fun `getById does not mark expense as overdue when dueDate is in the future`() {
        val expense = makeExpense(dueDate = LocalDate.now().plusDays(10))
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))

        val result = expenseService.getById(1L)

        assertEquals(false, result.isOverdue)
    }
}
