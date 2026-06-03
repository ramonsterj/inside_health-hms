@file:Suppress("MatchingDeclarationName") // Create + Update typealias share one file

package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateLabProviderRequest(
    @field:NotBlank(message = "Provider name is required")
    @field:Size(max = 150, message = "Provider name must not exceed 150 characters")
    val name: String,

    @field:Size(max = 50, message = "Provider code must not exceed 50 characters")
    val code: String? = null,

    val active: Boolean = true,
)

typealias UpdateLabProviderRequest = CreateLabProviderRequest
