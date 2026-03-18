package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateBankAccountRequest
import com.insidehealthgt.hms.dto.request.UpdateBankAccountRequest
import com.insidehealthgt.hms.dto.response.BankAccountResponse
import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.BankAccountRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BankAccountService(
    private val bankAccountRepository: BankAccountRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<BankAccountResponse> {
        val accounts = bankAccountRepository.findAllByOrderByNameAsc()
        return buildResponseList(accounts)
    }

    @Transactional(readOnly = true)
    fun findAllActive(): List<BankAccountResponse> {
        val accounts = bankAccountRepository.findAllByActiveTrueOrderByNameAsc()
        return buildResponseList(accounts)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): BankAccountResponse {
        val account = findEntityById(id)
        return buildResponse(account)
    }

    @Transactional
    fun create(request: CreateBankAccountRequest): BankAccountResponse {
        if (request.accountType == BankAccountType.PETTY_CASH) {
            throw BadRequestException("Petty cash account cannot be created manually")
        }
        if (bankAccountRepository.existsByName(request.name)) {
            throw BadRequestException("Bank account with name '${request.name}' already exists")
        }
        val account = BankAccount(
            name = request.name,
            bankName = request.bankName?.takeIf { it.isNotBlank() },
            accountNumber = request.accountNumber?.takeIf { it.isNotBlank() },
            accountType = request.accountType,
            currency = request.currency,
            openingBalance = request.openingBalance,
            notes = request.notes?.takeIf { it.isNotBlank() },
        )
        val saved = bankAccountRepository.save(account)
        return buildResponse(saved)
    }

    @Transactional
    fun update(id: Long, request: UpdateBankAccountRequest): BankAccountResponse {
        val account = findEntityById(id)
        if (bankAccountRepository.existsByNameExcludingId(request.name, id)) {
            throw BadRequestException("Bank account with name '${request.name}' already exists")
        }
        val requestedIsPettyCash = request.accountType == BankAccountType.PETTY_CASH
        if (account.isPettyCash != requestedIsPettyCash) {
            throw BadRequestException("The Petty Cash account type cannot be changed manually")
        }
        account.name = request.name
        account.bankName = request.bankName?.takeIf { it.isNotBlank() }
        account.accountNumber = request.accountNumber?.takeIf { it.isNotBlank() }
        account.accountType = request.accountType
        account.currency = request.currency
        account.active = request.active
        account.notes = request.notes?.takeIf { it.isNotBlank() }
        val saved = bankAccountRepository.save(account)
        return buildResponse(saved)
    }

    @Transactional
    fun delete(id: Long) {
        val account = findEntityById(id)
        if (account.isPettyCash) {
            throw BadRequestException("The Petty Cash account cannot be deleted")
        }
        account.deletedAt = LocalDateTime.now()
        bankAccountRepository.save(account)
    }

    fun findEntityById(id: Long): BankAccount = bankAccountRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Bank account not found with id: $id") }

    fun findPettyCashEntity(): BankAccount? = bankAccountRepository.findByIsPettyCashTrue()

    fun computeBookBalance(account: BankAccount): BigDecimal {
        val expensePayments = bankAccountRepository.sumExpensePaymentsByBankAccountId(account.id!!)
        val income = bankAccountRepository.sumIncomeByBankAccountId(account.id!!)
        return account.openingBalance.add(income).subtract(expensePayments)
    }

    private fun buildResponseList(accounts: List<BankAccount>): List<BankAccountResponse> {
        val userIds = accounts.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.toSet()
        val usersById = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        }
        return accounts.map { buildResponse(it, usersById) }
    }

    private fun buildResponse(account: BankAccount, usersById: Map<Long, User> = emptyMap()): BankAccountResponse {
        val bookBalance = computeBookBalance(account)
        val createdByUser = account.createdBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        val updatedByUser = account.updatedBy?.let { usersById[it] ?: userRepository.findById(it).orElse(null) }
        return BankAccountResponse.from(account, bookBalance, createdByUser, updatedByUser)
    }
}
