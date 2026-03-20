package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.BankStatement
import com.insidehealthgt.hms.entity.BankStatementStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class BankStatementResponse(
    val id: Long,
    val bankAccountId: Long,
    val bankAccountName: String,
    val fileName: String,
    val statementDate: LocalDate,
    val status: BankStatementStatus,
    val totalRows: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val acknowledgedCount: Int,
    val suggestedCount: Int,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val endingBalance: BigDecimal?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(statement: BankStatement): BankStatementResponse = BankStatementResponse(
            id = statement.id!!,
            bankAccountId = statement.bankAccount.id!!,
            bankAccountName = statement.bankAccount.name,
            fileName = statement.fileName,
            statementDate = statement.statementDate,
            status = statement.status,
            totalRows = statement.totalRows,
            matchedCount = statement.matchedCount,
            unmatchedCount = statement.unmatchedCount,
            acknowledgedCount = statement.acknowledgedCount,
            suggestedCount = statement.suggestedCount,
            periodStart = statement.periodStart,
            periodEnd = statement.periodEnd,
            endingBalance = statement.endingBalance,
            createdAt = statement.createdAt,
            updatedAt = statement.updatedAt,
        )
    }
}
