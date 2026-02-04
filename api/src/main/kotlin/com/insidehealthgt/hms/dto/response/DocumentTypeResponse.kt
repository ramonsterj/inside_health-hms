@file:Suppress("FunctionSignature", "ClassSignature") // Multiline parameter style

package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.DocumentType
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class DocumentTypeResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val displayOrder: Int,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedAt: LocalDateTime?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(
            documentType: DocumentType,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ) = DocumentTypeResponse(
            id = documentType.id!!,
            code = documentType.code,
            name = documentType.name,
            description = documentType.description,
            displayOrder = documentType.displayOrder,
            createdAt = documentType.createdAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedAt = documentType.updatedAt,
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )

        fun fromSimple(documentType: DocumentType) = DocumentTypeResponse(
            id = documentType.id!!,
            code = documentType.code,
            name = documentType.name,
            description = documentType.description,
            displayOrder = documentType.displayOrder,
            createdAt = documentType.createdAt,
            createdBy = null,
            updatedAt = documentType.updatedAt,
            updatedBy = null,
        )
    }
}

data class DocumentTypeSummaryResponse(
    val id: Long,
    val code: String,
    val name: String,
) {
    companion object {
        fun from(documentType: DocumentType) = DocumentTypeSummaryResponse(
            id = documentType.id!!,
            code = documentType.code,
            name = documentType.name,
        )
    }
}
