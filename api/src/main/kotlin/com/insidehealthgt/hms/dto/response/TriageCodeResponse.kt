package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.TriageCode
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class TriageCodeResponse(
    val id: Long,
    val code: String,
    val color: String,
    val description: String?,
    val displayOrder: Int,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedAt: LocalDateTime?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(triageCode: TriageCode, createdByUser: User? = null, updatedByUser: User? = null) = TriageCodeResponse(
            id = triageCode.id!!,
            code = triageCode.code,
            color = triageCode.color,
            description = triageCode.description,
            displayOrder = triageCode.displayOrder,
            createdAt = triageCode.createdAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedAt = triageCode.updatedAt,
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )

        fun fromSimple(triageCode: TriageCode) = TriageCodeResponse(
            id = triageCode.id!!,
            code = triageCode.code,
            color = triageCode.color,
            description = triageCode.description,
            displayOrder = triageCode.displayOrder,
            createdAt = triageCode.createdAt,
            createdBy = null,
            updatedAt = triageCode.updatedAt,
            updatedBy = null,
        )
    }
}
