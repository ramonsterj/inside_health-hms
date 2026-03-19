package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.BankAccountColumnMapping
import com.insidehealthgt.hms.entity.StatementFileType

data class ColumnMappingResponse(
    val id: Long,
    val bankAccountId: Long,
    val fileType: StatementFileType,
    val hasHeader: Boolean,
    val dateColumn: String,
    val descriptionColumn: String?,
    val referenceColumn: String?,
    val debitColumn: String,
    val creditColumn: String,
    val balanceColumn: String?,
    val dateFormat: String,
    val skipRows: Int,
) {
    companion object {
        fun from(mapping: BankAccountColumnMapping): ColumnMappingResponse = ColumnMappingResponse(
            id = mapping.id!!,
            bankAccountId = mapping.bankAccount.id!!,
            fileType = mapping.fileType,
            hasHeader = mapping.hasHeader,
            dateColumn = mapping.dateColumn,
            descriptionColumn = mapping.descriptionColumn,
            referenceColumn = mapping.referenceColumn,
            debitColumn = mapping.debitColumn,
            creditColumn = mapping.creditColumn,
            balanceColumn = mapping.balanceColumn,
            dateFormat = mapping.dateFormat,
            skipRows = mapping.skipRows,
        )
    }
}
