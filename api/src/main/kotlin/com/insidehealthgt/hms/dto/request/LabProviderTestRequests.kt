package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateLabProviderTestRequest(
    @field:NotNull(message = "Canonical test ID is required")
    @field:Positive(message = "Canonical test ID must be positive")
    val labTestId: Long,

    @field:Size(max = 200, message = "Display name must not exceed 200 characters")
    val displayName: String? = null,

    @field:NotNull(message = "Cost is required")
    @field:PositiveOrZero(message = "Cost must not be negative")
    val cost: BigDecimal,

    @field:NotNull(message = "Sales price is required")
    @field:Positive(message = "Sales price must be greater than zero")
    val salesPrice: BigDecimal,

    val active: Boolean = true,
)

/** PUT contract excludes [CreateLabProviderTestRequest.labTestId] — the canonical test cannot change. */
data class UpdateLabProviderTestRequest(
    @field:Size(max = 200, message = "Display name must not exceed 200 characters")
    val displayName: String? = null,

    @field:NotNull(message = "Cost is required")
    @field:PositiveOrZero(message = "Cost must not be negative")
    val cost: BigDecimal,

    @field:NotNull(message = "Sales price is required")
    @field:Positive(message = "Sales price must be greater than zero")
    val salesPrice: BigDecimal,

    val active: Boolean = true,
)
