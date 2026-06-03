@file:Suppress("FunctionSignature") // Multiline parameter style, matches DocumentTypeResponse

package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.LabProvider
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class LabProviderResponse(
    val id: Long,
    val name: String,
    val code: String?,
    val active: Boolean,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
    val updatedAt: LocalDateTime?,
    val updatedBy: UserSummaryResponse?,
) {
    companion object {
        fun from(
            provider: LabProvider,
            createdByUser: User? = null,
            updatedByUser: User? = null,
        ) = LabProviderResponse(
            id = provider.id!!,
            name = provider.name,
            code = provider.code,
            active = provider.active,
            createdAt = provider.createdAt,
            createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            updatedAt = provider.updatedAt,
            updatedBy = updatedByUser?.let { UserSummaryResponse.from(it) },
        )

        fun fromSimple(provider: LabProvider) = from(provider)
    }
}

/** Slim provider reference embedded in a lab order response. */
data class LabProviderSummary(val id: Long, val name: String) {
    companion object {
        fun from(provider: LabProvider) = LabProviderSummary(id = provider.id!!, name = provider.name)
    }
}
