package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "{validation.refreshToken.required}")
    val refreshToken: String,
)
