package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "{validation.identifier.required}")
    val identifier: String,

    @field:NotBlank(message = "{validation.password.required}")
    val password: String,
)
