package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateBankAccountRequest
import com.insidehealthgt.hms.dto.request.UpdateBankAccountRequest
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.BankAccountRepository
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
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BankAccountServiceTest {

    private lateinit var bankAccountRepository: BankAccountRepository
    private lateinit var userRepository: UserRepository
    private lateinit var bankAccountService: BankAccountService

    private fun makeAccount(
        name: String = "Test Account",
        accountType: BankAccountType = BankAccountType.CHECKING,
        isPettyCash: Boolean = false,
        id: Long = 1L,
    ): BankAccount {
        val account = BankAccount(
            name = name,
            accountType = accountType,
            currency = "GTQ",
            openingBalance = BigDecimal("1000.00"),
            isPettyCash = isPettyCash,
        )
        account.id = id
        return account
    }

    @BeforeEach
    fun setUp() {
        bankAccountRepository = mock()
        userRepository = mock()
        bankAccountService = BankAccountService(bankAccountRepository, userRepository)

        whenever(userRepository.findAllById(any())).thenReturn(emptyList())
    }

    // ─── findAll ───────────────────────────────────────────────────────────────

    @Test
    fun `findAll returns mapped responses`() {
        val account = makeAccount()
        whenever(bankAccountRepository.findAllByOrderByNameAsc()).thenReturn(listOf(account))
        whenever(bankAccountRepository.sumExpensePaymentsByBankAccountId(1L)).thenReturn(BigDecimal.ZERO)
        whenever(bankAccountRepository.sumIncomeByBankAccountId(1L)).thenReturn(BigDecimal.ZERO)

        val result = bankAccountService.findAll()

        assertEquals(1, result.size)
        assertEquals("Test Account", result[0].name)
    }

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    fun `create throws BadRequestException when account type is PETTY_CASH`() {
        val request = CreateBankAccountRequest(
            name = "My Cash",
            accountType = BankAccountType.PETTY_CASH,
            currency = "GTQ",
            openingBalance = BigDecimal.ZERO,
        )

        assertThrows<BadRequestException> { bankAccountService.create(request) }
        verify(bankAccountRepository, never()).save(any())
    }

    @Test
    fun `create throws BadRequestException when name already exists`() {
        val request = CreateBankAccountRequest(
            name = "Existing Account",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
            openingBalance = BigDecimal.ZERO,
        )
        whenever(bankAccountRepository.existsByName("Existing Account")).thenReturn(true)

        assertThrows<BadRequestException> { bankAccountService.create(request) }
    }

    @Test
    fun `create saves and returns new account`() {
        val request = CreateBankAccountRequest(
            name = "New Account",
            accountType = BankAccountType.SAVINGS,
            currency = "GTQ",
            openingBalance = BigDecimal("500.00"),
        )
        whenever(bankAccountRepository.existsByName("New Account")).thenReturn(false)
        val saved = makeAccount(name = "New Account", accountType = BankAccountType.SAVINGS)
        whenever(bankAccountRepository.save(any<BankAccount>())).thenReturn(saved)
        whenever(bankAccountRepository.sumExpensePaymentsByBankAccountId(1L)).thenReturn(BigDecimal.ZERO)
        whenever(bankAccountRepository.sumIncomeByBankAccountId(1L)).thenReturn(BigDecimal.ZERO)

        val result = bankAccountService.create(request)

        assertNotNull(result)
        assertEquals("New Account", result.name)
        assertFalse(result.isPettyCash)
    }

    @Test
    fun `create trims blank optional fields to null`() {
        val request = CreateBankAccountRequest(
            name = "Account",
            bankName = "  ",
            accountNumber = "",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
            openingBalance = BigDecimal.ZERO,
            notes = "   ",
        )
        whenever(bankAccountRepository.existsByName(any())).thenReturn(false)
        val saved = makeAccount()
        whenever(bankAccountRepository.save(any<BankAccount>())).thenAnswer { invocation ->
            val a = invocation.getArgument<BankAccount>(0)
            assertNull(a.bankName)
            assertNull(a.accountNumber)
            assertNull(a.notes)
            saved
        }
        whenever(bankAccountRepository.sumExpensePaymentsByBankAccountId(any())).thenReturn(BigDecimal.ZERO)
        whenever(bankAccountRepository.sumIncomeByBankAccountId(any())).thenReturn(BigDecimal.ZERO)

        bankAccountService.create(request)
    }

    // ─── update ────────────────────────────────────────────────────────────────

    @Test
    fun `update throws BadRequestException when changing petty cash account type`() {
        val pettyCash = makeAccount(accountType = BankAccountType.PETTY_CASH, isPettyCash = true)
        whenever(bankAccountRepository.findById(1L)).thenReturn(Optional.of(pettyCash))
        whenever(bankAccountRepository.existsByNameExcludingId(any(), any())).thenReturn(false)

        val request = UpdateBankAccountRequest(
            name = "Caja Chica",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
        )

        assertThrows<BadRequestException> { bankAccountService.update(1L, request) }
    }

    @Test
    fun `update throws BadRequestException when setting type to PETTY_CASH on non-petty-cash account`() {
        val account = makeAccount()
        whenever(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account))
        whenever(bankAccountRepository.existsByNameExcludingId(any(), any())).thenReturn(false)

        val request = UpdateBankAccountRequest(
            name = "Account",
            accountType = BankAccountType.PETTY_CASH,
            currency = "GTQ",
        )

        assertThrows<BadRequestException> { bankAccountService.update(1L, request) }
    }

    @Test
    fun `update throws BadRequestException when name already taken by another account`() {
        val account = makeAccount()
        whenever(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account))
        whenever(bankAccountRepository.existsByNameExcludingId("Other Name", 1L)).thenReturn(true)

        val request = UpdateBankAccountRequest(
            name = "Other Name",
            accountType = BankAccountType.CHECKING,
            currency = "GTQ",
        )

        assertThrows<BadRequestException> { bankAccountService.update(1L, request) }
    }

    // ─── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `delete throws BadRequestException for petty cash account`() {
        val pettyCash = makeAccount(isPettyCash = true)
        whenever(bankAccountRepository.findById(1L)).thenReturn(Optional.of(pettyCash))

        assertThrows<BadRequestException> { bankAccountService.delete(1L) }
        verify(bankAccountRepository, never()).save(any())
    }

    @Test
    fun `delete soft-deletes non-petty-cash account`() {
        val account = makeAccount()
        whenever(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account))
        whenever(bankAccountRepository.save(any<BankAccount>())).thenReturn(account)

        bankAccountService.delete(1L)

        assertNotNull(account.deletedAt)
        verify(bankAccountRepository).save(account)
    }

    @Test
    fun `delete throws ResourceNotFoundException when account not found`() {
        whenever(bankAccountRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { bankAccountService.delete(99L) }
    }

    // ─── computeBookBalance ────────────────────────────────────────────────────

    @Test
    fun `computeBookBalance subtracts expense payments from opening balance`() {
        val account = makeAccount()
        whenever(bankAccountRepository.sumExpensePaymentsByBankAccountId(1L)).thenReturn(BigDecimal("300.00"))
        whenever(bankAccountRepository.sumIncomeByBankAccountId(1L)).thenReturn(BigDecimal.ZERO)

        val balance = bankAccountService.computeBookBalance(account)

        assertEquals(BigDecimal("700.00"), balance)
    }

    @Test
    fun `computeBookBalance returns opening balance when no payments`() {
        val account = makeAccount()
        whenever(bankAccountRepository.sumExpensePaymentsByBankAccountId(1L)).thenReturn(BigDecimal.ZERO)
        whenever(bankAccountRepository.sumIncomeByBankAccountId(1L)).thenReturn(BigDecimal.ZERO)

        val balance = bankAccountService.computeBookBalance(account)

        assertEquals(BigDecimal("1000.00"), balance)
    }
}
