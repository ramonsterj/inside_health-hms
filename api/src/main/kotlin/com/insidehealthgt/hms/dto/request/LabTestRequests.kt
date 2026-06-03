@file:Suppress("MatchingDeclarationName") // Create + Update typealias share one file

package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateLabTestRequest(
    @field:NotBlank(message = "Test name is required")
    @field:Size(max = 200, message = "Test name must not exceed 200 characters")
    val name: String,

    val active: Boolean = true,
)

typealias UpdateLabTestRequest = CreateLabTestRequest
