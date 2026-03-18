package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.BankAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface BankAccountRepository : JpaRepository<BankAccount, Long> {

    fun findAllByOrderByNameAsc(): List<BankAccount>

    fun findAllByActiveTrueOrderByNameAsc(): List<BankAccount>

    fun findByIsPettyCashTrue(): BankAccount?

    fun existsByName(name: String): Boolean

    @Query(
        "SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM BankAccount b WHERE b.name = :name AND b.id != :excludeId",
    )
    fun existsByNameExcludingId(name: String, excludeId: Long): Boolean

    /**
     * Sum all non-deleted expense payments for a given bank account.
     * Used to compute the book balance.
     */
    @Query(
        value = "SELECT COALESCE(SUM(ep.amount), 0) FROM expense_payments ep " +
            "JOIN expenses e ON e.id = ep.expense_id " +
            "WHERE ep.bank_account_id = :bankAccountId AND ep.deleted_at IS NULL AND e.deleted_at IS NULL",
        nativeQuery = true,
    )
    fun sumExpensePaymentsByBankAccountId(bankAccountId: Long): BigDecimal

    /**
     * Sum all non-deleted income records for a given bank account.
     * Used to compute the book balance.
     */
    @Query(
        value = "SELECT COALESCE(SUM(ir.amount), 0) FROM income_records ir " +
            "WHERE ir.bank_account_id = :bankAccountId AND ir.deleted_at IS NULL",
        nativeQuery = true,
    )
    fun sumIncomeByBankAccountId(bankAccountId: Long): BigDecimal
}
