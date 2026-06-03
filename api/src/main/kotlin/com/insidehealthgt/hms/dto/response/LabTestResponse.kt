@file:Suppress("FunctionSignature") // Multiline parameter style, matches DocumentTypeResponse

package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.LabTest
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class LabTestResponse(
    val id: Long,
    val name: String,
    val active: Boolean,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedAt: LocalDateTime?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(
            test: LabTest,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ) = LabTestResponse(
            id = test.id!!,
            name = test.name,
            active = test.active,
            createdAt = test.createdAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedAt = test.updatedAt,
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )

        fun fromSimple(test: LabTest) = from(test)
    }
}
