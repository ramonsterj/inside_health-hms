package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.BankAccount
import com.insidehealthgt.hms.entity.BankAccountType
import com.insidehealthgt.hms.entity.User
import java.math.BigDecimal
import java.time.LocalDateTime

data class BankAccountResponse(
    val id: Long,
    val name: String,
    val bankName: String?,
    val maskedAccountNumber: String?,
    val accountType: BankAccountType,
    val currency: String,
    val openingBalance: BigDecimal,
    val bookBalance: BigDecimal,
    val isPettyCash: Boolean,
    val active: Boolean,
    val notes: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(
            account: BankAccount,
            bookBalance: BigDecimal,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ): BankAccountResponse = BankAccountResponse(
            id = account.id!!,
            name = account.name,
            bankName = account.bankName,
            maskedAccountNumber = account.accountNumber?.let { maskAccountNumber(it) },
            accountType = account.accountType,
            currency = account.currency,
            openingBalance = account.openingBalance,
            bookBalance = bookBalance,
            isPettyCash = account.isPettyCash,
            active = account.active,
            notes = account.notes,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )

        internal fun maskAccountNumber(accountNumber: String): String = if (accountNumber.length > VISIBLE_DIGITS) {
            "*".repeat(accountNumber.length - VISIBLE_DIGITS) + accountNumber.takeLast(VISIBLE_DIGITS)
        } else {
            accountNumber
        }

        private const val VISIBLE_DIGITS = 4
    }
}
