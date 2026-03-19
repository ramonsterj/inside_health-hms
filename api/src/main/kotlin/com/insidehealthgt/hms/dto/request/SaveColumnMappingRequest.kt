package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.StatementFileType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class SaveColumnMappingRequest(

    @field:NotNull(message = "File type is required")
    val fileType: StatementFileType,

    val hasHeader: Boolean = true,

    @field:NotBlank(message = "Date column is required")
    @field:Size(max = 50, message = "Date column must not exceed 50 characters")
    val dateColumn: String,

    @field:Size(max = 50, message = "Description column must not exceed 50 characters")
    val descriptionColumn: String? = null,

    @field:Size(max = 50, message = "Reference column must not exceed 50 characters")
    val referenceColumn: String? = null,

    @field:NotBlank(message = "Debit column is required")
    @field:Size(max = 50, message = "Debit column must not exceed 50 characters")
    val debitColumn: String,

    @field:NotBlank(message = "Credit column is required")
    @field:Size(max = 50, message = "Credit column must not exceed 50 characters")
    val creditColumn: String,

    @field:Size(max = 50, message = "Balance column must not exceed 50 characters")
    val balanceColumn: String? = null,

    @field:Size(max = 30, message = "Date format must not exceed 30 characters")
    val dateFormat: String = "dd/MM/yyyy",

    @field:PositiveOrZero(message = "Skip rows must be zero or positive")
    val skipRows: Int = 0,
)
