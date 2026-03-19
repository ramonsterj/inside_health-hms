package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.BankStatementRow
import com.insidehealthgt.hms.entity.MatchStatus
import com.insidehealthgt.hms.entity.MatchedEntityType
import java.math.BigDecimal
import java.time.LocalDate

data class BankStatementRowResponse(
    val id: Long,
    val bankStatementId: Long,
    val rowNumber: Int,
    val transactionDate: LocalDate,
    val description: String?,
    val reference: String?,
    val debitAmount: BigDecimal?,
    val creditAmount: BigDecimal?,
    val balance: BigDecimal?,
    val matchStatus: MatchStatus,
    val matchedEntityType: MatchedEntityType?,
    val matchedEntityId: Long?,
    val matchedEntityDescription: String?,
    val acknowledgedReason: String?,
) {
    companion object {
        fun from(row: BankStatementRow, matchedEntityDescription: String? = null): BankStatementRowResponse =
            BankStatementRowResponse(
                id = row.id!!,
                bankStatementId = row.bankStatement.id!!,
                rowNumber = row.rowNumber,
                transactionDate = row.transactionDate,
                description = row.description,
                reference = row.reference,
                debitAmount = row.debitAmount,
                creditAmount = row.creditAmount,
                balance = row.balance,
                matchStatus = row.matchStatus,
                matchedEntityType = row.matchedEntityType,
                matchedEntityId = row.matchedEntityId,
                matchedEntityDescription = matchedEntityDescription,
                acknowledgedReason = row.acknowledgedReason,
            )
    }
}
