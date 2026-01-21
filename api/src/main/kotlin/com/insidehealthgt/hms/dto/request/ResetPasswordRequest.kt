package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ResetPasswordRequest(
    @field:NotBlank(message = "{validation.token.required}")
    val token: String,

    @field:NotBlank(message = "{validation.newPassword.required}")
    @field:Size(min = 8, message = "{validation.newPassword.min}")
    val newPassword: String,
)
