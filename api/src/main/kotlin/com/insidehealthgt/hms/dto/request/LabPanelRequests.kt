@file:Suppress("MatchingDeclarationName") // Create + Update typealias share one file

package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class CreateLabPanelRequest(
    @field:NotBlank(message = "Panel name is required")
    @field:Size(max = 200, message = "Panel name must not exceed 200 characters")
    val name: String,

    val active: Boolean = true,

    @field:NotEmpty(message = "A panel must contain at least one canonical test")
    val labTestIds: List<Long>,
)

typealias UpdateLabPanelRequest = CreateLabPanelRequest
