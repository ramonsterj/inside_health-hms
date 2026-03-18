package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateIncomeRequest
import com.insidehealthgt.hms.dto.request.UpdateIncomeRequest
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.Income
import com.insidehealthgt.hms.entity.IncomeCategory
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.IncomeRepository
import com.insidehealthgt.hms.repository.InvoiceRepository
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

class IncomeServiceTest {

    private lateinit var incomeRepository: IncomeRepository
    private lateinit var bankAccountService: BankAccountService
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var userRepository: UserRepository
    private lateinit var incomeService: IncomeService

    private fun makeBankAccount(id: Long = 5L): BankAccount {
        val account = BankAccount(
            name = "Caja Chica",
            accountType = BankAccountType.PETTY_CASH,
            currency = "GTQ",
            openingBalance = BigDecimal("2000.00"),
            isPettyCash = true,
        )
        account.id = id
        return account
    }

    private fun makeIncome(
        amount: BigDecimal = BigDecimal("1500.00"),
        id: Long = 1L,
        bankAccount: BankAccount = makeBankAccount(),
    ): Income {
        val income = Income(
            description = "Patient payment",
            category = IncomeCategory.PATIENT_PAYMENT,
            amount = amount,
            incomeDate = LocalDate.now(),
            bankAccount = bankAccount,
        )
        income.id = id
        return income
    }

    @BeforeEach
    fun setUp() {
        incomeRepository = mock()
        bankAccountService = mock()
        invoiceRepository = mock()
        userRepository = mock()

        incomeService = IncomeService(incomeRepository, bankAccountService, invoiceRepository, userRepository)

        whenever(userRepository.findAllById(any())).thenReturn(emptyList())
    }

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    fun `create saves income and returns response`() {
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(5L)).thenReturn(bankAccount)
        whenever(invoiceRepository.existsById(10L)).thenReturn(true)
        val saved = makeIncome(bankAccount = bankAccount)
        whenever(incomeRepository.save(any<Income>())).thenReturn(saved)

        val request = CreateIncomeRequest(
            description = "Patient payment",
            category = IncomeCategory.PATIENT_PAYMENT,
            amount = BigDecimal("1500.00"),
            incomeDate = LocalDate.now(),
            bankAccountId = 5L,
            invoiceId = 10L,
        )

        val result = incomeService.create(request)

        assertNotNull(result)
        assertEquals("Patient payment", result.description)
        verify(incomeRepository).save(any())
    }

    @Test
    fun `create throws BadRequestException when invoiceId does not exist`() {
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(5L)).thenReturn(bankAccount)
        whenever(invoiceRepository.existsById(99L)).thenReturn(false)

        val request = CreateIncomeRequest(
            description = "Invoice income",
            category = IncomeCategory.PATIENT_PAYMENT,
            amount = BigDecimal("500.00"),
            incomeDate = LocalDate.now(),
            bankAccountId = 5L,
            invoiceId = 99L,
        )

        assertThrows<BadRequestException> { incomeService.create(request) }
        verify(incomeRepository, never()).save(any())
    }

    @Test
    fun `create with valid invoiceId saves income`() {
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(5L)).thenReturn(bankAccount)
        whenever(invoiceRepository.existsById(10L)).thenReturn(true)
        val saved = makeIncome(bankAccount = bankAccount)
        whenever(incomeRepository.save(any<Income>())).thenReturn(saved)

        val request = CreateIncomeRequest(
            description = "Invoice payment",
            category = IncomeCategory.PATIENT_PAYMENT,
            amount = BigDecimal("800.00"),
            incomeDate = LocalDate.now(),
            bankAccountId = 5L,
            invoiceId = 10L,
        )

        val result = incomeService.create(request)

        assertNotNull(result)
        verify(incomeRepository).save(any())
    }

    // ─── update ────────────────────────────────────────────────────────────────

    @Test
    fun `update throws ResourceNotFoundException when income does not exist`() {
        whenever(incomeRepository.findById(99L)).thenReturn(Optional.empty())

        val request = UpdateIncomeRequest(
            description = "Updated",
            category = IncomeCategory.OTHER_INCOME,
            amount = BigDecimal("200.00"),
            incomeDate = LocalDate.now(),
            bankAccountId = 5L,
            invoiceId = 10L,
        )

        assertThrows<ResourceNotFoundException> { incomeService.update(99L, request) }
    }

    @Test
    fun `update modifies income fields and saves`() {
        val income = makeIncome()
        whenever(incomeRepository.findById(1L)).thenReturn(Optional.of(income))
        val bankAccount = makeBankAccount()
        whenever(bankAccountService.findEntityById(5L)).thenReturn(bankAccount)
        whenever(invoiceRepository.existsById(10L)).thenReturn(true)
        whenever(incomeRepository.save(any<Income>())).thenAnswer { it.arguments[0] }

        val request = UpdateIncomeRequest(
            description = "Insurance reimbursement",
            category = IncomeCategory.INSURANCE_PAYMENT,
            amount = BigDecimal("3000.00"),
            incomeDate = LocalDate.now(),
            bankAccountId = 5L,
            invoiceId = 10L,
        )

        val result = incomeService.update(1L, request)

        assertEquals("Insurance reimbursement", result.description)
        assertEquals(BigDecimal("3000.00"), result.amount)
        assertEquals(IncomeCategory.INSURANCE_PAYMENT, result.category)
        verify(incomeRepository).save(any())
    }

    // ─── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `delete soft-deletes income record`() {
        val income = makeIncome()
        whenever(incomeRepository.findById(1L)).thenReturn(Optional.of(income))
        whenever(incomeRepository.save(any<Income>())).thenAnswer { it.arguments[0] }

        incomeService.delete(1L)

        assertNotNull(income.deletedAt)
        verify(incomeRepository).save(income)
    }

    @Test
    fun `delete throws ResourceNotFoundException when income does not exist`() {
        whenever(incomeRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { incomeService.delete(99L) }
        verify(incomeRepository, never()).save(any())
    }

    // ─── getById ───────────────────────────────────────────────────────────────

    @Test
    fun `getById returns income response`() {
        val bankAccount = makeBankAccount()
        val income = makeIncome(bankAccount = bankAccount)
        whenever(incomeRepository.findById(1L)).thenReturn(Optional.of(income))

        val result = incomeService.getById(1L)

        assertEquals(1L, result.id)
        assertEquals(BigDecimal("1500.00"), result.amount)
        assertEquals(5L, result.bankAccountId)
    }

    @Test
    fun `getById throws ResourceNotFoundException when not found`() {
        whenever(incomeRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { incomeService.getById(99L) }
    }
}
